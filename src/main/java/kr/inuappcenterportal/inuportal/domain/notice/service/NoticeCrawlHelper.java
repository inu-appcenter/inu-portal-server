package kr.inuappcenterportal.inuportal.domain.notice.service;

import kr.inuappcenterportal.inuportal.domain.notice.enums.Department;
import kr.inuappcenterportal.inuportal.domain.notice.model.DepartmentNotice;
import kr.inuappcenterportal.inuportal.domain.notice.model.Notice;
import kr.inuappcenterportal.inuportal.domain.notice.model.NoticeCreatedEvent;
import kr.inuappcenterportal.inuportal.domain.notice.repository.DepartmentNoticeRepository;
import kr.inuappcenterportal.inuportal.domain.notice.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
public class NoticeCrawlHelper {

    private final NoticeRepository noticeRepository;
    private final DepartmentNoticeRepository departmentNoticeRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean saveOrUpdateNotice(
            String categoryName,
            String subCategory,
            String title,
            String writer,
            String createDate,
            String link,
            String description,
            boolean shouldNotify
    ) {
        Optional<Notice> existingNotice = noticeRepository.findByUrl(link);
        if (existingNotice.isPresent()) {
            Notice notice = existingNotice.get();
            boolean isModified = !Objects.equals(notice.getSubCategory(), subCategory)
                    || !Objects.equals(notice.getTitle(), title)
                    || !Objects.equals(notice.getWriter(), writer)
                    || !Objects.equals(notice.getDescription(), description);

            notice.update(subCategory, title, writer, description);

            if (isModified) {
                notice.markContentPending();
                log.info("[학교공지] RSS 메타데이터 변경 감지 -> 본문 재크롤링 예약: [{}] {}", categoryName, title);
            }
            return false; // 기존 공지 업데이트
        } else {
            Notice notice = Notice.builder()
                    .category(categoryName)
                    .subCategory(subCategory)
                    .title(title)
                    .writer(writer)
                    .createDate(createDate)
                    .url(link)
                    .description(description)
                    .build();
            Notice savedNotice = noticeRepository.save(notice);

            log.info("[학교공지] 새 공지 발견 및 저장 완료: [{}] {}", categoryName, title);

            if (shouldNotify) {
                eventPublisher.publishEvent(new NoticeCreatedEvent(savedNotice));
            }
            return true; // 신규 공지 등록
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DepartmentNoticeSaveResult saveOrUpdateDepartmentNotice(
            Department department,
            String title,
            LocalDate date,
            long views,
            String link,
            Consumer<DepartmentNotice> contentSyncAction
    ) {
        Optional<DepartmentNotice> existingNotice = departmentNoticeRepository.findFirstByDepartmentAndUrl(department, link);
        if (existingNotice.isEmpty()) {
            existingNotice = departmentNoticeRepository.findFirstByDepartmentAndTitleAndCreateDate(department, title, date);
        }

        DepartmentNotice departmentNotice;
        boolean isNew = false;

        if (existingNotice.isPresent()) {
            departmentNotice = existingNotice.get();
            departmentNotice.updateListing(title, date, views, link);
        } else {
            departmentNotice = departmentNoticeRepository.save(
                    DepartmentNotice.create(department, title, date, views, link)
            );
            isNew = true;
        }

        // 트랜잭션 내에서 본문 동기화 등 추가 액션 실행
        contentSyncAction.accept(departmentNotice);

        return new DepartmentNoticeSaveResult(departmentNotice, isNew);
    }

    @lombok.Getter
    @RequiredArgsConstructor
    public static class DepartmentNoticeSaveResult {
        private final DepartmentNotice departmentNotice;
        private final boolean isNew;
    }
}
