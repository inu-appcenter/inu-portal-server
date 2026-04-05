package kr.inuappcenterportal.inuportal.domain.directory.service;

import kr.inuappcenterportal.inuportal.domain.directory.enums.DirectoryCategory;
import kr.inuappcenterportal.inuportal.domain.directory.model.CollegeOfficeContact;
import kr.inuappcenterportal.inuportal.domain.directory.model.DirectoryEntry;
import kr.inuappcenterportal.inuportal.domain.directory.model.DirectorySource;
import kr.inuappcenterportal.inuportal.domain.directory.repository.CollegeOfficeContactRepository;
import kr.inuappcenterportal.inuportal.domain.directory.repository.DirectoryEntryRepository;
import kr.inuappcenterportal.inuportal.domain.directory.repository.DirectorySourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DirectoryPersistenceService {

    private final DirectoryEntryRepository directoryEntryRepository;
    private final DirectorySourceRepository directorySourceRepository;
    private final CollegeOfficeContactRepository collegeOfficeContactRepository;

    @Transactional
    public void replaceEntries(DirectoryCategory category, List<DirectoryEntry> entries) {
        directoryEntryRepository.deleteByCategory(category);
        directoryEntryRepository.saveAll(entries);
    }

    @Transactional
    public void replaceSources(DirectoryCategory category, List<DirectorySource> sources) {
        directorySourceRepository.deleteByCategory(category);
        directorySourceRepository.saveAll(sources);
    }

    @Transactional
    public void replaceCollegeOfficeContacts(List<CollegeOfficeContact> contacts) {
        collegeOfficeContactRepository.deleteAllInBatch();
        collegeOfficeContactRepository.saveAll(contacts);
    }
}
