package kr.inuappcenterportal.inuportal.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import kr.inuappcenterportal.inuportal.config.TokenProvider;
import kr.inuappcenterportal.inuportal.dto.*;
import kr.inuappcenterportal.inuportal.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.exception.ex.MyUnauthorizedException;
import kr.inuappcenterportal.inuportal.service.MemberService;
import kr.inuappcenterportal.inuportal.service.PostService;
import kr.inuappcenterportal.inuportal.service.ReplyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name="Members", description = "회원 API")
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@RequestMapping("/api/members")
public class MemberController {
    private final TokenProvider tokenProvider;
    private final MemberService memberService;
    private final PostService postService;
    private final ReplyService replyService;

    @Operation(summary = "회원 가입",description = "바디에 {email(@가 들어간 이메일 형식이어야 합니다.),password,nickname}을 json 형식으로 보내주세요. 성공 시 가입한 회원의 데이터베이스 아이디 값이 {data: id}으로 보내집니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201",description = "회원가입성공",content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "400",description = "동일한 이메일이 존재합니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PostMapping("")
    public ResponseEntity<ResponseDto<Long>> join(@Valid @RequestBody MemberSaveDto memberSaveDto){
        Long id = memberService.join(memberSaveDto);
        log.info("회원 join 호출 id:{}",id);
        return new ResponseEntity<>(new ResponseDto<>(id,"회원가입성공"), HttpStatus.CREATED);
    }


    @Operation(summary = "회원 정보 수정",description = "url 헤더에 Auth 토큰,바디에 {password,nickname}을 json 형식으로 보내주세요.성공 시 수정된 회원의 데이터베이스 아이디 값이 {data: id}으로 보내집니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "회원정보수정성공",content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "404",description = "존재하지 않는 회원입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PutMapping("")
    public ResponseEntity<ResponseDto<Long>> update(@RequestBody MemberUpdateDto memberUpdateDto,HttpServletRequest httpServletRequest){
        Long id = Long.valueOf(tokenProvider.getUsername(httpServletRequest.getHeader("Auth")));
        log.info("회원 정보 수정 호출 id:{}",id);
        Long member_id = memberService.updateMember(id, memberUpdateDto);
        return new ResponseEntity<>(new ResponseDto<>(member_id,"회원 정보 수정 성공"),HttpStatus.OK);
    }

    @Operation(summary = "회원 삭제",description = "url 헤더에 Auth 토큰을 담아 보내주세요. 성공 시 삭제한 회원의 데이터베이스 아이디 값이 {data: id}으로 보내집니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "회원삭제성공",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 회원입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @DeleteMapping("")
    public ResponseEntity<ResponseDto<Long>> delete(HttpServletRequest httpServletRequest){
        Long id = Long.valueOf(tokenProvider.getUsername(httpServletRequest.getHeader("Auth")));
        log.info("회원 탈퇴 호출 id:{}",id);
        memberService.delete(id);
        return new ResponseEntity<>(new ResponseDto<>(id,"회원삭제성공"), HttpStatus.OK);
    }

    @Operation(summary = "로그인",description = "바디에 {email,password}을 json 형식으로 보내주세요. {data: 토큰} 형식으로 로그인 성공 시 토큰이 발급됩니다. 토큰 유효시간은 24시간입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "로그인 성공",content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "401",description = "존재하지 않는 아이디(이메일)입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "401",description = "비밀번호가 일치하지 않습니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<ResponseDto<String>> login(@Valid @RequestBody MemberLoginDto memberLoginDto){
        log.info("로그인 호출");
        String token = memberService.login(memberLoginDto);
        return new ResponseEntity<>(new ResponseDto<>(token,"로그인 성공, 토근이 발급되었습니다."),HttpStatus.OK);
    }

    @Operation(summary = "회원 가져오기",description = "url 헤더에 Auth 토큰을 담아 보내주세요")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "회원가져오기성공",content = @Content(schema = @Schema(implementation = MemberResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 회원입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @GetMapping("")
    public ResponseEntity<ResponseDto<MemberResponseDto>> getMember(HttpServletRequest httpServletRequest){
        Long id = Long.valueOf(tokenProvider.getUsername(httpServletRequest.getHeader("Auth")));
        log.info("회원 이메일 가져오기 호출 id:{}",id);
        return new ResponseEntity<>(new ResponseDto<>(memberService.getMember(id),"회원 가져오기 성공"),HttpStatus.OK);
    }

    @Operation(summary = "모든 회원 가져오기")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "모든 회원가져오기성공",content = @Content(schema = @Schema(implementation = MemberResponseDto.class)))
    })
    @GetMapping("/all")
    public ResponseEntity<ResponseDto<List<MemberResponseDto>>> getAllMember(){
        return new ResponseEntity<>(new ResponseDto<>(memberService.getAllMember(),"모든 회원 가져오기 성공"),HttpStatus.OK);
    }

    @Operation(summary = "회원이 작성한 모든 글 가져오기",description = "url 헤더에 Auth 토큰을 담아 보내주세요")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "회원이 작성한 모든 글 가져오기성공",content = @Content(schema = @Schema(implementation = PostListResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 회원입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @GetMapping("/posts")
    public ResponseEntity<ResponseDto<List<PostListResponseDto>>> getAllPost(HttpServletRequest httpServletRequest){
        Long id = Long.valueOf(tokenProvider.getUsername(httpServletRequest.getHeader("Auth")));
        log.info("회원이 작성한 모든 글 가져오기 호출 id:{}",id);
        return new ResponseEntity<>(new ResponseDto<>(postService.getPostByMember(id),"회원이 작성한 모든 게시글 가져오기 성공"),HttpStatus.OK);
    }

    @Operation(summary = "회원이 스크랩한 모든 글 가져오기",description = "url 헤더에 Auth 토큰을 담아 보내주세요")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "회원이 스크랩한 모든 글 가져오기성공",content = @Content(schema = @Schema(implementation = PostListResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 회원입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @GetMapping("/scraps")
    public ResponseEntity<ResponseDto<List<PostListResponseDto>>> getAllScrap(HttpServletRequest httpServletRequest){
        Long id = Long.valueOf(tokenProvider.getUsername(httpServletRequest.getHeader("Auth")));
        log.info("회원이 스크랩한 모든 글 가져오기 호출 id:{}",id);
        return new ResponseEntity<>(new ResponseDto<>(postService.getScrapsByMember(id),"회원이 스크랩한 모든 게시글 가져오기 성공"),HttpStatus.OK);
    }

    @Operation(summary = "회원이 좋아요한 모든 글 가져오기",description = "url 헤더에 Auth 토큰을 담아 보내주세요")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "회원이 스크랩한 모든 글 가져오기성공",content = @Content(schema = @Schema(implementation = PostListResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 회원입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @GetMapping("/likes")
    public ResponseEntity<ResponseDto<List<PostListResponseDto>>> getAllLike(HttpServletRequest httpServletRequest){
        Long id = Long.valueOf(tokenProvider.getUsername(httpServletRequest.getHeader("Auth")));
        log.info("회원이 좋아요한 모든 글 가져오기 호출 id:{}",id);
        return new ResponseEntity<>(new ResponseDto<>(postService.getLikeByMember(id),"회원이 좋아요한 모든 게시글 가져오기 성공"),HttpStatus.OK);
    }

    @Operation(summary = "회원이 작성한 모든 댓글 가져오기",description = "url 헤더에 Auth 토큰을 담아 보내주세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "회원이 스크랩한 모든 글 가져오기성공",content = @Content(schema = @Schema(implementation = PostListResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 회원입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @GetMapping("/replies")
    public ResponseEntity<ResponseDto<List<ReplyListResponseDto>>> getAllReply(HttpServletRequest httpServletRequest){
        Long id = Long.valueOf(tokenProvider.getUsername(httpServletRequest.getHeader("Auth")));
        log.info("회원이 작성한 모든 댓글 호출 id:{}",id);
        return new ResponseEntity<>(new ResponseDto<>(replyService.getReplyByMember(id),"회원이 작성한 모든 댓글 가져오기 성공"),HttpStatus.OK);
    }

    @Operation(summary = "회원의 비밀번호 일치여부 확인",description = "url 헤더에 Auth 토큰을, 바디에 {password} json 형식으로 보내주세요. 성공 시 일치 여부가 {data:boolean} 형식으로 보내집니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "회원의 비밀번호 일치 확인 성공",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 회원입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PostMapping("/checkPassword")
    public ResponseEntity<ResponseDto<Boolean>> checkPassword(HttpServletRequest httpServletRequest, @Valid@RequestBody MemberPasswordDto memberPasswordDto){
        Long id = Long.valueOf(tokenProvider.getUsername(httpServletRequest.getHeader("Auth")));
        log.info("회원의 비밀번호 일치여부 확인 호출 id:{}",id);
        return new ResponseEntity<>(new ResponseDto<>(memberService.checkPassword(id,memberPasswordDto),"회원의 비밀번호 일치여부 확인 성공"),HttpStatus.OK);
    }

    @Operation(summary = "인증 메일 보내기",description = "바디에 {email}을 json 형식으로 보내주세요. 성공 시 발송완료된 이메일이 {data:email} 형식으로 보내집니다. 인증코드의 유효시간은 30분입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "인증 메일 보내기 성공",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "400",description = "동일한 이메일이 존재합니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "만료된 이메일이거나, 인증 요청을 하지 않은 이메일입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PostMapping("/sendMail")
    public ResponseEntity<ResponseDto<String>> sendMail(@Valid@RequestBody EmailDto emailDto){
        log.info("인증 이메일 보내기 호출 email:{}",emailDto.getEmail());
        return new ResponseEntity<>(new ResponseDto<>(memberService.sendMail(emailDto),"인증 메일 보내기 성공"),HttpStatus.OK);
    }

    @Operation(summary = "가입 인증번호 일치 확인",description = "바디에 {email,numbers}을 json 형식으로 보내주세요. 성공 시 인증번호 일치 여부가 {data:boolean} 형식으로 보내집니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "가입 인증번호 일치 확인 성공",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "만료된 이메일이거나, 인증 요청을 하지 않은 이메일입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PostMapping("/checkMail")
    public ResponseEntity<ResponseDto<Boolean>> checkMail(@Valid@RequestBody EmailCheckDto emailCheckDto){
        log.info("가입 인증번호 확인 호출 email:{}",emailCheckDto.getEmail());
        return new ResponseEntity<>(new ResponseDto<>(memberService.checkNumbers(emailCheckDto),"가입 인증번호 일치확인 성공"),HttpStatus.OK);
    }



}
