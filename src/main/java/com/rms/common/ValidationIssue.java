package com.rms.common;

public record ValidationIssue(String severity, String field, String message) {
    public static final String ERROR = "ERROR";
    public static final String WARNING = "WARNING";

    public static ValidationIssue error(String field, String message) {
        return new ValidationIssue(ERROR, field, message);
    }

    public static ValidationIssue warning(String field, String message) {
        return new ValidationIssue(WARNING, field, message);
    }

    public boolean isError() {
        return ERROR.equals(severity);
    }
}
