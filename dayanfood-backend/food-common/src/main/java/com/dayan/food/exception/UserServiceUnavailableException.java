package com.dayan.food.exception;
public class UserServiceUnavailableException extends RuntimeException {
    public UserServiceUnavailableException(Throwable cause) { super("User service unavailable", cause); }
}
