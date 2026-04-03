package kr.inuappcenterportal.inuportal.domain.directory.repository;

import jakarta.transaction.Transactional;
import kr.inuappcenterportal.inuportal.domain.directory.enums.DirectoryCategory;
import kr.inuappcenterportal.inuportal.domain.directory.model.CollegeOfficeContact;
import kr.inuappcenterportal.inuportal.domain.directory.model.DirectoryEntry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional
class DirectorySearchRepositoryTest {

    @Autowired
    private DirectoryEntryRepository directoryEntryRepository;

    @Autowired
    private CollegeOfficeContactRepository collegeOfficeContactRepository;

    @Test
    @DisplayName("전화번호부 엔트리는 이름, 업무, 전화번호로 검색할 수 있다")
    void searchDirectoryEntries() {
        LocalDateTime now = LocalDateTime.now();
        directoryEntryRepository.saveAll(List.of(
                DirectoryEntry.create(
                        DirectoryCategory.HEADQUARTERS,
                        "대학본부",
                        "홍보과",
                        "김인천",
                        "주무관",
                        "홍보 업무",
                        "032-835-9999",
                        "0328359999",
                        "inu@example.com",
                        "https://example.com/profile",
                        1,
                        now
                ),
                DirectoryEntry.create(
                        DirectoryCategory.AFFILIATED_INSTITUTION,
                        "부속기관",
                        "학산도서관",
                        "이도서",
                        "사서",
                        "대출 안내",
                        "032-835-8888",
                        "0328358888",
                        "library@example.com",
                        null,
                        2,
                        now
                )
        ));

        Page<DirectoryEntry> nameMatches = directoryEntryRepository.searchAll(
                "김인천",
                "",
                PageRequest.of(0, 20)
        );
        Page<DirectoryEntry> dutyMatches = directoryEntryRepository.searchAllByCategory(
                DirectoryCategory.HEADQUARTERS,
                "홍보",
                "",
                PageRequest.of(0, 20)
        );
        Page<DirectoryEntry> phoneMatches = directoryEntryRepository.searchAll(
                "9999",
                "9999",
                PageRequest.of(0, 20)
        );

        assertThat(nameMatches.getTotalElements()).isEqualTo(1);
        assertThat(dutyMatches.getTotalElements()).isEqualTo(1);
        assertThat(phoneMatches.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("학과사무실 연락처는 학과명, 위치, 전화번호로 검색할 수 있다")
    void searchCollegeOfficeContacts() {
        LocalDateTime now = LocalDateTime.now();
        collegeOfficeContactRepository.saveAll(List.of(
                CollegeOfficeContact.create(
                        "공과대학",
                        "7호관",
                        "컴퓨터공학부",
                        "032-835-1234",
                        "0328351234",
                        "https://cse.inu.ac.kr",
                        "7호관 301호",
                        "https://www.inu.ac.kr/isc/6071/subview.do",
                        1,
                        now
                ),
                CollegeOfficeContact.create(
                        "인문대학",
                        "15호관",
                        "국어국문학과",
                        "032-835-5678",
                        "0328355678",
                        "https://korean.inu.ac.kr",
                        "15호관 511호",
                        "https://www.inu.ac.kr/isc/6071/subview.do",
                        2,
                        now
                )
        ));

        Page<CollegeOfficeContact> departmentMatches = collegeOfficeContactRepository.searchAll(
                "컴퓨터",
                "",
                PageRequest.of(0, 20)
        );
        Page<CollegeOfficeContact> locationMatches = collegeOfficeContactRepository.searchAllByCollegeName(
                "공과대학",
                "301호",
                "",
                PageRequest.of(0, 20)
        );
        Page<CollegeOfficeContact> phoneMatches = collegeOfficeContactRepository.searchAll(
                "1234",
                "1234",
                PageRequest.of(0, 20)
        );

        assertThat(departmentMatches.getTotalElements()).isEqualTo(1);
        assertThat(locationMatches.getTotalElements()).isEqualTo(1);
        assertThat(phoneMatches.getTotalElements()).isEqualTo(1);
    }
}
