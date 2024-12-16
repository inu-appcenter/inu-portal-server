package kr.inuappcenterportal.inuportal.domain.category.repository;

import kr.inuappcenterportal.inuportal.domain.category.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    boolean existsByCategory(String category);
    Optional<Category> findByCategory(String category);
}
