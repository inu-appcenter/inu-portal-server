package kr.inuappcenterportal.inuportal.domain.reservation.implement;

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

    public ReservationDetail get(Long reservationId) {
        return ReservationDetail.from(reservationRepository.findById(reservationId).orElseThrow(() -> new MyException(MyErrorCode.REPLY_NOT_FOUND)));
    }

    public ListResponseDto<ReservationPreview> getListByItemId(Long itemId, int page) {
        Page<Reservation> reservations = reservationRepository.findListByItemId(itemId, PageRequest.of(--page, 8, Sort.by(Sort.Direction.DESC, "id")));
        List<ReservationPreview> list = reservations.stream().map(ReservationPreview::from).toList();
        long total = reservations.getTotalElements();
        long pages = reservations.getTotalPages();
        return ListResponseDto.of(pages, total, list);
    }

    public ListResponseDto<ReservationPreview> getList( int page) {
        Page<Reservation> reservations = reservationRepository.findAll(PageRequest.of(--page, 8, Sort.by(Sort.Direction.DESC, "id")));
        List<ReservationPreview> list = reservations.stream().map(ReservationPreview::from).toList();
        long total = reservations.getTotalElements();
        long pages = reservations.getTotalPages();
        return ListResponseDto.of(pages, total, list);
    }


}
