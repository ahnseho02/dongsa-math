#!/usr/bin/env bash
# 백업에서 되돌린다. 지금 들어 있는 데이터는 사라진다.
#
#   ./deploy/restore.sh backups/dongsa-20260101-0300.sql.gz
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(dirname "$HERE")"
FILE="${1:?되돌릴 백업 파일을 지정하세요}"

source "$ROOT/.env"
echo "!! ${POSTGRES_DB:-dongsa} 의 지금 데이터가 모두 지워지고 $FILE 로 바뀝니다."
read -r -p "되돌리려면 yes 를 적으세요: " CONFIRM
[ "$CONFIRM" = "yes" ] || { echo "그만둡니다."; exit 1; }

COMPOSE="docker compose -f $HERE/docker-compose.prod.yml --env-file $ROOT/.env"
$COMPOSE stop app
gunzip -c "$FILE" | $COMPOSE exec -T db psql -U "${POSTGRES_USER:-dongsa}" -d postgres \
  -c "drop database if exists ${POSTGRES_DB:-dongsa};" -c "create database ${POSTGRES_DB:-dongsa};"
gunzip -c "$FILE" | $COMPOSE exec -T db psql -U "${POSTGRES_USER:-dongsa}" -d "${POSTGRES_DB:-dongsa}"
$COMPOSE start app
echo "되돌렸습니다."
