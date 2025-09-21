package kr.inuappcenterportal.inuportal.global.logging.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "회원 로그 응답")
public record LoggingMemberResponse(

        @Schema(description = "회원 수")
        Integer memberCount,

        @Schema(description = "회원 Id 목록")
        List<String> memberId

) {
    public static LoggingMemberResponse of(Integer memberCount, List<String> memberId) {
        return new LoggingMemberResponse(memberCount, memberId);
    }
}