# 동사로 푸는 수학 — 백엔드

초등 문장제 수학 학습 서비스의 서버.

아이들이 문장제를 틀리는 건 계산을 못해서가 아니라 **어떤 식을 세울지 몰라서**다.
그래서 답을 바로 묻지 않고 **단서 → 연산 → 식 → 답** 네 단계로 나눠서 묻는다.
어느 단계에서 막히는지가 그대로 기록되고, 그 기록이 이 서비스가 파는 것이다.

**만들어진 것**

- 원장(고모)이 가입하면 계정과 학원이 같이 만들어지고 **학원 코드**가 발급된다
- 학생은 이메일도 비밀번호도 없이 **학원 코드 + 이름 + PIN 네 자리**로 로그인한다
- 원장은 강사 계정을 만들고, 강사와 원장이 반과 학생을 관리한다
- 모든 데이터는 학원 단위로 갈린다 — 다른 학원 데이터는 id 를 알아도 보이지 않는다
- **문제 템플릿 28종**으로 학년에 맞는 문장제를 만든다. 조사가 자동으로 맞고, 받아올림 여부까지 고를 수 있다
- 학습지마다 **6글자 코드**가 붙는다. 같은 코드를 넣으면 똑같은 문제가 다시 나온다
- 과제를 반이나 아이에게 내주고, **네 단계로 나눠 채점**한다
- 누가 어느 단계에서 막히는지 **리포트**로 나온다

- 실제 PostgreSQL 로 배포할 준비가 되어 있다 (마이그레이션, Docker, 시도 제한)
- **폰에서 바로 쓰는 화면이 붙어 있다** — 서버가 같이 서빙하므로 따로 띄울 것이 없다
- 원장이 **학생 성적을 누적으로** 보고, 필요하면 **계정을 완전히 지운다**
- **60초 연산 게임**과 학원 순위표

**아직 없는 것** — 수업 영상 구간 연결, 반 편성 화면

## 실행

```bash
./gradlew bootRun
```

브라우저에서 `http://localhost:8080` 을 열면 **앱이 바로 나온다.** 8080 이 이미 쓰이고 있으면 포트를 바꾼다.

```bash
./gradlew bootRun --args='--server.port=8090'
```

개발 프로필은 파일 기반 H2 라서 껐다 켜도 데이터가 남는다.
DB 를 눈으로 보려면 `http://localhost:8080/h2-console`
(JDBC URL `jdbc:h2:file:./data/dongsa`, 사용자 `sa`, 비밀번호 없음).
데이터를 비우려면 `data/` 폴더를 지우면 된다.

```bash
./gradlew test        # 테스트 148개
```

### 운영과 같은 구성으로 (PostgreSQL)

```bash
docker compose up -d db      # DB 만 띄우고 앱은 IntelliJ 에서
docker compose up --build    # 앱까지 컨테이너로
```

앱 컨테이너까지 뜨면 `http://localhost:8080/actuator/health` 가 `{"status":"UP"}` 이다.
정리는 `docker compose down -v`.

## 처음 써 보기

```bash
# 1. 고모 가입 — 학원까지 같이 만들어진다
curl -s -X POST localhost:8080/api/auth/signup -H 'Content-Type: application/json' -d '{
  "email":"gomo@example.com","password":"password123",
  "name":"고모","academyName":"동사수학학원"}'

# 응답의 token 과 user.academyCode 를 적어 둔다
```

`api.http` 파일을 IntelliJ 에서 열면 위 과정을 클릭만으로 순서대로 실행할 수 있다.

## API

