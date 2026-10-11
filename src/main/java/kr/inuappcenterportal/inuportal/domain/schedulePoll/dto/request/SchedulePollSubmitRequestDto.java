package kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.dto.AvailableRangeDto;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.enums.JoinSource;
import kr.inuappcenterportal.inuportal.domain.schedulePoll.enums.SubmissionVia;

import java.util.List;
import java.util.UUID;

public record SchedulePollSubmitRequestDto(
        @Schema(description = "제출 시도 ID(UUID). 시도마다 새로 만들고, 같은 시도의 재전송에는 같은 값을 쓴다",
                example = "8f0c1a8e-2b7c-4f9e-9a7a-1f6f8c2d9b31")
        @NotNull
        UUID clientSubmissionId,

        @Schema(description = "입력 화면을 열 때 받은 submissionRevision. 첫 제출이면 null", example = "3")
        Integer baseSubmissionRevision,

        @Schema(description = "SUBMIT 칠한 대로 / ANYTIME 언제든 괜찮아요(서버가 범위 전체로 채움)", example = "SUBMIT")
        @NotNull
        SubmissionVia via,

        @Schema(description = "가능 시간 구간 목록. 빈 배열 허용. ANYTIME이면 무시")
        @NotNull
        List<AvailableRangeDto> available,

        @Schema(description = "시간표에서 불러온 수업 ID. 저장만 하고 검증하지 않음", example = "[4412, 4413]")
        List<Long> importedCourseOfferingIds,

        @Schema(description = "첫 제출 시 필수. LINK / CHAT_CARD (통계용)", example = "LINK")
        JoinSource joinSource
) {
}
