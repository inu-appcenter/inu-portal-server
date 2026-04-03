package kr.inuappcenterportal.inuportal.domain.directory.service;

import kr.inuappcenterportal.inuportal.domain.directory.enums.DirectoryCategory;
import kr.inuappcenterportal.inuportal.domain.directory.enums.DirectorySourceTemplateType;
import kr.inuappcenterportal.inuportal.domain.directory.model.DirectorySource;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

class DirectorySourceParserTest {

    @Test
    @DisplayName("Parses university inventory rows and infers template types")
    void parseUniversitySources() {
        Document document = Jsoup.parse("""
                <div class="func-list">
                    <ul class="univ-list">
                        <li class="univ-item">
                            <strong class="univ-name"><a href="https://www.inu.ac.kr/sites/science/index.do">Science College</a></strong>
                            <ul class="dept-list">
                                <a href="https://www.inu.ac.kr/chem/2376/subview.do" target="_blank"><li class="dept-item">Chemistry</li></a>
                                <a href="https://bio.inu.ac.kr/bio/index.do" target="_blank"><li class="dept-item">Biological Science</li></a>
                            </ul>
                        </li>
                    </ul>
                </div>
                """, "https://inu.ac.kr/staffSearch/inu/srchView.do");
        LocalDateTime syncedAt = LocalDateTime.of(2026, 4, 3, 22, 0);

        List<DirectorySource> sources = DirectorySourceParser.parseSources(
                document,
                DirectoryCategory.UNIVERSITY,
                syncedAt
        );

        Assertions.assertEquals(2, sources.size());
        Assertions.assertEquals("Science College", sources.get(0).getParentName());
        Assertions.assertEquals("Chemistry", sources.get(0).getSourceName());
        Assertions.assertEquals("https://www.inu.ac.kr/sites/science/index.do", sources.get(0).getParentUrl());
        Assertions.assertEquals("https://www.inu.ac.kr/chem/2376/subview.do", sources.get(0).getSourceUrl());
        Assertions.assertEquals(DirectorySourceTemplateType.SUBVIEW_DO, sources.get(0).getTemplateType());
        Assertions.assertEquals(1, sources.get(0).getDisplayOrder());
        Assertions.assertEquals(DirectorySourceTemplateType.INDEX_DO, sources.get(1).getTemplateType());
        Assertions.assertEquals(syncedAt, sources.get(1).getLastSyncedAt());
    }

    @Test
    @DisplayName("Parses graduate-school inventory rows with tracks and strips URL fragments")
    void parseGraduateSchoolSources() {
        Document document = Jsoup.parse("""
                <div class="gradschool-list">
                    <div class="gradschool-item">
                        <div class="gradschool-name"><a href="https://www.inu.ac.kr/sites/grad/index.do">Graduate School</a></div>
                        <div class="gradschool-content">
                            <div class="track-list">
                                <div class="track-name">Humanities</div>
                                <div class="major-list">
                                    <a href="https://grad.inu.ac.kr/german/1853/subview.do" target="_blank"><span class="major-item">German Language</span></a>
                                    <a href="https://inugsl.inu.ac.kr/_NBoard/board.php?bo_table=professor#close" target="_blank"><span class="major-item">Logistics Graduate School</span></a>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
                """, "https://inu.ac.kr/staffSearch/inu/srchView.do");
        LocalDateTime syncedAt = LocalDateTime.of(2026, 4, 3, 22, 5);

        List<DirectorySource> sources = DirectorySourceParser.parseSources(
                document,
                DirectoryCategory.GRADUATE_SCHOOL,
                syncedAt
        );

        Assertions.assertEquals(2, sources.size());
        Assertions.assertEquals("Graduate School", sources.get(0).getParentName());
        Assertions.assertEquals("Humanities", sources.get(0).getSectionName());
        Assertions.assertEquals("German Language", sources.get(0).getSourceName());
        Assertions.assertEquals("https://grad.inu.ac.kr/german/1853/subview.do", sources.get(0).getSourceUrl());
        Assertions.assertEquals(DirectorySourceTemplateType.SUBVIEW_DO, sources.get(0).getTemplateType());
        Assertions.assertEquals("https://inugsl.inu.ac.kr/_NBoard/board.php?bo_table=professor", sources.get(1).getSourceUrl());
        Assertions.assertEquals(DirectorySourceTemplateType.BOARD_PHP, sources.get(1).getTemplateType());
        Assertions.assertEquals(syncedAt, sources.get(1).getLastSyncedAt());
    }
}
