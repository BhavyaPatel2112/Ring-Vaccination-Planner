package com.ringvax.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal CSV line reader used by {@link CsvParser}.
 *
 * <p>Supports comma separators, double-quoted fields (so names may contain
 * commas) and doubled quotes inside quoted fields. Blank lines are skipped but
 * still counted, so reported line numbers match what the user sees in a text
 * editor. A UTF-8 byte order mark at the start of the file is ignored.
 */
final class CsvReader {

    /** A non-blank row with its 1-based line number. */
    record Row(int line, String raw) {
    }

    private final BufferedReader in;
    private int lineNumber;

    CsvReader(Reader reader) {
        this.in = reader instanceof BufferedReader b ? b : new BufferedReader(reader);
    }

    /** Returns the next non-blank row, or {@code null} at the end of the file. */
    Row next() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                lineNumber++;
                if (lineNumber == 1 && !line.isEmpty() && line.charAt(0) == '﻿') {
                    line = line.substring(1);
                }
                if (!line.isBlank()) {
                    return new Row(lineNumber, line);
                }
            }
            return null;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Splits one CSV line into trimmed fields.
     *
     * @throws IllegalArgumentException if a quoted field is not closed
     */
    static List<String> split(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                inQuotes = true;
            } else if (c == ',') {
                fields.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if (inQuotes) {
            throw new IllegalArgumentException("unclosed quote");
        }
        fields.add(current.toString().trim());
        return fields;
    }
}
