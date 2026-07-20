package kr.inuappcenterportal.inuportal.domain.notice.service;

import kr.inuappcenterportal.inuportal.domain.notice.model.Notice;
import kr.inuappcenterportal.inuportal.domain.notice.model.NoticeCreatedEvent;
import kr.inuappcenterportal.inuportal.domain.notice.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class NoticeCrawlHelper {

    private final NoticeRepository noticeRepository;
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
            notice.update(subCategory, title, writer, description);
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
}
