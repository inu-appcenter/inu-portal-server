package kr.inuappcenterportal.inuportal.domain.dailyBrief.repository;

import kr.inuappcenterportal.inuportal.domain.dailyBrief.model.DailyBriefSetting;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DailyBriefSettingRepository extends JpaRepository<DailyBriefSetting, Long> {

    Optional<DailyBriefSetting> findByMember(Member member);

    Optional<DailyBriefSetting> findByMemberId(Long memberId);

    @Query("SELECT s FROM DailyBriefSetting s JOIN FETCH s.member WHERE s.timetableAlertEnabled = true AND s.timetablePreAlertEnabled = true")
    List<DailyBriefSetting> findAllTimetablePreAlertEnabled();

    @Query("SELECT s FROM DailyBriefSetting s JOIN FETCH s.member WHERE s.timetableAlertEnabled = true AND s.timetableDailyBriefEnabled = true AND s.timetableDailyBriefTime = :time")
    List<DailyBriefSetting> findAllTimetableDailyBriefByTime(@Param("time") String time);

    @Query("SELECT s FROM DailyBriefSetting s JOIN FETCH s.member WHERE s.scheduleAlertEnabled = true AND s.scheduleDailyBriefTime = :time")
    List<DailyBriefSetting> findAllScheduleDailyBriefByTime(@Param("time") String time);
}
