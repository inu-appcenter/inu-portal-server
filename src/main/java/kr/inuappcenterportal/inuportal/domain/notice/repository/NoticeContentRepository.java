package kr.inuappcenterportal.inuportal.domain.notice.repository;

import kr.inuappcenterportal.inuportal.domain.notice.model.NoticeContent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeContentRepository extends JpaRepository<NoticeContent, Long> {
}
