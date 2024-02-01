package kr.inuappcenterportal.inuportal.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import kr.inuappcenterportal.inuportal.config.TokenProvider;
import kr.inuappcenterportal.inuportal.dto.MemberLoginDto;
import kr.inuappcenterportal.inuportal.dto.MemberSaveDto;
import kr.inuappcenterportal.inuportal.dto.MemberUpdateDto;
import kr.inuappcenterportal.inuportal.dto.ResponseDto;
import kr.inuappcenterportal.inuportal.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@RequestMapping("/api/members")
public class MemberController {
    private final TokenProvider tokenProvider;
    private final MemberService memberService;

    @Operation(summary = "회원 가입",description = "바디에 {email,password}을 json 형식으로 보내주세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "201",description = "회원가입성공")
    })
    @PostMapping("")
    public ResponseEntity<?> join(@Valid @RequestBody MemberSaveDto memberSaveDto){
        Long id = memberService.join(memberSaveDto);
        log.info("회원 join 호출 id:{}",id);
        return new ResponseEntity<>(new ResponseDto<>(id,"회원가입성공"), HttpStatus.CREATED);
    }


    @Parameter(name = "Auth",description = "로그인 후 발급 받은 토큰",required = true,in = ParameterIn.HEADER)
    @Operation(summary = "회원 정보 수정",description = "url 헤더에 Auth 토큰,바디에 {password}을 json 형식으로 보내주세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "회원정보수정성공")
    })
    @PutMapping("")
    public ResponseEntity<?> update(@RequestHeader("Auth") String token, @RequestBody MemberUpdateDto memberUpdateDto){
        Long id = Long.valueOf(tokenProvider.getUsername(token));
        log.info("회원 비밀번호 변경 호출 id:{}",id);
        Long member_id = memberService.changePassword(id, memberUpdateDto);
        return new ResponseEntity<>(new ResponseDto<>(member_id,"비밀번호 변경 성공"),HttpStatus.OK);
    }

    @Parameter(name = "Auth",description = "로그인 후 발급 받은 토큰",required = true,in = ParameterIn.HEADER)
    @Operation(summary = "회원 삭제",description = "url 헤더에 Auth 토큰을 담아 보내주세요")
    @ApiResponses({
            @ApiResponse(responseCode = "204",description = "회원삭제성공")
    })
    @DeleteMapping("")
    public ResponseEntity<?> delete(@RequestHeader("Auth") String token){
        Long id = Long.valueOf(tokenProvider.getUsername(token));
        log.info("회원 탈퇴 호출 id:{}",id);
        memberService.delete(id);
        return new ResponseEntity<>(new ResponseDto<>(id,"회원삭제성공"), HttpStatus.NO_CONTENT);
    }

    @Operation(summary = "로그인 시 토근이 발급됩니다",description = "바디에 {email,password}을 json 형식으로 보내주세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "로그인 성공")
    })
    @PostMapping("/login")
    public ResponseEntity<?> Login(@Valid @RequestBody MemberLoginDto memberLoginDto){
        log.info("로그인 호출");
        String token = memberService.login(memberLoginDto);
        return new ResponseEntity<>(token,HttpStatus.OK);
    }

    @Parameter(name = "Auth",description = "로그인 후 발급 받은 토큰",required = true,in = ParameterIn.HEADER)
    @Operation(summary = "회원 가져오기",description = "url 헤더에 Auth 토큰을 담아 보내주세요")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "회원가져오기성공")
    })
    @GetMapping("")
    public ResponseEntity<?> getMember(@RequestHeader("Auth") String token){
        Long id = Long.valueOf(tokenProvider.getUsername(token));
        log.info("회원 이메일 가져오기 호출 id:{}",id);
        return new ResponseEntity<>(memberService.getMember(id),HttpStatus.OK);
    }

    //@Parameter(name = "Auth",description = "로그인 후 발급 받은 토큰",required = true,in = ParameterIn.HEADER)
    @Operation(summary = "모든 회원 가져오기")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "모든 회원가져오기성공")
    })
    @GetMapping("/all")
    public ResponseEntity<?> getAllMember(){
        return new ResponseEntity<>(memberService.getAllMember(),HttpStatus.OK);
    }


    
}
