package kr.inuappcenterportal.inuportal.domain.lostProperty.dto;

import lombok.Getter;

@Getter
public class LostPropertyPreview {

    private Long id;
    private String name;
    private int imageCount;

    public LostPropertyPreview(Long id, String name, int imageCount) {
        this.id = id;
        this.name = name;
        this.imageCount = imageCount;
    }
}
