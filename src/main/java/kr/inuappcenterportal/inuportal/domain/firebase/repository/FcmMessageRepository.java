package kr.inuappcenterportal.inuportal.domain.firebase.repository;


import kr.inuappcenterportal.inuportal.domain.firebase.model.FcmMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FcmMessageRepository extends JpaRepository<FcmMessage,Long> {
}
