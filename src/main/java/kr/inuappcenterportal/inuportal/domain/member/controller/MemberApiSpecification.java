package kr.inuappcenterportal.inuportal.domain.member.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import kr.inuappcenterportal.inuportal.domain.department.enums.Department;
import kr.inuappcenterportal.inuportal.domain.member.dto.LocationUpdateRequestDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.FreshmanLoginDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.OnboardingRequestDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.MergeFreshmanRequestDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.MergeFreshmanResponseDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.SyncAcademicRequestDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.LoginDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.MemberResponseDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.MemberUpdateNicknameDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.NearbyVisibilityRequestDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.SchoolDepartmentUpdateRequestDto;
import kr.inuappcenterportal.inuportal.domain.member.dto.TokenDto;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.post.dto.PostListResponseDto;
import kr.inuappcenterportal.inuportal.domain.reply.dto.ReplyListResponseDto;
import kr.inuappcenterportal.inuportal.global.dto.ListResponseDto;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name="Members", description = "회원 API")
public interface MemberApiSpecification {

    @Operation(summary = "회원 닉네임/횃불이 이미지/실명 변경",description = "url 헤더에 Auth 토큰,바디에 {nickname,fireId(횃불이 이미지 번호),realName,realNameSource,consentTermsVersion}을 json 형식으로 보내주세요. 보낸 칸만 바뀝니다. realName이 빈 문자열이면 실명을 삭제합니다(동의 철회). 실명을 넣을 때는 realNameSource(MANUAL/PORTAL_SCRAPE)가 필요하고, PORTAL_SCRAPE면 consentTermsVersion도 필요합니다. 성공 시 수정된 회원의 데이터베이스 아이디 값이 {data: id}으로 보내집니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "회원 닉네임/횃불이 이미지 변경 성공",content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "404",description = "존재하지 않는 회원입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "400",description = "입력한 닉네임과 현재 닉네임이 동일합니다. / 동일한 닉네임이 존재합니다. / 닉네임, 횃불이 아이디 모두 공백입니다. / 닉네임이 빈칸 혹은 공백입니다. / 실명은 2~30자의 한글·영문·공백만 입력할 수 있습니다. / 실명 출처(realNameSource)가 필요합니다. / 포털에서 가져온 실명은 약관 버전(consentTermsVersion)이 필요합니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    ResponseEntity<ResponseDto<Long>> updateNicknameFireId(@Valid@RequestBody MemberUpdateNicknameDto memberUpdateNicknameDto, @Parameter(hidden = true) @AuthenticationPrincipal Member member);

    @Operation(summary = "회원탈퇴",description = "url 헤더에 Auth 토큰을 담아 보내주세요. 계정을 탈퇴 상태로 바꾸고 개인정보와 개인화 데이터(시간표, 성적, 알림 설정 등)를 삭제합니다. 작성한 게시글·댓글은 남고 작성자는 '탈퇴한 사용자'로 표시됩니다. 탈퇴 후에는 기존 토큰으로 접근할 수 없고(401 탈퇴한 회원입니다.), 같은 포털 계정으로 다시 로그인하면 새 계정이 만들어집니다. 성공 시 탈퇴한 회원의 데이터베이스 아이디 값이 {data: id}으로 보내집니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "회원탈퇴 성공",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "401",description = "탈퇴한 회원입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 유저입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    ResponseEntity<ResponseDto<Long>> withdraw(@Parameter(hidden = true) @AuthenticationPrincipal Member member);

    @Operation(summary = "로그인",description = "바디에 {studentId,password}을 json 형식으로 보내주세요. 토큰 유효시간은 2시간, 리프레시 토큰의 유효시간은 1일입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "로그인 성공, 토근이 발급되었습니다.",content = @Content(schema = @Schema(implementation = TokenDto.class))),
            @ApiResponse(responseCode = "401",description = "학번 또는 비밀번호가 틀립니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class))),
    })
    ResponseEntity<ResponseDto<TokenDto>> login(@Valid @RequestBody LoginDto loginDto);

    @Operation(summary = "토큰 재발급",description = "헤더에 refresh 토큰을 보내주세요. 토큰 유효시간은 2시간, 리프레시 토큰의 유효시간은 1일입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "토큰 재발급 성공",content = @Content(schema = @Schema(implementation = TokenDto.class)))
            ,@ApiResponse(responseCode = "401",description = "만료된 토큰입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    ResponseEntity<ResponseDto<TokenDto>> refresh(HttpServletRequest httpServletRequest);

    @Operation(summary = "회원 가져오기",description = "url 헤더에 Auth 토큰을 담아 보내주세요")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "회원 가져오기 성공",content = @Content(schema = @Schema(implementation = MemberResponseDto.class)))
    })
    ResponseEntity<ResponseDto<MemberResponseDto>> getMember(@Parameter(hidden = true) @AuthenticationPrincipal Member member);

    @Operation(summary = "모든 회원 가져오기")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "모든 회원가져오기성공",content = @Content(schema = @Schema(implementation = MemberResponseDto.class)))
    })
    ResponseEntity<ResponseDto<List<MemberResponseDto>>> getAllMember();

    @Operation(summary = "회원이 작성한 모든 글 가져오기",description = "url 헤더에 Auth 토큰을 담아 보내주세요. 정렬기준 sort(date/공백(최신순), like)를, 페이지(공백일 시 1)를 보내주세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "회원이 작성한 모든 글 가져오기성공",content = @Content(schema = @Schema(implementation = PostListResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 회원입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "400",description = "정렬의 기준값이 올바르지 않습니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    ResponseEntity<ResponseDto<List<PostListResponseDto>>> getAllPost(@Parameter(hidden = true) @AuthenticationPrincipal Member member, @RequestParam(required = false,defaultValue = "date") String sort);

    @Operation(summary = "회원이 스크랩한 모든 글 가져오기",description = "url 헤더에 Auth 토큰을 담아 보내주세요. 정렬기준 sort(date/공백(최신순), like, scrap)를, 페이지(공백일 시 1)를 보내주세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "회원이 스크랩한 모든 글 가져오기성공",content = @Content(schema = @Schema(implementation = ListResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 회원입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "400",description = "정렬의 기준값이 올바르지 않습니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    ResponseEntity<ResponseDto<ListResponseDto>> getAllScrap(@Parameter(hidden = true) @AuthenticationPrincipal Member member, @RequestParam(required = false,defaultValue = "date") String sort
    ,@RequestParam(required = false,defaultValue = "1") @Min(1) int page);

    @Operation(summary = "회원이 좋아요한 모든 글 가져오기",description = "url 헤더에 Auth 토큰을 담아 보내주세요. 정렬기준 sort(date/공백(최신순), like,scrap)를 보내주세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "회원이 스크랩한 모든 글 가져오기성공",content = @Content(schema = @Schema(implementation = PostListResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 회원입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "400",description = "정렬의 기준값이 올바르지 않습니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    ResponseEntity<ResponseDto<List<PostListResponseDto>>> getAllLike(@Parameter(hidden = true) @AuthenticationPrincipal Member member, @RequestParam(required = false,defaultValue = "date") String sort);

    @Operation(summary = "회원이 작성한 모든 댓글 가져오기",description = "url 헤더에 Auth 토큰을 담아 보내주세요. 정렬기준 sort(date/공백(최신순), like)를 보내주세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "회원이 스크랩한 모든 글 가져오기성공",content = @Content(schema = @Schema(implementation = PostListResponseDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 회원입니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
            ,@ApiResponse(responseCode = "400",description = "정렬의 기준값이 올바르지 않습니다.",content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    ResponseEntity<ResponseDto<List<ReplyListResponseDto>>> getAllReply(@Parameter(hidden = true) @AuthenticationPrincipal Member member, @RequestParam(required = false,defaultValue = "date") String sort);

    @Operation(summary = "회원 학과 수정", description = "회원의 학과 정보를 수정합니다. <br><br>url 헤더에 Auth 토큰을 담아 보내주세요.<br>요청 파라미터로 department(학과)를 보내주세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "회원 학과 수정 성공", content = @Content(schema = @Schema(implementation = MemberResponseDto.class)))
    })
    ResponseEntity<ResponseDto<MemberResponseDto>> getMember(@Parameter(hidden = true) @AuthenticationPrincipal Member member,
                                                                    @RequestParam Department department);

    ResponseEntity<ResponseDto<MemberResponseDto>> updateSchoolDepartment(
            @Parameter(hidden = true) @AuthenticationPrincipal Member member,
            @Valid @RequestBody SchoolDepartmentUpdateRequestDto request
    );

    @Operation(summary = "회원 약관 동의", description = "회원이 약관에 동의합니다. <br><br>url 헤더에 Auth 토큰을 담아 보내주세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "회원 약관 동의 성공", content = @Content(schema = @Schema(implementation = MemberResponseDto.class)))
    })
    ResponseEntity<ResponseDto<MemberResponseDto>> agreeTerms(@Parameter(hidden = true) @AuthenticationPrincipal Member member);

    @Operation(summary = "전체 채팅 푸시 알림 설정 토글", description = "전체 채팅 푸시 알림을 켜거나 끕니다.")
    ResponseEntity<ResponseDto<Boolean>> toggleChatPush(@Parameter(hidden = true) @AuthenticationPrincipal Member member);

    @Operation(summary = "주변 친구 찾기 위치 노출 설정 변경", description = "주변 친구 찾기 기능의 위치 노출 여부를 설정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "주변 친구 찾기 노출 설정이 변경되었습니다.", content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "유효성 검사 실패", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    ResponseEntity<ResponseDto<Void>> updateNearbyVisibility(
            @Parameter(hidden = true) @AuthenticationPrincipal Member member,
            @Valid @RequestBody NearbyVisibilityRequestDto requestDto);

    @Operation(summary = "내 위치 정보 갱신", description = "회원의 현재 위도, 경도 위치 정보를 갱신합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "위치 정보가 갱신되었습니다.", content = @Content(schema = @Schema(implementation = ResponseDto.class))),
            @ApiResponse(responseCode = "400", description = "위치 값이 올바르지 않습니다. / 위치 파라미터가 필요합니다.", content = @Content(schema = @Schema(implementation = ResponseDto.class)))
    })
    ResponseEntity<ResponseDto<Void>> updateLocation(
            @Parameter(hidden = true) @AuthenticationPrincipal Member member,
            @RequestBody LocationUpdateRequestDto requestDto);

    @Operation(summary = "신입생 로그인/가입", description = "신입생 전산망 인증 정보를 바탕으로 신입생 계정을 생성/조회하고 JWT 토큰을 발급합니다.")
    ResponseEntity<ResponseDto<TokenDto>> freshmanLogin(
            @Valid @RequestBody FreshmanLoginDto dto,
            @RequestHeader(value = "X-Firebase-AppCheck", required = false) String appCheckToken,
            HttpServletRequest request
    );

    @Operation(summary = "온보딩 완료", description = "신규 가입 유저의 닉네임, 실명, 학과, 캐릭터 설정을 완료하고 정규 서비스 이용 권한을 활성화합니다.")
    ResponseEntity<ResponseDto<MemberResponseDto>> completeOnboarding(
            @Parameter(hidden = true) @AuthenticationPrincipal Member member,
            @Valid @RequestBody OnboardingRequestDto dto
    );

    @Operation(summary = "신입생 계정 강제 통합 감지 및 병합", description = "3월 포털 학번 첫 로그인 시 공식 학적(이름, 생년월일)과 일치하는 신입생 계정을 100% 무조건 강제 통합합니다.")
    ResponseEntity<ResponseDto<MergeFreshmanResponseDto>> checkAndMergeFreshman(
            @Parameter(hidden = true) @AuthenticationPrincipal Member member,
            @Valid @RequestBody MergeFreshmanRequestDto dto
    );

    @Operation(summary = "사후 공식 학적 동기화", description = "온보딩 시 학적 스크래핑 실패로 직접 입력한 학생이 사후에 학적 동기화를 완료할 때 호출합니다.")
    ResponseEntity<ResponseDto<MemberResponseDto>> syncAcademic(
            @Parameter(hidden = true) @AuthenticationPrincipal Member member,
            @Valid @RequestBody SyncAcademicRequestDto dto
    );
}
