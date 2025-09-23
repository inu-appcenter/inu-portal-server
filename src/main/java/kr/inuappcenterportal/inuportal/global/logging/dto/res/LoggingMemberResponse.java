package kr.inuappcenterportal.inuportal.global.logging.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "회원 로그 응답")
public record LoggingMemberResponse(

        @Schema(description = "회원 수")
        Integer memberCount,

        @Schema(description = "회원 Id 목록")
        List<String> memberIds

) {
    public static LoggingMemberResponse of(Integer memberCount, List<String> memberIds) {
        return new LoggingMemberResponse(memberCount, memberIds);
    }
}