#!/usr/bin/env bash
#
# DuckDNS 주소가 항상 이 서버를 가리키게 한다.
# 오라클 인스턴스를 껐다 켜면 IP 가 바뀔 수 있다 (고정 IP 를 예약하면 안 바뀐다).
#
#   .env 에 아래 두 줄을 넣고
#     DUCKDNS_SUBDOMAIN=dongsa        # dongsa.duckdns.org 이면 dongsa
#     DUCKDNS_TOKEN=받은-토큰
#   crontab -e 로
#     */5 * * * * /home/ubuntu/dongsa-math/deploy/duckdns.sh >> /home/ubuntu/duckdns.log 2>&1
set -euo pipefail

ROOT="$(dirname "$(cd "$(dirname "$0")" && pwd)")"
source "$ROOT/.env"

: "${DUCKDNS_SUBDOMAIN:?.env 에 DUCKDNS_SUBDOMAIN 을 넣으세요}"
: "${DUCKDNS_TOKEN:?.env 에 DUCKDNS_TOKEN 을 넣으세요}"

RESULT=$(curl -fsS "https://www.duckdns.org/update?domains=${DUCKDNS_SUBDOMAIN}&token=${DUCKDNS_TOKEN}&ip=")
echo "$(date '+%F %T')  duckdns: $RESULT"
[ "$RESULT" = "OK" ]
