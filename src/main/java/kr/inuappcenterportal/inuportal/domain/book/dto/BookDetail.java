package kr.inuappcenterportal.inuportal.domain.book.dto;

import kr.inuappcenterportal.inuportal.domain.book.enums.TransactionStatus;
import kr.inuappcenterportal.inuportal.domain.book.model.Book;
import lombok.Builder;
import lombok.Getter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class BookDetail {

    private Long id;
    private String name;
    private String author;
    private int price;
    private String content;
    private TransactionStatus transactionStatus;
    private int imageCount;
    @DateTimeFormat(pattern = "yyyy.MM.dd")
    private LocalDate createDate;
    @DateTimeFormat(pattern = "yyyy.MM.dd HH:mm:ss")
    private LocalDateTime modifiedDate;

    public static BookDetail from(Book book) {
        return BookDetail.builder()
                .id(book.getId())
                .name(book.getName())
                .author(book.getAuthor())
                .price(book.getPrice())
                .content(book.getContent())
                .transactionStatus(book.getTransactionStatus())
                .imageCount(book.getImageCount())
                .createDate(book.getCreateDate())
                .modifiedDate(book.getModifiedDate())
                .build();
    }

}
