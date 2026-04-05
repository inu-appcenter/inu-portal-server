package kr.inuappcenterportal.inuportal.domain.directory.repository;

import kr.inuappcenterportal.inuportal.domain.directory.enums.DirectoryCategory;
import kr.inuappcenterportal.inuportal.domain.directory.model.DirectorySource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DirectorySourceRepository extends JpaRepository<DirectorySource, Long> {

    Page<DirectorySource> findAllByCategory(DirectoryCategory category, Pageable pageable);

    List<DirectorySource> findAllByCategoryOrderByDisplayOrderAscIdAsc(DirectoryCategory category);

    long deleteByCategory(DirectoryCategory category);
}
