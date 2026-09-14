package kr.inuappcenterportal.inuportal.suggestion;

import kr.inuappcenterportal.inuportal.domain.image.service.ImageService;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.suggestion.dto.SuggestionListResponse;
import kr.inuappcenterportal.inuportal.domain.suggestion.dto.SuggestionRequest;
import kr.inuappcenterportal.inuportal.domain.suggestion.dto.SuggestionResponse;
import kr.inuappcenterportal.inuportal.domain.suggestion.dto.SuggestionStatusRequest;
import kr.inuappcenterportal.inuportal.domain.suggestion.enums.SuggestionCategory;
import kr.inuappcenterportal.inuportal.domain.suggestion.enums.SuggestionStatus;
import kr.inuappcenterportal.inuportal.domain.suggestion.model.Suggestion;
import kr.inuappcenterportal.inuportal.domain.suggestion.repository.SuggestionRepository;
import kr.inuappcenterportal.inuportal.domain.suggestion.service.SuggestionService;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
public class SuggestionServiceTest {

    @InjectMocks
    private SuggestionService suggestionService;

    @Mock
    private SuggestionRepository suggestionRepository;

    @Mock
    private ImageService imageService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(suggestionService, "suggestionImagePath", tempDir.toString());
    }

    @Test
    @DisplayName("건의사항 등록 테스트 (이미지 없음)")
    public void saveSuggestion() throws IOException {
        Member member = mock(Member.class);

        SuggestionRequest suggestionRequest = SuggestionRequest.builder()
                .content("이미지 업로드가 안 돼요")
                .cheerMessage("항상 감사합니다")
                .category("BUG_REPORT")
                .appVersion("1.0.0")
                .osType("IOS")
                .osVersion("17.4")
                .deviceModel("iPhone15,3")
                .build();

        Suggestion suggestion = Suggestion.create("내용", null, member, SuggestionCategory.BUG_REPORT, null, null, null, null);
        ReflectionTestUtils.setField(suggestion, "id", 1L);

        when(suggestionRepository.save(any(Suggestion.class))).thenReturn(suggestion);

        Long suggestionId = suggestionService.saveSuggestion(suggestionRequest, member, null);

        Assertions.assertThat(suggestionId).isEqualTo(1L);
        verify(suggestionRepository).save(any(Suggestion.class));
        verifyNoInteractions(imageService);
    }

    @Test
    @DisplayName("건의사항 등록 테스트 (이미지 2장 첨부)")
    public void saveSuggestion_withImages() throws IOException {
        Member member = mock(Member.class);

        SuggestionRequest suggestionRequest = SuggestionRequest.builder()
                .content("이미지 업로드가 안 돼요")
                .category("BUG_REPORT")
                .build();

        Suggestion suggestion = Suggestion.create("내용", null, member, SuggestionCategory.BUG_REPORT, null, null, null, null);
        ReflectionTestUtils.setField(suggestion, "id", 1L);
        when(suggestionRepository.save(any(Suggestion.class))).thenReturn(suggestion);

        List<MultipartFile> images = new ArrayList<>();
        images.add(new MockMultipartFile("images", "a.png", "image/png", new byte[]{1, 2, 3}));
        images.add(new MockMultipartFile("images", "b.jpg", "image/jpeg", new byte[]{4, 5, 6}));

        Long suggestionId = suggestionService.saveSuggestion(suggestionRequest, member, images);

        Assertions.assertThat(suggestionId).isEqualTo(1L);
        Assertions.assertThat(suggestion.getImageCount()).isEqualTo(2);
        verify(imageService).saveImage(eq(1L), eq(images), anyString());
    }

    @Test
    @DisplayName("건의사항 등록 실패 테스트 (이미지 6장 초과)")
    public void saveSuggestion_fail_tooManyImages() {
        Member member = mock(Member.class);
        SuggestionRequest suggestionRequest = SuggestionRequest.builder()
                .content("내용").category("BUG_REPORT").build();

        List<MultipartFile> images = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            images.add(new MockMultipartFile("images", "img" + i + ".png", "image/png", new byte[]{1}));
        }

        Assertions.assertThatThrownBy(() -> suggestionService.saveSuggestion(suggestionRequest, member, images))
                .isInstanceOf(MyException.class)
                .hasFieldOrPropertyWithValue("errorCode", MyErrorCode.SUGGESTION_IMAGE_LIMIT_EXCEEDED);

        verifyNoInteractions(suggestionRepository, imageService);
    }

    @Test
    @DisplayName("건의사항 등록 실패 테스트 (이미지가 아닌 파일)")
    public void saveSuggestion_fail_invalidImageType() {
        Member member = mock(Member.class);
        SuggestionRequest suggestionRequest = SuggestionRequest.builder()
                .content("내용").category("BUG_REPORT").build();

        List<MultipartFile> images = new ArrayList<>();
        images.add(new MockMultipartFile("images", "doc.pdf", "application/pdf", new byte[]{1, 2, 3}));

        Assertions.assertThatThrownBy(() -> suggestionService.saveSuggestion(suggestionRequest, member, images))
                .isInstanceOf(MyException.class)
                .hasFieldOrPropertyWithValue("errorCode", MyErrorCode.INVALID_IMAGE_TYPE);

        verifyNoInteractions(suggestionRepository, imageService);
    }

    @Test
    @DisplayName("건의사항 상세조회 성공 테스트 (작성자 본인)")
    public void getSuggestion_success() {
        Member member = mock(Member.class);
        when(member.getId()).thenReturn(1L);
        when(member.getNickname()).thenReturn("nickname");

        Suggestion suggestion = Suggestion.create("내용", null, member, SuggestionCategory.BUG_REPORT, null, null, null, null);
        ReflectionTestUtils.setField(suggestion, "id", 1L);
        ReflectionTestUtils.setField(suggestion, "createDate", LocalDateTime.now());
        ReflectionTestUtils.setField(suggestion, "modifiedDate", LocalDateTime.now());

        when(suggestionRepository.findByIdWithMember(1L)).thenReturn(Optional.of(suggestion));

        SuggestionResponse response = suggestionService.getSuggestion(1L, member);

        Assertions.assertThat(response.getId()).isEqualTo(1L);
        Assertions.assertThat(response.getMemberId()).isEqualTo(1L);
        Assertions.assertThat(response.getMemberNickname()).isEqualTo("nickname");
    }

    @Test
    @DisplayName("건의사항 상세조회 실패 테스트 (작성자도 관리자도 아님)")
    public void getSuggestion_fail_authorization() {
        Member writer = mock(Member.class);
        when(writer.getId()).thenReturn(1L);

        Member other = mock(Member.class);
        when(other.getId()).thenReturn(2L);
        when(other.getRoles()).thenReturn(List.of("ROLE_USER"));

        Suggestion suggestion = Suggestion.create("내용", null, writer, SuggestionCategory.BUG_REPORT, null, null, null, null);
        ReflectionTestUtils.setField(suggestion, "id", 1L);

        when(suggestionRepository.findByIdWithMember(1L)).thenReturn(Optional.of(suggestion));

        Assertions.assertThatThrownBy(() -> suggestionService.getSuggestion(1L, other))
                .isInstanceOf(MyException.class)
                .hasFieldOrPropertyWithValue("errorCode", MyErrorCode.HAS_NOT_SUGGESTION_AUTHORIZATION);
    }

    @Test
    @DisplayName("건의사항 상세조회 실패 테스트 (존재하지 않음)")
    public void getSuggestion_fail_notFound() {
        Member member = mock(Member.class);

        when(suggestionRepository.findByIdWithMember(1L)).thenReturn(Optional.empty());

        Assertions.assertThatThrownBy(() -> suggestionService.getSuggestion(1L, member))
                .isInstanceOf(MyException.class)
                .hasFieldOrPropertyWithValue("errorCode", MyErrorCode.SUGGESTION_NOT_FOUND);
    }

    @Test
    @DisplayName("건의사항 이미지 조회 성공 테스트 (작성자 본인)")
    public void getSuggestionImage_success() {
        Member member = mock(Member.class);
        when(member.getId()).thenReturn(1L);

        Suggestion suggestion = Suggestion.create("내용", null, member, SuggestionCategory.BUG_REPORT, null, null, null, null);
        ReflectionTestUtils.setField(suggestion, "id", 1L);

        when(suggestionRepository.findByIdWithMember(1L)).thenReturn(Optional.of(suggestion));
        when(imageService.getImage(eq(1L), eq(1L), anyString())).thenReturn(new byte[]{1, 2, 3});

        byte[] result = suggestionService.getSuggestionImage(1L, 1L, member);

        Assertions.assertThat(result).containsExactly(1, 2, 3);
    }

    @Test
    @DisplayName("건의사항 이미지 조회 실패 테스트 (작성자도 관리자도 아님)")
    public void getSuggestionImage_fail_authorization() {
        Member writer = mock(Member.class);
        when(writer.getId()).thenReturn(1L);

        Member other = mock(Member.class);
        when(other.getId()).thenReturn(2L);
        when(other.getRoles()).thenReturn(List.of("ROLE_USER"));

        Suggestion suggestion = Suggestion.create("내용", null, writer, SuggestionCategory.BUG_REPORT, null, null, null, null);
        ReflectionTestUtils.setField(suggestion, "id", 1L);

        when(suggestionRepository.findByIdWithMember(1L)).thenReturn(Optional.of(suggestion));

        Assertions.assertThatThrownBy(() -> suggestionService.getSuggestionImage(1L, 1L, other))
                .isInstanceOf(MyException.class)
                .hasFieldOrPropertyWithValue("errorCode", MyErrorCode.HAS_NOT_SUGGESTION_AUTHORIZATION);

        verifyNoInteractions(imageService);
    }

    @Test
    @DisplayName("건의사항 이미지 조회 실패 테스트 (건의사항 없음)")
    public void getSuggestionImage_fail_notFound() {
        Member member = mock(Member.class);

        when(suggestionRepository.findByIdWithMember(1L)).thenReturn(Optional.empty());

        Assertions.assertThatThrownBy(() -> suggestionService.getSuggestionImage(1L, 1L, member))
                .isInstanceOf(MyException.class)
                .hasFieldOrPropertyWithValue("errorCode", MyErrorCode.SUGGESTION_NOT_FOUND);

        verifyNoInteractions(imageService);
    }

    @Test
    @DisplayName("건의사항 목록조회 테스트 (관리자 - 전체조회)")
    public void getSuggestionList_admin() {
        Member admin = mock(Member.class);
        when(admin.getRoles()).thenReturn(List.of("ROLE_ADMIN"));

        Member writer = mock(Member.class);
        when(writer.getId()).thenReturn(1L);
        when(writer.getNickname()).thenReturn("nickname");

        Suggestion suggestion = Suggestion.create("내용", null, writer, SuggestionCategory.BUG_REPORT, null, null, null, null);
        ReflectionTestUtils.setField(suggestion, "id", 1L);
        ReflectionTestUtils.setField(suggestion, "createDate", LocalDateTime.now());
        ReflectionTestUtils.setField(suggestion, "modifiedDate", LocalDateTime.now());

        Pageable pageable = PageRequest.of(0, 8);
        Page<Suggestion> mockPage = new PageImpl<>(List.of(suggestion), pageable, 1);

        when(suggestionRepository.findAllWithMember(any(Pageable.class))).thenReturn(mockPage);

        SuggestionListResponse response = suggestionService.getSuggestionList(1, admin);

        Assertions.assertThat(response.getTotal()).isEqualTo(1);
        Assertions.assertThat(response.getSuggestions()).hasSize(1);
        verify(suggestionRepository).findAllWithMember(pageable);
    }

    @Test
    @DisplayName("건의사항 목록조회 테스트 (일반 사용자 - 본인 글만)")
    public void getSuggestionList_user() {
        Member member = mock(Member.class);
        when(member.getRoles()).thenReturn(List.of("ROLE_USER"));
        when(member.getId()).thenReturn(1L);
        when(member.getNickname()).thenReturn("nickname");

        Suggestion suggestion = Suggestion.create("내용", null, member, SuggestionCategory.BUG_REPORT, null, null, null, null);
        ReflectionTestUtils.setField(suggestion, "id", 1L);
        ReflectionTestUtils.setField(suggestion, "createDate", LocalDateTime.now());
        ReflectionTestUtils.setField(suggestion, "modifiedDate", LocalDateTime.now());

        Pageable pageable = PageRequest.of(0, 8);
        Page<Suggestion> mockPage = new PageImpl<>(List.of(suggestion), pageable, 1);

        when(suggestionRepository.findAllByMemberAndIsDeletedFalse(any(Member.class), any(Pageable.class))).thenReturn(mockPage);

        SuggestionListResponse response = suggestionService.getSuggestionList(1, member);

        Assertions.assertThat(response.getTotal()).isEqualTo(1);
        Assertions.assertThat(response.getSuggestions()).hasSize(1);
        verify(suggestionRepository).findAllByMemberAndIsDeletedFalse(member, pageable);
    }

    @Test
    @DisplayName("건의사항 삭제 성공 테스트 (작성자 본인)")
    public void deleteSuggestion_success() {
        Member member = mock(Member.class);
        when(member.getId()).thenReturn(1L);

        Suggestion suggestion = Suggestion.create("내용", null, member, SuggestionCategory.BUG_REPORT, null, null, null, null);
        ReflectionTestUtils.setField(suggestion, "id", 1L);

        when(suggestionRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(suggestion));

        Long deletedId = suggestionService.deleteSuggestion(1L, member);

        Assertions.assertThat(deletedId).isEqualTo(1L);
        Assertions.assertThat(suggestion.getIsDeleted()).isTrue();
    }

    @Test
    @DisplayName("건의사항 삭제 성공 테스트 (관리자가 타인 글 삭제)")
    public void deleteSuggestion_success_admin() {
        Member writer = mock(Member.class);
        when(writer.getId()).thenReturn(1L);

        Member admin = mock(Member.class);
        when(admin.getId()).thenReturn(2L);
        when(admin.getRoles()).thenReturn(List.of("ROLE_ADMIN"));

        Suggestion suggestion = Suggestion.create("내용", null, writer, SuggestionCategory.BUG_REPORT, null, null, null, null);
        ReflectionTestUtils.setField(suggestion, "id", 1L);

        when(suggestionRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(suggestion));

        Long deletedId = suggestionService.deleteSuggestion(1L, admin);

        Assertions.assertThat(deletedId).isEqualTo(1L);
        Assertions.assertThat(suggestion.getIsDeleted()).isTrue();
    }

    @Test
    @DisplayName("건의사항 삭제 실패 테스트 (작성자도 관리자도 아님)")
    public void deleteSuggestion_fail_authorization() {
        Member writer = mock(Member.class);
        when(writer.getId()).thenReturn(1L);

        Member other = mock(Member.class);
        when(other.getId()).thenReturn(2L);
        when(other.getRoles()).thenReturn(List.of("ROLE_USER"));

        Suggestion suggestion = Suggestion.create("내용", null, writer, SuggestionCategory.BUG_REPORT, null, null, null, null);
        ReflectionTestUtils.setField(suggestion, "id", 1L);

        when(suggestionRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(suggestion));

        Assertions.assertThatThrownBy(() -> suggestionService.deleteSuggestion(1L, other))
                .isInstanceOf(MyException.class)
                .hasFieldOrPropertyWithValue("errorCode", MyErrorCode.HAS_NOT_SUGGESTION_AUTHORIZATION);

        Assertions.assertThat(suggestion.getIsDeleted()).isFalse();
    }

    @Test
    @DisplayName("건의사항 처리 상태 변경 성공 테스트")
    public void changeSuggestionStatus_success() {
        Member writer = mock(Member.class);

        Suggestion suggestion = Suggestion.create("내용", null, writer, SuggestionCategory.BUG_REPORT, null, null, null, null);
        ReflectionTestUtils.setField(suggestion, "id", 1L);

        SuggestionStatusRequest suggestionStatusRequest = SuggestionStatusRequest.builder()
                .status("COMPLETED")
                .build();

        when(suggestionRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(suggestion));

        Long changedId = suggestionService.changeSuggestionStatus(1L, suggestionStatusRequest);

        Assertions.assertThat(changedId).isEqualTo(1L);
        Assertions.assertThat(suggestion.getStatus()).isEqualTo(SuggestionStatus.COMPLETED);
    }

    @Test
    @DisplayName("건의사항 처리 상태 변경 실패 테스트 (잘못된 상태값)")
    public void changeSuggestionStatus_fail_wrongStatus() {
        Member writer = mock(Member.class);

        Suggestion suggestion = Suggestion.create("내용", null, writer, SuggestionCategory.BUG_REPORT, null, null, null, null);
        ReflectionTestUtils.setField(suggestion, "id", 1L);

        SuggestionStatusRequest suggestionStatusRequest = SuggestionStatusRequest.builder()
                .status("NOT_A_REAL_STATUS")
                .build();

        when(suggestionRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(suggestion));

        Assertions.assertThatThrownBy(() -> suggestionService.changeSuggestionStatus(1L, suggestionStatusRequest))
                .isInstanceOf(MyException.class)
                .hasFieldOrPropertyWithValue("errorCode", MyErrorCode.WRONG_SUGGESTION_STATUS);

        Assertions.assertThat(suggestion.getStatus()).isEqualTo(SuggestionStatus.RECEIVED);
    }
}
