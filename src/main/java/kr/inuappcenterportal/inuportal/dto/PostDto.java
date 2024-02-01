package kr.inuappcenterportal.inuportal.dto;

import jakarta.validation.constraints.NotBlank;
import kr.inuappcenterportal.inuportal.domain.BaseTimeEntity;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PostDto extends BaseTimeEntity {

    @NotBlank
    private String title;

    @NotBlank
    private String content;

    @NotBlank
    private String category;

    @Builder
    public PostDto(String title, String content, String category){
        this.title = title;
        this.content = content;
        this.category = category;
    }


}
