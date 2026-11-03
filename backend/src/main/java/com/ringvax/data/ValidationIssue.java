package com.ringvax.data;

/**
 * One problem found while reading an input file. Rejected rows and merged
 * duplicates are always reported this way so nothing is dropped silently.
 *
 * @param file    file name, for example "contacts.csv"
 * @param line    1-based line number, or 0 when the issue is about the whole file
 * @param code    short machine-readable code such as {@code SELF_CONTACT}
 * @param message human-readable explanation
 */
public record ValidationIssue(String file, int line, String code, String message) {

    @Override
    public String toString() {
        return line > 0 ? file + " line " + line + ": " + message : file + ": " + message;
    }
}
