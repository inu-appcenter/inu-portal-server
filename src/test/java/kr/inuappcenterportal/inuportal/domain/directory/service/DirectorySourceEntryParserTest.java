package kr.inuappcenterportal.inuportal.domain.directory.service;

import kr.inuappcenterportal.inuportal.domain.directory.enums.DirectoryCategory;
import kr.inuappcenterportal.inuportal.domain.directory.enums.DirectorySourceTemplateType;
import kr.inuappcenterportal.inuportal.domain.directory.model.DirectoryEntry;
import kr.inuappcenterportal.inuportal.domain.directory.model.DirectorySource;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

class DirectorySourceEntryParserTest {

    @Test
    @DisplayName("Parses K2Web professor cards into directory entries")
    void parseK2WebProfessorCards() {
        Document document = Jsoup.parse("""
                <ul class="_wizOdr _prFlList prfl-list">
                    <li class="_prFlLi thumbLi">
                        <div class="con-top">
                            <div class="txtBox">
                                <div class="prof-rank"><p>교수</p></div>
                                <strong>김강우</strong>
                                <div class="btn-detail">
                                    <a href="/profl/chem/1550/95004/empView.do">상세보기</a>
                                </div>
                            </div>
                        </div>
                        <div class="artclInfo">
                            <dl><dt>직책/직급</dt><dd>교수</dd></dl>
                            <dl><dt>주전공</dt><dd>무기화학</dd></dl>
                            <dl><dt>담당과목</dt><dd>무기화학실험</dd></dl>
                            <dl><dt>전화번호</dt><dd>032-835-8237</dd></dl>
                            <dl><dt>이메일</dt><dd><a href="mailto:kimkw@inu.ac.kr">kimkw@inu.ac.kr</a></dd></dl>
                        </div>
                    </li>
                </ul>
                """, "https://www.inu.ac.kr/chem/2376/subview.do");
        DirectorySource source = DirectorySource.create(
                DirectoryCategory.UNIVERSITY,
                "자연과학대학",
                null,
                "화학과",
                "https://www.inu.ac.kr/sites/science/index.do",
                "https://www.inu.ac.kr/chem/2376/subview.do",
                DirectorySourceTemplateType.SUBVIEW_DO,
                1,
                LocalDateTime.of(2026, 4, 3, 23, 0)
        );
        LocalDateTime syncedAt = LocalDateTime.of(2026, 4, 3, 23, 5);

        List<DirectoryEntry> entries = DirectorySourceEntryParser.parseDocument(
                document,
                source,
                syncedAt,
                source.getSourceUrl()
        );

        Assertions.assertEquals(1, entries.size());
        DirectoryEntry entry = entries.get(0);
        Assertions.assertEquals("자연과학대학", entry.getAffiliation());
        Assertions.assertEquals("화학과", entry.getDetailAffiliation());
        Assertions.assertEquals("김강우", entry.getName());
        Assertions.assertEquals("교수", entry.getPosition());
        Assertions.assertEquals("주전공: 무기화학\n담당과목: 무기화학실험", entry.getDuties());
        Assertions.assertEquals("032-835-8237", entry.getPhoneNumber());
        Assertions.assertEquals("0328358237", entry.getPhoneNumberNormalized());
        Assertions.assertEquals("kimkw@inu.ac.kr", entry.getEmail());
        Assertions.assertEquals("https://www.inu.ac.kr/profl/chem/1550/95004/empView.do", entry.getProfileUrl());
        Assertions.assertEquals(syncedAt, entry.getLastSyncedAt());
    }

