package kr.inuappcenterportal.inuportal.domain.book.implement;

import kr.inuappcenterportal.inuportal.domain.book.dto.BookDetail;
import kr.inuappcenterportal.inuportal.domain.book.dto.BookPreview;
import kr.inuappcenterportal.inuportal.domain.book.enums.TransactionStatus;
import kr.inuappcenterportal.inuportal.domain.book.model.Book;
import kr.inuappcenterportal.inuportal.domain.book.repository.BookRepository;
import kr.inuappcenterportal.inuportal.global.dto.ListResponseDto;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import kr.inuappcenterportal.inuportal.global.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BookProcessor {

    private final BookRepository bookRepository;
    private final ImageService imageService;

    public Long register(Book book) {
        bookRepository.save(book);
        return book.getId();
    }

    @Transactional(readOnly = true)
    public Page<Book> getList(int page) {
        return bookRepository.findAll(PageRequest.of(--page, 8, Sort.by(Sort.Direction.DESC, "id")));
    }

    public ListResponseDto<BookPreview> getListWithThumbnails(Page<Book> books) {
        List<BookPreview> bookPreviews = books.stream()
                .map(book -> BookPreview.of(book, Base64.getEncoder().encodeToString(imageService.getThumbnail(book.getId())))).toList();
        long total = books.getTotalElements();
        long pages = books.getTotalPages();
        return ListResponseDto.of(pages, total, bookPreviews);
    }

    public BookDetail getDetail(Long bookId) {
        Book book = bookRepository.findById(bookId).orElseThrow(() -> new MyException(MyErrorCode.BOOK_NOT_FOUND));
        List<byte[]> images = imageService.getImages(bookId);
        return BookDetail.of(book, images);
    }

    @Transactional
    public Long toggleTransactionStatus(Long bookId) {
        Book book = bookRepository.findById(bookId).orElseThrow(() -> new MyException(MyErrorCode.BOOK_NOT_FOUND));
        book.toggleTransactionStatus();
        return bookId;
    }

    @Transactional(readOnly = true)
    public Page<Book> getListOnlyAvailable(int page) {
        return bookRepository.findAllByTransactionStatus(TransactionStatus.AVAILABLE,PageRequest.of(--page, 8, Sort.by(Sort.Direction.DESC, "id")));
    }


}
