package kr.inuappcenterportal.inuportal.domain.book.repository;

import kr.inuappcenterportal.inuportal.domain.book.enums.TransactionStatus;
import kr.inuappcenterportal.inuportal.domain.book.model.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<Book, Long> {

    Page<Book> findAll(Pageable pageable);

    @Query("SELECT b FROM Book b WHERE b.transactionStatus = :status")
    Page<Book> findAllByTransactionStatus(@Param("status") TransactionStatus status, Pageable pageable);

    @Query("SELECT b FROM Book b WHERE b.name LIKE CONCAT('%', :query, '%') OR b.author LIKE CONCAT('%', :query, '%')")
    Page<Book> searchBook(@Param("query") String query, Pageable pageable);


}
