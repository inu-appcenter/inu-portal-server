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
import kr.inuappcenterportal.inuportal.oracleRepository.SchoolLoginRepository;
import kr.inuappcenterportal.inuportal.service.MemberService;
import kr.inuappcenterportal.inuportal.service.PostService;
import kr.inuappcenterportal.inuportal.service.ReplyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
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
    private final SchoolLoginRepository schoolLoginRepository;

    @Operation(summary = "회원 가입",description = "바디에 {email(@가 들어간 이메일 형식이어야 합니다.),password,nickname}을 json 형식으로 보내주세요. 성공 시 가입한 회원의 데이터베이스 아이디 값이 {data: id}으로 보내집니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201",description = "회원가입성공",content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "400",description = "동일한 이메일이 존재합니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "403",description = "인증되지 않은 이메일입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PostMapping("")
    public ResponseEntity<ResponseDto<Long>> join(@Valid @RequestBody MemberSaveDto memberSaveDto){
        Long id = memberService.join(memberSaveDto);
        log.info("회원 join 호출 id:{}",id);
        return new ResponseEntity<>(new ResponseDto<>(id,"회원가입성공"), HttpStatus.CREATED);
    }


    @Operation(summary = "회원 비밀번호 변경",description = "url 헤더에 Auth 토큰,바디에 {password,newPassword}을 json 형식으로 보내주세요.성공 시 수정된 회원의 데이터베이스 아이디 값이 {data: id}으로 보내집니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "회원 비밀번호 변경 성공",content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "404",description = "존재하지 않는 회원입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "401",description = "비밀번호가 틀립니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PutMapping("/password")
    public ResponseEntity<ResponseDto<Long>> updatePassword(@Valid@RequestBody MemberUpdatePasswordDto memberUpdatePasswordDto, @AuthenticationPrincipal Member member){
        log.info("회원 비밀번호 변경 호출 id:{}",member.getId());
        Long memberId = memberService.updateMemberPassword(member.getId(), memberUpdatePasswordDto);
        return new ResponseEntity<>(new ResponseDto<>(memberId,"회원 비밀번호 변경 성공"),HttpStatus.OK);
    }

    @Operation(summary = "회원 닉네임/횃불이 이미지 변경",description = "url 헤더에 Auth 토큰,바디에 {nickname,fireId(횃불이 이미지 번호)}을 json 형식으로 보내주세요.성공 시 수정된 회원의 데이터베이스 아이디 값이 {data: id}으로 보내집니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "회원 닉네임 변경 성공",content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "404",description = "존재하지 않는 회원입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "400",description = "입력한 닉네임과 현재 닉네임이 동일합니다. / 동일한 닉네임이 존재합니다. / 닉네임, 횃불이 아이디 모두 공백입니다. / 닉네임이 빈칸 혹은 공백입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PutMapping("")
    public ResponseEntity<ResponseDto<Long>> updateNicknameFireId(@Valid@RequestBody MemberUpdateNicknameDto memberUpdateNicknameDto, @AuthenticationPrincipal Member member){
        log.info("회원 닉네임 변경 호출 id:{}",member.getId());
        Long memberId = memberService.updateMemberNicknameFireId(member.getId(), memberUpdateNicknameDto);
        return new ResponseEntity<>(new ResponseDto<>(memberId,"회원 닉네임/횃불이 이미지 변경 성공"),HttpStatus.OK);
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
        return new ResponseEntity<>(new ResponseDto<>(id,"회원삭제성공"), HttpStatus.OK);
    }

    @Operation(summary = "로그인",description = "바디에 {email,password}을 json 형식으로 보내주세요. 토큰 유효시간은 2시간, 리프레시 토큰의 유효시간은 1일입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "로그인 성공",content = @Content(schema = @Schema(implementation = TokenDto.class))),
            @ApiResponse(responseCode = "401",description = "존재하지 않는 아이디(이메일)입니다. / 비밀번호가 일치하지 않습니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class))),
    })
    @PostMapping("/login")
    public ResponseEntity<ResponseDto<TokenDto>> login(@Valid @RequestBody MemberLoginDto memberLoginDto){
        log.info("로그인 호출");
        return new ResponseEntity<>(new ResponseDto<>(memberService.login(memberLoginDto),"로그인 성공, 토근이 발급되었습니다."),HttpStatus.OK);
    }

    @Operation(summary = "토큰 재발급",description = "헤더에 refresh 토큰을 보내주세요. 토큰 유효시간은 2시간, 리프레시 토큰의 유효시간은 1일입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "토큰 재발급 성공",content = @Content(schema = @Schema(implementation = TokenDto.class)))
            ,@ApiResponse(responseCode = "401",description = "만료된 토큰입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 회원입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PostMapping("/refresh")
    public ResponseEntity<ResponseDto<TokenDto>> refresh(HttpServletRequest httpServletRequest){
        log.info("토큰 재발급 호출");
        return new ResponseEntity<>(new ResponseDto<>(memberService.refreshToken(httpServletRequest.getHeader("refresh")),"토큰 재발급 성공"),HttpStatus.OK);
    }

    @Operation(summary = "회원 가져오기",description = "url 헤더에 Auth 토큰을 담아 보내주세요")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "회원가져오기성공",content = @Content(schema = @Schema(implementation = MemberResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 회원입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @GetMapping("")
    public ResponseEntity<ResponseDto<MemberResponseDto>> getMember(@AuthenticationPrincipal Member member){
        log.info("회원 이메일 가져오기 호출 id:{}",member.getId());
        return new ResponseEntity<>(new ResponseDto<>(memberService.getMember(member),"회원 가져오기 성공"),HttpStatus.OK);
    }

    @Operation(summary = "모든 회원 가져오기")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "모든 회원가져오기성공",content = @Content(schema = @Schema(implementation = MemberResponseDto.class)))
    })
    @GetMapping("/all")
    public ResponseEntity<ResponseDto<List<MemberResponseDto>>> getAllMember(){
        return new ResponseEntity<>(new ResponseDto<>(memberService.getAllMember(),"모든 회원 가져오기 성공"),HttpStatus.OK);
    }

    @Operation(summary = "회원이 작성한 모든 글 가져오기",description = "url 헤더에 Auth 토큰을 담아 보내주세요. 정렬기준 sort(date/공백(최신순), like)를, 페이지(공백일 시 1)를 보내주세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "회원이 작성한 모든 글 가져오기성공",content = @Content(schema = @Schema(implementation = PostListResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 회원입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "400",description = "정렬의 기준값이 올바르지 않습니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @GetMapping("/posts")
    public ResponseEntity<ResponseDto<List<PostListResponseDto>>> getAllPost(@AuthenticationPrincipal Member member, @RequestParam(required = false) String sort
    ,@RequestParam(required = false,defaultValue = "1") @Min(1) int page){
        log.info("회원이 작성한 모든 글 가져오기 호출 id:{}",member.getId());
        return new ResponseEntity<>(new ResponseDto<>(postService.getPostByMember(member,sort,page),"회원이 작성한 모든 게시글 가져오기 성공"),HttpStatus.OK);
    }

    @Operation(summary = "회원이 스크랩한 모든 글 가져오기",description = "url 헤더에 Auth 토큰을 담아 보내주세요. 정렬기준 sort(date/공백(최신순), like, scrap)를, 페이지(공백일 시 1)를 보내주세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "회원이 스크랩한 모든 글 가져오기성공",content = @Content(schema = @Schema(implementation = ListResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 회원입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "400",description = "정렬의 기준값이 올바르지 않습니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @GetMapping("/scraps")
    public ResponseEntity<ResponseDto<ListResponseDto>> getAllScrap(@AuthenticationPrincipal Member member, @RequestParam(required = false) String sort
    ,@RequestParam(required = false,defaultValue = "1") @Min(1) int page){
        log.info("회원이 스크랩한 모든 글 가져오기 호출 id:{}",member.getId());
        return new ResponseEntity<>(new ResponseDto<>(postService.getScrapsByMember(member,sort,page),"회원이 스크랩한 모든 게시글 가져오기 성공"),HttpStatus.OK);
    }

    @Operation(summary = "회원이 좋아요한 모든 글 가져오기",description = "url 헤더에 Auth 토큰을 담아 보내주세요. 정렬기준 sort(date/공백(최신순), like,scrap)를 보내주세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "회원이 스크랩한 모든 글 가져오기성공",content = @Content(schema = @Schema(implementation = PostListResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 회원입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "400",description = "정렬의 기준값이 올바르지 않습니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @GetMapping("/likes")
    public ResponseEntity<ResponseDto<List<PostListResponseDto>>> getAllLike(@AuthenticationPrincipal Member member, @RequestParam(required = false) String sort){
        log.info("회원이 좋아요한 모든 글 가져오기 호출 id:{}",member.getId());
        return new ResponseEntity<>(new ResponseDto<>(postService.getLikeByMember(member,sort),"회원이 좋아요한 모든 게시글 가져오기 성공"),HttpStatus.OK);
    }

    @Operation(summary = "회원이 작성한 모든 댓글 가져오기",description = "url 헤더에 Auth 토큰을 담아 보내주세요. 정렬기준 sort(date/공백(최신순), like)를 보내주세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "회원이 스크랩한 모든 글 가져오기성공",content = @Content(schema = @Schema(implementation = PostListResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 회원입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "400",description = "정렬의 기준값이 올바르지 않습니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @GetMapping("/replies")
    public ResponseEntity<ResponseDto<List<ReplyListResponseDto>>> getAllReply(@AuthenticationPrincipal Member member, @RequestParam(required = false) String sort){
        log.info("회원이 작성한 모든 댓글 호출 id:{}",member.getId());
        return new ResponseEntity<>(new ResponseDto<>(replyService.getReplyByMember(member,sort),"회원이 작성한 모든 댓글 가져오기 성공"),HttpStatus.OK);
    }


    @Operation(summary = "인증 메일 보내기",description = "바디에 {email}을 json 형식으로 보내주세요. 성공 시 발송완료된 이메일이 {data:email} 형식으로 보내집니다. 인증코드의 유효시간은 30분입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "인증 메일 보내기 성공",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "400",description = "동일한 이메일이 존재합니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "만료된 이메일이거나, 인증 요청을 하지 않은 이메일입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PostMapping("/code")
    public ResponseEntity<ResponseDto<String>> sendMail(@Valid@RequestBody EmailDto emailDto){
        log.info("인증 이메일 보내기 호출 email:{}",emailDto.getEmail());
        return new ResponseEntity<>(new ResponseDto<>(memberService.sendMail(emailDto),"인증 메일 보내기 성공"),HttpStatus.OK);
    }

    @Operation(summary = "가입 인증번호 일치 확인",description = "바디에 {email,numbers}을 json 형식으로 보내주세요. 성공 시 인증번호 일치 여부가 {data:boolean} 형식으로 보내집니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "가입 인증번호 일치 확인 성공",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "만료된 이메일이거나, 인증 요청을 하지 않은 이메일입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PostMapping("/verify")
    public ResponseEntity<ResponseDto<Boolean>> checkMail(@Valid@RequestBody EmailCheckDto emailCheckDto){
        log.info("가입 인증번호 확인 호출 email:{}",emailCheckDto.getEmail());
        return new ResponseEntity<>(new ResponseDto<>(memberService.checkNumbers(emailCheckDto),"가입 인증번호 일치확인 성공"),HttpStatus.OK);
    }

    @Operation(summary = "테스트용 api 사용 x  ",description = "바디에 {학번(id),비밀번호(password}을 json 형식으로 보내주세요. 성공 시 인증번호 일치 여부가 {data:boolean} 형식으로 보내집니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "가입 인증번호 일치 확인 성공",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "만료된 이메일이거나, 인증 요청을 하지 않은 이메일입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PostMapping("/login/school")
    public ResponseEntity<ResponseDto<Boolean>> checkLogin(@Valid@RequestBody MemberLoginDto memberLoginDto) throws SQLException, ClassNotFoundException {;
        return new ResponseEntity<>(new ResponseDto<>(schoolLoginRepository.loginCheck(memberLoginDto.getEmail(),memberLoginDto.getPassword()),"로그인 성공 여부 테스트"),HttpStatus.OK);
    }


}
