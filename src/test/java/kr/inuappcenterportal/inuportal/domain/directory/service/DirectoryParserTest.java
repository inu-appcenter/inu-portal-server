package kr.inuappcenterportal.inuportal.domain.directory.service;

import kr.inuappcenterportal.inuportal.domain.directory.enums.DirectoryCategory;
import kr.inuappcenterportal.inuportal.domain.directory.model.DirectoryEntry;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

class DirectoryParserTest {

    @Test
    @DisplayName("Reads total pages from the paging block")
    void extractTotalPages() {
        Document document = Jsoup.parse("""
                <div class="_paging">
                    <div class="_inner">
                        <p class="_pageState">
                            <span class="_curPage">1</span>
                            <span class="_totPage">49</span>
                        </p>
                    </div>
                </div>
                """);

        Assertions.assertEquals(49, DirectoryParser.extractTotalPages(document));
    }

    @Test
    @DisplayName("Parses a directory row and normalizes its phone number")
    void parseEntries() {
        Document document = Jsoup.parse("""
                <div class="func-table">
                    <table>
                        <tbody>
                            <tr>
                                <td>Audit</td>
                                <td>Audit Office</td>
                                <td>Officer</td>
                                <td class="align-l">
                                    <pre>
                                    First line
                                    Second line
                                    </pre>
                                </td>
                                <td>032-835-9013</td>
                            </tr>
                        </tbody>
                    </table>
                </div>
                """);
        LocalDateTime syncedAt = LocalDateTime.of(2026, 4, 3, 10, 15);

        List<DirectoryEntry> entries = DirectoryParser.parseEntries(
                document,
                DirectoryCategory.HEADQUARTERS,
                0,
                syncedAt
        );

        Assertions.assertEquals(1, entries.size());

        DirectoryEntry entry = entries.get(0);
        Assertions.assertEquals(DirectoryCategory.HEADQUARTERS, entry.getCategory());
        Assertions.assertEquals("Audit", entry.getAffiliation());
        Assertions.assertEquals("Audit Office", entry.getDetailAffiliation());
        Assertions.assertEquals("Officer", entry.getPosition());
        Assertions.assertEquals("First line\nSecond line", entry.getDuties());
        Assertions.assertEquals("032-835-9013", entry.getPhoneNumber());
        Assertions.assertEquals("0328359013", entry.getPhoneNumberNormalized());
        Assertions.assertEquals(1, entry.getDisplayOrder());
        Assertions.assertEquals(syncedAt, entry.getLastSyncedAt());
    }
}
