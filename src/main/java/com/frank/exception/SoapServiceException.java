package com.frank.exception;

public class SoapServiceException extends RuntimeException {

    public SoapServiceException(String message) {
        super(message);
    }

    public SoapServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
