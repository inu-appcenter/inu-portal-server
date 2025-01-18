package kr.inuappcenterportal.inuportal.domain.book.service;

import kr.inuappcenterportal.inuportal.domain.book.dto.BookDetail;
import kr.inuappcenterportal.inuportal.domain.book.dto.BookPreview;
import kr.inuappcenterportal.inuportal.domain.book.dto.BookRegister;
import kr.inuappcenterportal.inuportal.domain.book.dto.BookUpdate;
import kr.inuappcenterportal.inuportal.domain.book.implement.BookProcessor;
import kr.inuappcenterportal.inuportal.global.dto.ListResponseDto;
import kr.inuappcenterportal.inuportal.global.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookService {

    private final BookProcessor bookProcessor;
    private final ImageService imageService;
    @Value("${bookImagePath}")
    private String bookImagePath;


    public Long register(BookRegister request, List<MultipartFile> images) throws IOException {
        Long bookId = bookProcessor.register(request, images);
        imageService.saveImage(bookId, images, bookImagePath);
        return bookId;
    }

    public ListResponseDto<BookPreview> getList(int page) {
        return bookProcessor.getList(page);
    }

    public BookDetail get(Long bookId) {
        return bookProcessor.getDetail(bookId);
    }

    public void toggleTransactionStatus(Long bookId) {
        bookProcessor.toggleTransactionStatus(bookId);
    }

    public ListResponseDto<BookPreview> getListOnlyAvailable(int page) {
        return bookProcessor.getListOnlyAvailable(page);
    }

    public void delete(Long bookId) {
        bookProcessor.delete(bookId);
        imageService.deleteImages(bookId, bookImagePath);
    }

    public void update(BookUpdate bookUpdate, List<MultipartFile> images, Long bookId) throws IOException {
        bookProcessor.update(bookUpdate, bookId);
        imageService.updateImages(bookId, images, bookImagePath);
    }

}
