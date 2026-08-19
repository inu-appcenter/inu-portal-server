package kr.inuappcenterportal.inuportal.suggestion;

import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.domain.suggestion.dto.SuggestionAnswerRequest;
import kr.inuappcenterportal.inuportal.domain.suggestion.dto.SuggestionListResponse;
import kr.inuappcenterportal.inuportal.domain.suggestion.dto.SuggestionRequest;
import kr.inuappcenterportal.inuportal.domain.suggestion.dto.SuggestionResponse;
import kr.inuappcenterportal.inuportal.domain.suggestion.enums.SuggestionCategory;
import kr.inuappcenterportal.inuportal.domain.suggestion.enums.SuggestionStatus;
import kr.inuappcenterportal.inuportal.domain.suggestion.model.Suggestion;
import kr.inuappcenterportal.inuportal.domain.suggestion.repository.SuggestionRepository;
import kr.inuappcenterportal.inuportal.domain.suggestion.service.SuggestionService;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class SuggestionServiceTest {

    @InjectMocks
    private SuggestionService suggestionService;

    @Mock
    private SuggestionRepository suggestionRepository;

    @Test
    @DisplayName("건의사항 등록 테스트")
    public void saveSuggestion() {
        Member member = mock(Member.class);

        SuggestionRequest suggestionRequest = SuggestionRequest.builder()
                .content("이미지 업로드가 안 돼요")
                .cheerMessage("항상 감사합니다")
                .category("BUG")
                .appVersion("1.0.0")
                .osType("IOS")
                .osVersion("17.4")
                .deviceModel("iPhone15,3")
                .build();

        Suggestion suggestion = Suggestion.create("내용", null, member, SuggestionCategory.BUG, null, null, null, null);
        ReflectionTestUtils.setField(suggestion, "id", 1L);

        when(suggestionRepository.save(any(Suggestion.class))).thenReturn(suggestion);

        Long suggestionId = suggestionService.saveSuggestion(suggestionRequest, member);

        Assertions.assertThat(suggestionId).isEqualTo(1L);
        verify(suggestionRepository).save(any(Suggestion.class));
    }

    @Test
    @DisplayName("건의사항 상세조회 성공 테스트 (작성자 본인)")
    public void getSuggestion_success() {
        Member member = mock(Member.class);
        when(member.getId()).thenReturn(1L);
        when(member.getNickname()).thenReturn("nickname");

        Suggestion suggestion = Suggestion.create("내용", null, member, SuggestionCategory.BUG, null, null, null, null);
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

        Suggestion suggestion = Suggestion.create("내용", null, writer, SuggestionCategory.BUG, null, null, null, null);
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
    @DisplayName("건의사항 목록조회 테스트 (관리자 - 전체조회)")
    public void getSuggestionList_admin() {
        Member admin = mock(Member.class);
        when(admin.getRoles()).thenReturn(List.of("ROLE_ADMIN"));

        Member writer = mock(Member.class);
        when(writer.getId()).thenReturn(1L);
        when(writer.getNickname()).thenReturn("nickname");

        Suggestion suggestion = Suggestion.create("내용", null, writer, SuggestionCategory.BUG, null, null, null, null);
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

        Suggestion suggestion = Suggestion.create("내용", null, member, SuggestionCategory.BUG, null, null, null, null);
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

        Suggestion suggestion = Suggestion.create("내용", null, member, SuggestionCategory.BUG, null, null, null, null);
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

        Suggestion suggestion = Suggestion.create("내용", null, writer, SuggestionCategory.BUG, null, null, null, null);
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

        Suggestion suggestion = Suggestion.create("내용", null, writer, SuggestionCategory.BUG, null, null, null, null);
        ReflectionTestUtils.setField(suggestion, "id", 1L);

        when(suggestionRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(suggestion));

        Assertions.assertThatThrownBy(() -> suggestionService.deleteSuggestion(1L, other))
                .isInstanceOf(MyException.class)
                .hasFieldOrPropertyWithValue("errorCode", MyErrorCode.HAS_NOT_SUGGESTION_AUTHORIZATION);

        Assertions.assertThat(suggestion.getIsDeleted()).isFalse();
    }

    @Test
    @DisplayName("건의사항 답변등록 성공 테스트")
    public void answerSuggestion_success() {
        Member writer = mock(Member.class);

        Suggestion suggestion = Suggestion.create("내용", null, writer, SuggestionCategory.BUG, null, null, null, null);
        ReflectionTestUtils.setField(suggestion, "id", 1L);

        SuggestionAnswerRequest suggestionAnswerRequest = SuggestionAnswerRequest.builder()
                .status("COMPLETED")
                .answerContent("다음 업데이트에 반영했습니다.")
                .build();

        when(suggestionRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(suggestion));

        Long answeredId = suggestionService.answerSuggestion(1L, suggestionAnswerRequest);

        Assertions.assertThat(answeredId).isEqualTo(1L);
        Assertions.assertThat(suggestion.getStatus()).isEqualTo(SuggestionStatus.COMPLETED);
        Assertions.assertThat(suggestion.getAnswerContent()).isEqualTo("다음 업데이트에 반영했습니다.");
        Assertions.assertThat(suggestion.getAnswerDate()).isNotNull();
    }

    @Test
    @DisplayName("건의사항 답변등록 실패 테스트 (잘못된 상태값)")
    public void answerSuggestion_fail_wrongStatus() {
        Member writer = mock(Member.class);

        Suggestion suggestion = Suggestion.create("내용", null, writer, SuggestionCategory.BUG, null, null, null, null);
        ReflectionTestUtils.setField(suggestion, "id", 1L);

        SuggestionAnswerRequest suggestionAnswerRequest = SuggestionAnswerRequest.builder()
                .status("NOT_A_REAL_STATUS")
                .answerContent("다음 업데이트에 반영했습니다.")
                .build();

        when(suggestionRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(suggestion));

        Assertions.assertThatThrownBy(() -> suggestionService.answerSuggestion(1L, suggestionAnswerRequest))
                .isInstanceOf(MyException.class)
                .hasFieldOrPropertyWithValue("errorCode", MyErrorCode.WRONG_SUGGESTION_STATUS);

        Assertions.assertThat(suggestion.getAnswerContent()).isNull();
    }
}
