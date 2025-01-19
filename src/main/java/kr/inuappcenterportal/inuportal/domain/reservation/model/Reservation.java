package kr.inuappcenterportal.inuportal.domain.reservation.model;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.domain.reservation.enums.ReservationStatus;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long itemId;

    private Long memberId;

    private LocalDateTime startDateTime;

    private LocalDateTime endDateTime;

    private LocalDateTime createdAt;

    @Enumerated(value = EnumType.STRING)
    private ReservationStatus reservationStatus;

    @Builder
    public Reservation(Long itemId, Long memberId, LocalDateTime startDateTime, LocalDateTime endDateTime, LocalDateTime createdAt, ReservationStatus reservationStatus) {
        this.itemId = itemId;
        this.memberId = memberId;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.createdAt = createdAt;
        this.reservationStatus = reservationStatus;
    }

    public static Reservation create(Long itemId, Long memberId, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        return Reservation.builder()
                .itemId(itemId)
                .memberId(memberId)
                .startDateTime(startDateTime)
                .endDateTime(endDateTime)
                .createdAt(LocalDateTime.now())
                .reservationStatus(ReservationStatus.CONFIRM)
                .build();
    }
}
