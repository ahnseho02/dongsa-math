# ── 1단계: 빌드 ───────────────────────────────────────────────
FROM eclipse-temurin:21-jdk AS build
WORKDIR /build

# 의존성만 먼저 받아 둔다. 소스만 바뀌면 이 층은 캐시가 살아 있어 빌드가 빠르다.
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

COPY src src
RUN ./gradlew --no-daemon bootJar -x test

# ── 2단계: 실행 ───────────────────────────────────────────────
FROM eclipse-temurin:21-jre
WORKDIR /app

# 루트로 돌리지 않는다
RUN useradd --system --create-home --uid 1001 app
USER app

COPY --from=build --chown=app:app /build/build/libs/*.jar app.jar

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -Duser.timezone=Asia/Seoul"
EXPOSE 8080

# 컨테이너가 살아 있는지가 아니라 앱이 응답하는지를 본다
HEALTHCHECK --interval=15s --timeout=3s --start-period=40s --retries=5 \
  CMD ["sh", "-c", "curl -fsS http://localhost:8080/actuator/health || exit 1"]

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
