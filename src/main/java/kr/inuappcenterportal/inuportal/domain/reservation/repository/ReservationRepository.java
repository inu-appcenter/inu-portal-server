package kr.inuappcenterportal.inuportal.domain.reservation.repository;

import kr.inuappcenterportal.inuportal.domain.reservation.model.Reservation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;


import java.time.LocalDateTime;
import java.util.List;


public interface ReservationRepository extends JpaRepository<Reservation ,Long> {

    boolean existsByMemberId(Long memberId);

    Page<Reservation> findListByItemId(Long itemId, Pageable pageable);

    void deleteByItemId(Long itemId);

    Page<Reservation> findAllByMemberId(Pageable pageable, Long memberId);

    List<Reservation> findByEndDateTimeBefore(LocalDateTime endDateTime);
}
