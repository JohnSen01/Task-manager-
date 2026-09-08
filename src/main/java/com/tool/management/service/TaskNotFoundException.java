package com.tool.management.service;

public class TaskNotFoundException extends RuntimeException {
    public TaskNotFoundException(String message) { super(message); }
}
