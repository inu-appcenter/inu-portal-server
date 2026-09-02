package kr.inuappcenterportal.inuportal.domain.dailyBrief.service;

import kr.inuappcenterportal.inuportal.domain.dailyBrief.dto.req.DailyBriefSettingRequestDto;
import kr.inuappcenterportal.inuportal.domain.dailyBrief.dto.res.DailyBriefSettingResponseDto;
import kr.inuappcenterportal.inuportal.domain.dailyBrief.model.DailyBriefSetting;
import kr.inuappcenterportal.inuportal.domain.dailyBrief.repository.DailyBriefSettingRepository;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DailyBriefService {

    private final DailyBriefSettingRepository dailyBriefSettingRepository;

    @Transactional
    public DailyBriefSettingResponseDto getSettings(Member member) {
        if (member == null) {
            throw new MyException(MyErrorCode.MEMBER_NOT_FOUND);
        }

        DailyBriefSetting setting = dailyBriefSettingRepository.findByMember(member)
                .orElseGet(() -> dailyBriefSettingRepository.save(DailyBriefSetting.createDefault(member)));

        return DailyBriefSettingResponseDto.from(setting);
    }

    @Transactional
    public DailyBriefSettingResponseDto updateSettings(Member member, DailyBriefSettingRequestDto requestDto) {
        if (member == null) {
            throw new MyException(MyErrorCode.MEMBER_NOT_FOUND);
        }

        DailyBriefSetting setting = dailyBriefSettingRepository.findByMember(member)
                .orElseGet(() -> dailyBriefSettingRepository.save(DailyBriefSetting.createDefault(member)));

        setting.update(requestDto);
        return DailyBriefSettingResponseDto.from(setting);
    }

    @Transactional
    public void createDefaultSetting(Member member) {
        if (member == null) {
            return;
        }
        if (dailyBriefSettingRepository.findByMember(member).isEmpty()) {
            dailyBriefSettingRepository.save(DailyBriefSetting.createDefault(member));
        }
    }

    @Transactional
    public int backfillDefaultSettings() {
        int insertedCount = dailyBriefSettingRepository.backfillDefaultSettingsForAllMembers();
        log.info("[Daily Brief] 기본 알림 설정 백필 완료: {}명의 회원에게 기본 설정 추가됨", insertedCount);
        return insertedCount;
    }
}
