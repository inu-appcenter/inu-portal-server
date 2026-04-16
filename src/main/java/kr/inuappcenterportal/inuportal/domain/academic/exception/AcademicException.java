package kr.inuappcenterportal.inuportal.domain.academic.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AcademicException extends RuntimeException {

    private final HttpStatus status;

    public AcademicException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
