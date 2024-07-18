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
        return new ResponseEntity<>(ResponseDto.of(-1,ex.getMessage()),HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MyException.class)
    public ResponseEntity<ResponseDto<Integer>> MyException(MyException ex){
        log.error("예외 발생 msg:{}",ex.getErrorCode().getMessage());
        return new ResponseEntity<>(ResponseDto.of(-1,ex.getErrorCode().getMessage()),ex.getErrorCode().getStatus());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseDto<Integer>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex){
        BindingResult bindingResult = ex.getBindingResult();
        FieldError fieldError = bindingResult.getFieldError();
        String message = fieldError.getDefaultMessage();
        log.error("유효성 검사 예외 발생 msg:{}",message);
        return new ResponseEntity<>(ResponseDto.of(-1,message),HttpStatus.BAD_REQUEST);
    }


    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ResponseDto<Integer>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        String errorMessage = "요청한 JSON 데이터를 읽을 수 없습니다: " + ex.getMessage();
        return new ResponseEntity<>(ResponseDto.of(-1,errorMessage), HttpStatus.BAD_REQUEST);
    }

}
