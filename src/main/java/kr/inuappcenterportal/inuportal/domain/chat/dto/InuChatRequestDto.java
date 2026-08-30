package kr.inuappcenterportal.inuportal.domain.chat.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InuChatRequestDto {

    private String question;

    @Builder.Default
    private List<Object> history = new ArrayList<>();

    public static InuChatRequestDto of(String question) {
        return InuChatRequestDto.builder()
                .question(question)
                .history(new ArrayList<>())
                .build();
    }

    public static InuChatRequestDto of(String question, List<Object> history) {
        return InuChatRequestDto.builder()
                .question(question)
                .history(history != null ? history : new ArrayList<>())
                .build();
    }
}
