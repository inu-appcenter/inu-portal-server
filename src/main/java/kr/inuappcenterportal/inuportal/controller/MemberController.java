package kr.inuappcenterportal.inuportal.controller;

import jakarta.validation.Valid;
import kr.inuappcenterportal.inuportal.config.TokenProvider;
import kr.inuappcenterportal.inuportal.domain.Member;
import kr.inuappcenterportal.inuportal.dto.MemberLoginDto;
import kr.inuappcenterportal.inuportal.dto.MemberSaveDto;
import kr.inuappcenterportal.inuportal.dto.ResponseDto;
import kr.inuappcenterportal.inuportal.service.MemberService;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "http://portal.inuappcenter.kr/")
@RequestMapping("/api/members")
public class MemberController {
    private final TokenProvider tokenProvider;
    private final MemberService memberService;


    @PostMapping("")
    public ResponseEntity<?> join(@Valid @RequestBody MemberSaveDto memberSaveDto){
        Long id = memberService.join(memberSaveDto);
        log.info("회원 join 호출 id:{}",id);
        return new ResponseEntity<>(new ResponseDto(id,"회원가입성공"), HttpStatus.CREATED);
    }

    @PutMapping("")
    public ResponseEntity<?> update(@RequestHeader("auth") String token, @RequestParam("password") String password){
        Long id = Long.valueOf(tokenProvider.getUsername(token));
        Long member_id = memberService.changePassword(id, password);
        return new ResponseEntity<>(new ResponseDto(member_id,"비밀번호 변경 성공"),HttpStatus.OK);
    }

    @DeleteMapping("")
    public ResponseEntity<?> delete(@RequestHeader("auth") String token){
        Long id = Long.valueOf(tokenProvider.getUsername(token));
        memberService.delete(id);
        return new ResponseEntity<>(new ResponseDto(id,"회원삭제성공"), HttpStatus.NO_CONTENT);
    }

    @PostMapping("/login")
    public ResponseEntity<?> Login(@Valid @RequestBody MemberLoginDto memberLoginDto){
        log.info("로그인 호출");
        String token = memberService.login(memberLoginDto);
        return new ResponseEntity<>(token,HttpStatus.OK);
    }

    @GetMapping("")
    public ResponseEntity<?> getMember(@RequestHeader("auth") String token){
        Long id = Long.valueOf(tokenProvider.getUsername(token));
        return new ResponseEntity<>(memberService.getMember(id),HttpStatus.OK);
    }


    
}
