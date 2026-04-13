package kr.inuappcenterportal.inuportal.domain.cafeteria.service.instagram;

import kr.inuappcenterportal.inuportal.domain.cafeteria.service.instagram.SecondDormitoryInstagramPythonRunner.PythonRunResult;
import kr.inuappcenterportal.inuportal.domain.cafeteria.service.instagram.dto.InstagramMenuPost;
import kr.inuappcenterportal.inuportal.domain.cafeteria.service.instagram.dto.SecondDormitoryDailyMenu;
import kr.inuappcenterportal.inuportal.global.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecondDormitoryInstagramCrawlService {

    private static final String CAFETERIA_NAME = "2기숙사 식당";
    private static final String DEFAULT_MENU = "업데이트 전";
    private static final String BREAKFAST_NOT_AVAILABLE = "-";
    private static final String WEEK_META_KEY = "cafeteria:second-dormitory:week";

    private final RedisService redisService;
    private final SecondDormitoryInstagramParser parser;
    private final SecondDormitoryInstagramProperties properties;
    private final SecondDormitoryInstagramPythonRunner pythonRunner;
    private final SecondDormitoryInstagramOutputReader outputReader;

    private final AtomicBoolean running = new AtomicBoolean(false);

    @EventListener(ApplicationReadyEvent.class)
    public void initSecondDormitoryInstagramCafeteria() {
        refreshMenus("startup");
    }

    @Scheduled(
            cron = "${app.cafeteria.instagram.second-dormitory.morning-cron:0 0 9 * * *}",
            zone = "${app.cafeteria.instagram.second-dormitory.zone:Asia/Seoul}"
    )
    public void refreshMorningMenus() {
        refreshMenus("morning");
    }

    @Scheduled(
            cron = "${app.cafeteria.instagram.second-dormitory.afternoon-cron:0 0 17 * * *}",
            zone = "${app.cafeteria.instagram.second-dormitory.zone:Asia/Seoul}"
    )
    public void refreshAfternoonMenus() {
        refreshMenus("afternoon");
    }

    public void refreshMenus(String trigger) {
        LocalDate today = null;
        List<InstagramMenuPost> posts = List.of();
        SecondDormitoryDailyMenu todayMenu = null;
        PythonRunResult runResult = null;

        if (!properties.isEnabled()) {
            return;
        }

        if (!running.compareAndSet(false, true)) {
            log.info("2기숙사 식당 인스타 크롤링이 이미 실행 중입니다. trigger={}", trigger);
            return;
        }

        try {
            today = LocalDate.now(properties.resolveZoneId());
            runResult = pythonRunner.runScraper();
            logPythonRunResult(runResult);

            if (!runResult.isSuccess()) {
                throw new IllegalStateException("Python scraper exited with code " + runResult.exitCode());
            }

            posts = outputReader.readHistoryPosts(runResult.historyFile());
            logCollectedPosts(posts, runResult.historyFile().toString());

            todayMenu = parser.parseToday(posts, today, properties.resolveZoneId());
            logParsedTodayMenu(today, posts, todayMenu);
            if (!todayMenu.hasAnyMenu()) {
                log.warn("2기숙사 식당 오늘 메뉴를 찾지 못했습니다. trigger={}", trigger);
                return;
            }

            try {
                ensureWeekInitialized(today);
                saveTodayMenus(today, todayMenu);
                log.info(
                        "2기숙사 식당 메뉴 저장 완료. trigger={}, lunch={}, dinner={}",
                        trigger,
                        todayMenu.hasLunch(),
                        todayMenu.hasDinner()
                );
            } catch (Exception e) {
                logFailureContext(trigger, today, posts, todayMenu, runResult);
                log.warn("2기숙사 식당 인스타 메뉴 저장 실패. trigger={}, message={}", trigger, e.getMessage(), e);
            }
        } catch (IOException | InterruptedException | TimeoutException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            logFailureContext(trigger, today, posts, todayMenu, runResult);
            log.warn("2기숙사 식당 Python 실행 실패. trigger={}, message={}", trigger, e.getMessage(), e);
        } catch (Exception e) {
            logFailureContext(trigger, today, posts, todayMenu, runResult);
            log.warn("2기숙사 식당 인스타 메뉴 크롤링 또는 파싱 실패. trigger={}, message={}", trigger, e.getMessage(), e);
        } finally {
            cleanupUnusedGeneratedFiles(runResult);
            running.set(false);
        }
    }

    private void logPythonRunResult(PythonRunResult runResult) {
        log.info(
                "2기숙사 식당 Python 실행 완료. exitCode={}, outputFile={}, historyFile={}",
                runResult.exitCode(),
                runResult.outputFile(),
                runResult.historyFile()
        );

        if (runResult.stdout() != null && !runResult.stdout().isBlank()) {
            log.info("2기숙사 식당 Python stdout={}", sanitizeForLog(runResult.stdout()));
        }
        if (runResult.stderr() != null && !runResult.stderr().isBlank()) {
            log.warn("2기숙사 식당 Python stderr={}", sanitizeForLog(runResult.stderr()));
        }
    }

    private void ensureWeekInitialized(LocalDate today) {
        String currentWeek = today.get(IsoFields.WEEK_BASED_YEAR) + "-W"
                + String.format("%02d", today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
        String savedWeek = redisService.getValue(WEEK_META_KEY);
        if (currentWeek.equals(savedWeek)) {
            return;
        }

        log.info(
                "2기숙사 식당 주간 초기화 시작. savedWeek={}, currentWeek={}, defaultLunchDinner={}, breakfast={}",
                savedWeek,
                currentWeek,
                DEFAULT_MENU,
                BREAKFAST_NOT_AVAILABLE
        );

        for (int day = 1; day <= 7; day++) {
            storeMealWithLog(day, 1, "조식", BREAKFAST_NOT_AVAILABLE);
            storeMealWithLog(day, 2, "중식", DEFAULT_MENU);
            storeMealWithLog(day, 3, "석식", DEFAULT_MENU);
        }

        redisService.storeValue(WEEK_META_KEY, currentWeek);
        log.info("2기숙사 식당 주간 초기화 완료. currentWeek={}", currentWeek);
    }

    private void saveTodayMenus(LocalDate today, SecondDormitoryDailyMenu todayMenu) {
        int day = today.getDayOfWeek().getValue();
        log.info(
                "2기숙사 식당 Redis 저장 준비. day={}, lunchMenu={}, dinnerMenu={}",
                day,
                sanitizeForLog(todayMenu.lunchMenu()),
                sanitizeForLog(todayMenu.dinnerMenu())
        );

        storeMealWithLog(day, 1, "조식", BREAKFAST_NOT_AVAILABLE);
        if (todayMenu.hasLunch()) {
            storeMealWithLog(day, 2, "중식", todayMenu.lunchMenu());
        }
        if (todayMenu.hasDinner()) {
            storeMealWithLog(day, 3, "석식", todayMenu.dinnerMenu());
        }
    }

    private void storeMealWithLog(int day, int slot, String mealType, String value) {
        log.info(
                "2기숙사 식당 Redis 저장 시도. day={}, slot={}, mealType={}, value={}",
                day,
                slot,
                mealType,
                sanitizeForLog(value)
        );
        try {
            redisService.storeMeal(CAFETERIA_NAME, day, slot, value);
            log.info(
                    "2기숙사 식당 Redis 저장 성공. day={}, slot={}, mealType={}, value={}",
                    day,
                    slot,
                    mealType,
                    sanitizeForLog(value)
            );
        } catch (Exception e) {
            log.warn(
                    "2기숙사 식당 Redis 저장 실패. day={}, slot={}, mealType={}, value={}, message={}",
                    day,
                    slot,
                    mealType,
                    sanitizeForLog(value),
                    e.getMessage(),
                    e
            );
            throw e;
        }
    }

    private void logCollectedPosts(List<InstagramMenuPost> posts, String historyFilePath) {
        log.info("2기숙사 식당 게시물 수집 완료. count={}, historyFile={}", posts.size(), historyFilePath);
        for (InstagramMenuPost post : posts) {
            log.info(
                    "2기숙사 식당 수집 게시물. postUrl={}, publishedAt={}, caption={}",
                    post.postUrl(),
                    post.publishedAt(),
                    sanitizeForLog(post.caption())
            );
        }
    }

    private void logParsedTodayMenu(LocalDate today, List<InstagramMenuPost> posts, SecondDormitoryDailyMenu todayMenu) {
        long todayPostCount = posts.stream()
                .filter(post -> post.publishedAt() != null)
                .filter(post -> post.publishedAt().atZoneSameInstant(properties.resolveZoneId()).toLocalDate().equals(today))
                .count();
        log.info(
                "2기숙사 식당 오늘 메뉴 파싱 완료. date={}, todayPostCount={}, lunchMenu={}, dinnerMenu={}",
                today,
                todayPostCount,
                sanitizeForLog(todayMenu.lunchMenu()),
                sanitizeForLog(todayMenu.dinnerMenu())
        );
    }

    private void logFailureContext(
            String trigger,
            LocalDate today,
            List<InstagramMenuPost> posts,
            SecondDormitoryDailyMenu todayMenu,
            PythonRunResult runResult
    ) {
        log.warn(
                "2기숙사 식당 실패 컨텍스트. trigger={}, date={}, collectedPostCount={}, lunchMenu={}, dinnerMenu={}",
                trigger,
                today,
                posts == null ? 0 : posts.size(),
                todayMenu == null ? null : sanitizeForLog(todayMenu.lunchMenu()),
                todayMenu == null ? null : sanitizeForLog(todayMenu.dinnerMenu())
        );

        if (runResult != null) {
            log.warn(
                    "2기숙사 식당 Python 실행 컨텍스트. exitCode={}, outputFile={}, historyFile={}, stdout={}, stderr={}",
                    runResult.exitCode(),
                    runResult.outputFile(),
                    runResult.historyFile(),
                    sanitizeForLog(runResult.stdout()),
                    sanitizeForLog(runResult.stderr())
            );
        }

        if (posts == null || posts.isEmpty()) {
            return;
        }

        for (InstagramMenuPost post : posts) {
            log.warn(
                    "2기숙사 식당 실패 시 수집 게시물. postUrl={}, publishedAt={}, caption={}",
                    post.postUrl(),
                    post.publishedAt(),
                    sanitizeForLog(post.caption())
            );
        }
    }

    private void cleanupUnusedGeneratedFiles(PythonRunResult runResult) {
        if (runResult == null) {
            return;
        }

        deleteIfExists(runResult.outputFile(), "latest post output");

        Path debugCaptionFile = runResult.outputFile().getParent().resolve("debug_caption.json");
        deleteIfExists(debugCaptionFile, "debug caption output");
    }

    private void deleteIfExists(Path path, String label) {
        try {
            if (Files.deleteIfExists(path)) {
                log.info("2기숙사 식당 사용하지 않는 Python 생성 파일 삭제 완료. label={}, path={}", label, path);
            }
        } catch (IOException e) {
            log.warn(
                    "2기숙사 식당 사용하지 않는 Python 생성 파일 삭제 실패. label={}, path={}, message={}",
                    label,
                    path,
                    e.getMessage()
            );
        }
    }

    private String sanitizeForLog(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("\r", "\\r").replace("\n", "\\n").trim();
    }
}
