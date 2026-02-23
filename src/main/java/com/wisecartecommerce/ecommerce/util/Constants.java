package com.wisecartecommerce.ecommerce.util;

public class Constants {
    
    // Pagination
    public static final String DEFAULT_PAGE = "0";
    public static final String DEFAULT_SIZE = "20";
    public static final String DEFAULT_SORT_BY = "createdAt";
    public static final String DEFAULT_SORT_DIR = "desc";
    
    // File upload
    public static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    public static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 5MB
    public static final String[] ALLOWED_IMAGE_TYPES = {
        "image/jpeg", "image/png", "image/gif", "image/webp"
    };
    public static final String[] ALLOWED_DOCUMENT_TYPES = {
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "text/plain"
    };
    
    // Validation
    public static final int MIN_PASSWORD_LENGTH = 6;
    public static final int MAX_NAME_LENGTH = 100;
    public static final int MAX_EMAIL_LENGTH = 255;
    public static final int MAX_PHONE_LENGTH = 20;
    public static final int MAX_ADDRESS_LENGTH = 255;
    public static final int MAX_CITY_LENGTH = 100;
    public static final int MAX_STATE_LENGTH = 100;
    public static final int MAX_COUNTRY_LENGTH = 100;
    public static final int MAX_POSTAL_CODE_LENGTH = 20;
    
    // Order
    public static final String ORDER_NUMBER_PREFIX = "ORD";
    public static final int ORDER_NUMBER_LENGTH = 10;
    
    // Cart
    public static final int MAX_CART_QUANTITY = 100;
    public static final int MIN_CART_QUANTITY = 1;
    
    // Product
    public static final int LOW_STOCK_THRESHOLD = 10;
    public static final int MAX_PRODUCT_NAME_LENGTH = 255;
    public static final int MAX_SKU_LENGTH = 100;
    public static final int MAX_DESCRIPTION_LENGTH = 1000;
    
    // Email
    public static final String VERIFICATION_EMAIL_SUBJECT = "Verify your email address";
    public static final String PASSWORD_RESET_EMAIL_SUBJECT = "Password Reset Request";
    public static final String ORDER_CONFIRMATION_EMAIL_SUBJECT = "Order Confirmation";
    
    // Date format
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    
    // Currency
    public static final String DEFAULT_CURRENCY = "USD";
    public static final String CURRENCY_SYMBOL = "$";
    public static final int DEFAULT_DECIMAL_PLACES = 2;
    
    // Cache
    public static final int CACHE_EXPIRY_MINUTES = 30;
    public static final String PRODUCT_CACHE = "products";
    public static final String CATEGORY_CACHE = "categories";
    public static final String USER_CACHE = "users";
}   