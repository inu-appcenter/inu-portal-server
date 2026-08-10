package kr.inuappcenterportal.inuportal.global.exception;


import jakarta.validation.ConstraintViolationException;
import kr.inuappcenterportal.inuportal.domain.academic.exception.AcademicException;
import kr.inuappcenterportal.inuportal.global.dto.ResponseDto;
import kr.inuappcenterportal.inuportal.global.exception.ex.MyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class MyExceptionHandler {

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ResponseDto<Integer>> handleConstraintViolationException(ConstraintViolationException ex) {
        log.error("유효성 검사 예외 발생 msg:{}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ResponseDto.of(-1, ex.getMessage()));
    }

    @ExceptionHandler(MyException.class)
    public ResponseEntity<ResponseDto<Integer>> MyException(MyException ex) {
        log.error("예외 발생 msg:{}", ex.getErrorCode().getMessage());
        return ResponseEntity.status(ex.getErrorCode().getStatus()).body(ResponseDto.of(-1, ex.getErrorCode().getMessage()));
    }

    @ExceptionHandler(AcademicException.class)
    public ResponseEntity<ResponseDto<Integer>> handleAcademicException(AcademicException ex) {
        log.error("학적 조회 예외 발생 msg:{}", ex.getMessage());
        return ResponseEntity.status(ex.getStatus()).body(ResponseDto.of(-1, ex.getMessage()));
    }

    // @Valid, RequestBody DTO 검증 실패 처리
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseDto<Integer>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        BindingResult bindingResult = ex.getBindingResult();
        FieldError fieldError = bindingResult.getFieldError();
        String message = fieldError.getDefaultMessage();
        log.error("유효성 검사 예외 발생 msg:{}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ResponseDto.of(-1, message));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ResponseDto<Integer>> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        log.error("요청 파라미터 타입 변환 예외 발생 parameter:{}, value:{}", ex.getName(), ex.getValue());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ResponseDto.of(-1, "잘못된 입력값입니다."));
    }


    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ResponseDto<Integer>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        String errorMessage = "요청한 JSON 데이터를 읽을 수 없습니다: " + ex.getMessage();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ResponseDto.of(-1, errorMessage));
    }

}
