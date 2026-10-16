"""Persistent local development runner. Requires built JARs and installed web dependencies."""
from pathlib import Path
import json
import hashlib
import zipfile
import os
import secrets
import shutil
import socket
import subprocess
import time
import urllib.request
import urllib.error

ROOT = Path(__file__).resolve().parents[2]
STATE = ROOT / ".local"
STATE.mkdir(exist_ok=True)
CONFIG = STATE / "environment.json"
if not CONFIG.exists():
    CONFIG.write_text(json.dumps({
        "LOCAL_DB_PASSWORD": secrets.token_hex(24),
        "LOCAL_DB_ROOT_PASSWORD": secrets.token_hex(24),
        "LOCAL_REDIS_PASSWORD": secrets.token_hex(24),
        "USER_SERVICE_TOKEN": secrets.token_hex(32),
        "INITIAL_ADMIN_PASSWORD": "Local" + secrets.token_hex(5) + "9",
        "LOCAL_MYSQL_IMAGE": os.environ.get("LOCAL_MYSQL_IMAGE", "mysql:8.4"),
    }, indent=2), encoding="utf-8")
config = json.loads(CONFIG.read_text(encoding="utf-8"))
(STATE / "docker.env").write_text("".join(f"{key}={value}\n" for key, value in config.items() if key.startswith("LOCAL_")), encoding="utf-8")
(STATE / "admin.txt").write_text("Local development administrator\nUsername: local_admin\nPassword: " + config["INITIAL_ADMIN_PASSWORD"] + "\n", encoding="utf-8")

bundled_node = Path.home() / ".cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin/node.exe"
node = os.environ.get("TERRA_NODE") or (str(bundled_node) if bundled_node.exists() else shutil.which("node"))
java = shutil.which("java")
if not node or not java:
    raise RuntimeError("Java 21 and Node 22.14+ are required")
required = [ROOT / "web/node_modules/vite/bin/vite.js"] + [
    ROOT / "dayanfood-backend" / module / "target" / (module + "-0.0.1-SNAPSHOT.jar")
    for module in ("food-core-boot", "food-user-boot", "food-media-boot", "food-gateway")]
missing = [str(path) for path in required if not path.exists()]
if missing:
    raise RuntimeError("Build backend JARs and install web dependencies first: " + ", ".join(missing))
# Spring Boot incremental packaging can leave older nested module JARs behind.
for executable in required[1:]:
    with zipfile.ZipFile(executable) as archive:
        for name in archive.namelist():
            if name.startswith("BOOT-INF/lib/food-") and name.endswith(".jar"):
                candidates = list((ROOT / "dayanfood-backend").glob("*/target/" + Path(name).name))
                if candidates and hashlib.sha256(archive.read(name)).digest() != hashlib.sha256(candidates[0].read_bytes()).digest():
                    raise RuntimeError("Stale nested dependency in " + executable.name + "; run mvn clean package first")
for port in (18080, 18081, 18082, 18083, 5173):
    with socket.socket() as probe:
        if probe.connect_ex(("127.0.0.1", port)) == 0:
            raise RuntimeError(f"Port {port} is already in use; do not start a second local stack")
compose = ["docker", "compose", "--env-file", str(STATE / "docker.env"), "-f", str(ROOT / "ops/local/compose.yml")]
subprocess.run(compose + ["up", "-d", "--wait"], check=True, env={**os.environ, **config})
base_env = {**os.environ,
    "DB_URL": "jdbc:mysql://127.0.0.1:13306/dayan_food_local?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=Asia/Shanghai",
    "DB_USERNAME": "terra_local", "DB_PASSWORD": config["LOCAL_DB_PASSWORD"],
    "REDIS_HOST": "127.0.0.1", "REDIS_PORT": "16379", "REDIS_PASSWORD": config["LOCAL_REDIS_PASSWORD"],
    "SPRING_PROFILES_ACTIVE": "local", "SESSION_COOKIE_SECURE": "false", "SERVER_ADDRESS": "127.0.0.1",
    "USER_SERVICE_URL": "http://127.0.0.1:18083", "USER_SERVICE_TOKEN": config["USER_SERVICE_TOKEN"],
    "CORE_SERVICE_URL": "http://127.0.0.1:18081", "MEDIA_SERVICE_URL": "http://127.0.0.1:18082",
    "CORS_ALLOWED_ORIGINS": "http://localhost:5173,http://127.0.0.1:5173",
    "UPLOAD_DIRECTORY": str(ROOT / "dayanfood-backend/uploads"),
}
# Each child receives its own port; inherited shell settings must not override the gateway.
base_env.pop("SERVER_PORT", None)
processes = []
state = {"runner_pid": os.getpid(), "status": "starting", "processes": []}

def save():
    (STATE / "runtime.json").write_text(json.dumps(state, indent=2), encoding="utf-8")

def start(name, args, environment, url, cwd=ROOT):
    output = (STATE / (name + ".log")).open("w", encoding="utf-8")
    process = subprocess.Popen(args, cwd=cwd, env=environment, stdout=output, stderr=subprocess.STDOUT,
        creationflags=subprocess.CREATE_NO_WINDOW if os.name == "nt" else 0)
    processes.append((process, output))
    state["processes"].append({"name": name, "pid": process.pid, "url": url})
    save()
    for _ in range(120):
        if process.poll() is not None:
            raise RuntimeError(f"{name} exited; inspect .local/{name}.log")
        try:
            with urllib.request.urlopen(url, timeout=2):
                print(name + " ready: " + url, flush=True)
                return
        except urllib.error.HTTPError as error:
            if error.code in (401, 403, 404):
                print(name + " ready: " + url, flush=True)
                return
        except (OSError, TimeoutError):
            pass
        time.sleep(1)
    raise RuntimeError(name + " startup timed out")

try:
    save()
    for name, port in [("food-core-boot", 18081), ("food-user-boot", 18083), ("food-media-boot", 18082), ("food-gateway", 18080)]:
        env = {**base_env}
        env["GATEWAY_PORT" if name == "food-gateway" else "SERVER_PORT"] = str(port)
        if name == "food-user-boot":
            env.update(REGISTRATION_EMAIL_VERIFICATION_ENABLED="false", INITIAL_ADMIN_ENABLED="true",
                INITIAL_ADMIN_USERNAME="local_admin", INITIAL_ADMIN_PASSWORD=config["INITIAL_ADMIN_PASSWORD"])
        jar = ROOT / "dayanfood-backend" / name / "target" / (name + "-0.0.1-SNAPSHOT.jar")
        path = "/api/auth/csrf" if name == "food-user-boot" else "/api/regions"
        start(name, [java, "-jar", str(jar)], env, f"http://127.0.0.1:{port}{path}")
    start("web", [node, str(ROOT / "web/node_modules/vite/bin/vite.js"), "--host", "127.0.0.1", "--port", "5173", "--strictPort"],
        base_env, "http://127.0.0.1:5173", ROOT / "web")
    state["status"] = "ready"
    save()
    print("LOCAL_STACK_READY http://localhost:5173", flush=True)
    while all(process.poll() is None for process, _ in processes):
        time.sleep(5)
    raise RuntimeError("A local service stopped; inspect .local logs")
except BaseException as error:
    state["status"] = "stopped"
    state["error"] = str(error)
    save()
    raise
finally:
    for process, output in reversed(processes):
        if process.poll() is None:
            process.terminate()
            try:
                process.wait(15)
            except subprocess.TimeoutExpired:
                process.kill()
        output.close()
