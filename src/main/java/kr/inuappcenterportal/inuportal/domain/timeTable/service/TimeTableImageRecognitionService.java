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
            당신은 대학교 시간표 이미지를 분석하여 강의 목록을 정밀하게 추출하는 시각 분석 AI 전문가입니다.
            제공된 시간표 이미지를 분석하여 모든 강의 블록의 정보를 JSON 규격에 맞추어 정확하게 추출하세요.

            [시간표 구조 분석 규칙]
            1. 상단 가로축: 일반적으로 요일(월, 화, 수, 목, 금, 토)을 나타냅니다.
            2. 좌측 세로축: 시간(09:00, 10:00, 10:30 등) 또는 교시(1교시, 2교시 등)를 나타냅니다.
            3. 강의 셀(블록): 보통 [과목명], [교수명], [강의실] 순서로 여러 줄에 걸쳐 적혀 있습니다.
               - 과목명: 셀의 가장 대표적인 타이틀입니다.
               - 교수명: 사람이름 (예: 홍길동, 김철수 등). 없으면 빈 문자열("")로 설정하세요.
               - 강의실: 건물/호수 (예: "7-302", "정201", "16호관 203호" 등). 없으면 빈 문자열("")로 설정하세요.
               - 수강번호/학수번호: [12345]나 영숫자 조합 코드가 적혀있다면 subjectNumber에 채우세요. 없으면 빈 문자열("")로 설정하세요.
            4. 동일 과목 통합: 동일한 과목이 여러 요일 또는 시간대로 나뉘어 있다면(예: 월 10:00~11:15, 수 10:00~11:15), 하나의 과목 항목 아래 meetings 배열에 각 시간대를 담으세요.
            5. 요일(day) 표기: MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY 중 하나로 정확히 매핑하세요.
            6. 시간(startTime, endTime) 표기: 반드시 24시간 형식 "HH:mm" (예: "09:00", "10:15", "13:30", "15:00")으로 기재하세요.

            [출력 형식 제한]
            반드시 설명이나 인사말, Markdown 코드 블록(```json 등) 없이, 오직 아래와 같은 순수 JSON 배열만 출력하세요:
            [
              {
                "title": "자료구조",
                "professor": "홍길동",
                "classroom": "7-302",
                "subjectNumber": "",
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
                if (top.score() >= 50) {
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

    private int calculateMatchScore(
            ExtractedCourse extracted,
            CourseOffering offering,
            List<CourseMeetingResponseDto> meetings
    ) {
        int score = 0;

        // 1. 수강번호 / 학수번호 일치 여부 (최우선)
        if (extracted.subjectNumber() != null && !extracted.subjectNumber().isBlank()) {
            if (extracted.subjectNumber().equalsIgnoreCase(offering.getSubjectNumber())) {
                return 1000;
            }
        }

        String extractedTitleNorm = normalizeText(extracted.title());
        String offeringTitleNorm = normalizeText(offering.getCourse().getTitle());

        // 2. 과목명 비교
        if (extractedTitleNorm.equals(offeringTitleNorm)) {
            score += 60;
        } else if (offeringTitleNorm.contains(extractedTitleNorm) || extractedTitleNorm.contains(offeringTitleNorm)) {
            score += 35;
        } else {
            double sim = calculateSimilarity(extractedTitleNorm, offeringTitleNorm);
            if (sim >= 0.7) {
                score += (int) (sim * 40);
            }
        }

        // 3. 교수명 비교
        if (extracted.professor() != null && !extracted.professor().isBlank()) {
            String extractedProfNorm = normalizeText(extracted.professor());
            String offeringProfNorm = normalizeText(offering.getProfessor());
            if (offeringProfNorm != null && !offeringProfNorm.isBlank()) {
                if (extractedProfNorm.equals(offeringProfNorm)) {
                    score += 30;
                } else if (offeringProfNorm.contains(extractedProfNorm) || extractedProfNorm.contains(offeringProfNorm)) {
                    score += 15;
                } else if (score > 40) {
                    // 과목명은 일치하나 교수명이 완전히 다르면 감점 (다른 분반일 가능성)
                    score -= 15;
                }
            }
        }

        // 4. 요일 및 시간대 비교
        if (meetings != null && !meetings.isEmpty() && extracted.meetings() != null && !extracted.meetings().isEmpty()) {
            for (TimeTableImageMeetingDto em : extracted.meetings()) {
                for (CourseMeetingResponseDto om : meetings) {
                    if (em.day() != null && em.day().name().equalsIgnoreCase(om.day().name())) {
                        score += 15;
                        if (isTimeClose(em.startTime(), om.startTime()) && isTimeClose(em.endTime(), om.endTime())) {
                            score += 25;
                        }
                    }
                }
            }
        }

        return score;
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
