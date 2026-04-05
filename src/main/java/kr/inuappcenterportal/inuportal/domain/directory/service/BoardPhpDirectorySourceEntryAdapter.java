package kr.inuappcenterportal.inuportal.domain.directory.service;

import kr.inuappcenterportal.inuportal.domain.directory.enums.DirectorySourceTemplateType;
import kr.inuappcenterportal.inuportal.domain.directory.model.DirectoryEntry;
import kr.inuappcenterportal.inuportal.domain.directory.model.DirectorySource;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Component
class BoardPhpDirectorySourceEntryAdapter extends AbstractDirectorySourceEntryAdapter {

    @Override
    public boolean supports(DirectorySource source) {
        return source.getTemplateType() == DirectorySourceTemplateType.BOARD_PHP;
    }

    @Override
    public List<DirectoryEntry> crawl(DirectorySource source, LocalDateTime syncedAt) throws IOException {
        Document document = fetchDocument(source.getSourceUrl());
        return DirectorySourceEntryParser.parseDocument(document, source, syncedAt, source.getSourceUrl());
    }
}
