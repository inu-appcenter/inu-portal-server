package kr.inuappcenterportal.inuportal.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String category;

    @Column
    private String title;

    @Column
    private String writer;

    @Column
    private String date;

    @Column
    private String url;

    @Builder
    public Notice(String category, String title, String writer, String date, String url){
        this.category = category;
        this.title = title;
        this.writer = writer;
        this.date = date;
        this.url = url;
    }
}
