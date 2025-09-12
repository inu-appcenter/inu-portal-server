package kr.inuappcenterportal.inuportal.domain.category.model;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.domain.category.enums.CategoryType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name="category")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private CategoryType type;

    @Builder
    public Category(String category, CategoryType type){
        this.category = category;
        this.type = type;
    }

    public void changeName(String category){
        this.category = category;
    }
}
