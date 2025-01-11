package kr.inuappcenterportal.inuportal.domain.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "회원정보 응답Dto")
@Getter
@NoArgsConstructor
public class MemberResponseDto {
    @Schema(description = "회원의 데이터베이스 아이디값")
    private Long id;
    @Schema(description = "닉네임",example = "인천대팁쟁이")
    private String nickname;
    @Schema(description = "횃불이 이미지 번호")
    private Long fireId;
    @Schema(description = "회원의 권한")
    private String role;

    @Builder
    private MemberResponseDto(Member member, String role){
        this.id = member.getId();
        this.nickname = member.getNickname();
        this.fireId = member.getFireId();
        this.role = role;
    }

    public static MemberResponseDto userMember(Member member){
        return MemberResponseDto.builder().member(member).role("user").build();
    }
    public static MemberResponseDto adminMember(Member member){
        return MemberResponseDto.builder().member(member).role("admin").build();
    }
}
