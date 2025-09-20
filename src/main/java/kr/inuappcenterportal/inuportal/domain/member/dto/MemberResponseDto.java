package kr.inuappcenterportal.inuportal.domain.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.notice.enums.Department;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @Schema(description = "약관 동의 여부", example = "false")
    private Boolean termsAgreed;

    @Builder
    private MemberResponseDto(Member member, String role, Department department) {
        this.id = member.getId();
        this.nickname = member.getNickname();
        this.fireId = member.getFireId();
        this.role = role;
        this.department = department == null? null : department.getDepartmentName();
        this.termsAgreed = member.getTermsAgreed();
    }

    public static MemberResponseDto userMember(Member member){
        return MemberResponseDto.builder().member(member).role("user").department(member.getDepartment()).build();
    }
    public static MemberResponseDto adminMember(Member member){
        return MemberResponseDto.builder().member(member).role("admin").department(member.getDepartment()).build();
    }
}
