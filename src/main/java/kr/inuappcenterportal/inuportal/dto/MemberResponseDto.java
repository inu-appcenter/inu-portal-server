package kr.inuappcenterportal.inuportal.dto;

import kr.inuappcenterportal.inuportal.domain.Member;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MemberResponseDto {
    private Long id;
    private String email;

    @Builder
    public MemberResponseDto(Member member){
        this.id = member.getId();
        this.email = member.getEmail();
    }
}
