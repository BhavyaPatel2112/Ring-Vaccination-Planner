package com.ringvax.data;

import java.util.List;

/**
 * Thrown when an input file cannot be used at all: it is empty, its header is
 * wrong, it has no valid people, or it names a confirmed case that does not exist.
 * The API turns this into a 400 response that lists every issue.
 */
public class CsvFormatException extends RuntimeException {

    private final transient List<ValidationIssue> issues;

    public CsvFormatException(List<ValidationIssue> issues) {
        super(summary(issues));
        this.issues = List.copyOf(issues);
    }

    public CsvFormatException(ValidationIssue issue) {
        this(List.of(issue));
    }

    public List<ValidationIssue> issues() {
        return issues;
    }

    private static String summary(List<ValidationIssue> issues) {
        if (issues.isEmpty()) {
            return "Invalid input file";
        }
        String first = issues.get(0).toString();
        return issues.size() == 1 ? first : first + " (and " + (issues.size() - 1) + " more)";
    }
}
