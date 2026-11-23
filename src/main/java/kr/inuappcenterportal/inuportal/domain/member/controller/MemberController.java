package kr.inuappcenterportal.inuportal.domain.member.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import kr.inuappcenterportal.inuportal.domain.member.dto.LoginDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.MemberResponseDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.MemberUpdateNicknameDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.MemberProfileResponseDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.TokenDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.SchoolDepartmentUpdateRequestDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.NearbyVisibilityRequestDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.LocationUpdateRequestDto;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.member.service.MemberService;
import kr.inuappcenterportal.inuportal.domain.member.service.MemberWithdrawalService;
import kr.inuappcenterportal.inuportal.domain.department.enums.Department;
import kr.inuappcenterportal.inuportal.domain.post.dto.PostListResponseDto;
import kr.inuappcenterportal.inuportal.domain.post.service.PostService;
import kr.inuappcenterportal.inuportal.domain.reply.dto.ReplyListResponseDto;
import kr.inuappcenterportal.inuportal.domain.reply.service.ReplyService;
import kr.inuappcenterportal.inuportal.global.dto.ListResponseDto;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberController implements MemberApiSpecification {
    private final MemberService memberService;
    private final MemberWithdrawalService memberWithdrawalService;
    private final PostService postService;
    private final ReplyService replyService;

    @PutMapping("")
    public ResponseEntity<ResponseDto<Long>> updateNicknameFireId(@Valid@RequestBody MemberUpdateNicknameDto memberUpdateNicknameDto, @AuthenticationPrincipal Member member){
        log.info("회원 닉네임 변경 호출 id:{}",member.getId());
        Long memberId = memberService.updateMemberNicknameFireId(member.getId(), memberUpdateNicknameDto);
        return ResponseEntity.ok(ResponseDto.of(memberId,"회원 닉네임/횃불이 이미지 변경 성공"));
    }

    @DeleteMapping("")
    public ResponseEntity<ResponseDto<Long>> withdraw(@AuthenticationPrincipal Member member){
        log.info("회원 탈퇴 호출 id:{}",member.getId());
        Long id = member.getId();
        memberWithdrawalService.withdraw(id);
        return ResponseEntity.ok(ResponseDto.of(id,"회원탈퇴 성공"));
    }

    @PostMapping("/login")
    public ResponseEntity<ResponseDto<TokenDto>> login(@Valid @RequestBody LoginDto loginDto){
        //log.info("로그인 호출");
        return ResponseEntity.ok(ResponseDto.of(memberService.schoolLogin(loginDto),"로그인 성공, 토근이 발급되었습니다."));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ResponseDto<TokenDto>> refresh(HttpServletRequest httpServletRequest){
        //log.info("토큰 재발급 호출");
        return ResponseEntity.ok(ResponseDto.of(memberService.refreshToken(httpServletRequest.getHeader("refresh")),"토큰 재발급 성공"));
    }

    @GetMapping("")
    public ResponseEntity<ResponseDto<MemberResponseDto>> getMember(@AuthenticationPrincipal Member member){
        //log.info("회원 가져오기 호출 id:{}",member.getId());
        return ResponseEntity.ok(ResponseDto.of(memberService.getCurrentMember(member),"회원 가져오기 성공"));
    }

    @GetMapping("/all")
    public ResponseEntity<ResponseDto<List<MemberResponseDto>>> getAllMember(){
        return ResponseEntity.ok(ResponseDto.of(memberService.getAllMember(),"모든 회원 가져오기 성공"));
    }



    @GetMapping("/posts")
    public ResponseEntity<ResponseDto<List<PostListResponseDto>>> getAllPost(@AuthenticationPrincipal Member member, @RequestParam(required = false,defaultValue = "date") String sort){
        //log.info("회원이 작성한 모든 글 가져오기 호출 id:{}",member.getId());
        return ResponseEntity.ok(ResponseDto.of(postService.getPostByMember(member,sort),"회원이 작성한 모든 게시글 가져오기 성공"));
    }

    @GetMapping("/scraps")
    public ResponseEntity<ResponseDto<ListResponseDto>> getAllScrap(@AuthenticationPrincipal Member member, @RequestParam(required = false,defaultValue = "date") String sort
    ,@RequestParam(required = false,defaultValue = "1") @Min(1) int page){
        //log.info("회원이 스크랩한 모든 글 가져오기 호출 id:{}",member.getId());
        return ResponseEntity.ok(ResponseDto.of(postService.getScrapsByMember(member,sort,page),"회원이 스크랩한 모든 게시글 가져오기 성공"));
    }

    @GetMapping("/likes")
    public ResponseEntity<ResponseDto<List<PostListResponseDto>>> getAllLike(@AuthenticationPrincipal Member member, @RequestParam(required = false,defaultValue = "date") String sort){
        //log.info("회원이 좋아요한 모든 글 가져오기 호출 id:{}",member.getId());
        return ResponseEntity.ok(ResponseDto.of(postService.getLikeByMember(member,sort),"회원이 좋아요한 모든 게시글 가져오기 성공"));
    }

    @GetMapping("/replies")
    public ResponseEntity<ResponseDto<List<ReplyListResponseDto>>> getAllReply(@AuthenticationPrincipal Member member, @RequestParam(required = false,defaultValue = "date") String sort){
        //log.info("회원이 작성한 모든 댓글 호출 id:{}",member.getId());
        return ResponseEntity.ok(ResponseDto.of(replyService.getReplyByMember(member,sort),"회원이 작성한 모든 댓글 가져오기 성공"));
    }

    @PutMapping("/department")
    public ResponseEntity<ResponseDto<MemberResponseDto>> getMember(@AuthenticationPrincipal Member member,
                                                                    @RequestParam Department department){
        return ResponseEntity.ok(ResponseDto.of(memberService.updateMemberDepartment(member.getId(), department),"회원 학과 수정 성공"));
    }

    @PatchMapping("/school-department")
    public ResponseEntity<ResponseDto<MemberResponseDto>> updateSchoolDepartment(
            @AuthenticationPrincipal Member member,
            @Valid @RequestBody SchoolDepartmentUpdateRequestDto request
    ) {
        return ResponseEntity.ok(ResponseDto.of(
                memberService.updateSchoolDepartment(member.getId(), request.departmentCode()),
                "회원 학교 학과 수정 성공"));
    }

    @PutMapping("/terms")
    public ResponseEntity<ResponseDto<MemberResponseDto>> agreeTerms(@AuthenticationPrincipal Member member){
        return ResponseEntity.ok(ResponseDto.of(memberService.agreeTerms(member.getId()),"회원 약관 동의 성공"));
    }

    @PatchMapping("/push-setting/chat")
    public ResponseEntity<ResponseDto<Boolean>> toggleChatPush(@AuthenticationPrincipal Member member) {
        boolean isEnabled = memberService.toggleChatPush(member.getId());
        return ResponseEntity.ok(ResponseDto.of(isEnabled, "전체 채팅 알림 설정이 " + (isEnabled ? "켜졌습니다" : "꺼졌습니다")));
    }

    @PatchMapping("/nearby-visibility")
    public ResponseEntity<ResponseDto<Void>> updateNearbyVisibility(
            @AuthenticationPrincipal Member member,
            @Valid @RequestBody NearbyVisibilityRequestDto requestDto) {
        memberService.updateNearbyVisibility(member.getId(), requestDto.getEnabled());
        return ResponseEntity.ok(ResponseDto.of(null, "주변 친구 찾기 노출 설정이 변경되었습니다."));
    }

    @PutMapping("/location")
    public ResponseEntity<ResponseDto<Void>> updateLocation(
            @AuthenticationPrincipal Member member,
            @RequestBody LocationUpdateRequestDto requestDto) {
        memberService.updateLocation(member.getId(), requestDto);
        return ResponseEntity.ok(ResponseDto.of(null, "위치 정보가 갱신되었습니다."));
    }
}
