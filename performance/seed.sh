#!/usr/bin/env sh
set -eu

count="${1:-10000}"
case "$count" in *[!0-9]*|'') echo "count must be a positive integer" >&2; exit 2;; esac
if [ "$count" -lt 1 ] || [ "$count" -gt 50000 ]; then
  echo "count must be between 1 and 50000" >&2
  exit 2
fi

: "${PERF_DB_PASSWORD:?set PERF_DB_PASSWORD}"
compose="docker compose -f performance/compose.yml"

$compose exec -T mysql mysql -uroot -p"$PERF_DB_PASSWORD" dayan_food <<SQL
SET @target = $count;
DELETE FROM food WHERE created_by = 'perf_seed';
INSERT INTO food (
  name, region_id, latitude, longitude, address, summary, story, ingredients,
  image_url, heat, created_by, review_status, reviewed_by, created_at, province, city
)
SELECT
  CONCAT('性能样本菜品-', n),
  1 + MOD(n, 4),
  18.2 + MOD(n * 7919, 350000) / 10000,
  73.2 + MOD(n * 3571, 620000) / 10000,
  CONCAT('测试地址 ', n),
  CONCAT('用于固定规模性能验证的摘要 ', n, '，不代表真实线上数据。'),
  RPAD(CONCAT('性能样本故事 ', n, ' '), 600, '内容'),
  ELT(1 + MOD(n, 5), '牛肉、花椒', '豆腐、辣椒', '鱼、醋', '面粉、猪肉', '时蔬、菌菇'),
  NULL,
  MOD(n * 97, 10000),
  'perf_seed',
  'APPROVED',
  'perf_admin',
  TIMESTAMPADD(SECOND, -n, NOW()),
  ELT(1 + MOD(n, 4), '四川', '广东', '江苏', '陕西'),
  ELT(1 + MOD(n, 4), '成都', '潮汕', '苏州', '西安')
FROM (
  SELECT ones.d + tens.d * 10 + hundreds.d * 100 + thousands.d * 1000 + ten_thousands.d * 10000 + 1 AS n
  FROM (SELECT 0 d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) ones
  CROSS JOIN (SELECT 0 d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) tens
  CROSS JOIN (SELECT 0 d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) hundreds
  CROSS JOIN (SELECT 0 d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) thousands
  CROSS JOIN (SELECT 0 d UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4) ten_thousands
) numbers
WHERE n <= @target;
SELECT COUNT(*) AS seeded_foods FROM food WHERE created_by = 'perf_seed';
SQL
