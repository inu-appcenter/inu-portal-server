package kr.inuappcenterportal.inuportal.domain.book.implement;

import kr.inuappcenterportal.inuportal.domain.book.dto.BookDetail;
import kr.inuappcenterportal.inuportal.domain.book.dto.BookPreview;
import kr.inuappcenterportal.inuportal.domain.book.dto.BookRegister;
import kr.inuappcenterportal.inuportal.domain.book.dto.BookUpdate;
import kr.inuappcenterportal.inuportal.domain.book.enums.TransactionStatus;
import kr.inuappcenterportal.inuportal.domain.book.model.Book;
import kr.inuappcenterportal.inuportal.domain.book.repository.BookRepository;
import kr.inuappcenterportal.inuportal.global.dto.ListResponseDto;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyErrorCode;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BookProcessor {

    private final BookRepository bookRepository;

    public Long register(BookRegister request, List<MultipartFile> images) {
        Book book = bookRepository.save(Book.create(request.getName(), request.getAuthor(), request.getPrice(), request.getContent(), images.size()));
        return book.getId();
    }

    @Transactional(readOnly = true)
    public ListResponseDto<BookPreview> getList(int page) {
        Page<Book> books = bookRepository.findAll(PageRequest.of(--page, 8, Sort.by(Sort.Direction.DESC, "id")));
        List<BookPreview> bookPreviews = getBookPreviews(books);
        return getBookPreviewListResponseDto(books, bookPreviews);
    }

    @Transactional(readOnly = true)
    public BookDetail getDetail(Long bookId) {
        Book book = bookRepository.findById(bookId).orElseThrow(() -> new MyException(MyErrorCode.BOOK_NOT_FOUND));
        return BookDetail.from(book);
    }

    @Transactional
    public void toggleTransactionStatus(Long bookId) {
        Book book = bookRepository.findById(bookId).orElseThrow(() -> new MyException(MyErrorCode.BOOK_NOT_FOUND));
        book.toggleTransactionStatus();
    }

    @Transactional(readOnly = true)
    public ListResponseDto<BookPreview> getListOnlyAvailable(int page) {
        Page<Book> books = bookRepository.findAllByTransactionStatus(TransactionStatus.AVAILABLE,PageRequest.of(--page, 8, Sort.by(Sort.Direction.DESC, "id")));
        List<BookPreview> bookPreviews = getBookPreviews(books);

        return getBookPreviewListResponseDto(books, bookPreviews);
    }

    @Transactional
    public void delete(Long bookId) {
        Book book = bookRepository.findById(bookId).orElseThrow(() -> new MyException(MyErrorCode.BOOK_NOT_FOUND));
        book.delete();
    }

    @Transactional
    public void update(BookUpdate bookUpdate, int imageCount, Long bookId) {
        Book book = bookRepository.findById(bookId).orElseThrow(() -> new MyException(MyErrorCode.BOOK_NOT_FOUND));
        book.update(bookUpdate.getName(), bookUpdate.getAuthor(), bookUpdate.getPrice(), bookUpdate.getContent(), imageCount);
    }

    private List<BookPreview> getBookPreviews(Page<Book> books) {
        return books.stream()
                .map(BookPreview::from)
                .toList();
    }

    private static ListResponseDto<BookPreview> getBookPreviewListResponseDto(Page<Book> books, List<BookPreview> bookPreviews) {
        long total = books.getTotalElements();
        long pages = books.getTotalPages();
        return ListResponseDto.of(pages, total, bookPreviews);
    }
}
