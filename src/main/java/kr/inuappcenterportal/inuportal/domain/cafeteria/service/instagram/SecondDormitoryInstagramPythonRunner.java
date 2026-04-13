package kr.inuappcenterportal.inuportal.domain.cafeteria.service.instagram;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class SecondDormitoryInstagramPythonRunner {

    private final SecondDormitoryInstagramProperties properties;

    @Value("${installPath:}")
    private String installPath;

    public PythonRunResult runScraper() throws IOException, InterruptedException, TimeoutException {
        return runScraper(properties.getResolvedRecentCount());
    }

    public PythonRunResult runScraper(int recentCount) throws IOException, InterruptedException, TimeoutException {
        Path scriptDir = properties.resolveScriptDir().toAbsolutePath().normalize();
        Path scriptFile = properties.resolveScriptFile().toAbsolutePath().normalize();
        Path outputFile = properties.resolveOutputFile().toAbsolutePath().normalize();
        Path historyFile = properties.resolveHistoryOutputFile().toAbsolutePath().normalize();
        Path profileDir = properties.resolveProfileDir().toAbsolutePath().normalize();
        Path outputDir = outputFile.getParent();
        Path historyDir = historyFile.getParent();

        validateScriptSource(scriptDir, scriptFile);
        ensureRuntimeDirectories(outputDir, historyDir, profileDir);
        logResolvedPaths(scriptDir, scriptFile, outputDir, historyDir, profileDir);

        List<String> command = new ArrayList<>();
        String pythonCommand = properties.getPythonCommand() == null ? "" : properties.getPythonCommand().trim();
        if (pythonCommand.isBlank()) {
            throw new IOException("Python command is blank.");
        }

        command.add(pythonCommand);
        command.add(scriptFile.toString());
        command.add("--username");
        command.add(properties.getUsername());
        command.add("--output");
        command.add(outputFile.toString());
        command.add("--history-output");
        command.add(historyFile.toString());
        command.add("--profile-dir");
        command.add(profileDir.toString());
        command.add("--recent-count");
        command.add(String.valueOf(Math.max(recentCount, 1)));

        String resolvedChromeDriverPath = resolveChromeDriverPath();
        if (resolvedChromeDriverPath != null) {
            command.add("--chromedriver-path");
            command.add(resolvedChromeDriverPath);
        }

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(scriptDir.toFile());
        processBuilder.environment().put("PYTHONIOENCODING", "utf-8");

        log.info("2기숙사 식당 Python 스크립트 실행. command={}", String.join(" ", command));
        Process process = processBuilder.start();

        CompletableFuture<String> stdoutFuture = CompletableFuture.supplyAsync(() -> readStream(process.getInputStream()));
        CompletableFuture<String> stderrFuture = CompletableFuture.supplyAsync(() -> readStream(process.getErrorStream()));

        boolean finished = process.waitFor(properties.getResolvedTimeoutSeconds(), TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new TimeoutException("Python scraper timed out after " + properties.getResolvedTimeoutSeconds() + " seconds.");
        }

        try {
            return new PythonRunResult(
                    command,
                    process.exitValue(),
                    stdoutFuture.get(),
                    stderrFuture.get(),
                    outputFile,
                    historyFile
            );
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            throw new IOException("Python scraper output stream read failed: " + cause.getMessage(), cause);
        }
    }

    private void logResolvedPaths(
            Path scriptDir,
            Path scriptFile,
            Path outputDir,
            Path historyDir,
            Path profileDir
    ) {
        log.info(
                "2기숙사 식당 Python 경로 확인. scriptDir={} exists={}, scriptFile={} exists={}, outputDir={} exists={}, historyDir={} exists={}, profileDir={} exists={}",
                scriptDir,
                Files.isDirectory(scriptDir),
                scriptFile,
                Files.isRegularFile(scriptFile),
                outputDir,
                outputDir != null && Files.isDirectory(outputDir),
                historyDir,
                historyDir != null && Files.isDirectory(historyDir),
                profileDir,
                Files.isDirectory(profileDir)
        );
    }

    private void validateScriptSource(Path scriptDir, Path scriptFile) throws IOException {
        if (!Files.isDirectory(scriptDir)) {
            throw new IOException("Instagram script directory not found: " + scriptDir);
        }
        if (!Files.isRegularFile(scriptFile)) {
            throw new IOException("Instagram script file not found: " + scriptFile);
        }
    }

    private void ensureRuntimeDirectories(
            Path outputDir,
            Path historyDir,
            Path profileDir
    ) throws IOException {
        if (outputDir == null) {
            throw new IOException("Instagram output directory path is invalid.");
        }
        if (historyDir == null) {
            throw new IOException("Instagram history output directory path is invalid.");
        }
        createDirectoryIfMissing(outputDir, "output");
        createDirectoryIfMissing(historyDir, "history output");
        createDirectoryIfMissing(profileDir, "chrome profile");
    }

    private void createDirectoryIfMissing(Path directory, String label) throws IOException {
        if (Files.isDirectory(directory)) {
            return;
        }

        Files.createDirectories(directory);
        log.info("2기숙사 식당 Python 실행용 디렉터리 자동 생성. label={}, path={}", label, directory);
    }

    private String resolveChromeDriverPath() {
        String configuredPath = installPath == null ? "" : installPath.trim();
        if (configuredPath.isBlank()) {
            return null;
        }

        try {
            Path path = Path.of(configuredPath);
            if (Files.exists(path)) {
                return path.toAbsolutePath().normalize().toString();
            }
        } catch (InvalidPathException e) {
            log.warn("2기숙사 식당 Python 실행용 chromedriver 경로가 잘못되었습니다. installPath={}, message={}", configuredPath, e.getMessage());
        }

        return null;
    }

    private String readStream(InputStream inputStream) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining(System.lineSeparator()));
        } catch (IOException e) {
            return "stream read failed: " + e.getMessage();
        }
    }

    public record PythonRunResult(
            List<String> command,
            int exitCode,
            String stdout,
            String stderr,
            Path outputFile,
            Path historyFile
    ) {
        public boolean isSuccess() {
            return exitCode == 0;
        }
    }
}
