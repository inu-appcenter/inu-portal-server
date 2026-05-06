package kr.inuappcenterportal.inuportal.domain.lostProperty.dto;

import lombok.Getter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime; // LocalDate 대신 LocalDateTime 사용

@Getter
public class LostPropertyPreview {

    private Long id;
    private String name;
    private String content;
    private int imageCount;
    @DateTimeFormat(pattern = "yyyy.MM.dd HH:mm:ss") // 패턴 변경
    private LocalDateTime createdAt; // LocalDate 대신 LocalDateTime 사용
    @DateTimeFormat(pattern = "yyyy.MM.dd HH:mm:ss")
    private LocalDateTime modifiedDate;

    public LostPropertyPreview(Long id, String name, String content, int imageCount, LocalDateTime createdAt, LocalDateTime modifiedDate) {
        this.id = id;
        this.name = name;
        this.content = content;
        this.imageCount = imageCount;
        this.createdAt = createdAt;
        this.modifiedDate = modifiedDate;
    }
}