| 메서드 | 경로 | 누가 | 하는 일 |
|---|---|---|---|
| POST | `/api/auth/signup` | 누구나 | 원장 가입 + 학원 생성 |
| POST | `/api/auth/login` | 누구나 | 원장·강사 로그인 |
| POST | `/api/auth/student/login` | 누구나 | 학생 로그인 (코드+이름+PIN) |
| GET | `/api/auth/me` | 로그인한 사람 | 내 정보 |
| GET | `/api/academy` | 강사 | 학원 정보 + 학원 코드 |
| PATCH | `/api/academy` | **원장** | 학원 이름 변경 |
| GET | `/api/academy/teachers` | 강사 | 강사 목록 |
| POST | `/api/academy/teachers` | **원장** | 강사 계정 만들기 |
| PATCH | `/api/academy/teachers/{id}/active` | **원장** | 강사 사용 중지·재개 |
| GET | `/api/classrooms` | 강사 | 반 목록 |
| POST | `/api/classrooms` | 강사 | 반 만들기 |
| PATCH | `/api/classrooms/{id}` | 강사 | 반 수정 |
| DELETE | `/api/classrooms/{id}` | 강사 | 반 삭제 (학생은 남는다) |
| GET | `/api/students?classroomId=` | 강사 | 학생 목록 |
| POST | `/api/students` | 강사 | 학생 등록 |
| PATCH | `/api/students/{id}` | 강사 | 학생 정보 수정 |
| PATCH | `/api/students/{id}/pin` | 강사 | PIN 다시 발급 |
| PATCH | `/api/students/{id}/active` | 강사 | 학생 사용 중지·재개 |
| GET | `/api/students/report` | 강사 | 학생 전체 성적 (약한 아이가 위) |
| GET | `/api/students/{id}/report` | 강사 | 한 아이의 누적 성적 |
| DELETE | `/api/students/{id}` | **원장** | 학생 완전 삭제 (성적까지) |
| GET | `/api/games/ranking` | 강사 | 연산 게임 순위 |
| POST | `/api/my/games` | **학생** | 게임 시작 (정답 없이 문제만) |
| POST | `/api/my/games/{id}/finish` | **학생** | 답 제출·채점 |
| GET | `/api/my/games/ranking` | **학생** | 순위와 내 등수 |
| GET | `/api/worksheets/options?grade=` | 강사 | 그 학년에 낼 수 있는 유형과 숫자 범위 |
| POST | `/api/worksheets` | 강사 | 학습지 만들기 (정답 포함) |
| POST | `/api/assignments` | 강사 | 과제 내주기 |
| GET | `/api/assignments` | 강사 | 과제 목록 |
| GET | `/api/assignments/{id}` | 강사 | 누가 어디까지 풀었는지 |
| GET | `/api/assignments/{id}/report` | 강사 | 단계별·유형별 취약점 |
| PATCH | `/api/assignments/{id}/closed` | 강사 | 마감·재개 |
| GET | `/api/my/assignments` | **학생** | 내 과제 목록 |
| GET | `/api/my/assignments/{id}` | **학생** | 문제 풀기 (정답 없음) |
| POST | `/api/my/assignments/{id}/answers` | **학생** | 한 단계 제출 |

토큰은 `Authorization: Bearer <token>` 헤더로 보낸다.
유효기간은 선생님 12시간, 학생 30일이다 (아이를 자꾸 다시 로그인시키지 않으려고).

## 써 보기

`./gradlew bootRun` 후 `http://localhost:8080`.

1. **선생님 → 학원 만들기** — 가입하면 여섯 글자 학원 코드가 나온다
2. **학생 추가** — 비밀번호를 비워 두면 서버가 만들어 준다. **그 화면에서만 보이므로** 적어 둘 것
3. **과제 탭 → 과제 내기** — 학년·유형·문항수를 고르고 받을 아이를 고른다.
   유형은 **'종합 — 전체 유형 섞기'** 로 한 번에 고를 수 있다 (실전에 가장 가깝다)
4. **나가기 → 학생 탭** — 학원 코드 + 이름 + 비밀번호로 들어간다
5. 문장에서 **단서 단어를 누르고 → 연산 → 식 → 답** 순서로 푼다
6. **60초 계산 대결** 로 연산 게임을 해 보고 순위를 확인한다
7. 다시 선생님으로 들어가 **과제를 열면 리포트**, **성적 탭**에서 학생 전체를 본다
8. 학생을 누르면 **누적 성적 + 비밀번호 재발급 · 중지 · 삭제**

