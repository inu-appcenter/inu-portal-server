package kr.inuappcenterportal.inuportal.domain.schedulePoll.exception;

import jakarta.validation.ConstraintViolationException;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

// 시간 맞추기 컨트롤러에서 난 오류만 { "data": { "code": … }, "msg": … } 로 응답한다.
// 다른 API는 기존 MyExceptionHandler 형식(data = -1)을 그대로 쓴다.
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "kr.inuappcenterportal.inuportal.domain.schedulePoll")
public class SchedulePollExceptionHandler {

    @ExceptionHandler(SchedulePollException.class)
    public ResponseEntity<ResponseDto<SchedulePollErrorResponse>> handleSchedulePollException(SchedulePollException ex) {
        log.warn("시간 맞추기 예외 code:{}", ex.getErrorCode());
        return toResponse(ex.getErrorCode());
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            ConstraintViolationException.class
    })
    public ResponseEntity<ResponseDto<SchedulePollErrorResponse>> handleValidationException(Exception ex) {
        log.warn("시간 맞추기 요청 형식 오류 msg:{}", ex.getMessage());
        return toResponse(SchedulePollErrorCode.VALIDATION_ERROR);
    }

    // 시간표·회원 등 다른 도메인이 던진 오류. 그쪽이 정한 상태 코드와 문구를 그대로 쓰고 모양만 맞춘다.
    // (아래 RuntimeException 처리가 먼저 잡아 4xx가 500으로 바뀌는 것을 막는다.)
    @ExceptionHandler(MyException.class)
    public ResponseEntity<ResponseDto<SchedulePollErrorResponse>> handleMyException(MyException ex) {
        log.warn("시간 맞추기 처리 중 다른 도메인 예외 code:{}", ex.getErrorCode());
        return ResponseEntity.status(ex.getErrorCode().getStatus())
                .body(ResponseDto.of(new SchedulePollErrorResponse(ex.getErrorCode().name()), ex.getErrorCode().getMessage()));
    }

    // 결과 계산 실패 등 예상하지 못한 오류. 트랜잭션은 이미 롤백된 상태다.
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ResponseDto<SchedulePollErrorResponse>> handleRuntimeException(RuntimeException ex) {
        log.error("시간 맞추기 처리 중 알 수 없는 오류", ex);
        return toResponse(SchedulePollErrorCode.INTERNAL_ERROR);
    }

    // 오류 응답의 data 부분: { "code": "POLL_FULL" }
    public record SchedulePollErrorResponse(String code) {
    }

    private ResponseEntity<ResponseDto<SchedulePollErrorResponse>> toResponse(SchedulePollErrorCode errorCode) {
        return ResponseEntity.status(errorCode.getStatus())
                .body(ResponseDto.of(new SchedulePollErrorResponse(errorCode.name()), errorCode.getMessage()));
    }
}
