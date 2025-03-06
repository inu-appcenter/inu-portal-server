package kr.inuappcenterportal.inuportal.book;

import jakarta.transaction.Transactional;
import kr.inuappcenterportal.inuportal.domain.book.model.Book;
import kr.inuappcenterportal.inuportal.domain.book.repository.BookRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional
public class BookRepositoryTest {

    @Autowired
    BookRepository bookRepository;


    @Test
    @DisplayName("책을 검색합니다.")
    public void searchTest(){
        Book book1 = Book.builder().name("홍길동전").author("홍길동").content("1").build();
        Book book2 = Book.builder().name("토마토마토마토전대머리").author("이순신").content("1").build();
        Book book3 = Book.builder().name("포켓몬 전국 대도감").author("대머리").content("1").build();
        bookRepository.saveAll(List.of(book1,book2,book3));

        Page<Book> books = bookRepository.searchBook("머리", PageRequest.of(0,8));
        Assertions.assertEquals(books.getTotalElements(),2);

        books = bookRepository.searchBook("국대", PageRequest.of(0,8));
        Assertions.assertEquals(books.getTotalElements(),0);

        books = bookRepository.searchBook("순신", PageRequest.of(0,8));
        Assertions.assertEquals(books.getTotalElements(),1);
    }
}
