"""Five-process regression against disposable local MySQL/Redis (see community boundary doc).

Requires built JARs, Java 21, DB_URL/DB_USERNAME/DB_PASSWORD and REDIS_HOST/REDIS_PORT.
Creates fixtures in a terra_audit_* database; never resets a schema or existing data.
"""
from concurrent.futures import ThreadPoolExecutor
from datetime import date
from pathlib import Path
import http.cookiejar
import json
import os
import re
import secrets
import socket
import subprocess
import time
import urllib.error
import urllib.request


ROOT = Path(__file__).resolve().parents[2]
BACKEND = ROOT / "dayanfood-backend"
OUTPUT = Path(os.environ.get("COMMUNITY_TEST_OUTPUT", ROOT / ".local/community-validation"))


def main():
    if not re.fullmatch(r"jdbc:mysql://(?:127\.0\.0\.1|localhost):\d+/terra_audit_[A-Za-z0-9_]+(?:\?.*)?",
                        os.environ.get("DB_URL", "")):
        raise RuntimeError("Use an isolated local terra_audit_* schema; business databases are prohibited")
    if os.environ.get("REDIS_HOST") not in ("127.0.0.1", "localhost") or not os.environ.get("REDIS_PORT"):
        raise RuntimeError("Set an isolated local REDIS_HOST and REDIS_PORT")
    OUTPUT.mkdir(parents=True, exist_ok=True)
    with socket.socket() as reservation:
        reservation.bind(("127.0.0.1", 0))
        user_port = reservation.getsockname()[1]
    token = secrets.token_urlsafe(48)
    admin_name, password = "community_" + secrets.token_hex(6), "Test" + secrets.token_hex(5) + "9"
    environment = {**os.environ, "SPRING_PROFILES_ACTIVE": "local", "SERVER_ADDRESS": "127.0.0.1",
                   "SESSION_COOKIE_SECURE": "false", "INITIAL_ADMIN_ENABLED": "false",
                   "USER_SERVICE_URL": f"http://127.0.0.1:{user_port}", "USER_SERVICE_TOKEN": token,
                   "UPLOAD_DIRECTORY": str(OUTPUT / "uploads")}
    environment.pop("SERVER_PORT", None)
    processes, results = [], []

    def start(module, extra):
        log = OUTPUT / (module + f"-{len(processes)}.log")
        output = log.open("w", encoding="utf-8")
        process = subprocess.Popen([os.environ.get("JAVA", "java"), "-jar",
                                    str(BACKEND / module / "target" / f"{module}-0.0.1-SNAPSHOT.jar")],
                                   env={**environment, **extra}, stdout=output, stderr=subprocess.STDOUT,
                                   creationflags=subprocess.CREATE_NO_WINDOW if os.name == "nt" else 0)
        processes.append((process, output))
        for _ in range(120):
            text = log.read_text(encoding="utf-8", errors="replace")
            match = re.search(r"(?:Tomcat started on port|Netty started on port) (\d+)", text)
            if match and "Started " in text:
                return int(match.group(1)), process
            if process.poll() is not None:
                raise RuntimeError(f"{module} exited; inspect {log}")
            time.sleep(1)
        raise RuntimeError(f"{module} startup timed out; inspect {log}")

    def stop(process):
        if process.poll() is None:
            process.terminate()
            try:
                process.wait(15)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait(5)

    def request(base, path, method="GET", data=None, headers=None, expected=200, opener=None):
        headers = headers or {}
        if isinstance(data, dict):
            data = json.dumps(data).encode()
            headers = {**headers, "Content-Type": "application/json"}
        req = urllib.request.Request(base + path, data=data, method=method, headers=headers)
        try:
            with (opener or urllib.request.build_opener()).open(req, timeout=20) as response:
                status, body = response.status, response.read()
        except urllib.error.HTTPError as error:
            status, body = error.code, error.read()
        assert status == expected, (path, status, body[:500])
        results.append({"path": path, "method": method, "status": status})
        return json.loads(body) if body else None

    try:
        core, core_process = start("food-core-boot", {"SERVER_PORT": "0"})
        users, user_process = start("food-user-boot", {"SERVER_PORT": str(user_port),
            "INITIAL_ADMIN_ENABLED": "true", "INITIAL_ADMIN_USERNAME": admin_name,
            "INITIAL_ADMIN_PASSWORD": password, "REGISTRATION_EMAIL_VERIFICATION_ENABLED": "false"})
        media, _ = start("food-media-boot", {"SERVER_PORT": "0", "CORE_SERVICE_URL": f"http://127.0.0.1:{core}"})
        community, community_process = start("food-community-boot", {"SERVER_PORT": "0"})
        gateway, _ = start("food-gateway", {"GATEWAY_PORT": "0", "CORE_SERVICE_URL": f"http://127.0.0.1:{core}",
            "MEDIA_SERVICE_URL": f"http://127.0.0.1:{media}", "COMMUNITY_SERVICE_URL": f"http://127.0.0.1:{community}"})
        base = f"http://127.0.0.1:{gateway}"

        class Client:
            def __init__(self):
                self.cookies = http.cookiejar.CookieJar()
                self.opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.cookies))
                self.headers = {}

            def call(self, path, method="GET", data=None, expected=200, headers=None):
                return request(base, path, method, data, self.headers if headers is None else headers, expected, self.opener)

            def csrf(self):
                csrf = self.call("/api/auth/csrf")
                self.headers = {csrf["headerName"]: csrf["token"]}

            def login(self, name, role="USER"):
                self.csrf()
                user = self.call("/api/auth/login", "POST", {"username": name, "password": password, "role": role})
                self.csrf()
                self.headers["X-Expected-User-Id"] = str(user["id"])
                return user

        admin, alice, bob = Client(), Client(), Client()
        admin_user = admin.login(admin_name, "ADMIN")
        accounts = []
        for client in (alice, bob):
            name = "member_" + secrets.token_hex(6)
            client.csrf()
            registered = client.call("/api/auth/register", "POST", {"username": name, "password": password,
                "displayName": "Community test", "email": name + "@example.com"}, 201)
            client.call("/api/auth/me", expected=401)
            assert registered["role"] == "USER"
            accounts.append(client.login(name))
        regions = request(base, "/api/regions")

        def create_food(client, name):
            return client.call("/api/foods", "POST", {"name": name, "regionId": regions[0]["id"],
                "latitude": 30.67, "longitude": 104.06, "summary": "Regression", "story": "Disposable fixture",
                "ingredients": "Rice", "tagIds": []}, 201)

        food = create_food(admin, "Community " + secrets.token_hex(6))
        fid = food["id"]
        prefix = f"/api/foods/{fid}"
        pending = create_food(alice, "Pending " + secrets.token_hex(6))
        # Only approved foods may be attached, including by their creator.
        for path, payload in [(f"/api/profile/favorites/{pending['id']}", None),
                              (f"/api/foods/{pending['id']}/like", None),
                              (f"/api/foods/{pending['id']}/comments", {"content": "Hidden"}),
                              (f"/api/foods/{pending['id']}/check-ins", {"eatenOn": str(date.today())})]:
            alice.call(path, "POST", payload, 404)
        alice.call("/api/profile/wishlist", "POST", {"foodId": pending["id"]}, 404)
        request(base, f"/api/foods/{pending['id']}/comments", expected=404)

        for path in ("/api/profile/favorites", "/api/profile/wishlist", "/api/profile/check-ins", "/api/profile/stats"):
            request(base, path, expected=401)
        favorite = f"/api/profile/favorites/{fid}"
        assert alice.call(favorite, "POST")["favorited"]
        assert alice.call(favorite, "POST")["favorited"]
        assert alice.call(favorite + "/status")["favorited"]
        assert len(alice.call("/api/profile/favorites")) == 1
        assert alice.call("/api/profile/favorites/page")["total"] == 1
        assert not bob.call(favorite + "/status")["favorited"]
        alice.call("/api/profile/stats")
        assert not alice.call(favorite, "DELETE")["favorited"]
        wish = alice.call("/api/profile/wishlist", "POST", {"foodId": fid}, 201)
        assert wish["matches"][0]["food"]["id"] == fid
        assert alice.call(f"/api/profile/wishlist/foods/{fid}/status")["listed"]
        alice.call("/api/profile/wishlist", "POST", {"foodId": fid}, 409)
        bob.call(f"/api/profile/wishlist/{wish['id']}", "DELETE", expected=404)
        alice.call(f"/api/profile/wishlist/{wish['id']}", "DELETE", expected=204)

        # A catalog write in another JVM must invalidate the community's local match cache.
        text = "Match" + secrets.token_hex(8)
        wish = alice.call("/api/profile/wishlist", "POST", {"content": text}, 201)
        def matches():
            page = alice.call("/api/profile/wishlist/page")
            return next(item for item in page["items"] if item["id"] == wish["id"])["matches"]
        previous_matches = matches()
        matched_food = create_food(admin, text)
        assert all(item["food"]["id"] != matched_food["id"] for item in previous_matches)
        assert any(item["food"]["id"] == matched_food["id"] for item in matches())
        alice.call("/api/profile/wishlist")

        assert request(base, prefix + "/like/status")["likeCount"] == 0
        assert alice.call(prefix + "/like", "POST")["likeCount"] == 1
        assert alice.call(prefix + "/like", "POST")["likeCount"] == 1
        assert alice.call(prefix + "/like/unlike", "POST")["likeCount"] == 0
        alice.call(prefix + "/comments", "POST", {"content": "Ordinary comment"}, 201)
        assert len(request(base, prefix + "/comments")) == 1
        payload = {"eatenOn": str(date.today()), "note": "Public check-in", "visibility": "PUBLIC", "timezone": "Asia/Shanghai"}
        key = "test-" + secrets.token_hex(12)
        headers = {**alice.headers, "Idempotency-Key": key,
                   "Cookie": "; ".join(f"{c.name}={c.value}" for c in alice.cookies)}
        with ThreadPoolExecutor(max_workers=2) as executor:
            futures = [executor.submit(request, base, prefix + "/check-ins", "POST", payload, headers, 201) for _ in range(2)]
            checkin, duplicate = [future.result() for future in futures]
        assert checkin["id"] == duplicate["id"]
        assert request(base, prefix + "/comments/page")["total"] == 2
        alice.call(prefix + "/check-ins", "POST", {**payload, "note": "Changed"}, 409, headers)
        owned = f"/api/profile/check-ins/{checkin['id']}"
        bob.call(owned, expected=404)
        bob.call(owned, "PATCH", {**payload, "version": checkin["version"]}, 404)
        bob.call(owned + "?version=0", "DELETE", expected=404)
        assert alice.call("/api/profile/check-ins?visibility=PUBLIC")["total"] == 1
        # A version conflict must roll back the associated comment update, too.
        alice.call(owned, "PATCH", {**payload, "note": "Must roll back", "version": checkin["version"] + 10}, 409)
        assert all(c["content"] != "Must roll back" for c in request(base, prefix + "/comments"))
        private = alice.call(owned, "PATCH", {**payload, "visibility": "PRIVATE", "version": checkin["version"]})
        assert request(base, prefix + "/comments/page")["total"] == 1
        public = alice.call(owned, "PATCH", {**payload, "note": "Published again", "version": private["version"]})
        assert any(c["content"] == "Published again" for c in request(base, prefix + "/comments"))
        alice.call(owned + f"?version={private['version']}", "DELETE", expected=409)
        alice.call(owned + f"?version={public['version']}", "DELETE", expected=204)
        assert request(base, prefix + "/comments/page")["total"] == 1
        alice.call(prefix + "/check-ins", "POST", payload, 410, headers)
        alice.call(owned, expected=404)
        alice.call(favorite, "POST", expected=403, headers={})
        alice.call(favorite, "POST", expected=409, headers={**alice.headers, "X-Expected-User-Id": str(accounts[1]["id"])})

        # Mixed prefixes stay with their existing owners; no other business is bundled into community.
        for path in ("/api/profile/foods", "/api/profile/footprints", "/api/achievements/me", "/api/etchings/me"):
            alice.call(path)
        profile = request(base, f"/api/users/{admin_user['id']}")
        assert profile["username"] == admin_name
        for port, path in [(core, prefix + "/comments"), (core, "/api/profile/favorites"),
                           (core, f"/api/users/{admin_user['id']}"), (community, "/api/foods/catalog"),
                           (community, "/api/auth/me"), (community, "/api/images"),
                           (users, prefix + "/comments"), (media, prefix + "/comments")]:
            request(f"http://127.0.0.1:{port}", path, opener=alice.opener, expected=404)
        request(base, f"/api/internal/users/{accounts[0]['id']}/identity", headers={"X-User-Service-Token": token}, expected=404)
        admin.call(f"/api/admin/users/{accounts[0]['id']}/active", "PATCH", {"active": False})
        alice.call("/api/profile/favorites", expected=401)
        alice.call("/api/auth/me", expected=401)
        admin.call(f"/api/admin/users/{accounts[0]['id']}/active", "PATCH", {"active": True})
        alice.login(accounts[0]["username"])

        stop(user_process)
        alice.call("/api/profile/favorites", expected=503)
        request(base, prefix + "/comments")
        request(base, "/api/foods/catalog?pageSize=1")
        _, user_process = start("food-user-boot", {"SERVER_PORT": str(user_port)})
        alice.call("/api/profile/favorites")
        alice.call("/api/auth/me")
        # Community can restart independently without invalidating the Redis session.
        stop(community_process)
        request(base, "/api/foods/catalog?pageSize=1")
        alice.call("/api/auth/me")
        _, community_process = start("food-community-boot", {"SERVER_PORT": str(community)})
        alice.call("/api/profile/favorites")
        stop(core_process)
        request(base, prefix + "/comments")
        alice.call("/api/profile/stats")
        alice.call("/api/auth/logout", "POST")
        alice.call("/api/profile/favorites", expected=401)
        print(f"COMMUNITY_SPLIT_OK: {len(results)} HTTP checks passed", flush=True)
    finally:
        for process, output in reversed(processes):
            stop(process)
            output.close()
        (OUTPUT / "results.json").write_text(json.dumps(results, indent=2), encoding="utf-8")


if __name__ == "__main__":
    main()
