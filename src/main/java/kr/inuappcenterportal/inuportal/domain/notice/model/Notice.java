package kr.inuappcenterportal.inuportal.domain.notice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "notice")
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String category;

    @Column(name = "sub_category")
    private String subCategory;

    @Column
    private String title;

    @Column
    private String writer;

    @Column(name = "create_date")
    private String createDate;

    @Column(length = 512, unique = true)
    private String url;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder
    public Notice(String category, String subCategory, String title, String writer, String createDate, String url, String description) {
        this.category = category;
        this.subCategory = subCategory;
        this.title = title;
        this.writer = writer;
        this.createDate = createDate;
        this.url = url;
        this.description = description;
    }

    public void update(String subCategory, String title, String writer, String description) {
        this.subCategory = subCategory;
        this.title = title;
        this.writer = writer;
        this.description = description;
    }
}
