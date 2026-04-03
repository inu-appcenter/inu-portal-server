package kr.inuappcenterportal.inuportal.domain.directory.service;

import kr.inuappcenterportal.inuportal.domain.directory.model.DirectoryEntry;
import kr.inuappcenterportal.inuportal.domain.directory.model.DirectorySource;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

public interface DirectorySourceEntryAdapter {

    boolean supports(DirectorySource source);

    List<DirectoryEntry> crawl(DirectorySource source, LocalDateTime syncedAt) throws IOException;
}
