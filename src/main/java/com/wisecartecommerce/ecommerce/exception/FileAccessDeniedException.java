package com.wisecartecommerce.ecommerce.exception;

public class FileAccessDeniedException extends RuntimeException {
    
    public FileAccessDeniedException() {
        super();
    }
    
    public FileAccessDeniedException(String message) {
        super(message);
    }
    
    public FileAccessDeniedException(String message, Throwable cause) {
        super(message, cause);
    }
}