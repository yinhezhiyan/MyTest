package com.example.exception;

import com.example.common.ResultCode;

public class CustomException extends RuntimeException {
    private String code;
    private String msg;

    public CustomException(String msg) {
        this(ResultCode.ERROR, msg);
    }

    public CustomException(String code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public String getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }
}
