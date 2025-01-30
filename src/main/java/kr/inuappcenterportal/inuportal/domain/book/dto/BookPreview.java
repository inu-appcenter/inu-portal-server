package kr.inuappcenterportal.inuportal.domain.book.dto;

import kr.inuappcenterportal.inuportal.domain.book.enums.TransactionStatus;
import kr.inuappcenterportal.inuportal.domain.book.model.Book;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BookPreview {
    private Long id;
    private String name;
    private String author;
    private int price;
    private TransactionStatus transactionStatus;
    private int imageCount;

    public static BookPreview from(Book book) {
        return BookPreview.builder()
                .id(book.getId())
                .name(book.getName())
                .author(book.getAuthor())
                .price(book.getPrice())
                .transactionStatus(book.getTransactionStatus())
                .imageCount(book.getImageCount())
                .build();
    }
}
