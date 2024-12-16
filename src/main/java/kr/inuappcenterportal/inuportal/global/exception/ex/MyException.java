package kr.inuappcenterportal.inuportal.global.exception.ex;

import lombok.Getter;

@Getter
public class MyException extends RuntimeException{
    private MyErrorCode errorCode;

    public MyException(MyErrorCode errorCode){
        this.errorCode = errorCode;
    }
}
