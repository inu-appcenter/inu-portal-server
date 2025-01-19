package kr.inuappcenterportal.inuportal.domain.lostProperty.dto;

import kr.inuappcenterportal.inuportal.domain.lostProperty.model.LostProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LostPropertyDetail {
    private Long id;
    private String name;
    private String content;
    private int imageCount;

    public static LostPropertyDetail from(LostProperty lostProperty) {
        return LostPropertyDetail.builder()
                .id(lostProperty.getId())
                .name(lostProperty.getName())
                .content(lostProperty.getContent())
                .imageCount(lostProperty.getImageCount())
                .build();
    }
}
