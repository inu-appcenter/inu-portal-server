package kr.inuappcenterportal.inuportal.domain.book.model;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.domain.book.enums.TransactionStatus;
import kr.inuappcenterportal.inuportal.domain.member.model.Member;
import kr.inuappcenterportal.inuportal.global.model.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Book extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String author;

    @Column
    private int price;

    @Column
    @Enumerated(EnumType.STRING)
    private TransactionStatus transactionStatus;

    @Column(nullable = false,length = 2000)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Member member;

    @Builder
    public Book(String name, String author, int price, TransactionStatus transactionStatus, String content, Member member) {
        this.name = name;
        this.author = author;
        this.price = price;
        this.transactionStatus = transactionStatus;
        this.content = content;
        this.member = member;
    }

    public static Book create(String name, String author, int price, String content, Member member) {
        return Book.builder()
                .name(name)
                .author(author)
                .price(price)
                .transactionStatus(TransactionStatus.AVAILABLE)
                .content(content)
                .member(member)
                .build();
    }

    public void toggleTransactionStatus() {
       transactionStatus = transactionStatus.toggle();
    }


}
