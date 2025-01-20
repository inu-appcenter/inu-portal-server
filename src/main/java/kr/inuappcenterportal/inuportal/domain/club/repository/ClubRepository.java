package kr.inuappcenterportal.inuportal.domain.club.repository;

import kr.inuappcenterportal.inuportal.domain.club.model.Club;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClubRepository extends JpaRepository<Club,Long> {
    List<Club> findByCategory(String category);
}
