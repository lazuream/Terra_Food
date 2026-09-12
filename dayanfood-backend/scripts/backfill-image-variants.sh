#!/usr/bin/env sh
set -eu
JAR_PATH=${1:?usage: backfill-image-variants.sh <jar> [extra spring arguments]}
shift
exec java -jar "$JAR_PATH" \
  --server.port=0 \
  --app.upload.backfill-enabled=true \
  --app.upload.backfill-dry-run="${IMAGE_BACKFILL_DRY_RUN:-true}" \
  --app.upload.backfill-batch-size="${IMAGE_BACKFILL_BATCH_SIZE:-100}" \
  --app.upload.backfill-retry-failed="${IMAGE_BACKFILL_RETRY_FAILED:-false}" \
  --app.upload.backfill-after="${IMAGE_BACKFILL_AFTER:-}" \
  "$@"
