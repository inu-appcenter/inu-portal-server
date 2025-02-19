package kr.inuappcenterportal.inuportal.domain.reservation.model;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.domain.reservation.enums.ReservationStatus;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "reservation")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_id")
    private Long itemId;

    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "start_date_time")
    private LocalDateTime startDateTime;

    @Column(name = "end_date_time")
    private LocalDateTime endDateTime;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "reservation_status")
    @Enumerated(value = EnumType.STRING)
    private ReservationStatus reservationStatus;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Builder
    public Reservation(Long itemId, Long memberId, LocalDateTime startDateTime, LocalDateTime endDateTime, LocalDateTime createdAt, ReservationStatus reservationStatus, String phoneNumber) {
        this.itemId = itemId;
        this.memberId = memberId;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.createdAt = createdAt;
        this.reservationStatus = reservationStatus;
        this.phoneNumber = phoneNumber;
    }

    public static Reservation create(Long itemId, Long memberId, LocalDateTime startDateTime, LocalDateTime endDateTime, String phoneNumber) {
        return Reservation.builder()
                .itemId(itemId)
                .memberId(memberId)
                .startDateTime(startDateTime)
                .endDateTime(endDateTime)
                .createdAt(LocalDateTime.now())
                .reservationStatus(ReservationStatus.PENDING)
                .phoneNumber(phoneNumber)
                .build();
    }

    public void confirmOrReject(String status) {
        if (ReservationStatus.CONFIRM.name().equals(status)) this.reservationStatus = ReservationStatus.CONFIRM;
        else if(ReservationStatus.CANCELED.name().equals(status)) this.reservationStatus = ReservationStatus.CANCELED;
        else throw new MyException(MyErrorCode.WRONG_RESERVATION_STATUS);
    }
}
