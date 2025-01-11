package kr.inuappcenterportal.inuportal.domain.book.dto;

import kr.inuappcenterportal.inuportal.domain.book.enums.TransactionStatus;
import kr.inuappcenterportal.inuportal.domain.book.model.Book;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

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
    private List<byte[]> images;

    public static BookDetail of(Book book, List<byte[]> images) {
        return BookDetail.builder()
                .id(book.getId())
                .name(book.getName())
                .author(book.getAuthor())
                .price(book.getPrice())
                .content(book.getContent())
                .transactionStatus(book.getTransactionStatus())
                .imageCount(images.size())
                .images(images)
                .build();
    }

}
