package com.trading.service;

public class MapServiceUnavailableException extends RuntimeException {
    public MapServiceUnavailableException(String message) {
        super(message);
    }

    public MapServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
