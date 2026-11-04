package com.ringvax.bench;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** A small table written as CSV, for raw benchmark results. */
public final class CsvTable {

    private final List<String> header;
    private final List<List<String>> rows = new ArrayList<>();

    public CsvTable(String... header) {
        this.header = List.of(header);
    }

    /** Adds a row; numbers are written without locale-specific separators. */
    public CsvTable add(Object... values) {
        if (values.length != header.size()) {
            throw new IllegalArgumentException("expected " + header.size() + " values, got " + values.length);
        }
        List<String> row = new ArrayList<>();
        for (Object value : values) {
            row.add(format(value));
        }
        rows.add(row);
        return this;
    }

    public int size() {
        return rows.size();
    }

    public String toCsv() {
        StringBuilder out = new StringBuilder();
        out.append(String.join(",", header.stream().map(CsvTable::escape).toList())).append('\n');
        for (List<String> row : rows) {
            out.append(String.join(",", row.stream().map(CsvTable::escape).toList())).append('\n');
        }
        return out.toString();
    }

    public void write(Path file) {
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            Files.writeString(file, toCsv());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String format(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Double d) {
            return String.format(Locale.ROOT, "%.4f", d);
        }
        return value.toString();
    }

    private static String escape(String text) {
        if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
            return '"' + text.replace("\"", "\"\"") + '"';
        }
        return text;
    }
}
