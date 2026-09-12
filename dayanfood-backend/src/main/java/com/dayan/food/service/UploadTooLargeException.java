package com.dayan.food.service;

public class UploadTooLargeException extends RuntimeException {
    public UploadTooLargeException(String message) { super(message); }
}
