#!/usr/bin/env bash
# 데이터베이스를 하루 한 번 받아 두고 14일치만 남긴다.
#
#   chmod +x deploy/backup.sh
#   crontab -e
#   0 3 * * * /home/ubuntu/dongsa-math/deploy/backup.sh >> /home/ubuntu/backup.log 2>&1
#
# 받아 둔 파일은 서버 밖으로도 한 벌 옮겨 두는 편이 좋다. 서버가 통째로 날아가면 같이 사라진다.
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(dirname "$HERE")"
OUT="${BACKUP_DIR:-$ROOT/backups}"
KEEP_DAYS="${KEEP_DAYS:-14}"

mkdir -p "$OUT"
source "$ROOT/.env"

FILE="$OUT/dongsa-$(date +%Y%m%d-%H%M).sql.gz"
docker compose -f "$HERE/docker-compose.prod.yml" --env-file "$ROOT/.env" \
  exec -T db pg_dump -U "${POSTGRES_USER:-dongsa}" "${POSTGRES_DB:-dongsa}" | gzip > "$FILE"

echo "$(date '+%F %T')  백업 완료  $FILE  ($(du -h "$FILE" | cut -f1))"

# 오래된 것 정리
find "$OUT" -name "dongsa-*.sql.gz" -mtime "+$KEEP_DAYS" -delete
echo "$(date '+%F %T')  ${KEEP_DAYS}일 지난 백업 정리, 남은 파일 $(ls -1 "$OUT" | wc -l | tr -d ' ')개"
