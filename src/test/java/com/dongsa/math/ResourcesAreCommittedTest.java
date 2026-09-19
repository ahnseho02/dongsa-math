package com.dongsa.math;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 리소스가 저장소에 실제로 들어 있는지 확인한다.
 *
 * 실제로 당한 적이 있다. `.gitignore` 의 `data/` 가 최상위 개발 DB 폴더만 가리킨다고 생각했는데,
 * 앞에 / 가 없으면 어느 깊이에서든 맞는다. 그래서 `src/main/resources/data/problem-templates.json`
 * 이 조용히 빠졌고, 로컬에서는 멀쩡한데 배포하면 첫 부팅에서 죽는 상태가 됐다.
 *
 * 로컬 파일만 보는 테스트로는 절대 못 잡는다. git 에 물어봐야 한다.
 */
@DisplayName("리소스가 저장소에 들어 있는가")
class ResourcesAreCommittedTest {

    @Test
    @DisplayName("src/main/resources 의 파일이 하나도 빠지지 않았다")
    void everyResourceIsTracked() throws Exception {
        Path root = Path.of("src/main/resources");
        assumeTrue(Files.isDirectory(root), "리소스 폴더가 없으면 볼 것이 없다");

        Set<String> tracked = new HashSet<>(git("ls-files", "src/main/resources"));
        assumeTrue(!tracked.isEmpty(), "git 저장소가 아니면 건너뛴다");

        List<String> missing = new ArrayList<>();
        try (Stream<Path> files = Files.walk(root)) {
            files.filter(Files::isRegularFile).forEach(path -> {
                String relative = path.toString().replace('\\', '/');
                if (!tracked.contains(relative)) {
                    missing.add(relative);
                }
            });
        }

        assertThat(missing)
                .as("저장소에 없는 리소스입니다. .gitignore 가 걸러 버렸는지 확인하세요 "
                    + "(예: 'data/' 는 어느 깊이에서든 맞습니다 — '/data/' 로 적어야 최상위만 가리킵니다)")
                .isEmpty();
    }

    @Test
    @DisplayName("문제 템플릿과 마이그레이션은 특히 꼭 있어야 한다")
    void criticalResourcesExist() {
        assertThat(getClass().getClassLoader().getResource("data/problem-templates.json"))
                .as("이게 없으면 학습지를 한 장도 못 만든다").isNotNull();
        assertThat(getClass().getClassLoader().getResource("db/migration/V1__init.sql"))
                .as("이게 없으면 테이블이 만들어지지 않는다").isNotNull();
        assertThat(getClass().getClassLoader().getResource("static/index.html"))
                .as("이게 없으면 화면이 안 뜬다").isNotNull();
        assertThat(getClass().getClassLoader().getResource("static/app.js")).isNotNull();
    }

    private List<String> git(String... args) throws Exception {
        List<String> command = new ArrayList<>(List.of("git"));
        command.addAll(List.of(args));
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line.strip());
            }
        }
        return process.waitFor() == 0 ? lines : List.of();
    }
}
