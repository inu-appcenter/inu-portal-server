package kr.inuappcenterportal.inuportal.exception.ex;

import lombok.Getter;

@Getter
public class MyDuplicateException extends RuntimeException{
    private MyErrorCode errorCode;

    public  MyDuplicateException(MyErrorCode errorCode){
        this.errorCode = errorCode;
    }
}
