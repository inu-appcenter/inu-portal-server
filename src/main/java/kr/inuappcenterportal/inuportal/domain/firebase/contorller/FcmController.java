package kr.inuappcenterportal.inuportal.domain.firebase.contorller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.inuappcenterportal.inuportal.domain.firebase.dto.TokenRequestDto;
import kr.inuappcenterportal.inuportal.domain.firebase.service.FcmService;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Tokens", description = "Fcm 토큰 API")
@Slf4j
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/tokens")
@RequiredArgsConstructor
public class FcmController {
    private final FcmService fcmService;

    @PostMapping("")
    public ResponseEntity<ResponseDto<Long>> saveToken(@Valid@RequestBody TokenRequestDto tokenRequestDto, @AuthenticationPrincipal Member member){
        fcmService.saveToken(tokenRequestDto.getToken(), member==null?null:member.getId());
        return ResponseEntity.ok(ResponseDto.of(1L,"토큰 등록 성공"));
    }

    @DeleteMapping("")
    public ResponseEntity<ResponseDto<Long>> deleteToken(@AuthenticationPrincipal Member member){
        fcmService.deleteToken( member.getId());
        return ResponseEntity.ok(ResponseDto.of(1L,"토큰에서 회원 정보 삭제 성공"));
    }
}
