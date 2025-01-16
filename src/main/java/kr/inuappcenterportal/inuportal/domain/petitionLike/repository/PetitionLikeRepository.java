package kr.inuappcenterportal.inuportal.domain.petitionLike.repository;

import kr.inuappcenterportal.inuportal.domain.petitionLike.model.PetitionLike;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.petition.model.Petition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PetitionLikeRepository extends JpaRepository<PetitionLike, Long> {
    boolean existsByMemberAndPetition(Member member, Petition petition);
    Optional<PetitionLike> findPetitionLikeByMemberAndPetition(Member member, Petition petition);
}
