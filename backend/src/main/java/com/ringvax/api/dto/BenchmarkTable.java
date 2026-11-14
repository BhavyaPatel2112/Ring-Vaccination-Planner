package com.ringvax.api.dto;

import java.util.List;

/** One benchmark result table, read from a CSV file or produced by a live run. */
public record BenchmarkTable(String name, List<String> header, List<List<String>> rows) {
}
