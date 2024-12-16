package kr.inuappcenterportal.inuportal.domain.notice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
    private Long id;

    @Column
    private String category;

    @Column
    private String title;

    @Column
    private String writer;

    @Column(name="create_date")
    private String createDate;

    @Column Long view;

    @Column(length = 512)
    private String url;

    @Builder
    public Notice(String category, String title, String writer, String createDate, String url, long view, Long id){
        this.id = id;
        this.category = category;
        this.title = title;
        this.writer = writer;
        this.createDate = createDate;
        this.view = view;
        this.url = url;
    }
}
