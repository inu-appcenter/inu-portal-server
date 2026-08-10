package kr.inuappcenterportal.inuportal.domain.member.controller;

import jakarta.validation.Valid;
import kr.inuappcenterportal.inuportal.domain.member.dto.GradeRecordResponseDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.GradeRecordSaveRequestDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.GradeRecordUpdateRequestDto;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.service.GradeRecordService;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterTerm;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/grades")
public class GradeController {

    private final GradeRecordService gradeRecordService;

    @GetMapping("/all")
    public ResponseEntity<ResponseDto<List<GradeRecordResponseDto>>> getAllGradeRecord(
            @AuthenticationPrincipal Member member
    ) {
        List<GradeRecordResponseDto> response = gradeRecordService.getAllGradeRecord(member.getId());

        return ResponseEntity.ok(ResponseDto.of(response, "내 성적 전체 조회 성공"));
    }


    /**
     * 특정 학기의 내 전체 성적 조회 컨트롤러
     */
    @GetMapping
    public ResponseEntity<ResponseDto<List<GradeRecordResponseDto>>> getGradeRecord(
            @AuthenticationPrincipal Member member,
            @RequestParam Integer year,
            @RequestParam SemesterTerm term

    ) {
        List<GradeRecordResponseDto> grades = gradeRecordService.getGradeRecord(member.getId(), year, term);
        return ResponseEntity.ok(ResponseDto.of(grades, year + "/" + term + " 내 성적 조회 성공"));
    }


    /**
     * 성적 저장 및 업데이트 컨트롤러
     */
    @PutMapping
    public ResponseEntity<ResponseDto<List<GradeRecordResponseDto>>> upsertGradeRecord(
            @AuthenticationPrincipal Member member,
            @Valid @RequestBody GradeRecordSaveRequestDto request
    ) {
        List<GradeRecordResponseDto> response = gradeRecordService.replaceGradeRecord(request, member.getId());

        return ResponseEntity.ok(ResponseDto.of(response, "내 성적 저장 성공"));
    }

    /**
     * 개별 성적 수정 컨트롤러
     */
    @PatchMapping("/{gradeRecordId}")
    public ResponseEntity<ResponseDto<GradeRecordResponseDto>> updateGradeRecord(
            @AuthenticationPrincipal Member member,
            @PathVariable Long gradeRecordId,
            @Valid @RequestBody GradeRecordUpdateRequestDto request
    ) {
        GradeRecordResponseDto updatedGradeRecord =
                gradeRecordService.updateGradeRecord(member.getId(), gradeRecordId, request);

        return ResponseEntity.ok(ResponseDto.of(updatedGradeRecord, "내 성적 저장 및 수정 성공"));
    }


    /**
     * 특정 학기의 전체 성적 삭제 컨트롤러
     */
    @DeleteMapping
    public ResponseEntity<ResponseDto<Void>> deleteAllGradeRecord(
            @AuthenticationPrincipal Member member,
            @RequestParam Integer year,
            @RequestParam SemesterTerm term
    ) {
        gradeRecordService.deleteAllGradeRecord(member.getId(), year, term);

        return ResponseEntity.ok(ResponseDto.of(null, year + "/" + term + " 전체 성적 삭제 성공"));
    }


    /**
     * 특정 성적 삭제 컨트롤러
     */
    @DeleteMapping("/{gradeRecordId}")
    public ResponseEntity<ResponseDto<Void>> deleteGradeRecord(
            @AuthenticationPrincipal Member member,
            @PathVariable Long gradeRecordId
    ) {
        gradeRecordService.deleteGradeRecord(member.getId(), gradeRecordId);

        return ResponseEntity.ok(ResponseDto.of(null, "성적 삭제 성공"));
    }
}
