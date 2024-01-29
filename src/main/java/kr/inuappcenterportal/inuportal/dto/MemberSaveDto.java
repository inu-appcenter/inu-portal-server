package kr.inuappcenterportal.inuportal.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import kr.inuappcenterportal.inuportal.domain.Member;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MemberSaveDto {
    @Email
    @NotEmpty
    private String email;

    @NotEmpty
    private String password;

    @Builder
    public MemberSaveDto(String email, String password){
        this.email= email;
        this.password = password;
    }

    public Member toEntity(){
        return Member.builder()
                .email(email)
                .password(password)
                .build();
    }
}
