#!/usr/bin/env bash
#
# 오라클 클라우드(또는 아무 우분투 서버)에서 한 번만 돌리면 되는 설치 스크립트.
#
#   git clone https://github.com/ahnseho02/dongsa-math.git
#   cd dongsa-math && ./deploy/setup.sh
#
# 여러 번 돌려도 안전하다. 이미 되어 있는 것은 건너뛴다.
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(dirname "$HERE")"
ENV_FILE="$ROOT/.env"
COMPOSE_FILE="$HERE/docker-compose.prod.yml"

say()  { printf "\n\033[1m%s\033[0m\n" "$*"; }
ok()   { printf "  \033[32m✓\033[0m %s\n" "$*"; }
warn() { printf "  \033[33m!\033[0m %s\n" "$*"; }
die()  { printf "\n  \033[31m✗ %s\033[0m\n\n" "$*" >&2; exit 1; }

[ "$(uname -s)" = "Linux" ] || die "서버(리눅스)에서 돌리는 스크립트입니다. 맥에서는 docker-compose.prod.yml 을 직접 쓰세요."

# ── 1. 도커 ────────────────────────────────────────────────
say "1/5  도커"
if command -v docker >/dev/null 2>&1; then
  ok "이미 설치됨 ($(docker --version | cut -d, -f1))"
else
  curl -fsSL https://get.docker.com | sudo sh >/dev/null
  sudo usermod -aG docker "$USER"
  ok "설치했습니다"
  warn "도커 권한이 적용되려면 다시 로그인해야 합니다. 'exit' 후 다시 접속해서 이 스크립트를 한 번 더 돌리세요."
  exit 0
fi
docker ps >/dev/null 2>&1 || die "도커를 쓸 권한이 없습니다. 'exit' 후 다시 접속해서 다시 돌려 주세요."

# ── 2. 방화벽 ──────────────────────────────────────────────
# 오라클은 콘솔(Security List)과 서버 안(iptables) 두 군데를 다 열어야 한다.
# 여기서는 서버 안쪽만 연다. 콘솔 쪽은 웹에서 직접 해야 한다.
say "2/5  서버 안쪽 방화벽 (80, 443)"
open_port() {
  local port=$1
  if sudo iptables -C INPUT -p tcp --dport "$port" -j ACCEPT 2>/dev/null; then
    ok "$port 이미 열려 있음"
    return
  fi
  local line
  line=$(sudo iptables -L INPUT --line-numbers -n | awk '$2=="REJECT"||$2=="DROP"{print $1; exit}')
  if [ -n "$line" ]; then
    sudo iptables -I INPUT "$line" -p tcp --dport "$port" -j ACCEPT
  else
    sudo iptables -A INPUT -p tcp --dport "$port" -j ACCEPT
  fi
  ok "$port 열었습니다"
}
open_port 80
open_port 443

if command -v ufw >/dev/null 2>&1 && sudo ufw status 2>/dev/null | head -1 | grep -q active; then
  sudo ufw allow 80/tcp >/dev/null && sudo ufw allow 443/tcp >/dev/null
  ok "ufw 에도 열었습니다"
fi

if ! command -v netfilter-persistent >/dev/null 2>&1; then
  sudo DEBIAN_FRONTEND=noninteractive apt-get install -y -qq iptables-persistent >/dev/null 2>&1 || true
fi
if command -v netfilter-persistent >/dev/null 2>&1; then
  sudo netfilter-persistent save >/dev/null 2>&1 && ok "재부팅해도 유지되게 저장했습니다"
else
  warn "iptables-persistent 를 못 깔았습니다. 재부팅하면 방화벽 설정이 풀릴 수 있습니다."
fi

# ── 3. 설정값 ──────────────────────────────────────────────
say "3/5  설정값"
if [ -f "$ENV_FILE" ]; then
  ok ".env 가 이미 있습니다 (건드리지 않습니다)"
else
  echo "  접속할 주소를 적어 주세요."
  echo "  도메인이 없으면 duckdns.org 에서 무료로 받으세요 (예: dongsa.duckdns.org)"
  read -r -p "  주소: " DOMAIN
  [ -n "$DOMAIN" ] || die "주소를 적어야 인증서를 받을 수 있습니다."

  cat > "$ENV_FILE" <<EOF
DOMAIN=$DOMAIN
POSTGRES_DB=dongsa
POSTGRES_USER=dongsa
POSTGRES_PASSWORD=$(openssl rand -hex 24)
JWT_SECRET=$(openssl rand -base64 48)
EOF
  chmod 600 "$ENV_FILE"
  ok "비밀번호와 토큰 키를 만들어 .env 에 넣었습니다"
fi
DOMAIN=$(grep '^DOMAIN=' "$ENV_FILE" | cut -d= -f2-)

# 주소가 이 서버를 가리키는지 미리 본다. 아니면 인증서 발급이 실패한다.
MY_IP=$(curl -fsS --max-time 5 https://api.ipify.org 2>/dev/null || echo "")
DNS_IP=$(getent hosts "$DOMAIN" 2>/dev/null | awk '{print $1; exit}' || echo "")
if [ -n "$MY_IP" ] && [ -n "$DNS_IP" ] && [ "$MY_IP" != "$DNS_IP" ]; then
  warn "$DOMAIN 은 $DNS_IP 를 가리키는데 이 서버는 $MY_IP 입니다."
  warn "이대로면 HTTPS 인증서 발급이 실패합니다. DNS 를 고치고 다시 돌리세요."
  read -r -p "  그래도 계속할까요? (yes): " GO
  [ "$GO" = "yes" ] || exit 1
elif [ -n "$DNS_IP" ]; then
  ok "$DOMAIN → $DNS_IP (이 서버가 맞습니다)"
fi

# ── 4. 띄우기 ──────────────────────────────────────────────
say "4/5  빌드하고 띄우기 (처음에는 5분쯤 걸립니다)"
docker compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d --build

printf "  기다리는 중"
READY=""
for _ in $(seq 1 90); do
  if [ "$(docker inspect -f '{{.State.Health.Status}}' dongsa-app-1 2>/dev/null || echo x)" = "healthy" ]; then
    READY=1; break
  fi
  printf "."; sleep 2
done
printf "\n"
if [ -n "$READY" ]; then
  ok "앱이 떴습니다"
else
  warn "앱이 아직 안 떴습니다. 로그를 확인하세요:"
  echo "    docker compose -f deploy/docker-compose.prod.yml --env-file .env logs app --tail 50"
fi

# ── 5. 백업 ────────────────────────────────────────────────
say "5/5  매일 백업"
CRON_LINE="0 3 * * * $HERE/backup.sh >> $ROOT/backup.log 2>&1"
if crontab -l 2>/dev/null | grep -qF "$HERE/backup.sh"; then
  ok "이미 걸려 있습니다"
else
  ( crontab -l 2>/dev/null; echo "$CRON_LINE" ) | crontab -
  ok "매일 새벽 3시에 백업하도록 걸었습니다 (14일 보관)"
fi

say "끝났습니다"
echo "  주소      https://$DOMAIN"
echo "  인증서를 받아 오는 데 1분쯤 걸립니다. 안 열리면 잠시 뒤 다시 해 보세요."
echo
echo "  상태 보기  docker compose -f deploy/docker-compose.prod.yml --env-file .env ps"
echo "  로그 보기  docker compose -f deploy/docker-compose.prod.yml --env-file .env logs -f app"
echo "  새 버전    git pull && ./deploy/setup.sh"
echo "  되돌리기   ./deploy/restore.sh backups/파일이름.sql.gz"
echo
