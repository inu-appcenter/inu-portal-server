package kr.inuappcenterportal.inuportal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.Member;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "회원정보 응답Dto")
@Getter
@NoArgsConstructor
public class MemberResponseDto {
    @Schema(description = "회원의 데이터베이스 아이디값")
    private Long id;
    @Schema(description = "이메일",example = "test@gmail.com")
    private String email;
    @Schema(description = "닉네임",example = "인천대팁쟁이")
    private String nickname;

    @Builder
    public MemberResponseDto(Member member){
        this.id = member.getId();
        this.email = member.getEmail();
        this.nickname = member.getNickname();
    }
}
