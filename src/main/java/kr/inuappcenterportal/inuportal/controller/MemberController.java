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
    public ResponseEntity<?> join(@Valid @RequestBody MemberSaveDto memberSaveDto){
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
    public ResponseEntity<?> update(@RequestBody MemberUpdateDto memberUpdateDto,HttpServletRequest httpServletRequest){
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
    public ResponseEntity<?> delete(HttpServletRequest httpServletRequest){
        Long id = Long.valueOf(tokenProvider.getUsername(httpServletRequest.getHeader("Auth")));
        log.info("회원 탈퇴 호출 id:{}",id);
        memberService.delete(id);
        return new ResponseEntity<>(new ResponseDto<>(id,"회원삭제성공"), HttpStatus.OK);
    }

    @Operation(summary = "로그인",description = "바디에 {email,password}을 json 형식으로 보내주세요. {data: 토큰} 형식으로 로그인 성공 시 토큰이 발급됩니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "로그인 성공",content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "401",description = "존재하지 않는 아이디(이메일)입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "401",description = "비밀번호가 일치하지 않습니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody MemberLoginDto memberLoginDto){
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
    public ResponseEntity<?> getMember(HttpServletRequest httpServletRequest){
        Long id = Long.valueOf(tokenProvider.getUsername(httpServletRequest.getHeader("Auth")));
        log.info("회원 이메일 가져오기 호출 id:{}",id);
        return new ResponseEntity<>(memberService.getMember(id),HttpStatus.OK);
    }

    @Operation(summary = "모든 회원 가져오기")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "모든 회원가져오기성공",content = @Content(schema = @Schema(implementation = MemberResponseDto.class)))
    })
    @GetMapping("/all")
    public ResponseEntity<?> getAllMember(){
        return new ResponseEntity<>(memberService.getAllMember(),HttpStatus.OK);
    }

    @Operation(summary = "회원이 작성한 모든 글 가져오기",description = "url 헤더에 Auth 토큰을 담아 보내주세요")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "회원이 작성한 모든 글 가져오기성공",content = @Content(schema = @Schema(implementation = PostListResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 회원입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @GetMapping("/posts")
    public ResponseEntity<?> getAllPost(HttpServletRequest httpServletRequest){
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
    public ResponseEntity<?> getAllScrap(HttpServletRequest httpServletRequest){
        Long id = Long.valueOf(tokenProvider.getUsername(httpServletRequest.getHeader("Auth")));
        log.info("회원이 스크랩한 모든 글 가져오기 호출 id:{}",id);
        return new ResponseEntity<>(new ResponseDto<>(postService.getAllScraps(id),"회원이 스크랩한 모든 게시글 가져오기 성공"),HttpStatus.OK);
    }

    @Operation(summary = "회원이 작성한 모든 댓글 가져오기",description = "url 헤더에 Auth 토큰을 담아 보내주세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "회원이 스크랩한 모든 글 가져오기성공",content = @Content(schema = @Schema(implementation = PostListResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 회원입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    @GetMapping("/replies")
    public ResponseEntity<?> getAllReply(HttpServletRequest httpServletRequest){
        Long id = Long.valueOf(tokenProvider.getUsername(httpServletRequest.getHeader("Auth")));
        log.info("회원이 작성한 모든 댓글 호출 id:{}",id);
        return new ResponseEntity<>(new ResponseDto<>(replyService.getReplyByMember(id),"회원이 작성한 모든 댓글 가져오기 성공"),HttpStatus.OK);
    }
}
