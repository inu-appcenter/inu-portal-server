package kr.inuappcenterportal.inuportal.domain.lostProperty.dto;

import lombok.Getter;

@Getter
public class LostPropertyPreview {

    private Long id;
    private String name;

    public LostPropertyPreview(Long id, String name) {
        this.id = id;
        this.name = name;
    }
}
