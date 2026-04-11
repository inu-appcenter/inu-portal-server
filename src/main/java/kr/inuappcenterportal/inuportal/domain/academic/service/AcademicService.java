package kr.inuappcenterportal.inuportal.domain.academic.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.inuappcenterportal.inuportal.domain.academic.dto.AcademicBasicInfoRequestDto;
import kr.inuappcenterportal.inuportal.domain.academic.dto.AcademicBasicInfoResponseDto;
import kr.inuappcenterportal.inuportal.domain.academic.exception.AcademicException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoAlertPresentException;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class AcademicService {

    private static final String PORTAL_LOGIN_URL = "https://portal.inu.ac.kr:444/enview/user/login.face";
    private static final String ERP_SSO_URL = "http://erp.inu.ac.kr:8881/com/SsoCtr/initPageWork.do?loginGbn=sso";
    private static final String ERP_BASE_URL = "https://erp.inu.ac.kr:8443";
    private static final String ERP_REFERER = ERP_BASE_URL + "/nx/";
    private static final String MENU_ID = "M002043";
    private static final String PROGRAM_ID = "P001878";
    private static final String RECORD_SEPARATOR = String.valueOf((char) 30);
    private static final String UNIT_SEPARATOR = String.valueOf((char) 31);
    private static final String NULL_MARKER = String.valueOf((char) 3);
    private static final String[] COMMON_CODE_RPST_CODES = {
            "A0013", "CA001", "UB026", "UB006", "UB007", "UB008", "UB001", "UB002", "UB003", "UB004", "UB010"
    };
    private static final String[] COMMON_CODE_DATASETS = {
            "DS_GEN_GBN", "DS_NAT_GBN", "DS_HY_SEQ_GBN", "DS_CORS_GBN", "DS_SCHREG_ST_GBN",
            "DS_SCHREG_MOD_GBN", "DS_ENTR_GBN", "DS_ENTR_CLSF_GBN", "DS_CAPA_IO_GBN", "DS_SKIL_STD_GBN",
            "DS_MIL_FINISH_GBN"
    };
    private static final Pattern STUDENT_ID_PATTERN = Pattern.compile("temp_user_id\\s*=\\s*'([^']+)'");
    private static final Pattern WMONID_PATTERN = Pattern.compile("WMONID=([^\\u001e\\r\\n]+)");

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final AcademicSsvParser academicSsvParser;

    @Value("${installPath:}")
    private String installPath;

    public AcademicBasicInfoResponseDto getBasicInfo(AcademicBasicInfoRequestDto requestDto) {
        ChromeDriver driver = createDriver();

        try {
            loginToPortal(driver, requestDto.getPortalId(), requestDto.getPortalPassword());
            String studentId = resolveStudentId(driver.getPageSource(), requestDto.getPortalId());
            ErpSessionMetadata sessionMetadata = openErpSession(driver);

            requestMenuAuthority(sessionMetadata);
            String responseBody = requestBaseAcademicInfo(sessionMetadata, studentId);
            AcademicBasicInfoResponseDto basicInfo = academicSsvParser.parseAcademicBasicInfo(responseBody);

            try {
                Map<String, Map<String, String>> commonCodeMaps = requestCommonCodeNameMaps(sessionMetadata);
                Map<String, String> departmentNameMap = requestDepartmentNameMap(sessionMetadata);
                return enrichBasicInfo(basicInfo, commonCodeMaps, departmentNameMap);
            } catch (AcademicException exception) {
                log.warn("Academic code name enrichment skipped: {}", exception.getMessage());
                return basicInfo;
            }
        } finally {
            driver.quit();
        }
    }

    private ChromeDriver createDriver() {
        try {
            if (StringUtils.hasText(installPath) && Files.exists(Path.of(installPath))) {
                System.setProperty("webdriver.chrome.driver", installPath);
            }

            LoggingPreferences loggingPreferences = new LoggingPreferences();
            loggingPreferences.enable(LogType.PERFORMANCE, Level.ALL);

            ChromeOptions options = new ChromeOptions();
            options.setPageLoadStrategy(PageLoadStrategy.EAGER);
            options.addArguments("--remote-allow-origins=*");
            options.addArguments("--headless=new");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--window-size=1920,1080");
            options.setCapability("goog:loggingPrefs", loggingPreferences);

            return new ChromeDriver(options);
        } catch (WebDriverException exception) {
            log.error("Academic crawler driver initialization failed", exception);
            throw new AcademicException(HttpStatus.BAD_GATEWAY, "Failed to initialize ERP academic session.");
        }
    }

    private void loginToPortal(ChromeDriver driver, String portalId, String portalPassword) {
        try {
            driver.get(PORTAL_LOGIN_URL);

            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("userIdI")));

            driver.findElement(By.name("userIdI")).sendKeys(portalId);
            driver.findElement(By.name("passwordI")).sendKeys(portalPassword);
            driver.findElement(By.cssSelector("a.loginbtn")).click();

            wait.until(webDriver -> hasAlert(webDriver) || isPortalMainLoaded(webDriver));

            if (hasAlert(driver)) {
                Alert alert = driver.switchTo().alert();
                String alertText = alert.getText();
                alert.accept();
                log.info("Portal login alert detected: {}", alertText);
                throw new AcademicException(HttpStatus.UNAUTHORIZED, "Portal login failed.");
            }

            if (!isPortalMainLoaded(driver)) {
                throw new AcademicException(HttpStatus.UNAUTHORIZED, "Portal login failed.");
            }
        } catch (TimeoutException exception) {
            throw new AcademicException(HttpStatus.UNAUTHORIZED, "Portal login failed.");
        }
    }

    private boolean isPortalMainLoaded(WebDriver driver) {
        String url = driver.getCurrentUrl();
        String source = driver.getPageSource();
        return (url != null && url.contains("/enview/portal/"))
                || source.contains("fn_pageFilter('combined_info'")
                || source.contains("통합정보");
    }

    private boolean hasAlert(WebDriver driver) {
        try {
            driver.switchTo().alert();
            return true;
        } catch (NoAlertPresentException ignored) {
            return false;
        }
    }

    private String resolveStudentId(String pageSource, String portalId) {
        Matcher matcher = STUDENT_ID_PATTERN.matcher(pageSource);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return portalId;
    }

    private ErpSessionMetadata openErpSession(ChromeDriver driver) {
        clearPerformanceLogs(driver);

        try {
            driver.get(ERP_SSO_URL);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
            wait.until(webDriver -> webDriver.getCurrentUrl().contains("erp.inu.ac.kr:8443/nx/"));
            sleep(4000);

            Map<String, String> metadata = extractSsvMetadata(driver);
            String wmonId = StringUtils.hasText(metadata.get("WMONID"))
                    ? metadata.get("WMONID")
                    : extractWmonIdFromBrowser(driver);

            if (!StringUtils.hasText(wmonId)) {
                throw new AcademicException(HttpStatus.BAD_GATEWAY, "Failed to initialize ERP academic session.");
            }

            String cookieHeader = buildCookieHeader(driver);
            if (!StringUtils.hasText(cookieHeader)) {
                throw new AcademicException(HttpStatus.BAD_GATEWAY, "Failed to initialize ERP academic session.");
            }

            String userAgent = String.valueOf(((JavascriptExecutor) driver).executeScript("return navigator.userAgent;"));
            String loginDomain = StringUtils.hasText(metadata.get("login_domain")) ? metadata.get("login_domain") : "inu.ac.kr";
            boolean baExist = !"false".equalsIgnoreCase(metadata.getOrDefault("_ba_exist", "true"));

            return new ErpSessionMetadata(
                    cookieHeader,
                    userAgent,
                    wmonId,
                    loginDomain,
                    baExist,
                    metadata.get("_clck"),
                    metadata.get("_clsk")
            );
        } catch (TimeoutException exception) {
            throw new AcademicException(HttpStatus.BAD_GATEWAY, "Failed to initialize ERP academic session.");
        }
    }

    private void clearPerformanceLogs(ChromeDriver driver) {
        try {
            driver.manage().logs().get(LogType.PERFORMANCE);
        } catch (Exception ignored) {
            // Ignore when browser does not support log clearing.
        }
    }

    private Map<String, String> extractSsvMetadata(ChromeDriver driver) {
        Map<String, String> metadata = new LinkedHashMap<>();

        try {
            LogEntries logEntries = driver.manage().logs().get(LogType.PERFORMANCE);
            for (LogEntry logEntry : logEntries) {
                JsonNode root = objectMapper.readTree(logEntry.getMessage());
                JsonNode message = root.path("message");

                if (!"Network.requestWillBeSent".equals(message.path("method").asText())) {
                    continue;
                }

                String postData = message.path("params").path("request").path("postData").asText();
                if (!StringUtils.hasText(postData) || !postData.contains("WMONID=")) {
                    continue;
                }

                metadata.putAll(parseSsvVariables(postData));
                if (metadata.containsKey("WMONID")) {
                    return metadata;
                }
            }
        } catch (Exception exception) {
            log.warn("Failed to inspect ERP performance logs: {}", exception.getMessage());
        }

        return metadata;
    }

    private String extractWmonIdFromBrowser(ChromeDriver driver) {
        Set<String> scripts = Set.of(
                "return window.WMONID || window.wmonid || '';",
                "return window.sessionStorage ? (window.sessionStorage.getItem('WMONID') || '') : '';",
                "return window.localStorage ? (window.localStorage.getItem('WMONID') || '') : '';",
                "return document.body ? document.body.innerHTML : '';"
        );

        for (String script : scripts) {
            try {
                Object result = ((JavascriptExecutor) driver).executeScript(script);
                if (result == null) {
                    continue;
                }

                String text = String.valueOf(result);
                if ("return document.body ? document.body.innerHTML : '';".equals(script)) {
                    Matcher matcher = WMONID_PATTERN.matcher(text);
                    if (matcher.find()) {
                        return matcher.group(1);
                    }
                } else if (StringUtils.hasText(text)) {
                    return text;
                }
            } catch (Exception ignored) {
                // Ignore fallback script errors.
            }
        }

        return null;
    }

    private void requestMenuAuthority(ErpSessionMetadata sessionMetadata) {
        String body = buildBaseSsv(sessionMetadata)
                + "menuId=" + MENU_ID + RECORD_SEPARATOR;

        postSsv(
                ERP_BASE_URL + "/com/PermCtr/findMenuGrdOne.do?menuId=" + MENU_ID + "&pgmId=" + PROGRAM_ID,
                body,
                HttpStatus.BAD_GATEWAY,
                "Failed to initialize ERP academic session.",
                sessionMetadata
        );
    }

    private String requestBaseAcademicInfo(ErpSessionMetadata sessionMetadata, String studentId) {
        String body = buildBaseSsv(sessionMetadata)
                + "Dataset:DS_COND" + RECORD_SEPARATOR
                + "_RowType_" + UNIT_SEPARATOR + "stuno" + UNIT_SEPARATOR + "korNm" + UNIT_SEPARATOR + "gbn"
                + UNIT_SEPARATOR + "colgGrscCd" + UNIT_SEPARATOR + "colgCd" + UNIT_SEPARATOR + "earnMintStom" + RECORD_SEPARATOR
                + "U" + UNIT_SEPARATOR + studentId + UNIT_SEPARATOR + NULL_MARKER + UNIT_SEPARATOR + NULL_MARKER
                + UNIT_SEPARATOR + NULL_MARKER + UNIT_SEPARATOR + NULL_MARKER + UNIT_SEPARATOR + "1" + RECORD_SEPARATOR
                + "O" + UNIT_SEPARATOR + NULL_MARKER + UNIT_SEPARATOR + NULL_MARKER + UNIT_SEPARATOR + NULL_MARKER
                + UNIT_SEPARATOR + NULL_MARKER + UNIT_SEPARATOR + NULL_MARKER + UNIT_SEPARATOR + "1" + RECORD_SEPARATOR;

        return postSsv(
                ERP_BASE_URL + "/uni/sreg/TsimCtr/findBaseSchregInfoOne.do?menuId=" + MENU_ID + "&pgmId=" + PROGRAM_ID,
                body,
                HttpStatus.BAD_GATEWAY,
                "Failed to fetch academic info.",
                sessionMetadata
        );
    }

    private Map<String, Map<String, String>> requestCommonCodeNameMaps(ErpSessionMetadata sessionMetadata) {
        String body = buildBaseSsv(sessionMetadata)
                + "rpstCd=" + String.join("|", COMMON_CODE_RPST_CODES) + RECORD_SEPARATOR
                + "useYn=" + repeatValue("1", COMMON_CODE_DATASETS.length) + RECORD_SEPARATOR
                + "textMode=" + repeatValue("N", COMMON_CODE_DATASETS.length) + RECORD_SEPARATOR
                + "dataSet=" + String.join("|", COMMON_CODE_DATASETS) + RECORD_SEPARATOR;

        String responseBody = postSsv(
                ERP_BASE_URL + "/com/CodeCtr/findCodeComboList.do?menuId=" + MENU_ID + "&pgmId=" + PROGRAM_ID,
                body,
                HttpStatus.BAD_GATEWAY,
                "Failed to fetch academic code info.",
                sessionMetadata
        );

        Map<String, Map<String, String>> codeNameMaps = new LinkedHashMap<>();
        for (String datasetName : COMMON_CODE_DATASETS) {
            codeNameMaps.put(datasetName, academicSsvParser.parseCodeNameMap(responseBody, datasetName));
        }
        return codeNameMaps;
    }

    private Map<String, String> requestDepartmentNameMap(ErpSessionMetadata sessionMetadata) {
        String responseBody = postSsv(
                ERP_BASE_URL + "/uni/sreg/BaimCtr/findDeptCdList1.do?menuId=" + MENU_ID + "&pgmId=" + PROGRAM_ID,
                buildBaseSsv(sessionMetadata),
                HttpStatus.BAD_GATEWAY,
                "Failed to fetch academic code info.",
                sessionMetadata
        );

        return academicSsvParser.parseDepartmentNameMap(responseBody);
    }

    private AcademicBasicInfoResponseDto enrichBasicInfo(
            AcademicBasicInfoResponseDto basicInfo,
            Map<String, Map<String, String>> commonCodeMaps,
            Map<String, String> departmentNameMap
    ) {
        return basicInfo.toBuilder()
                .enrollmentStatusName(resolveCodeName(commonCodeMaps.get("DS_SCHREG_ST_GBN"), basicInfo.getEnrollmentStatusCode()))
                .entranceClassificationName(resolveCodeName(commonCodeMaps.get("DS_ENTR_CLSF_GBN"), basicInfo.getEntranceClassificationCode()))
                .entranceTypeName(resolveCodeName(commonCodeMaps.get("DS_ENTR_GBN"), basicInfo.getEntranceTypeCode()))
                .latestEnrollmentChangeName(resolveCodeName(commonCodeMaps.get("DS_SCHREG_MOD_GBN"), basicInfo.getLatestEnrollmentChangeCode()))
                .genderName(resolveCodeName(commonCodeMaps.get("DS_GEN_GBN"), basicInfo.getGenderCode()))
                .departmentName(firstNonBlank(basicInfo.getDepartmentName(), resolveCodeName(departmentNameMap, basicInfo.getDepartmentCode())))
                .majorName(firstNonBlank(basicInfo.getMajorName(), resolveCodeName(departmentNameMap, basicInfo.getMajorCode())))
                .collegeGroupName(resolveCodeName(departmentNameMap, basicInfo.getCollegeGroupCode()))
                .collegeName(firstNonBlank(basicInfo.getCollegeName(), resolveCodeName(departmentNameMap, basicInfo.getCollegeCode())))
                .courseName(resolveCodeName(commonCodeMaps.get("DS_CORS_GBN"), basicInfo.getCourseCode()))
                .semesterSequenceName(resolveCodeName(commonCodeMaps.get("DS_HY_SEQ_GBN"), basicInfo.getSemesterSequenceCode()))
                .nationalityName(resolveCodeName(commonCodeMaps.get("DS_NAT_GBN"), basicInfo.getNationalityCode()))
                .militaryStatusName(resolveCodeName(commonCodeMaps.get("DS_MIL_FINISH_GBN"), basicInfo.getMilitaryStatusCode()))
                .capacityIoName(resolveCodeName(commonCodeMaps.get("DS_CAPA_IO_GBN"), basicInfo.getCapacityIoCode()))
                .skillStandardName(resolveCodeName(commonCodeMaps.get("DS_SKIL_STD_GBN"), basicInfo.getSkillStandardCode()))
                .build();
    }

    private String postSsv(String url, String body, HttpStatus status, String message, ErpSessionMetadata sessionMetadata) {
        try {
            WebClient erpClient = webClient.mutate()
                    .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                    .build();

            return erpClient.post()
                    .uri(url)
                    .header(HttpHeaders.COOKIE, sessionMetadata.cookieHeader())
                    .header(HttpHeaders.ORIGIN, ERP_BASE_URL)
                    .header(HttpHeaders.REFERER, ERP_REFERER)
                    .header(HttpHeaders.USER_AGENT, sessionMetadata.userAgent())
                    .header("REQFOUNDATAION", "nexacro")
                    .contentType(MediaType.parseMediaType("text/plain;charset=UTF-8"))
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, response ->
                            response.bodyToMono(String.class)
                                    .map(ignored -> new AcademicException(status, message)))
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(30));
        } catch (AcademicException exception) {
            throw exception;
        } catch (Exception exception) {
            log.warn("Academic SSV call failed for url {}: {}", url, exception.getMessage());
            throw new AcademicException(status, message);
        }
    }

    private Map<String, String> parseSsvVariables(String postData) {
        Map<String, String> variables = new LinkedHashMap<>();
        String metadataBlock = postData.contains("Dataset:")
                ? postData.substring(0, postData.indexOf("Dataset:"))
                : postData;

        String[] records = metadataBlock.split(RECORD_SEPARATOR, -1);
        for (String record : records) {
            if (!record.contains("=")) {
                continue;
            }
            int separatorIndex = record.indexOf('=');
            String key = record.substring(0, separatorIndex);
            String value = record.substring(separatorIndex + 1);
            if (StringUtils.hasText(key)) {
                variables.put(key, value);
            }
        }
        return variables;
    }

    private String buildBaseSsv(ErpSessionMetadata sessionMetadata) {
        StringBuilder builder = new StringBuilder();
        builder.append("SSV:utf-8").append(RECORD_SEPARATOR)
                .append("WMONID=").append(sessionMetadata.wmonId()).append(RECORD_SEPARATOR)
                .append("_ba_exist=").append(sessionMetadata.baExist()).append(RECORD_SEPARATOR)
                .append("login_domain=").append(sessionMetadata.loginDomain()).append(RECORD_SEPARATOR);

        if (StringUtils.hasText(sessionMetadata.clck())) {
            builder.append("_clck=").append(sessionMetadata.clck()).append(RECORD_SEPARATOR);
        }
        if (StringUtils.hasText(sessionMetadata.clsk())) {
            builder.append("_clsk=").append(sessionMetadata.clsk()).append(RECORD_SEPARATOR);
        }

        builder.append("requestTimeStr=").append(System.currentTimeMillis()).append(RECORD_SEPARATOR);
        return builder.toString();
    }

    private String buildCookieHeader(ChromeDriver driver) {
        StringJoiner joiner = new StringJoiner("; ");
        for (Cookie cookie : driver.manage().getCookies()) {
            joiner.add(cookie.getName() + "=" + cookie.getValue());
        }
        return joiner.toString();
    }

    private String repeatValue(String value, int count) {
        StringJoiner joiner = new StringJoiner("|");
        for (int i = 0; i < count; i++) {
            joiner.add(value);
        }
        return joiner.toString();
    }

    private String resolveCodeName(Map<String, String> codeNameMap, String code) {
        if (!StringUtils.hasText(code) || codeNameMap == null) {
            return null;
        }
        return codeNameMap.get(code);
    }

    private String firstNonBlank(String primaryValue, String fallbackValue) {
        if (StringUtils.hasText(primaryValue)) {
            return primaryValue;
        }
        return StringUtils.hasText(fallbackValue) ? fallbackValue : null;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AcademicException(HttpStatus.BAD_GATEWAY, "Failed to initialize ERP academic session.");
        }
    }

    private record ErpSessionMetadata(
            String cookieHeader,
            String userAgent,
            String wmonId,
            String loginDomain,
            boolean baExist,
            String clck,
            String clsk
    ) {
    }
}
