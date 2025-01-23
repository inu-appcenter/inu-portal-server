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
    private TransactionStatus transactionStatus;
    private int imageCount;

    public static BookPreview from(Book book) {
        return BookPreview.builder()
                .id(book.getId())
                .name(book.getName())
                .author(book.getAuthor())
                .transactionStatus(book.getTransactionStatus())
                .imageCount(builder().imageCount)
                .build();
    }
}
