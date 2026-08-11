package kr.inuappcenterportal.inuportal.domain.mockRegistration.service;

import kr.inuappcenterportal.inuportal.domain.mockRegistration.repository.MockEnrollmentRepository;
import kr.inuappcenterportal.inuportal.domain.mockRegistration.repository.MockWatchlistRepository;
import kr.inuappcenterportal.inuportal.domain.semester.enums.SemesterStatus;
import kr.inuappcenterportal.inuportal.domain.semester.repository.SemesterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MockRegistrationCleanupService {
    private final SemesterRepository semesterRepository;
    private final MockEnrollmentRepository enrollmentRepository;
    private final MockWatchlistRepository watchlistRepository;

    @Transactional
    public void deleteClosedSemesterRecords() {
        semesterRepository.findAllByStatus(SemesterStatus.CLOSED).forEach(semester -> {
            enrollmentRepository.deleteAllBySemesterId(semester.getId());
            watchlistRepository.deleteAllBySemesterId(semester.getId());
        });
    }
}
