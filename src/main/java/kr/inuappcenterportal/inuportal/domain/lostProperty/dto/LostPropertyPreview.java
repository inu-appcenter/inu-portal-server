package kr.inuappcenterportal.inuportal.domain.lostProperty.dto;

import lombok.Getter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
public class LostPropertyPreview {

    private Long id;
    private String name;
    private String content;
    private int imageCount;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate createdAt;

    public LostPropertyPreview(Long id, String name, String content, int imageCount, LocalDate createdAt) {
        this.id = id;
        this.name = name;
        this.content = content;
        this.imageCount = imageCount;
        this.createdAt = createdAt;
    }
}
