package kr.inuappcenterportal.inuportal.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import kr.inuappcenterportal.inuportal.domain.Member;
import kr.inuappcenterportal.inuportal.dto.*;
import kr.inuappcenterportal.inuportal.service.MemberService;
import kr.inuappcenterportal.inuportal.service.PostService;
import kr.inuappcenterportal.inuportal.service.ReplyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name="Members", description = "회원 API")
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@RequestMapping("/api/members")
public class MemberController {
    private final MemberService memberService;
    private final PostService postService;
    private final ReplyService replyService;

    @Operation(summary = "회원 닉네임/횃불이 이미지 변경",description = "url 헤더에 Auth 토큰,바디에 {nickname,fireId(횃불이 이미지 번호)}을 json 형식으로 보내주세요.성공 시 수정된 회원의 데이터베이스 아이디 값이 {data: id}으로 보내집니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "회원 닉네임/횃불이 이미지 변경 성공",content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "404",description = "존재하지 않는 회원입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "400",description = "입력한 닉네임과 현재 닉네임이 동일합니다. / 동일한 닉네임이 존재합니다. / 닉네임, 횃불이 아이디 모두 공백입니다. / 닉네임이 빈칸 혹은 공백입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PutMapping("")
    public ResponseEntity<ResponseDto<Long>> updateNicknameFireId(@Valid@RequestBody MemberUpdateNicknameDto memberUpdateNicknameDto, @AuthenticationPrincipal Member member){
        log.info("회원 닉네임 변경 호출 id:{}",member.getId());
        Long memberId = memberService.updateMemberNicknameFireId(member.getId(), memberUpdateNicknameDto);
        return ResponseEntity.ok(ResponseDto.of(memberId,"회원 닉네임/횃불이 이미지 변경 성공"));
    }

    @Operation(summary = "회원 삭제",description = "url 헤더에 Auth 토큰을 담아 보내주세요. 성공 시 삭제한 회원의 데이터베이스 아이디 값이 {data: id}으로 보내집니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "회원삭제성공",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 회원입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @DeleteMapping("")
    public ResponseEntity<ResponseDto<Long>> delete(@AuthenticationPrincipal Member member){
        log.info("회원 탈퇴 호출 id:{}",member.getId());
        Long id = member.getId();
        memberService.delete(member);
        return ResponseEntity.ok(ResponseDto.of(id,"회원삭제성공"));
    }

    @Operation(summary = "로그인",description = "바디에 {studentId,password}을 json 형식으로 보내주세요. 토큰 유효시간은 2시간, 리프레시 토큰의 유효시간은 1일입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "로그인 성공, 토근이 발급되었습니다.",content = @Content(schema = @Schema(implementation = TokenDto.class))),
            @ApiResponse(responseCode = "401",description = "학번 또는 비밀번호가 틀립니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class))),
    })
    @PostMapping("/login")
    public ResponseEntity<ResponseDto<TokenDto>> login(@Valid @RequestBody LoginDto loginDto){
        log.info("로그인 호출");
        return ResponseEntity.ok(ResponseDto.of(memberService.schoolLogin(loginDto),"로그인 성공, 토근이 발급되었습니다."));
    }

    @Operation(summary = "토큰 재발급",description = "헤더에 refresh 토큰을 보내주세요. 토큰 유효시간은 2시간, 리프레시 토큰의 유효시간은 1일입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "토큰 재발급 성공",content = @Content(schema = @Schema(implementation = TokenDto.class)))
            ,@ApiResponse(responseCode = "401",description = "만료된 토큰입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PostMapping("/refresh")
    public ResponseEntity<ResponseDto<TokenDto>> refresh(HttpServletRequest httpServletRequest){
        log.info("토큰 재발급 호출");
        return ResponseEntity.ok(ResponseDto.of(memberService.refreshToken(httpServletRequest.getHeader("refresh")),"토큰 재발급 성공"));
    }

    @Operation(summary = "회원 가져오기",description = "url 헤더에 Auth 토큰을 담아 보내주세요")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "회원 가져오기 성공",content = @Content(schema = @Schema(implementation = MemberResponseDto.class)))
    })
    @GetMapping("")
    public ResponseEntity<ResponseDto<MemberResponseDto>> getMember(@AuthenticationPrincipal Member member){
        log.info("회원 가져오기 호출 id:{}",member.getId());
        return ResponseEntity.ok(ResponseDto.of(memberService.getMember(member),"회원 가져오기 성공"));
    }

    @Operation(summary = "모든 회원 가져오기")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "모든 회원가져오기성공",content = @Content(schema = @Schema(implementation = MemberResponseDto.class)))
    })
    @GetMapping("/all")
    public ResponseEntity<ResponseDto<List<MemberResponseDto>>> getAllMember(){
        return ResponseEntity.ok(ResponseDto.of(memberService.getAllMember(),"모든 회원 가져오기 성공"));
    }

    @Operation(summary = "회원이 작성한 모든 글 가져오기",description = "url 헤더에 Auth 토큰을 담아 보내주세요. 정렬기준 sort(date/공백(최신순), like)를, 페이지(공백일 시 1)를 보내주세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "회원이 작성한 모든 글 가져오기성공",content = @Content(schema = @Schema(implementation = PostListResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 회원입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "400",description = "정렬의 기준값이 올바르지 않습니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @GetMapping("/posts")
    public ResponseEntity<ResponseDto<List<PostListResponseDto>>> getAllPost(@AuthenticationPrincipal Member member, @RequestParam(required = false,defaultValue = "date") String sort){
        log.info("회원이 작성한 모든 글 가져오기 호출 id:{}",member.getId());
        return ResponseEntity.ok(ResponseDto.of(postService.getPostByMember(member,sort),"회원이 작성한 모든 게시글 가져오기 성공"));
    }

    @Operation(summary = "회원이 스크랩한 모든 글 가져오기",description = "url 헤더에 Auth 토큰을 담아 보내주세요. 정렬기준 sort(date/공백(최신순), like, scrap)를, 페이지(공백일 시 1)를 보내주세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "회원이 스크랩한 모든 글 가져오기성공",content = @Content(schema = @Schema(implementation = ListResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 회원입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "400",description = "정렬의 기준값이 올바르지 않습니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @GetMapping("/scraps")
    public ResponseEntity<ResponseDto<ListResponseDto>> getAllScrap(@AuthenticationPrincipal Member member, @RequestParam(required = false,defaultValue = "date") String sort
    ,@RequestParam(required = false,defaultValue = "1") @Min(1) int page){
        log.info("회원이 스크랩한 모든 글 가져오기 호출 id:{}",member.getId());
        return ResponseEntity.ok(ResponseDto.of(postService.getScrapsByMember(member,sort,page),"회원이 스크랩한 모든 게시글 가져오기 성공"));
    }

    @Operation(summary = "회원이 좋아요한 모든 글 가져오기",description = "url 헤더에 Auth 토큰을 담아 보내주세요. 정렬기준 sort(date/공백(최신순), like,scrap)를 보내주세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "회원이 스크랩한 모든 글 가져오기성공",content = @Content(schema = @Schema(implementation = PostListResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 회원입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "400",description = "정렬의 기준값이 올바르지 않습니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @GetMapping("/likes")
    public ResponseEntity<ResponseDto<List<PostListResponseDto>>> getAllLike(@AuthenticationPrincipal Member member, @RequestParam(required = false,defaultValue = "date") String sort){
        log.info("회원이 좋아요한 모든 글 가져오기 호출 id:{}",member.getId());
        return ResponseEntity.ok(ResponseDto.of(postService.getLikeByMember(member,sort),"회원이 좋아요한 모든 게시글 가져오기 성공"));
    }

    @Operation(summary = "회원이 작성한 모든 댓글 가져오기",description = "url 헤더에 Auth 토큰을 담아 보내주세요. 정렬기준 sort(date/공백(최신순), like)를 보내주세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "회원이 스크랩한 모든 글 가져오기성공",content = @Content(schema = @Schema(implementation = PostListResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 회원입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "400",description = "정렬의 기준값이 올바르지 않습니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @GetMapping("/replies")
    public ResponseEntity<ResponseDto<List<ReplyListResponseDto>>> getAllReply(@AuthenticationPrincipal Member member, @RequestParam(required = false,defaultValue = "date") String sort){
        log.info("회원이 작성한 모든 댓글 호출 id:{}",member.getId());
        return ResponseEntity.ok(ResponseDto.of(replyService.getReplyByMember(member,sort),"회원이 작성한 모든 댓글 가져오기 성공"));
    }


}
