# Terra_Food isolated performance environment

This environment is for the mini-host only. It creates a distinct Compose project,
database, Redis instance, upload volume, and loopback-only web port. It must never be
pointed at production or at the existing `terrafood-*` containers.

## Resource model

The four tested services are capped at 1,728 MiB in total: backend 768 MiB, MySQL
768 MiB, Redis 128 MiB, and nginx 64 MiB. The JVM heap is 384 MiB and Hikari is
limited to eight connections. Docker Compose applies per-container caps; this does
not reproduce the host kernel, storage, swap, or contention of a complete 2 GB VM.
Results must therefore be labelled as constrained synthetic evidence, not proof of
production performance.

## Run

From the repository root on the mini-host:

```sh
cp performance/.env.example performance/.env
# Replace both placeholder passwords and choose a unique COMPOSE_PROJECT_NAME.
set -a; . performance/.env; set +a
docker compose --env-file performance/.env -f performance/compose.yml up -d --build
sh performance/seed.sh 10000
mkdir -p performance/results
docker run --rm --network "${COMPOSE_PROJECT_NAME}_default" \
  -v "$PWD/performance:/work" -w /work node:22 \
  node api-benchmark.mjs
docker run --rm --network "${COMPOSE_PROJECT_NAME}_default" \
  -v "$PWD/performance:/work" -w /work \
  mcr.microsoft.com/playwright:v1.63.0-noble node browser-startup.mjs
docker compose --env-file performance/.env -f performance/compose.yml down -v
```

Use `1000`, `10000`, and `50000` for S, M, and L. Run baseline and changed commits
with the same project settings, dataset size, network profile, and host conditions.
The runner is intentionally outside the 1,728 MiB service budget.

Before cleanup, save `docker stats --no-stream`, service logs with secrets removed,
the application commit, image digests, migration list, and JSON result files. After
cleanup, verify that no containers, networks, or volumes remain for the run ID.

## Historical image variants

`dayanfood-backend/scripts/backfill-image-variants.sh` is manual and defaults to a
dry run with batches of 100. It only registers files in the configured local upload
directory. Run it against a backup-restored isolated environment before scheduling
any production operation.
