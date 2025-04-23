package kr.inuappcenterportal.inuportal.domain.firebase.repository;


import kr.inuappcenterportal.inuportal.domain.firebase.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message,Long> {
}
