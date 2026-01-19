package kr.inuappcenterportal.inuportal.domain.image.repository;

import kr.inuappcenterportal.inuportal.domain.image.domain.Image;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageRepository extends JpaRepository<Image, Long> {
}
