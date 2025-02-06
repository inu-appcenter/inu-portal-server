package kr.inuappcenterportal.inuportal.domain.reservation.repository;

import kr.inuappcenterportal.inuportal.domain.reservation.model.Reservation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;


public interface ReservationRepository extends JpaRepository<Reservation ,Long> {

    boolean existsByMemberId(Long memberId);

    Page<Reservation> findListByItemId(Long itemId, Pageable pageable);

    void deleteByItemId(Long itemId);

    Reservation findByItemId(Long itmeId);

    Page<Reservation> findAllByMemberId(Pageable pageable, Long memberId);
}
