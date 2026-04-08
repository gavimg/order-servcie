package com.gavi.microservices.order_service.exception;

public class InventoryServiceUnavailableException extends RuntimeException {

    public InventoryServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
