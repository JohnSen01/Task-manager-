package com.tool.management.service;

public class InvalidTaskStateException extends RuntimeException {
    public InvalidTaskStateException(String message) { super(message); }
}
