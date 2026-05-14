package kr.inuappcenterportal.inuportal.domain.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "친구 별명 설정 요청 DTO")
public class FriendAliasRequestDto {
    @Schema(description = "설정할 별명")
    private String alias;
}
