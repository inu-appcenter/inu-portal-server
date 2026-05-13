package kr.inuappcenterportal.inuportal.domain.member.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FriendRequestDto {
    @NotBlank(message = "상대방의 닉네임은 필수입니다.")
    private String nickname;
}
