package com.github.schmeedy.zonky.java;

public class HttpClientException extends RuntimeException {
    public HttpClientException(String message) {
        super(message);
    }

    public HttpClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
