package kr.inuappcenterportal.inuportal.domain.cafeteria.service.instagram;

import kr.inuappcenterportal.inuportal.domain.cafeteria.service.instagram.SecondDormitoryInstagramPythonRunner.PythonRunResult;
import kr.inuappcenterportal.inuportal.domain.cafeteria.service.instagram.dto.InstagramMenuPost;
import kr.inuappcenterportal.inuportal.domain.cafeteria.service.instagram.dto.SecondDormitoryDailyMenu;
import kr.inuappcenterportal.inuportal.domain.featureflag.service.FeatureFlagService;
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
import java.time.LocalTime;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    private final FeatureFlagService featureFlagService;

    private final AtomicBoolean running = new AtomicBoolean(false);

    @EventListener(ApplicationReadyEvent.class)
    public void initSecondDormitoryInstagramCafeteria() {
        refreshMenus("startup");
    }

    @Scheduled(
            cron = "${app.cafeteria.instagram.second-dormitory.lunch-retry-cron:0 */15 10-11 * * *}",
            zone = "${app.cafeteria.instagram.second-dormitory.zone:Asia/Seoul}"
    )
    public void refreshLunchRetryWindow() {
        if (!isRetryExecutionTime(MealWindow.LUNCH)) {
            return;
        }
        refreshMenus("lunch-retry", MealWindow.LUNCH);
    }

    @Scheduled(
            cron = "${app.cafeteria.instagram.second-dormitory.dinner-retry-cron:0 */15 17-18 * * *}",
            zone = "${app.cafeteria.instagram.second-dormitory.zone:Asia/Seoul}"
    )
    public void refreshDinnerRetryWindow() {
        if (!isRetryExecutionTime(MealWindow.DINNER)) {
            return;
        }
        refreshMenus("dinner-retry", MealWindow.DINNER);
    }

    public void refreshMenus(String trigger) {
        refreshMenus(trigger, null);
    }

    public void refreshMenus(String trigger, MealWindow targetMealWindow) {
        LocalDate today = null;
        List<InstagramMenuPost> posts = List.of();
        Map<LocalDate, SecondDormitoryDailyMenu> weeklyMenus = Map.of();
        PythonRunResult runResult = null;

        if (!properties.isEnabled()) {
            return;
        }

        if (!featureFlagService.isEnabled("SECOND_DORMITORY_CRAWL_ENABLED")) {
            return;
        }

        if (!running.compareAndSet(false, true)) {
            log.info("2기숙사 식당 인스타 메뉴가 이미 실행 중입니다. trigger={}", trigger);
            return;
        }

        try {
            today = LocalDate.now(properties.resolveZoneId());
            if (targetMealWindow != null && hasStoredMenuForToday(today, targetMealWindow)) {
                log.info("2기숙사 식당 재시도 스킵. trigger={}, target={}, date={}", trigger, targetMealWindow, today);
                return;
            }

            int recentCount = determineRecentCount(today, targetMealWindow);
            if (recentCount <= 0) {
                log.info("2기숙사 식당 스크래핑 스킵. trigger={}, target={}, date={}", trigger, targetMealWindow, today);
                return;
            }

            runResult = pythonRunner.runScraper(recentCount);
            logPythonRunResult(runResult);

            if (!runResult.isSuccess()) {
                throw new IllegalStateException("Python scraper exited with code " + runResult.exitCode());
            }

            posts = outputReader.readHistoryPosts(runResult.historyFile());
            logCollectedPosts(posts, runResult.historyFile().toString());

            weeklyMenus = parser.parseWeeklyMenus(posts, today, properties.resolveZoneId());
            logParsedWeeklyMenus(today, posts, weeklyMenus);

            if (weeklyMenus.isEmpty()) {
                log.warn("2기숙사 식당 현재 주차 메뉴를 찾지 못했습니다. trigger={}", trigger);
                return;
            }

            try {
                ensureWeekInitialized(today);
                saveWeeklyMenus(weeklyMenus);
                logTodayMenuStatus(today, weeklyMenus);
                log.info("2기숙사 식당 메뉴 저장 완료. trigger={}, savedDates={}", trigger, weeklyMenus.keySet());
            } catch (Exception e) {
                logFailureContext(trigger, today, posts, weeklyMenus, runResult);
                log.warn("2기숙사 식당 인스타 메뉴 저장 실패. trigger={}, message={}", trigger, e.getMessage(), e);
            }
        } catch (IOException | InterruptedException | TimeoutException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            logFailureContext(trigger, today, posts, weeklyMenus, runResult);
            log.warn("2기숙사 식당 Python 실행 실패. trigger={}, message={}", trigger, e.getMessage(), e);
        } catch (Exception e) {
            logFailureContext(trigger, today, posts, weeklyMenus, runResult);
            log.warn("2기숙사 식당 인스타 메뉴 크롤링 또는 파싱 실패. trigger={}, message={}", trigger, e.getMessage(), e);
        } finally {
            cleanupUnusedGeneratedFiles(runResult);
            running.set(false);
        }
    }

    private void logPythonRunResult(PythonRunResult runResult) {
        log.info(
                "2기숙사 식당 Python 실행 완료. exitCode={}, historyFile={}",
                runResult.exitCode(),
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

    private void saveWeeklyMenus(Map<LocalDate, SecondDormitoryDailyMenu> weeklyMenus) {
        for (Map.Entry<LocalDate, SecondDormitoryDailyMenu> entry : weeklyMenus.entrySet()) {
            saveDailyMenu(entry.getKey(), entry.getValue());
        }
    }

    private void saveDailyMenu(LocalDate menuDate, SecondDormitoryDailyMenu menu) {
        int day = menuDate.getDayOfWeek().getValue();
        log.info(
                "2기숙사 식당 Redis 저장 준비. menuDate={}, day={}, lunchMenu={}, dinnerMenu={}",
                menuDate,
                day,
                sanitizeForLog(menu.lunchMenu()),
                sanitizeForLog(menu.dinnerMenu())
        );

        storeMealWithLog(day, 1, "조식", BREAKFAST_NOT_AVAILABLE);
        if (menu.hasLunch()) {
            storeMealWithLog(day, 2, "중식", menu.lunchMenu());
        }
        if (menu.hasDinner()) {
            storeMealWithLog(day, 3, "석식", menu.dinnerMenu());
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

    private void logParsedWeeklyMenus(
            LocalDate today,
            List<InstagramMenuPost> posts,
            Map<LocalDate, SecondDormitoryDailyMenu> weeklyMenus
    ) {
        SecondDormitoryDailyMenu todayMenu = weeklyMenus.getOrDefault(today, SecondDormitoryDailyMenu.empty());
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

        for (Map.Entry<LocalDate, SecondDormitoryDailyMenu> entry : weeklyMenus.entrySet()) {
            log.info(
                    "2기숙사 식당 날짜별 메뉴 파싱 완료. menuDate={}, lunchMenu={}, dinnerMenu={}",
                    entry.getKey(),
                    sanitizeForLog(entry.getValue().lunchMenu()),
                    sanitizeForLog(entry.getValue().dinnerMenu())
            );
        }
    }

    private void logTodayMenuStatus(LocalDate today, Map<LocalDate, SecondDormitoryDailyMenu> weeklyMenus) {
        SecondDormitoryDailyMenu todayMenu = weeklyMenus.getOrDefault(today, SecondDormitoryDailyMenu.empty());
        if (!todayMenu.hasAnyMenu()) {
            log.warn("2기숙사 식당 오늘 메뉴는 아직 없지만, 같은 주차의 이전 게시글은 저장했습니다. date={}", today);
        }
    }

    private boolean isRetryExecutionTime(MealWindow mealWindow) {
        LocalTime now = LocalTime.now(properties.resolveZoneId());
        return switch (mealWindow) {
            case LUNCH -> now.getHour() == 10 || (now.getHour() == 11 && now.getMinute() <= 30);
            case DINNER -> now.getHour() == 17 || (now.getHour() == 18 && now.getMinute() <= 30);
        };
    }

    private boolean hasStoredMenuForToday(LocalDate today, MealWindow mealWindow) {
        int slot = mealWindow == MealWindow.LUNCH ? 2 : 3;
        String storedMenu = redisService.getMeal(CAFETERIA_NAME, today.getDayOfWeek().getValue(), slot);
        if (storedMenu == null || storedMenu.isBlank()) {
            return false;
        }
        return !DEFAULT_MENU.equals(storedMenu);
    }

    private int determineRecentCount(LocalDate today, MealWindow targetMealWindow) {
        List<ExpectedMealSlot> expectedSlots = buildExpectedMealSlots(today, targetMealWindow);
        int firstMissingIndex = findFirstMissingExpectedSlot(expectedSlots);
        if (firstMissingIndex < 0) {
            return 0;
        }

        ExpectedMealSlot firstMissingSlot = expectedSlots.get(firstMissingIndex);
        int desiredRecentCount = expectedSlots.size() - firstMissingIndex;
        int resolvedRecentCount = Math.min(Math.max(desiredRecentCount, 1), properties.getResolvedRecentCount());

        log.info(
                "2기숙사 식당 recent-count 계산 완료. date={}, target={}, expectedSlotCount={}, firstMissingDate={}, firstMissingMeal={}, desiredRecentCount={}, resolvedRecentCount={}",
                today,
                targetMealWindow,
                expectedSlots.size(),
                firstMissingSlot.date(),
                firstMissingSlot.mealWindow(),
                desiredRecentCount,
                resolvedRecentCount
        );
        return resolvedRecentCount;
    }

    private List<ExpectedMealSlot> buildExpectedMealSlots(LocalDate today, MealWindow targetMealWindow) {
        LocalDate startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1L);
        List<ExpectedMealSlot> expectedSlots = new ArrayList<>();

        for (LocalDate cursor = startOfWeek; !cursor.isAfter(today); cursor = cursor.plusDays(1)) {
            if (cursor.isBefore(today)) {
                expectedSlots.add(new ExpectedMealSlot(cursor, MealWindow.LUNCH));
                expectedSlots.add(new ExpectedMealSlot(cursor, MealWindow.DINNER));
                continue;
            }

            ExpectedTodayMeals expectedTodayMeals = resolveExpectedTodayMeals(targetMealWindow);
            if (expectedTodayMeals.includesLunch()) {
                expectedSlots.add(new ExpectedMealSlot(cursor, MealWindow.LUNCH));
            }
            if (expectedTodayMeals.includesDinner()) {
                expectedSlots.add(new ExpectedMealSlot(cursor, MealWindow.DINNER));
            }
        }

        return expectedSlots;
    }

    private ExpectedTodayMeals resolveExpectedTodayMeals(MealWindow targetMealWindow) {
        if (targetMealWindow == MealWindow.LUNCH) {
            return ExpectedTodayMeals.LUNCH_ONLY;
        }
        if (targetMealWindow == MealWindow.DINNER) {
            return ExpectedTodayMeals.BOTH;
        }

        LocalTime now = LocalTime.now(properties.resolveZoneId());
        if (now.isBefore(LocalTime.of(10, 0))) {
            return ExpectedTodayMeals.NONE;
        }
        if (now.isBefore(LocalTime.of(17, 0))) {
            return ExpectedTodayMeals.LUNCH_ONLY;
        }
        return ExpectedTodayMeals.BOTH;
    }

    private int findFirstMissingExpectedSlot(List<ExpectedMealSlot> expectedSlots) {
        for (int i = 0; i < expectedSlots.size(); i++) {
            ExpectedMealSlot slot = expectedSlots.get(i);
            if (isMealMissing(slot.date(), slot.mealWindow())) {
                return i;
            }
        }
        return -1;
    }

    private boolean isMealMissing(LocalDate date, MealWindow mealWindow) {
        int slot = mealWindow == MealWindow.LUNCH ? 2 : 3;
        String storedMenu = redisService.getMeal(CAFETERIA_NAME, date.getDayOfWeek().getValue(), slot);
        return storedMenu == null || storedMenu.isBlank() || DEFAULT_MENU.equals(storedMenu);
    }

    private void logFailureContext(
            String trigger,
            LocalDate today,
            List<InstagramMenuPost> posts,
            Map<LocalDate, SecondDormitoryDailyMenu> weeklyMenus,
            PythonRunResult runResult
    ) {
        log.warn(
                "2기숙사 식당 실패 컨텍스트. trigger={}, date={}, collectedPostCount={}, parsedDates={}",
                trigger,
                today,
                posts == null ? 0 : posts.size(),
                weeklyMenus == null ? List.of() : weeklyMenus.keySet()
        );

        if (runResult != null) {
            log.warn(
                    "2기숙사 식당 Python 실행 컨텍스트. exitCode={}, historyFile={}, stdout={}, stderr={}",
                    runResult.exitCode(),
                    runResult.historyFile(),
                    sanitizeForLog(runResult.stdout()),
                    sanitizeForLog(runResult.stderr())
            );
        }

        if (weeklyMenus != null && !weeklyMenus.isEmpty()) {
            for (Map.Entry<LocalDate, SecondDormitoryDailyMenu> entry : weeklyMenus.entrySet()) {
                log.warn(
                        "2기숙사 식당 실패 시 파싱 메뉴. menuDate={}, lunchMenu={}, dinnerMenu={}",
                        entry.getKey(),
                        sanitizeForLog(entry.getValue().lunchMenu()),
                        sanitizeForLog(entry.getValue().dinnerMenu())
                );
            }
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

        Path historyDir = runResult.historyFile().getParent();
        if (historyDir == null) {
            return;
        }

        Path debugCaptionFile = historyDir.resolve("debug_caption.json");
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

    private enum MealWindow {
        LUNCH,
        DINNER
    }

    private enum ExpectedTodayMeals {
        NONE(false, false),
        LUNCH_ONLY(true, false),
        BOTH(true, true);

        private final boolean includesLunch;
        private final boolean includesDinner;

        ExpectedTodayMeals(boolean includesLunch, boolean includesDinner) {
            this.includesLunch = includesLunch;
            this.includesDinner = includesDinner;
        }

        private boolean includesLunch() {
            return includesLunch;
        }

        private boolean includesDinner() {
            return includesDinner;
        }
    }

    private record ExpectedMealSlot(LocalDate date, MealWindow mealWindow) {
    }
}
