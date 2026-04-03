package kr.inuappcenterportal.inuportal.domain.directory.service;

import kr.inuappcenterportal.inuportal.domain.directory.model.CollegeOfficeContact;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

class CollegeOfficeContactParserTest {

    @Test
    @DisplayName("Parses college headings and department office rows")
    void parseCollegeOfficeContacts() {
        Document document = Jsoup.parse("""
                <article id="_contentBuilder" class="_contentBuilder">
                    <div class="_obj _objHeading">
                        <h2 class="objHeading_h2">인문대학 (15호관)</h2>
                    </div>
                    <div class="_obj _objHtml _absolute">
                        <div class="table_1">
                            <table>
                                <thead>
                                <tr>
                                    <th>학과</th>
                                    <th>학과사무실 번호</th>
                                    <th>학과 홈페이지</th>
                                    <th>위치</th>
                                </tr>
                                </thead>
                                <tbody>
                                <tr>
                                    <td>국어국문학과</td>
                                    <td>032-835-8110</td>
                                    <td><a href="https://korean.inu.ac.kr/" target="_blank">https://korean.inu.ac.kr</a></td>
                                    <td>15호관 511호</td>
                                </tr>
                                </tbody>
                            </table>
                        </div>
                    </div>
                    <div class="_obj _objHeading">
                        <h2 class="objHeading_h2">도시과학대학 (28호관)</h2>
                    </div>
                    <div class="_obj _objHtml _absolute">
                        <div class="table_1">
                            <table>
                                <tbody>
                                <tr>
                                    <td>건설환경공학</td>
                                    <td>032-835-8460</td>
                                    <td><a href="https://civil.inu.ac.kr/" target="_blank">https://civil.inu.ac.kr</a></td>
                                    <td></td>
                                </tr>
                                </tbody>
                            </table>
                        </div>
                    </div>
                </article>
                """, "https://www.inu.ac.kr/isc/6071/subview.do");
        LocalDateTime syncedAt = LocalDateTime.of(2026, 4, 4, 11, 0);

        List<CollegeOfficeContact> contacts = CollegeOfficeContactParser.parse(
                document,
                "https://www.inu.ac.kr/isc/6071/subview.do",
                syncedAt
        );

        Assertions.assertEquals(2, contacts.size());

        CollegeOfficeContact first = contacts.get(0);
        Assertions.assertEquals("인문대학", first.getCollegeName());
        Assertions.assertEquals("15호관", first.getCollegeLocationSummary());
        Assertions.assertEquals("국어국문학과", first.getDepartmentName());
        Assertions.assertEquals("032-835-8110", first.getOfficePhoneNumber());
        Assertions.assertEquals("0328358110", first.getOfficePhoneNumberNormalized());
        Assertions.assertEquals("https://korean.inu.ac.kr/", first.getHomepageUrl());
        Assertions.assertEquals("15호관 511호", first.getOfficeLocation());
        Assertions.assertEquals("https://www.inu.ac.kr/isc/6071/subview.do", first.getSourceUrl());
        Assertions.assertEquals(1, first.getDisplayOrder());
        Assertions.assertEquals(syncedAt, first.getLastSyncedAt());

        CollegeOfficeContact second = contacts.get(1);
        Assertions.assertEquals("도시과학대학", second.getCollegeName());
        Assertions.assertEquals("28호관", second.getCollegeLocationSummary());
        Assertions.assertEquals("건설환경공학", second.getDepartmentName());
        Assertions.assertEquals("", second.getOfficeLocation());
        Assertions.assertEquals(2, second.getDisplayOrder());
    }
}
