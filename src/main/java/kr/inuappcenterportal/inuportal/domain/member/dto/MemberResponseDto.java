package kr.inuappcenterportal.inuportal.domain.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.department.enums.Department;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

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

    @Schema(description = "학과")
    private String department;

    private String departmentCode;

    @Schema(description = "학과 enum 코드. 매핑을 못 찾으면 null (issue #400)", example = "COMPUTER_ENGINEERING")
    private String departmentEnum;

    private String studentId;

    @Schema(description = "약관 동의 여부", example = "false")
    private Boolean termsAgreed;

    @Schema(description = "가입 시각")
    private LocalDateTime joinedAt;

    @Schema(description = "회원정보 마지막 수정 시각")
    private LocalDateTime profileModifiedAt;

    @Schema(description = "마지막 접속 시각")
    private LocalDateTime lastSeenAt;

    @Schema(description = "전체 채팅 알림 수신 여부")
    private Boolean chatPushEnabled;

    @Schema(description = "주변 친구 찾기 위치 노출 여부")
    private Boolean nearbyVisibility;

    @Builder
    private MemberResponseDto(Member member, String role, Department department) {
        this.id = member.getId();
        this.nickname = member.getNickname();
        this.fireId = member.getFireId();
        this.role = role;
        this.department = department == null? null : department.getDepartmentName();
        this.departmentEnum = department == null ? null : department.name();
        if (member.getSchoolDepartment() != null) {
            this.department = member.getSchoolDepartment().getName();
            this.departmentCode = member.getSchoolDepartment().getCode();
            // 학사 학과명 -> Department enum 매핑(SchoolDepartmentNoticeMapper)이 있으면
            // 그 enum이 실제 학과와 더 가까우니 우선한다. 못 찾으면 위에서 넣은 값을 유지.
            Department resolved = member.getSchoolDepartment().getResolvedNoticeDepartment();
            if (resolved != null) {
                this.departmentEnum = resolved.name();
            }
        }
        this.studentId = member.getStudentId();
        this.termsAgreed = member.getTermsAgreed();
        this.joinedAt = member.getJoinedAt();
        this.profileModifiedAt = member.getProfileModifiedAt();
        this.lastSeenAt = member.getLastSeenAt();
        this.chatPushEnabled = member.getChatPushEnabled();
        this.nearbyVisibility = Boolean.TRUE.equals(member.getNearbyVisibility());
    }

    public static MemberResponseDto userMember(Member member){
        return MemberResponseDto.builder().member(member).role("user").department(member.getDepartment()).build();
    }
    public static MemberResponseDto adminMember(Member member){
        return MemberResponseDto.builder().member(member).role("admin").department(member.getDepartment()).build();
    }
}
