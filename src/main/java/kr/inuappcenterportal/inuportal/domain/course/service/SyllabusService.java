package kr.inuappcenterportal.inuportal.domain.course.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.inuappcenterportal.inuportal.domain.course.dto.SyllabusContent;
import kr.inuappcenterportal.inuportal.domain.course.model.CourseOffering;
import kr.inuappcenterportal.inuportal.domain.course.model.Syllabus;
import kr.inuappcenterportal.inuportal.domain.course.repository.CourseOfferingRepository;
import kr.inuappcenterportal.inuportal.domain.course.repository.SyllabusRepository;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterTerm;
import kr.inuappcenterportal.inuportal.domain.semester.model.Semester;
import kr.inuappcenterportal.inuportal.domain.semester.repository.SemesterRepository;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class SyllabusService {

    private final SyllabusRepository syllabusRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final SemesterRepository semesterRepository;
    private final ObjectMapper objectMapper;
    private final PlatformTransactionManager transactionManager;

    public void importFromJson(MultipartFile file) {
        List<SyllabusContent> contents = parse(file);

        // 엑셀 임포터와 동일: 항목별 REQUIRES_NEW → 하나 실패해도 전체 롤백 안 됨
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        int success = 0, skipped = 0;
        for (SyllabusContent content : contents) {
            try {
                tx.executeWithoutResult(status -> upsert(content));
                success++;
            } catch (Exception e) {
                skipped++;
                log.warn("강의계획서 적재 스킵. year={}, term={}, subjectCode={}, reason={}",
                        content.year(), content.semester(), content.subjectCode(), e.getMessage());
            }
        }
        log.info("강의계획서 적재 완료. total={}, success={}, skipped={}", contents.size(), success, skipped);
    }

    private List<SyllabusContent> parse(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            return objectMapper.readValue(is, new TypeReference<List<SyllabusContent>>() {
            });
        } catch (IOException e) {
            throw new MyException(MyErrorCode.INVALID_INPUT);
        }
    }

    private void upsert(SyllabusContent content) {
        SemesterTerm term = parseTerm(content.semester());          // "2학기" -> SECOND
        Semester semester = semesterRepository.findByYearAndTerm(content.year(), term)
                .orElseThrow(() -> new MyException(MyErrorCode.SEMESTER_NOT_FOUND));

        CourseOffering offering = courseOfferingRepository
                .findBySemesterIdAndSubjectNumber(semester.getId(), content.subjectCode())
                .orElseThrow(() -> new MyException(MyErrorCode.COURSE_NOT_FOUND));

        syllabusRepository.findByCourseOfferingId(offering.getId())
                .ifPresentOrElse(
                        existing -> existing.updateContent(content),
                        () -> syllabusRepository.save(Syllabus.create(offering, content))
                );
    }

    private SemesterTerm parseTerm(String value) {
        if (value == null || value.isBlank()) {
            throw new MyException(MyErrorCode.INVALID_INPUT);
        }
        return switch (value.trim()) {
            case "1학기" -> SemesterTerm.FIRST;
            case "2학기" -> SemesterTerm.SECOND;
            case "여름계절학기" -> SemesterTerm.SUMMER;
            case "겨울계절학기" -> SemesterTerm.WINTER;
            default -> throw new MyException(MyErrorCode.INVALID_INPUT);
        };
    }
}
