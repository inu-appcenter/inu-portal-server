package kr.inuappcenterportal.inuportal.domain.reservation.implement;

import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.repository.MemberRepository;
import kr.inuappcenterportal.inuportal.domain.reservation.dto.ReservationDetail;
import kr.inuappcenterportal.inuportal.domain.reservation.dto.ReservationPreview;
import kr.inuappcenterportal.inuportal.domain.reservation.model.Reservation;
import kr.inuappcenterportal.inuportal.domain.reservation.repository.ReservationRepository;
import kr.inuappcenterportal.inuportal.global.dto.ListResponseDto;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ReservationReader {

    private final ReservationRepository reservationRepository;
    private final MemberRepository memberRepository;

    public ReservationDetail getDetail(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId).orElseThrow(() -> new MyException(MyErrorCode.RESERVATION_NOT_FOUND));
        Member member = memberRepository.findById(reservation.getMemberId()).orElseThrow(() -> new MyException(MyErrorCode.RESERVATION_NOT_FOUND));
        return ReservationDetail.from(reservation, member.getStudentId());
    }

    public ListResponseDto<ReservationPreview> getListByItemId(Long itemId, int page) {
        Page<Reservation> reservations = reservationRepository.findListByItemId(itemId, PageRequest.of(--page, 8, Sort.by(Sort.Direction.DESC, "id")));
        List<ReservationPreview> list = reservations.stream()
                .map(reservation -> {
                    String studentId = memberRepository.findById(reservation.getMemberId())
                            .map(Member::getStudentId)
                            .orElse(null);
                    return ReservationPreview.from(reservation, studentId);
                })
                .toList();
        long total = reservations.getTotalElements();
        long pages = reservations.getTotalPages();
        return ListResponseDto.of(pages, total, list);
    }

    public ListResponseDto<ReservationPreview> getList(int page, Long memberId) {
        Page<Reservation> reservations = reservationRepository.findAllByMemberId(PageRequest.of(--page, 8, Sort.by(Sort.Direction.DESC, "id")), memberId);
        String studentId = memberRepository.findById(memberId)
                .map(Member::getStudentId)
                .orElse(null);
        List<ReservationPreview> list = reservations.stream()
                .map(reservation -> ReservationPreview.from(reservation, studentId))
                .toList();
        long total = reservations.getTotalElements();
        long pages = reservations.getTotalPages();
        return ListResponseDto.of(pages, total, list);
    }

    public Reservation get(Long reservationId) {
        return reservationRepository.findById(reservationId).orElseThrow(() -> new MyException(MyErrorCode.RESERVATION_NOT_FOUND));
    }


}
