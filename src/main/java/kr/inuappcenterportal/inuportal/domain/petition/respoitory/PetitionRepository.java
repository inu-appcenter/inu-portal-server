package kr.inuappcenterportal.inuportal.domain.petition.respoitory;

import jakarta.persistence.LockModeType;
import kr.inuappcenterportal.inuportal.domain.petition.model.Petition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PetitionRepository extends JpaRepository<Petition,Long> {
    Optional<Petition> findByIdAndIsDeletedFalse(Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Petition p WHERE p.id =:id AND p.isDeleted = false")
    Optional<Petition> findByIdWithLock(Long id);

    @Query("SELECT p FROM Petition p JOIN FETCH p.member WHERE p.isDeleted = false")
    Page<Petition> findAllWithMember(Pageable pageable);
    @Query("SELECT p FROM Petition p JOIN FETCH p.member WHERE p.id = :id AND p.isDeleted = false")
    Optional<Petition> findByIdWithMember(Long id);
}
