package kr.inuappcenterportal.inuportal.exception.ex;

import lombok.Getter;

@Getter
public class MyException extends RuntimeException{
    private MyErrorCode errorCode;

    public MyException(MyErrorCode errorCode){
        this.errorCode = errorCode;
    }
}
