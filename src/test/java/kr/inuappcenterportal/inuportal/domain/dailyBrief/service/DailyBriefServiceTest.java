package kr.inuappcenterportal.inuportal.domain.dailyBrief.service;

import kr.inuappcenterportal.inuportal.domain.dailyBrief.dto.req.DailyBriefSettingRequestDto;
import kr.inuappcenterportal.inuportal.domain.dailyBrief.dto.res.DailyBriefSettingResponseDto;
import kr.inuappcenterportal.inuportal.domain.dailyBrief.enums.ScheduleScope;
import kr.inuappcenterportal.inuportal.domain.dailyBrief.model.DailyBriefSetting;
import kr.inuappcenterportal.inuportal.domain.dailyBrief.repository.DailyBriefSettingRepository;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class DailyBriefServiceTest {

    @Mock
    private DailyBriefSettingRepository dailyBriefSettingRepository;

    @InjectMocks
    private DailyBriefService dailyBriefService;

    @Test
    @DisplayName("설정이 없을 때 기본 설정 생성 및 조회 테스트")
    void getSettings_default() {
        // given
        Member member = Member.builder()
                .studentId("202000001")
                .roles(List.of("ROLE_USER"))
                .build();

        given(dailyBriefSettingRepository.findByMember(member)).willReturn(Optional.empty());
        given(dailyBriefSettingRepository.save(any(DailyBriefSetting.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        DailyBriefSettingResponseDto result = dailyBriefService.getSettings(member);

        // then
        assertThat(result.timetableAlertEnabled()).isTrue();
        assertThat(result.timetablePreAlertMinutes()).isEqualTo(10);
        assertThat(result.timetableDailyBriefTime()).isEqualTo("08:00");
        assertThat(result.scheduleDailyBriefTime()).isEqualTo("08:30");
        assertThat(result.scheduleScope()).isEqualTo(ScheduleScope.ALL);
    }

    @Test
    @DisplayName("설정 업데이트 테스트")
    void updateSettings() {
        // given
        Member member = Member.builder()
                .studentId("202000001")
                .roles(List.of("ROLE_USER"))
                .build();

        DailyBriefSetting setting = DailyBriefSetting.createDefault(member);
        given(dailyBriefSettingRepository.findByMember(member)).willReturn(Optional.of(setting));

        DailyBriefSettingRequestDto requestDto = new DailyBriefSettingRequestDto(
                true,
                true,
                30,
                true,
                "09:00",
                false,
                "10:00",
                ScheduleScope.DEPT_ONLY
        );

        // when
        DailyBriefSettingResponseDto result = dailyBriefService.updateSettings(member, requestDto);

        // then
        assertThat(result.timetablePreAlertMinutes()).isEqualTo(30);
        assertThat(result.timetableDailyBriefTime()).isEqualTo("09:00");
        assertThat(result.scheduleAlertEnabled()).isFalse();
        assertThat(result.scheduleDailyBriefTime()).isEqualTo("10:00");
        assertThat(result.scheduleScope()).isEqualTo(ScheduleScope.DEPT_ONLY);
    }
}
