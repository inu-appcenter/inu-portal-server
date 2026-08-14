package kr.inuappcenterportal.inuportal.domain.mockRegistration.controller;

import jakarta.validation.Valid;
import kr.inuappcenterportal.inuportal.domain.course.dto.courseOffering.CourseOfferingResponseDto;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.mockRegistration.dto.MockCourseRequest;
import kr.inuappcenterportal.inuportal.domain.mockRegistration.dto.TimetableImportResponseDto;
import kr.inuappcenterportal.inuportal.domain.mockRegistration.service.MockRegistrationService;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/mock-registration")
public class MockRegistrationController {
    private final MockRegistrationService service;

    @GetMapping("/watchlist")
    public ResponseEntity<ResponseDto<List<CourseOfferingResponseDto>>> getWatchlist(@AuthenticationPrincipal Member member) {
        return ResponseEntity.ok(ResponseDto.of(service.getWatchlist(member), "모의 장바구니 조회 성공"));
    }
    @PostMapping("/watchlist/items")
    public ResponseEntity<ResponseDto<Void>> addWatchlist(@AuthenticationPrincipal Member member, @Valid @RequestBody MockCourseRequest request) {
        service.addWatchlist(member, request.courseOfferingId());
        return ResponseEntity.ok(ResponseDto.of(null, "모의 장바구니 추가 성공"));
    }
    @DeleteMapping("/watchlist/items/{courseOfferingId}")
    public ResponseEntity<ResponseDto<Void>> removeWatchlist(@AuthenticationPrincipal Member member, @PathVariable Long courseOfferingId) {
        service.removeWatchlist(member, courseOfferingId);
        return ResponseEntity.ok(ResponseDto.of(null, "모의 장바구니 삭제 성공"));
    }
    @PostMapping("/watchlist/imports/primary-timetable")
    public ResponseEntity<ResponseDto<TimetableImportResponseDto>> importTimetable(@AuthenticationPrincipal Member member) {
        return ResponseEntity.ok(ResponseDto.of(service.importPrimaryTimetable(member), "대표 시간표 가져오기 성공"));
    }
    @GetMapping("/enrollments")
    public ResponseEntity<ResponseDto<List<CourseOfferingResponseDto>>> getEnrollments(@AuthenticationPrincipal Member member) {
        return ResponseEntity.ok(ResponseDto.of(service.getEnrollments(member), "모의 수강신청 조회 성공"));
    }
    @PostMapping("/enrollments")
    public ResponseEntity<ResponseDto<Void>> enroll(@AuthenticationPrincipal Member member, @Valid @RequestBody MockCourseRequest request) {
        service.enroll(member, request.courseOfferingId());
        return ResponseEntity.ok(ResponseDto.of(null, "모의 수강신청 성공"));
    }
    @DeleteMapping("/enrollments/{courseOfferingId}")
    public ResponseEntity<ResponseDto<Void>> cancel(@AuthenticationPrincipal Member member, @PathVariable Long courseOfferingId) {
        service.cancelEnrollment(member, courseOfferingId);
        return ResponseEntity.ok(ResponseDto.of(null, "모의 수강신청 취소 성공"));
    }
}
