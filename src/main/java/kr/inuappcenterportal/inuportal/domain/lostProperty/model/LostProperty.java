package kr.inuappcenterportal.inuportal.domain.lostProperty.model;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.global.model.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "lost_property")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LostProperty extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false,length = 2000)
    private String content;

    @Column(name = "image_count")
    private int imageCount;

    @Builder
    public LostProperty(String name, String content, int imageCount) {
        this.name = name;
        this.content = content;
        this.imageCount = imageCount;
    }

    public static LostProperty create(String name, String content, int imageCount) {
        return LostProperty.builder()
                .name(name)
                .content(content)
                .imageCount(imageCount)
                .build();
    }

    public void update(String name, String content, int imageCount) {
        this.name = name;
        this.content = content;
        this.imageCount = imageCount;
    }
}
