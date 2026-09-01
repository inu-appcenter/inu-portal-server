package kr.inuappcenterportal.inuportal.domain.suggestion.repository;

import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.suggestion.model.Suggestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SuggestionRepository extends JpaRepository<Suggestion, Long> {

    Optional<Suggestion> findByIdAndIsDeletedFalse(Long id);

    @Query("SELECT s FROM Suggestion s JOIN FETCH s.member WHERE s.id = :id AND s.isDeleted = false")
    Optional<Suggestion> findByIdWithMember(@Param("id") Long id);

    @Query("SELECT s FROM Suggestion s JOIN FETCH s.member WHERE s.isDeleted = false")
    Page<Suggestion> findAllWithMember(Pageable pageable);

    Page<Suggestion> findAllByMemberAndIsDeletedFalse(Member member, Pageable pageable);
}
