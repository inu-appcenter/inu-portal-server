package kr.inuappcenterportal.inuportal.domain.timeTable.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.inuappcenterportal.inuportal.domain.course.dto.courseMeeting.CourseMeetingResponseDto;
import kr.inuappcenterportal.inuportal.domain.course.dto.courseOffering.CourseOfferingResponseDto;
import kr.inuappcenterportal.inuportal.domain.course.enums.courseOffering.DayOfWeek;
import kr.inuappcenterportal.inuportal.domain.course.model.CourseMeeting;
import kr.inuappcenterportal.inuportal.domain.course.model.CourseOffering;
import kr.inuappcenterportal.inuportal.domain.course.repository.CourseMeetingRepository;
import kr.inuappcenterportal.inuportal.domain.course.repository.CourseOfferingRepository;
import kr.inuappcenterportal.inuportal.domain.course.service.CourseMeetingService;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterStatus;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterTerm;
import kr.inuappcenterportal.inuportal.domain.semester.model.Semester;
import kr.inuappcenterportal.inuportal.domain.semester.repository.SemesterRepository;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.response.timeTableImage.TimeTableImageMeetingDto;
import kr.inuappcenterportal.inuportal.domain.timeTable.dto.response.timeTableImage.TimeTableImageRecognizeResponseDto;
import kr.inuappcenterportal.inuportal.global.dto.vllm.VllmChatMessageDto;
import kr.inuappcenterportal.inuportal.global.dto.vllm.VllmChatRequestDto;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import kr.inuappcenterportal.inuportal.global.service.VllmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TimeTableImageRecognitionService {

    private final VllmService vllmService;
    private final SemesterRepository semesterRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final CourseMeetingRepository courseMeetingRepository;
    private final CourseMeetingService courseMeetingService;
    private final ObjectMapper objectMapper;

    private static final String VISION_SYSTEM_PROMPT = """
            당신은 대학교 시간표 및 수강신청 이미지를 분석하여 강의 목록을 정밀하게 추출하는 시각 분석 AI 전문가입니다.
            제공된 이미지를 분석하여 모든 강의 항목의 정보를 JSON 규격에 맞추어 정확하게 추출하세요.

            [이미지 유형별 분석 규칙]

            ■ 유형 1: 2D 격자형 시간표 (에브리타임, 대학교 포털 시간표 등)
            1. 시간축 눈금 해석 (중요!):
               - 좌측 축의 숫자는 시간(Hour)입니다: 8, 9, 10, 11, 12, 1, 2, 3, 4, 5, 6, 7, 8...
               - 12 다음의 1, 2, 3, 4, 5, 6, 7, 8은 '오후 시간(13:00, 14:00, 15:00, 16:00, 17:00, 18:00, 19:00, 20:00)'을 의미합니다! 절대로 오전 1시나 09:00으로 임의 추정하지 마세요.
            2. 강의 블록의 시간 판독:
               - 블록의 상단 모서리와 하단 모서리가 좌측 숫자 눈금의 어디에 걸쳐 있는지 수평선을 따라 정밀하게 확인하여 시작 시간과 종료 시간을 판독하세요.
               - 시간표에 적힌 과목명을 1순위로 정확히 읽으세요 (예: '철근콘크리트구조2', '강구조1', '건축설비(2)', '건축구조실험(2)'). 과목명 뒤의 (2) 등 분반 표기도 과목명에 그대로 포함하세요.
            3. 하단 비대면/온라인 강의 및 기타 항목:
               - 시간표 그리드 하단에 별도로 나열된 비대면/e-러닝 과목(예: '문학과테마기행')도 놓치지 말고 추출하세요. (시간이 없으면 startTime="", endTime=""으로 기재)
               - '근로', '알바' 등 비정규 개인 일정 블록도 title: "근로"로 추출하세요.
            4. 동일 과목 통합: 동일한 과목이 여러 요일/시간대로 나뉘어 있다면(예: 월 10:00~11:30, 수 08:00~09:00), 하나의 과목 객체 아래 meetings 배열에 각 시간대를 담으세요.

            ■ 유형 2: 수강신청 / 장바구니 / 신청정보 목록형 화면 (수강신청 앱, 포털 리스트 등)
            1. 학수번호(수강번호) 추출 (최우선):
               - 주황색 또는 괄호 안에 적힌 10자리 숫자 코드(예: [0010517001], [0011918001])가 있다면 반드시 subjectNumber에 추출하세요.
            2. 과목명 추출:
               - '[75분수업]', '[온라인혼합형강좌]' 같은 부가 수식어는 제외하고 순수 과목명만 title에 기재하세요 (예: '음성인식입문', '심층학습', '시스템보안', '블록체인').
            3. 교수명 및 강의실 추출:
               - 교수 이름(예: '김우일', '김인수', '이승수', '박기석')을 professor에 기재하세요.
               - 건물-호수(예: '07-502', '07-505', '07-504')를 classroom에 기재하세요.
            4. 요일 및 교시/시간:
               - 요일(월, 화, 수, 목, 금)과 교시/시간 정보가 있으면 meetings에 기재하세요.

            [출력 형식 제한]
            설명이나 인사말, Markdown 코드 블록(```json 등) 없이, 오직 아래와 같은 순수 JSON 배열만 출력하세요:
            [
              {
                "title": "과목명",
                "professor": "교수명 (없으면 빈 문자열)",
                "classroom": "강의실 (없으면 빈 문자열)",
                "subjectNumber": "학수번호 (있으면 10자리 코드, 없으면 빈 문자열)",
                "meetings": [
                  {
                    "day": "MONDAY",
                    "startTime": "09:00",
                    "endTime": "10:15",
                    "classroom": "7-302"
                  }
                ]
              }
            ]
            """;

    public List<TimeTableImageRecognizeResponseDto> recognizeTimeTableImage(
            MultipartFile file,
            Integer year,
            SemesterTerm term
    ) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드된 시간표 이미지 파일이 비어 있습니다.");
        }

        Semester semester = resolveSemester(year, term);
        String imageUrl = convertFileToDataUrl(file);

        String visionModel = vllmService.getVisionModel();
        log.info("Requesting timetable vision recognition. model: {}, semester: {}-{}", visionModel, semester.getYear(), semester.getTerm());

        List<VllmChatMessageDto> messages = List.of(
                VllmChatMessageDto.system(VISION_SYSTEM_PROMPT),
                VllmChatMessageDto.userWithImage("이 대학교 시간표 이미지를 분석하여 강의 목록을 JSON 배열로 추출해 주세요.", imageUrl)
        );

        VllmChatRequestDto request = VllmChatRequestDto.builder()
                .model(visionModel)
                .messages(messages)
                .temperature(0.1)
                .maxTokens(1500)
                .stream(false)
                .build();

        String rawResponse = vllmService.chat(request);
        log.debug("vLLM vision response: {}", rawResponse);

        List<ExtractedCourse> extractedCourses = parseVisionResponse(rawResponse);
        log.info("Extracted {} courses from timetable image", extractedCourses.size());

        return matchWithDatabaseOfferings(semester, extractedCourses);
    }

    private Semester resolveSemester(Integer year, SemesterTerm term) {
        if (year != null && term != null) {
            return semesterRepository.findByYearAndTerm(year, term)
                    .orElseThrow(() -> new MyException(MyErrorCode.SEMESTER_NOT_FOUND));
        }
        return semesterRepository.findFirstByStatusOrderByStartDateDesc(SemesterStatus.OPEN)
                .or(() -> semesterRepository.findAllByOrderByStartDateDesc().stream().findFirst())
                .orElseThrow(() -> new MyException(MyErrorCode.SEMESTER_NOT_FOUND));
    }

    private String convertFileToDataUrl(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            BufferedImage original = ImageIO.read(new ByteArrayInputStream(bytes));

            // 이미지 디코딩 성공 시, 토큰 초과 방지를 위해 최대 해상도를 1280px로 조절
            if (original != null) {
                int width = original.getWidth();
                int height = original.getHeight();
                int maxDim = Math.max(width, height);
                final int TARGET_MAX_DIM = 1280;

                if (maxDim > TARGET_MAX_DIM) {
                    double scale = (double) TARGET_MAX_DIM / maxDim;
                    int newWidth = (int) Math.round(width * scale);
                    int newHeight = (int) Math.round(height * scale);

                    BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
                    Graphics2D g2d = resized.createGraphics();
                    g2d.setColor(Color.WHITE);
                    g2d.fillRect(0, 0, newWidth, newHeight);
                    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2d.drawImage(original, 0, 0, newWidth, newHeight, null);
                    g2d.dispose();

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(resized, "jpeg", baos);
                    String base64 = Base64.getEncoder().encodeToString(baos.toByteArray());
                    return "data:image/jpeg;base64," + base64;
                }
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                contentType = "image/jpeg";
            }
            String base64 = Base64.getEncoder().encodeToString(bytes);
            return "data:" + contentType + ";base64," + base64;
        } catch (IOException e) {
            log.error("Failed to read image bytes: ", e);
            throw new RuntimeException("이미지 파일 읽기 실패", e);
        }
    }

    private List<ExtractedCourse> parseVisionResponse(String response) {
        if (response == null || response.isBlank()) {
            return List.of();
        }

        String json = response.trim();
        // 마크다운 코드 블록 제거
        if (json.contains("```")) {
            int firstFence = json.indexOf("```");
            int firstNewline = json.indexOf('\n', firstFence);
            int lastFence = json.lastIndexOf("```");
            if (firstNewline != -1 && lastFence > firstNewline) {
                json = json.substring(firstNewline + 1, lastFence).trim();
            }
        }

        // JSON 배열 대괄호 위치 추출
        int startIdx = json.indexOf('[');
        int endIdx = json.lastIndexOf(']');
        if (startIdx != -1 && endIdx > startIdx) {
            json = json.substring(startIdx, endIdx + 1);
        }

        List<ExtractedCourse> list = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(json);
            if (root.isArray()) {
                for (JsonNode courseNode : root) {
                    String title = courseNode.path("title").asText("").trim();
                    String professor = courseNode.path("professor").asText("").trim();
                    String classroom = courseNode.path("classroom").asText("").trim();
                    String subjectNumber = courseNode.path("subjectNumber").asText("").trim();

                    List<TimeTableImageMeetingDto> meetings = new ArrayList<>();
                    JsonNode meetingsNode = courseNode.path("meetings");
                    if (meetingsNode.isArray()) {
                        for (JsonNode mNode : meetingsNode) {
                            String dayStr = mNode.path("day").asText("").trim().toUpperCase();
                            String startTime = normalizeTime(mNode.path("startTime").asText("").trim());
                            String endTime = normalizeTime(mNode.path("endTime").asText("").trim());
                            String room = mNode.path("classroom").asText("").trim();
                            if (room.isEmpty()) {
                                room = classroom;
                            }

                            DayOfWeek day = parseDayOfWeek(dayStr);
                            if (day != null) {
                                meetings.add(new TimeTableImageMeetingDto(day, startTime, endTime, room));
                            }
                        }
                    }

                    if (!title.isEmpty()) {
                        list.add(new ExtractedCourse(title, professor, classroom, subjectNumber, meetings));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse vision response JSON: {}", json, e);
        }
        return list;
    }

    private DayOfWeek parseDayOfWeek(String dayStr) {
        try {
            return DayOfWeek.valueOf(dayStr);
        } catch (Exception e) {
            // 한글 요일 대응
            return switch (dayStr) {
                case "월", "월요일" -> DayOfWeek.MONDAY;
                case "화", "화요일" -> DayOfWeek.TUESDAY;
                case "수", "수요일" -> DayOfWeek.WEDNESDAY;
                case "목", "목요일" -> DayOfWeek.THURSDAY;
                case "금", "금요일" -> DayOfWeek.FRIDAY;
                case "토", "토요일" -> DayOfWeek.SATURDAY;
                case "일", "일요일" -> DayOfWeek.SUNDAY;
                default -> null;
            };
        }
    }

    private String normalizeTime(String time) {
        if (time == null || time.isBlank()) {
            return "";
        }
        String clean = time.replaceAll("[^0-9:]", "");
        if (clean.length() == 4 && !clean.contains(":")) {
            clean = clean.substring(0, 2) + ":" + clean.substring(2);
        }
        if (clean.matches("^\\d{1,2}:\\d{2}$")) {
            String[] parts = clean.split(":");
            return String.format("%02d:%02d", Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        }
        return clean;
    }

    private List<TimeTableImageRecognizeResponseDto> matchWithDatabaseOfferings(
            Semester semester,
            List<ExtractedCourse> extractedCourses
    ) {
        List<CourseOffering> offerings = courseOfferingRepository.findAllBySemesterId(semester.getId());
        List<Long> offeringIds = offerings.stream().map(CourseOffering::getId).toList();

        Map<Long, List<CourseMeeting>> meetingsByOffering = courseMeetingRepository.findAllByCourseOfferingIdIn(offeringIds)
                .stream()
                .collect(Collectors.groupingBy(item -> item.getCourseOffering().getId()));

        Map<Long, List<CourseMeetingResponseDto>> mergedMeetingsMap = offerings.stream()
                .collect(Collectors.toMap(
                        CourseOffering::getId,
                        offering -> courseMeetingService.mergeContinuousMeetings(meetingsByOffering.getOrDefault(offering.getId(), List.of()))
                ));

        List<TimeTableImageRecognizeResponseDto> results = new ArrayList<>();

        for (ExtractedCourse extracted : extractedCourses) {
            List<ScoredOffering> scoredList = new ArrayList<>();

            for (CourseOffering offering : offerings) {
                int score = calculateMatchScore(extracted, offering, mergedMeetingsMap.get(offering.getId()));
                if (score >= 30) {
                    scoredList.add(new ScoredOffering(offering, score));
                }
            }

            scoredList.sort((a, b) -> Integer.compare(b.score(), a.score()));

            List<CourseOfferingResponseDto> candidates = scoredList.stream()
                    .limit(5)
                    .map(item -> CourseOfferingResponseDto.from(
                            item.offering(),
                            mergedMeetingsMap.getOrDefault(item.offering().getId(), List.of()),
                            true
                    ))
                    .toList();

            Long recommendedId = null;
            if (!scoredList.isEmpty()) {
                ScoredOffering top = scoredList.get(0);
                if (top.score() >= 50 || scoredList.size() == 1) {
                    recommendedId = top.offering().getId();
                }
            }

            results.add(new TimeTableImageRecognizeResponseDto(
                    extracted.title(),
                    extracted.professor(),
                    extracted.classroom(),
                    extracted.subjectNumber(),
                    extracted.meetings(),
                    candidates,
                    recommendedId
            ));
        }

        return results;
    }

    private static final Set<String> NON_COURSE_KEYWORDS = Set.of(
            "근로", "교내근로", "국가근로", "알바", "아르바이트", "봉사", "동아리", "스터디", "개인일정", "일정"
    );

    private int calculateMatchScore(
            ExtractedCourse extracted,
            CourseOffering offering,
            List<CourseMeetingResponseDto> meetings
    ) {
        // 1. 수강번호 / 학수번호 일치 여부 (최우선: 완벽 일치 시 즉시 1000점)
        if (extracted.subjectNumber() != null && !extracted.subjectNumber().isBlank()) {
            if (extracted.subjectNumber().equalsIgnoreCase(offering.getSubjectNumber())) {
                return 1000;
            }
        }

        String extractedTitleNorm = normalizeText(extracted.title());
        String offeringTitleNorm = normalizeText(offering.getCourse().getTitle());

        // 비정규 개인 일정 키워드(근로, 알바 등)는 교과목 DB와 매칭하지 않음
        if (NON_COURSE_KEYWORDS.contains(extractedTitleNorm)) {
            return 0;
        }

        // 2. 과목명 비교 (과목명 연관성이 없으면 시간표 시간대가 겹치더라도 매칭 절대 배제)
        int titleScore = 0;
        if (extractedTitleNorm.equals(offeringTitleNorm)) {
            titleScore = 100;
        } else if (offeringTitleNorm.contains(extractedTitleNorm) || extractedTitleNorm.contains(offeringTitleNorm)) {
            titleScore = 70;
        } else {
            double sim = calculateSimilarity(extractedTitleNorm, offeringTitleNorm);
            if (sim >= 0.45) {
                titleScore = (int) (sim * 60);
            }
        }

        // 과목명이 전혀 일치하지 않으면 후보 강좌로 절대 추천하지 않음
        if (titleScore == 0) {
            return 0;
        }

        int score = titleScore;

        // 3. 분반 힌트 판별 (예: 과목명이 "건축설비(2)"인 경우 -> offering.getSubjectNumber() 끝자리 "002" 분반 우대)
        String extractedDivision = extractDivisionHint(extracted.title());
        if (extractedDivision != null && offering.getSubjectNumber() != null) {
            String offeringSubject = offering.getSubjectNumber().trim();
            if (offeringSubject.length() >= 3) {
                String offeringDiv = offeringSubject.substring(offeringSubject.length() - 3).replaceAll("^0+", "");
                if (extractedDivision.equals(offeringDiv)) {
                    score += 40;
                }
            }
        }

        // 4. 교수명 비교
        if (extracted.professor() != null && !extracted.professor().isBlank()) {
            String extractedProfNorm = normalizeText(extracted.professor());
            String offeringProfNorm = normalizeText(offering.getProfessor());
            if (offeringProfNorm != null && !offeringProfNorm.isBlank()) {
                if (extractedProfNorm.equals(offeringProfNorm)) {
                    score += 35;
                } else if (offeringProfNorm.contains(extractedProfNorm) || extractedProfNorm.contains(offeringProfNorm)) {
                    score += 20;
                } else if (extractedProfNorm.length() >= 2 && offeringProfNorm.length() >= 2) {
                    score -= 10;
                }
            }
        }

        // 5. 강의실 비교
        if (extracted.classroom() != null && !extracted.classroom().isBlank()) {
            String extractedRoomNorm = normalizeText(extracted.classroom());
            if (meetings != null) {
                boolean roomMatched = meetings.stream().anyMatch(m -> {
                    String roomNorm = normalizeText(m.location());
                    return !roomNorm.isEmpty() && (roomNorm.contains(extractedRoomNorm) || extractedRoomNorm.contains(roomNorm));
                });
                if (roomMatched) {
                    score += 20;
                }
            }
        }

        // 6. 요일 및 시간대 비교 (동일 과목 내 올바른 분반을 고르기 위한 보조 가중치, 최대 40점)
        if (meetings != null && !meetings.isEmpty() && extracted.meetings() != null && !extracted.meetings().isEmpty()) {
            int meetingBonus = 0;
            for (TimeTableImageMeetingDto em : extracted.meetings()) {
                for (CourseMeetingResponseDto om : meetings) {
                    if (em.day() != null && em.day().name().equalsIgnoreCase(om.day().name())) {
                        meetingBonus += 10;
                        if (isTimeClose(em.startTime(), om.startTime()) && isTimeClose(em.endTime(), om.endTime())) {
                            meetingBonus += 15;
                        }
                    }
                }
            }
            score += Math.min(meetingBonus, 40);
        }

        return score;
    }

    private String extractDivisionHint(String title) {
        if (title == null) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\((\\d+)\\)").matcher(title);
        if (m.find()) {
            return m.group(1).replaceAll("^0+", "");
        }
        return null;
    }

    private boolean isTimeClose(String t1Str, LocalTime lt2) {
        if (t1Str == null || t1Str.isBlank() || lt2 == null) {
            return false;
        }
        try {
            LocalTime lt1 = LocalTime.parse(t1Str.length() == 5 ? t1Str : (t1Str + ":00").substring(0, 5));
            long diffMinutes = Math.abs(java.time.Duration.between(lt1, lt2).toMinutes());
            return diffMinutes <= 20;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private String normalizeText(String text) {
        if (text == null) return "";
        return text.replaceAll("[\\s\\[\\]\\(\\)\\-_,.]", "").toLowerCase();
    }

    private double calculateSimilarity(String s1, String s2) {
        if (s1.isEmpty() || s2.isEmpty()) return 0.0;
        int longer = Math.max(s1.length(), s2.length());
        int dist = editDistance(s1, s2);
        return (longer - dist) / (double) longer;
    }

    private int editDistance(String s1, String s2) {
        int[] costs = new int[s2.length() + 1];
        for (int j = 0; j <= s2.length(); j++) costs[j] = j;
        for (int i = 1; i <= s1.length(); i++) {
            costs[0] = i;
            int nw = i - 1;
            for (int j = 1; j <= s2.length(); j++) {
                int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]),
                        s1.charAt(i - 1) == s2.charAt(j - 1) ? nw : nw + 1);
                nw = costs[j];
                costs[j] = cj;
            }
        }
        return costs[s2.length()];
    }

    private record ExtractedCourse(
            String title,
            String professor,
            String classroom,
            String subjectNumber,
            List<TimeTableImageMeetingDto> meetings
    ) {}

    private record ScoredOffering(
            CourseOffering offering,
            int score
    ) {}
}
