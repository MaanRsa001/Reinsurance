package com.rms.common;

import java.util.List;

public class ValidationFailedException extends RuntimeException {
    private final transient List<ValidationIssue> issues;

    public ValidationFailedException(List<ValidationIssue> issues) {
        super("Validation failed with " + issues.stream().filter(ValidationIssue::isError).count() + " error(s)");
        this.issues = issues;
    }

    public List<ValidationIssue> getIssues() { return issues; }
}