    @Test
    @DisplayName("Parses board-style professor cards into directory entries")
    void parseBoardProfessorCards() {
        Document document = Jsoup.parse("""
                <div class="list-box-row-item">
                    <a href="https://inugsl.inu.ac.kr/_NBoard/board.php?bo_table=professor&wr_id=7" class="box">
                        <div class="content-box">
                            <div class="title">
                                <h4>안승범<sub>교수</sub></h4>
                            </div>
                            <div class="content">
                                <ul>
                                    <li><b>전공분야 : </b><p>물류정보시스템/화물교통</p></li>
                                    <li><b>전화번호 : </b><p>032-835-8191</p></li>
                                    <li><b>이메일 : </b><p>sbahn@inu.ac.kr</p></li>
                                </ul>
                            </div>
                        </div>
                    </a>
                </div>
                """, "https://inugsl.inu.ac.kr/_NBoard/board.php?bo_table=professor");
        DirectorySource source = DirectorySource.create(
                DirectoryCategory.GRADUATE_SCHOOL,
                "동북아물류대학원",
                null,
                "교수소개",
                "https://inugsl.inu.ac.kr",
                "https://inugsl.inu.ac.kr/_NBoard/board.php?bo_table=professor",
                DirectorySourceTemplateType.BOARD_PHP,
                1,
                LocalDateTime.of(2026, 4, 3, 23, 10)
        );
        LocalDateTime syncedAt = LocalDateTime.of(2026, 4, 3, 23, 15);

        List<DirectoryEntry> entries = DirectorySourceEntryParser.parseDocument(
                document,
                source,
                syncedAt,
                source.getSourceUrl()
        );

        Assertions.assertEquals(1, entries.size());
        DirectoryEntry entry = entries.get(0);
        Assertions.assertEquals("동북아물류대학원", entry.getAffiliation());
        Assertions.assertEquals("교수소개", entry.getDetailAffiliation());
        Assertions.assertEquals("안승범", entry.getName());
        Assertions.assertEquals("교수", entry.getPosition());
        Assertions.assertEquals("전공분야: 물류정보시스템/화물교통", entry.getDuties());
        Assertions.assertEquals("032-835-8191", entry.getPhoneNumber());
        Assertions.assertEquals("sbahn@inu.ac.kr", entry.getEmail());
        Assertions.assertEquals("https://inugsl.inu.ac.kr/_NBoard/board.php?bo_table=professor&wr_id=7", entry.getProfileUrl());
    }

    @Test
    @DisplayName("Parses generic contact tables for staff pages")
    void parseGenericContactTable() {
        Document document = Jsoup.parse("""
                <table>
                    <thead>
                    <tr>
                        <th>이름</th>
                        <th>직위</th>
                        <th>담당업무</th>
                        <th>전화번호</th>
                        <th>이메일</th>
                    </tr>
                    </thead>
                    <tbody>
                    <tr>
                        <td>홍길동</td>
                        <td>조교</td>
                        <td>대학원 행정 지원</td>
                        <td>032-835-0000</td>
                        <td>assistant@inu.ac.kr</td>
                    </tr>
                    </tbody>
                </table>
                """, "https://grad.inu.ac.kr/grad/9999/subview.do");
        DirectorySource source = DirectorySource.create(
                DirectoryCategory.GRADUATE_SCHOOL,
                "일반대학원",
                "인문사회계열",
                "국어국문학과",
                "https://grad.inu.ac.kr/grad/index.do",
                "https://grad.inu.ac.kr/grad/9999/subview.do",
                DirectorySourceTemplateType.SUBVIEW_DO,
                1,
                LocalDateTime.of(2026, 4, 3, 23, 20)
        );
        LocalDateTime syncedAt = LocalDateTime.of(2026, 4, 3, 23, 25);

        List<DirectoryEntry> entries = DirectorySourceEntryParser.parseDocument(
                document,
                source,
                syncedAt,
                source.getSourceUrl()
        );

        Assertions.assertEquals(1, entries.size());
        DirectoryEntry entry = entries.get(0);
        Assertions.assertEquals("일반대학원", entry.getAffiliation());
        Assertions.assertEquals("인문사회계열 / 국어국문학과", entry.getDetailAffiliation());
        Assertions.assertEquals("홍길동", entry.getName());
        Assertions.assertEquals("조교", entry.getPosition());
        Assertions.assertEquals("대학원 행정 지원", entry.getDuties());
        Assertions.assertEquals("032-835-0000", entry.getPhoneNumber());
        Assertions.assertEquals("assistant@inu.ac.kr", entry.getEmail());
        Assertions.assertEquals("https://grad.inu.ac.kr/grad/9999/subview.do", entry.getProfileUrl());
    }
}
