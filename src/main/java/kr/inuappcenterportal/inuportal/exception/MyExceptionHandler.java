package kr.inuappcenterportal.inuportal.exception;


import kr.inuappcenterportal.inuportal.dto.ResponseDto;
import kr.inuappcenterportal.inuportal.exception.ex.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolationException;

@Slf4j
@RestControllerAdvice
public class MyExceptionHandler {

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ResponseDto<Integer>> handleConstraintViolationException(ConstraintViolationException ex){
        log.error("유효성 검사 예외 발생 msg:{}",ex.getMessage());
        return new ResponseEntity<>(new ResponseDto<>(-1,ex.getMessage()),HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MyBadRequestException.class)
    public ResponseEntity<ResponseDto<Integer>> MyBadRequestException(MyBadRequestException ex){
        log.error("잘못된 요청 예외 발생 :{}",ex.getErrorCode().getMessage());
        return new ResponseEntity<>(new ResponseDto<>(-1,ex.getErrorCode().getMessage()),ex.getErrorCode().getStatus());
    }

    @ExceptionHandler(MyNotFoundException.class)
    public ResponseEntity<ResponseDto<Integer>> MyNotFoundException(MyNotFoundException ex){
        log.error("존재하지 않는 값 예외 발생 msg:{}",ex.getErrorCode().getMessage());
        return new ResponseEntity<>(new ResponseDto<>(-1,ex.getErrorCode().getMessage()),ex.getErrorCode().getStatus());
    }

    @ExceptionHandler(MyDuplicateException.class)
    public ResponseEntity<ResponseDto<Integer>> MyDuplicateException(MyDuplicateException ex){
        log.error("중복 값 예외 발생 msg:{}",ex.getErrorCode().getMessage());
        return new ResponseEntity<>(new ResponseDto<>(-1,ex.getErrorCode().getMessage()),ex.getErrorCode().getStatus());
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseDto<Integer>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex){
        BindingResult bindingResult = ex.getBindingResult();
        FieldError fieldError = bindingResult.getFieldError();
        String message = fieldError.getDefaultMessage();
        log.error("유효성 검사 예외 발생 msg:{}",message);
        return new ResponseEntity<>(new ResponseDto<>(-1,message),HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MyJwtException.class)
    public ResponseEntity<ResponseDto<Integer>> handleJwtException(MyJwtException ex){
        log.error("jwt 토큰 예외 발생 msg:{}",ex.getErrorCode().getMessage());
        return new ResponseEntity<>(new ResponseDto<>(-1,ex.getErrorCode().getMessage()),ex.getErrorCode().getStatus());
    }

    @ExceptionHandler(MyUnauthorizedException.class)
    public ResponseEntity<ResponseDto<Integer>> MyUnauthorizedException(MyUnauthorizedException ex){
        log.error("로그인 실패 msg:{}",ex.getErrorCode().getMessage());
        return new ResponseEntity<>(new ResponseDto<>(-1,ex.getErrorCode().getMessage()),ex.getErrorCode().getStatus());
    }

    @ExceptionHandler(MyNotPermittedException.class)
    public ResponseEntity<ResponseDto<Integer>> MyNotPermittedException(MyNotPermittedException ex){
        log.error("다른 사람의 할일 접근 시도 msg:{}",ex.getErrorCode().getMessage());
        return new ResponseEntity<>(new ResponseDto<>(-1,ex.getErrorCode().getMessage()),ex.getErrorCode().getStatus());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ResponseDto<Integer>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        String errorMessage = "요청한 JSON 데이터를 읽을 수 없습니다: " + ex.getMessage();
        return new ResponseEntity<>(new ResponseDto<>(-1,errorMessage), HttpStatus.BAD_REQUEST);
    }

}
