package com.tool.management.repository;

/**
 * Unchecked exception wrapping JDBC SQLExceptions from the repository layer.
 */
public class RepositoryException extends RuntimeException {
    public RepositoryException(String message) { super(message); }
    public RepositoryException(String message, Throwable cause) { super(message, cause); }
}