### 폰에서 보려면

같은 와이파이에 있는 폰에서 `http://<맥의 IP>:8080` 으로 접속한다 (`ipconfig getifaddr en0` 로 IP 확인).
사파리·크롬에서 **공유 → 홈 화면에 추가** 를 하면 아이콘이 생기고 주소창 없이 앱처럼 뜬다.

## 화면

`src/main/resources/static` 의 세 파일이 전부다 — `index.html`, `style.css`, `app.js`.
빌드 도구가 없어서 파일을 고치고 서버만 다시 띄우면 된다.
서버가 같이 서빙하므로 **출처가 같아 CORS 문제도 없다.**

**아이 화면은 정답을 모른다.** 서버가 내려보내지 않기 때문에 화면이 채점할 방법 자체가 없다.
단계마다 서버에 물어보고, 답이 온 뒤에야 정답을 표시한다.

**브라우저 기본 대화상자(alert·confirm)를 쓰지 않는다.**
"비밀번호를 적어 두세요" 같은 중요한 안내를 회색 시스템 팝업에 담을 일이 아니고, 폰에서 보기도 나쁘다.
앱 안 화면으로 만들었다.

React 로 옮기고 싶으면 `app.js` 의 `api()` 함수만 그대로 들고 가면 된다 — 나머지는 전부 그 위에 얹혀 있다.

## 학생 관리와 성적

**중지와 삭제는 다르다.**

- **중지**(`active=false`) — 로그인만 막히고 성적은 남는다. 그만둔 아이는 이쪽이다
- **완전 삭제** — 과제 기록·제출·게임 기록이 모두 사라지고 되돌릴 수 없다. **원장만** 할 수 있다

완전 삭제는 개인정보를 지워 달라는 요청처럼 정말 없애야 할 때를 위한 것이다.
외래키를 일부러 제한(restrict)으로 두어, 기록을 순서대로 지우지 않으면 삭제가 실패하게 했다 —
제약을 느슨하게 풀어 두면 실수로 지울 때 성적이 조용히 날아간다.
화면에서도 **학생 이름을 그대로 적어야** 버튼이 열린다.

**성적은 두 가지 단위로 본다.**

- **과제별** (`/api/assignments/{id}/report`) — 이번 숙제에서 누가 어디서 막혔나
- **학생별 누적** (`/api/students/{id}/report`) — 이 아이가 그동안 뭘 못하나

학원 전체 표는 **약한 아이가 위로** 오고, 아직 한 번도 안 푼 아이는 맨 아래로 간다.

## 연산 게임

문장제가 "왜 이 식인가" 를 묻는다면, 게임은 순수하게 계산 속도를 올리는 쪽이다.
둘 다 필요하다 — 리포트에서 '식은 세우는데 답에서 틀리는' 아이가 실제로 나온다.

60초 동안 40문제 중 몇 개를 맞히는지 겨룬다. 배운 연산만 나온다(곱셈 2학년, 나눗셈 3학년부터).

**점수를 꾸밀 수 없다.** 학습지와 같은 방식이다 — 문제만 내려보내고 답은 주지 않으며,
채점할 때 서버가 씨앗으로 문제를 다시 만든다. 한 판은 한 번만 제출되고,
시작 시각을 서버가 들고 있어 제한 시간이 크게 지나면 기록이 남지 않는다.

**순위는 아이마다 가장 잘한 한 판만** 센다. 많이 한다고 유리하지 않다.
같은 개수면 빨리 끝낸 쪽이 위다.

## 배포

**스키마는 마이그레이션이 만든다.** `src/main/resources/db/migration` 의 SQL 이 유일한 원본이고,
JPA 는 `ddl-auto: validate` 로 맞는지 검사만 한다. 엔티티와 마이그레이션이 어긋나면 **서버가 아예 뜨지 않는다.**
개발·테스트·운영이 모두 같은 SQL 을 쓰므로(H2 는 PostgreSQL 모드),
마이그레이션이 틀리면 배포가 아니라 테스트에서 먼저 깨진다.

