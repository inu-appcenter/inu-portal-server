package kr.inuappcenterportal.inuportal.domain.schedulePoll.model;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.global.model.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 처리한 제출 요청 기록. 같은 clientSubmissionId가 다시 오면(네트워크 재전송) 다시 처리하지 않고 처음 응답을 돌려준다.
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "schedule_poll_submit_request",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_schedule_poll_submit_request_client_id", columnNames = {"client_submission_id"})
        }
)
public class SchedulePollSubmitRequest extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_submission_id", nullable = false, length = 36)
    private String clientSubmissionId;

    @Column(name = "poll_id", nullable = false)
    private Long pollId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    // 처음 돌려준 응답(data 부분) JSON
    @Column(name = "response_json", nullable = false, columnDefinition = "TEXT")
    private String responseJson;

    private SchedulePollSubmitRequest(String clientSubmissionId, Long pollId, Long memberId, String responseJson) {
        this.clientSubmissionId = clientSubmissionId;
        this.pollId = pollId;
        this.memberId = memberId;
        this.responseJson = responseJson;
    }

    public static SchedulePollSubmitRequest create(String clientSubmissionId, Long pollId, Long memberId, String responseJson) {
        return new SchedulePollSubmitRequest(clientSubmissionId, pollId, memberId, responseJson);
    }
}
