package kr.inuappcenterportal.inuportal.global.model;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime; // LocalDate 대신 LocalDateTime 사용

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public class BaseTimeEntity {

    @CreatedDate
    @Column(name = "create_date", updatable = false) // 생성일은 업데이트되지 않도록 설정
    protected LocalDateTime createDate; // LocalDate 대신 LocalDateTime 사용

    @LastModifiedDate
    @Column(name = "modified_date")
    protected LocalDateTime modifiedDate;
}
