package com.community.athenixback.common.exception;

import lombok.Getter;

@Getter
public class AIServerException extends RuntimeException {
    private final String code;
    private final int httpStatus;

    public AIServerException(String code, String message, int httpStatus) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }
}
