package kr.inuappcenterportal.inuportal.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MemberLoginDto {

    @Email
    @NotEmpty
    private String email;

    @NotEmpty
    private String password;

    @Builder
    public MemberLoginDto(String email, String password){
        this.email = email;
        this.password = password;
    }
}
