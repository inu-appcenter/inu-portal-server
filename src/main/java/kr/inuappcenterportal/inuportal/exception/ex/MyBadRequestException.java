package kr.inuappcenterportal.inuportal.exception.ex;

import lombok.Getter;

@Getter
public class MyBadRequestException extends RuntimeException{
    private MyErrorCode errorCode;

    public  MyBadRequestException(MyErrorCode errorCode){
        this.errorCode = errorCode;

    }
}