스키마를 바꿀 때는 `V2__무엇을_바꿨는지.sql` 을 새로 만든다. V1 은 이미 적용된 뒤이므로 고치지 않는다.

### 어디에 올릴까

**완전 무료 + 항상 켜짐 + 제대로 된 HTTPS 주소** 셋을 다 주는 곳은 사실상 없다. 하나는 포기해야 한다.

| | 비용 | 항상 켜짐 | HTTPS 주소 | 서버 관리 |
|---|---|---|---|---|
| **오라클 클라우드 Always Free** | **0원** | O | DuckDNS 등으로 O | 직접 |
| Railway | 월 $5 | O | 무료 주소 제공 | 없음 |
| Render 무료 | 0원 | **X — 15분 쉬면 잠듦** | 무료 주소 제공 | 없음 |
| 집 컴퓨터 | 0원 | 켜 둘 때만 | 터널 필요 | 직접 |

**Render 무료는 이 서비스에 맞지 않는다.** 15분 쉬면 잠들어 다음 접속이 50초쯤 걸리고,
무료 PostgreSQL 은 30일이면 삭제된다. 학원 데이터를 둘 곳이 아니다.

**돈을 안 쓸 거면 오라클 클라우드 Always Free** 가 답이다.
평생 무료이고 춘천(서울) 리전이 있어 빠르다. ARM 4코어 / 24GB 는 이 앱에 과할 정도다.
카드 등록은 필요하지만 Always Free 안에서는 청구되지 않는다.
대신 서버를 직접 본다 — 아래 `deploy/` 에 필요한 것을 다 넣어 두었으니 명령 몇 줄이면 된다.

**손이 덜 가는 쪽을 원하면 Railway.** 월 $5 로 백업까지 알아서 해 준다.

### 오라클 클라우드에 올리기 (무료)

1. **인스턴스 만들기** — Compute → Instances → Create.
   Shape 을 `VM.Standard.A1.Flex` (ARM) 로 바꾸고 **OCPU 2 / 메모리 12GB** 정도면 넉넉하다.
   이미지는 Ubuntu 22.04. SSH 키를 받아 둔다.
   *ARM 이 "out of capacity" 로 안 만들어지는 일이 흔하다. 몇 시간 뒤 다시 시도하면 대개 된다.*

2. **방화벽 열기** — 두 군데를 다 열어야 한다. 하나만 열고 헤매기 쉽다.
   - 오라클 콘솔: VCN → Security List → Ingress 에 TCP 80, 443 추가
   - 서버 안: `sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 80 -j ACCEPT` (443 도 같이)
     후 `sudo netfilter-persistent save`

