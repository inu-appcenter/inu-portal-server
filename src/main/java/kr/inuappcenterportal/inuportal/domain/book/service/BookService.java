package kr.inuappcenterportal.inuportal.domain.book.service;

import kr.inuappcenterportal.inuportal.domain.book.dto.BookDetail;
import kr.inuappcenterportal.inuportal.domain.book.dto.BookPreview;
import kr.inuappcenterportal.inuportal.domain.book.implement.BookProcessor;
import kr.inuappcenterportal.inuportal.domain.book.model.Book;
import kr.inuappcenterportal.inuportal.global.dto.ListResponseDto;
import kr.inuappcenterportal.inuportal.global.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookProcessor bookProcessor;
    private final ImageService imageService;

    public Long register(Book book, List<MultipartFile> images) throws IOException {
        Long bookId = bookProcessor.register(book);
        imageService.saveBookImage(bookId, images);
        return bookId;
    }

    public ListResponseDto<BookPreview> getList(int page) {
        Page<Book> books = bookProcessor.getList(page);
        return bookProcessor.getListWithThumbnails(books);
    }

    public BookDetail get(Long bookId) {
        return bookProcessor.getDetail(bookId);
    }

    public Long toggleTransactionStatus(Long bookId) {
        return bookProcessor.toggleTransactionStatus(bookId);
    }

    public ListResponseDto<BookPreview> getListOnlyAvailable(int page) {
        Page<Book> books = bookProcessor.getListOnlyAvailable(page);
        return bookProcessor.getListWithThumbnails(books);
    }

}
