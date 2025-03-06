package kr.inuappcenterportal.inuportal.domain.book.model;

import jakarta.persistence.*;
import kr.inuappcenterportal.inuportal.domain.book.enums.TransactionStatus;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name="book")
public class Book{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String author;

    @Column
    private int price;

    @Column(name = "transaction_status")
    @Enumerated(EnumType.STRING)
    private TransactionStatus transactionStatus;

    @Column(nullable = false,length = 2000)
    private String content;

    @Column(name = "image_count")
    private int imageCount;

    @Column(name = "create_date")
    private LocalDate createDate;

    @Column(name = "modified_date")
    private LocalDateTime modifiedDate;


    @Builder
    public Book(String name, String author, int price, TransactionStatus transactionStatus, String content, int imageCount) {
        this.name = name;
        this.author = author;
        this.price = price;
        this.transactionStatus = transactionStatus;
        this.content = content;
        this.imageCount = imageCount;
        this.createDate = LocalDate.now();
        this.modifiedDate = LocalDateTime.now();
    }

    public static Book create(String name, String author, int price, String content, int imageCount) {
        return Book.builder()
                .name(name)
                .author(author)
                .price(price)
                .transactionStatus(TransactionStatus.AVAILABLE)
                .content(content)
                .imageCount(imageCount)
                .build();
    }

    public void toggleTransactionStatus() {
       transactionStatus = transactionStatus.toggle();
    }

    public void delete() {
        transactionStatus = transactionStatus.delete();
        imageCount = 0;
        this.modifiedDate = LocalDateTime.now();
    }

    public void update(String name, String author, int price, String content, int imageCount) {
        this.name = name;
        this.author = author;
        this.price = price;
        this.content = content;
        this.imageCount = imageCount;
        this.modifiedDate = LocalDateTime.now();
    }

}