3. **주소 만들기** — 도메인이 있으면 A 레코드를 서버 IP 로 맞춘다.
   없으면 [duckdns.org](https://www.duckdns.org) 에서 `무엇이든.duckdns.org` 를 **무료로** 받아 IP 를 넣는다.
   Let's Encrypt 인증서가 이 주소로 발급되므로 **HTTPS 까지 0원**이다.

4. **서버에서**

   ```bash
   # 도커 설치
   curl -fsSL https://get.docker.com | sudo sh
   sudo usermod -aG docker $USER && exec su - $USER

   # 받아서 설정
   git clone https://github.com/ahnseho02/dongsa-math.git
   cd dongsa-math
   cp deploy/env.example .env
   nano .env        # DOMAIN, POSTGRES_PASSWORD, JWT_SECRET 채우기
   openssl rand -base64 48   # JWT_SECRET 은 이 값으로

   # 띄우기
   docker compose -f deploy/docker-compose.prod.yml --env-file .env up -d --build
   ```

   caddy 가 인증서를 받아 오는 데 1분쯤 걸린다. 끝나면 `https://내주소` 로 열린다.

5. **백업 걸기** — 이게 제일 중요하다.

   ```bash
   chmod +x deploy/backup.sh
   crontab -e
   # 매일 새벽 3시
   0 3 * * * /home/ubuntu/dongsa-math/deploy/backup.sh >> /home/ubuntu/backup.log 2>&1
   ```

   14일치를 남기고 오래된 것은 지운다. 되돌릴 때는 `./deploy/restore.sh backups/파일이름.sql.gz`.
   **서버가 통째로 날아가면 백업도 같이 사라지므로** 가끔 노트북으로도 한 벌 내려받아 둔다.

   ```bash
   scp ubuntu@서버IP:~/dongsa-math/backups/*.sql.gz ~/Downloads/
   ```

새 버전을 올릴 때는 서버에서 `git pull && docker compose -f deploy/docker-compose.prod.yml --env-file .env up -d --build`.

`deploy/` 구성에서 **PostgreSQL 포트는 바깥에 열지 않는다.** 앱 컨테이너에서만 닿는다.

### Railway 에 올리기

1. **저장소 연결** — Railway 에서 `New Project → Deploy from GitHub repo` 로 이 저장소를 고른다.
   `railway.json` 이 있어서 Dockerfile 로 빌드하고 `/actuator/health/readiness` 로 상태를 본다.

2. **데이터베이스 추가** — 같은 프로젝트에서 `New → Database → PostgreSQL`.

3. **앱 서비스의 Variables 에 세 가지를 넣는다.**

   ```
   SPRING_PROFILES_ACTIVE = prod
   DATABASE_URL           = ${{Postgres.DATABASE_URL}}
   JWT_SECRET             = (아래 명령으로 만든 값)
   ```

   ```bash
   openssl rand -base64 48
   ```

   `${{Postgres.DATABASE_URL}}` 은 Railway 가 `postgresql://사용자:비밀번호@호스트:5432/디비` 형태로 채워 준다.
   JDBC 가 요구하는 형태와 다르지만 [DatabaseUrlNormalizer](src/main/java/com/dongsa/math/config/DatabaseUrlNormalizer.java)
   가 서버 시작 전에 바꿔 주므로 **그대로 넣으면 된다.** 사용자와 비밀번호도 여기서 꺼내 쓴다.

   `PORT` 는 Railway 가 알아서 넣는다. `CORS_ORIGINS` 는 프런트를 같은 서버가 서빙하므로 비워 둔다.

4. **주소 만들기** — `Settings → Networking → Generate Domain`.
   `https://...up.railway.app` 이 나온다. 이 주소를 고모에게 보내면 된다.

5. **첫 배포 확인**

   ```bash
   curl https://<주소>/actuator/health      # {"status":"UP"}
   ```

   서버가 처음 뜰 때 마이그레이션이 돌아 테이블이 만들어지고 문제 템플릿 28개가 들어간다.
   브라우저로 주소를 열어 **학원 만들기**부터 하면 된다.

이후로는 `main` 에 push 할 때마다 자동으로 다시 배포된다.
배포 중이던 요청은 끊기지 않는다 (graceful shutdown, 최대 20초 대기).

### 백업 — 이건 꼭 해 두세요

학원 데이터가 날아가면 복구할 방법이 없다. Railway 대시보드에서 PostgreSQL 백업이 켜져 있는지 확인하고,
중요한 시점에는 손으로도 한 벌 받아 둔다.

```bash
# Railway 대시보드 → Postgres → Connect 에서 접속 정보를 복사해서
pg_dump "postgresql://사용자:비밀번호@호스트:포트/디비" > dongsa-$(date +%Y%m%d).sql
```

### 알아 둘 것

이 서비스에는 **아이들의 이름과 성적**이 들어간다. Railway 는 해외(싱가포르 등)에 데이터를 둔다.
학원에서 학부모 동의를 받을 때 이 점을 알고 있어야 한다.
저장하는 개인정보는 이름과 학년뿐이고, 비밀번호는 해시로만 남는다.

**환경변수** (운영에서 반드시 넣을 것)

| 변수 | 설명 |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `DATABASE_URL` | `jdbc:postgresql://...` 또는 플랫폼이 주는 `postgresql://사용자:비밀번호@...` 그대로 |
| `DATABASE_USER` / `DATABASE_PASSWORD` | DB 계정. `DATABASE_URL` 안에 들어 있으면 생략 가능 |
| `JWT_SECRET` | **32바이트 이상의 임의 문자열.** 안 넣으면 서버가 뜨지 않는다 |
| `CORS_ORIGINS` | 프런트 주소. 쉼표로 여러 개 |
| `PORT` | 기본 8080 |

비밀키를 만들려면: `openssl rand -base64 48`

**확인한 것** — 실제 PostgreSQL 16 에 붙여 마이그레이션·시더·전체 흐름이 도는 것,
Docker 이미지 빌드와 컨테이너 기동, 개발용 비밀키로는 운영 프로필이 뜨지 않는 것,
`postgresql://` 형식 주소를 그대로 넣어도 붙는 것, readiness 가 6초 만에 올라오는 것,
SIGTERM 에 처리 중이던 요청을 끝내고 죽는 것.

## 문제 엔진

`com.dongsa.math.problem` 패키지. 문장은 DB 에, 숫자를 만드는 방법은 코드에 있다.

```
{name}{은/는} 어제 동화책을 {a}쪽 «읽었고», 오늘은 {b}쪽을 읽었습니다.
{name}{이/가} «이틀 동안» 읽은 동화책은 «모두» 몇 쪽입니까?
                                          ↑ 단서

→ 지혜는 어제 동화책을 326쪽 읽었고, 오늘은 426쪽을 읽었습니다.
  지혜가 이틀 동안 읽은 동화책은 모두 몇 쪽입니까?          답 752쪽
```

**조사 자동 처리** (`Josa`)
한글 음절에서 종성을 뽑아 `{은/는}` 을 고른다. 민우**는** / 서윤**이** / 준호**가**.
숫자로 끝나는 경우는 읽는 소리를 따른다 — 100 은 '백'이라 받침이 있고, 2 는 '이'라 없다.
`{으로/로}` 만 규칙이 달라서 ㄹ 받침에서는 '로'를 쓴다.

**제약 조건을 지키는 숫자** (`NumberFactory`)
"받아올림 있는 세 자리 덧셈"을 내려면 그냥 난수로는 안 된다. 조건에 맞을 때까지 다시 뽑는다.

다만 **받아올림이 없는** 덧셈은 다시 뽑기로 안 된다. 세 자리 수 세 개를 무작위로 뽑아
모든 자리의 합이 10 미만일 확률은 1%도 안 되기 때문이다.
그래서 이 경우는 자릿수마다 합이 9를 넘지 않게 **만들어 낸다**.

한 자리 수 뺄셈에는 받아내림이 있을 수 없다(앞의 수가 항상 크므로).
초1 에서 '받아내림 있음'을 골라도 없는 문제가 나가고, 그 사실을 테스트가 못박아 두고 있다.

**학년에 맞는 문장** (`ProblemTemplate.minGrade/maxGrade`)
초6 에 "색 테이프 41273cm 를 잘라 냈습니다" 가 나오면 안 된다.
템플릿마다 자연스러운 학년 범위를 두고, 큰 수용 문장(관객 수, 인구, 금액)을 따로 둔다.

**같은 학습지 다시 뽑기** (`WorksheetFactory`, `ReadableCode`)
문제를 저장하지 않고 **시드만** 저장한다. 조건과 시드가 같으면 항상 같은 문제가 나온다.
시드는 6글자 코드로 바뀌어 화면에 표시된다 — 아이가 헷갈릴 `I O 0 1` 은 알파벳에서 뺐다.

재현이 성립하려면 두 가지를 지켜야 한다. **템플릿 목록의 순서가 항상 같을 것**(그래서 `code` 로 정렬해서 가져온다),
그리고 **난수를 쓰는 순서가 항상 같을 것**. 이게 깨지면 조용히 다른 문제가 나오므로 테스트로 고정해 두었다.

**문장은 JSON 이 원본** (`resources/data/problem-templates.json`)
서버가 뜰 때 DB 와 맞춘다. 파일을 고치고 다시 띄우면 DB 가 따라오고, 바뀐 게 없으면 아무것도 안 건드린다.

## 과제와 채점

한 문제를 **단서 → 연산 → 식 → 답** 네 단계로 나눠서 묻는다.
답만 물으면 "계산은 되는데 문장을 못 읽는 아이"와 "문장은 읽는데 계산을 틀리는 아이"가
똑같은 점수로 나온다. 실제로 이렇게 갈린다.

```
3학년 덧뺄셈 숙제 — 학생 2명 중 2명 완료

이름       총점   단서   연산    식    답
김민우      75%    0% 100% 100% 100%   ← 문장을 못 읽는다
박서윤      75%  100% 100% 100%   0%   ← 계산에서 틀린다

진단: 문장에서 단서를 찾는 연습이 더 필요합니다. 계산보다 읽기를 먼저 보세요.
```

총점은 똑같이 75%다. 단계를 나누지 않으면 두 아이에게 같은 처방을 내리게 된다.

**정답은 서버 밖으로 나가지 않는다.**
아이에게 내려보내는 문제에는 `answer` 도 `cues` 도 `operation` 도 들어 있지 않다 —
그게 바로 물어볼 것들이기 때문이다. 채점은 서버가 시드로 문제를 다시 만들어서 한다.
이미 제출한 단계만 정답이 함께 온다(틀린 뒤에 알려 주려고).
`AssignmentFlowTest.studentNeverSeesTheAnswer` 가 응답 본문을 통째로 검사해서 이걸 지킨다.

**한 단계는 한 번만 받는다.**
같은 단계가 또 들어오면 처음 결과를 그대로 돌려주고 아무것도 바꾸지 않는다.
폰은 잘 끊기고, 끊겼다고 다시 눌렀을 때 점수가 달라지면 안 된다.
틀린 뒤에 정답을 보고 다시 보내도 점수를 딸 수 없다.

**앞 단계를 건너뛸 수 없다.**
식도 안 세우고 답부터 내면 409 다. 단계별 기록이 의미를 가지려면 순서가 지켜져야 한다.

**기본은 아이마다 다른 문제.**
과제 시드와 학생 번호를 섞어 아이별 시드를 만든다. 옆자리를 봐도 소용없다.
반 전체가 같은 시험지를 풀어야 하면 `sameForEveryone` 을 켠다.

**문제는 한 줄도 저장하지 않는다.**
과제에 저장하는 것은 조건과 시드뿐이고, 채점할 때마다 그 시드로 다시 만든다.
한 장이 최대 50문제라 비용은 무시할 수준이다.

**리포트는 쿼리 몇 번으로 끝난다.**
학생 30명이든 300명이든 `group by` 집계 세 번이다. 학생 수만큼 쿼리가 늘지 않는다.

## 설계에서 신경 쓴 것

**학생 로그인을 이메일로 하지 않는다.**
초등학생은 이메일이 없고 비밀번호를 잊어버린다. 그래서 학원 코드 + 이름 + PIN 네 자리를 쓴다.
학원 코드는 `I O 0 1` 처럼 헷갈리는 글자를 뺀 32글자로만 만든다 (`AcademyCodeGenerator`).

**같은 이름 + 같은 PIN 은 등록 단계에서 막는다.**
이름이 같은 아이가 둘인데 PIN 까지 같으면 로그인했을 때 누구인지 정할 수 없다.
사후에 터지게 두지 않고 `StudentService.requirePinIsDistinguishable` 에서 미리 거른다.

**PIN 평문은 만들 때 딱 한 번만 보인다.**
저장은 BCrypt 해시로만 한다. 잊어버리면 선생님이 다시 발급한다 — 원래 값을 알아낼 방법은 없다.

**학원 경계는 토큰에서 온다.**
JWT 에 학원 번호가 들어 있고, 조회는 전부 `findByIdAndAcademyId` 처럼 학원 번호와 함께 건다.
그래서 다른 학원 학생의 id 를 넣어도 403 이 아니라 **404** 가 난다 —
"권한이 없다"고 답하면 그 id 가 존재한다는 사실이 새어 나가기 때문이다. `AcademyIsolationTest` 가 이걸 지킨다.

**계정은 지우지 않고 중지한다.**
그만둔 강사와 학생은 `active=false` 가 된다. 로그인은 막히고 기록은 남는다.

**로그인 실패 이유를 자세히 알려 주지 않는다.**
"이메일이 없다"와 "비밀번호가 틀렸다"를 구분해 주면 가입 여부를 훑을 수 있다. 둘 다 같은 응답을 준다.

**개발용 비밀키로는 운영에 뜨지 않는다.**
`app.jwt.secret` 이 32바이트보다 짧거나, `prod` 프로필인데 개발용 기본값 그대로면
서버가 아예 시작하지 않는다. 잘못된 키로 조용히 떠 있는 것보다 낫다.

**PIN 은 1만 가지뿐이라 시도 횟수를 막는다.**
학원 코드와 이름만 알면 네 자리 PIN 은 전부 넣어 볼 수 있다.
PIN 을 길게 만들 수는 없으므로(초등학생이 쓴다) 계정별 10회, IP별 60회로 끊는다(10분 창).
막힌 뒤에는 진짜 PIN 을 넣어도 대조 자체를 하지 않는다.
없는 계정을 훑는 것도 똑같이 세므로 가입 여부가 새어 나가지 않는다.
한 대짜리 서버를 전제로 메모리에 들고 있다 — **서버를 여러 대로 늘리면 Redis 로 옮겨야 한다.**

**반을 지워도 기록은 남는다.**
학생과 과제의 반 연결만 끊고 지운다. 외래키에도 `on delete set null` 을 걸어 두었다.

## 앞으로

- [x] 회원가입과 학원·반·학생 관리
- [x] 문제 템플릿 엔진 (조사 처리, 학년별 제약 숫자 생성, 시드 재현)
- [x] 과제 배정과 단계별 채점
- [x] 단계별 · 유형별 취약점 리포트

- [x] 스키마 마이그레이션(Flyway) + PostgreSQL
- [x] Dockerfile + docker compose
- [x] CORS 허용 도메인 설정값화
- [x] 로그인 시도 횟수 제한
- [x] 학생 성적(누적)과 계정 완전 삭제
- [x] 연산 게임과 학원 순위

**배포 전에 남은 것**

- [x] 무중단 종료와 헬스 프로브 (liveness / readiness)
- [x] 플랫폼이 주는 `postgresql://` 주소 자동 변환
- [x] 백업·복구 스크립트 (`deploy/backup.sh`, `deploy/restore.sh`)
- [ ] **백업을 실제로 걸기** — 배포 직후 바로. 학원 데이터가 날아가면 복구할 방법이 없다
- [ ] 에러 추적(Sentry 등)과 접속 로그 보관
- [ ] 서버를 여러 대로 늘린다면 시도 제한을 Redis 로 옮기기

**그다음**

- [ ] 모바일 웹 프런트 (프로토타입을 실제 API 에 연결)
- [ ] 수업 영상 구간 연결 (유튜브 영상 번호 + 시작 시각만 저장 — 영상 파일은 우리가 갖지 않는다)

## 스택

Java 21 · Spring Boot 3.5.16 · Spring Security + JWT(jjwt) · JPA · H2(개발) / PostgreSQL(배포) · Gradle
