package kr.inuappcenterportal.inuportal.domain.notice.service;

import kr.inuappcenterportal.inuportal.domain.notice.model.DepartmentNotice;
import kr.inuappcenterportal.inuportal.domain.notice.repository.DepartmentNoticeRepository;
import kr.inuappcenterportal.inuportal.domain.schedule.model.Schedule;
import kr.inuappcenterportal.inuportal.domain.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DepartmentNoticeScheduleExtractPersistenceService {

    private final DepartmentNoticeRepository departmentNoticeRepository;
    private final ScheduleRepository scheduleRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markProcessing(Long noticeId) {
        DepartmentNotice departmentNotice = getNotice(noticeId);
        departmentNotice.markScheduleExtractProcessing();
        departmentNoticeRepository.save(departmentNotice);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markNoSchedule(Long noticeId) {
        DepartmentNotice departmentNotice = getNotice(noticeId);
        scheduleRepository.deleteBySourceNoticeIdAndAiGeneratedTrue(noticeId);
        departmentNotice.markScheduleNoSchedule();
        departmentNoticeRepository.save(departmentNotice);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveSuccess(Long noticeId, List<Schedule> schedules) {
        DepartmentNotice departmentNotice = getNotice(noticeId);
        scheduleRepository.deleteBySourceNoticeIdAndAiGeneratedTrue(noticeId);
        scheduleRepository.saveAll(schedules);
        departmentNotice.markScheduleExtractSuccess(schedules.size());
        departmentNoticeRepository.save(departmentNotice);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long noticeId, String reason) {
        DepartmentNotice departmentNotice = getNotice(noticeId);
        departmentNotice.markScheduleExtractFailed(reason);
        departmentNoticeRepository.save(departmentNotice);
    }

    private DepartmentNotice getNotice(Long noticeId) {
        return departmentNoticeRepository.findById(noticeId)
                .orElseThrow(() -> new IllegalStateException("학과 공지를 찾지 못했습니다. noticeId=" + noticeId));
    }
}
