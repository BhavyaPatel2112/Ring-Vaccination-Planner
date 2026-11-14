package com.ringvax.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ringvax.api.dto.BenchmarkRunRequest;
import com.ringvax.api.dto.BenchmarkTable;
import com.ringvax.bench.CsvTable;
import com.ringvax.bench.Harness;
import com.ringvax.bench.ScaleBenchmarks;

/**
 * Benchmark results for the benchmark page: the saved CSV files in
 * docs/benchmarks (measured with {@code BenchmarkRunner}), and quick live runs
 * at small sizes so the measurements can be repeated during the demo.
 */
@Service
public class BenchmarkService {

    private final Path dir;

    public BenchmarkService(@Value("${ringvax.benchmark-dir:../docs/benchmarks}") String dir) {
        this.dir = Path.of(dir);
    }

    /** Every saved result table, by file name without ".csv". Empty if the folder is missing. */
    public List<BenchmarkTable> saved() {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(f -> f.getFileName().toString().endsWith(".csv"))
                    .sorted()
                    .map(f -> parse(f.getFileName().toString().replace(".csv", ""), read(f)))
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Runs one experiment now, at three sizes up to {@code size}, and returns its table. */
    public BenchmarkTable run(BenchmarkRunRequest request) {
        int size = request.size() == null ? 100_000 : request.size();
        long seed = request.seed() == null ? PlanService.DEFAULT_SEED : request.seed();
        int[] sizes = {Math.max(100, size / 100), Math.max(100, size / 10), size};
        sizes = Arrays.stream(sizes).distinct().toArray();
        Harness harness = new Harness(1, 3);
        // Live runs answer an API request, so they do not print progress to the console.
        System.setProperty("ringvax.bench.quiet", "true");
        String experiment = request.experiment().trim().toLowerCase();
        CsvTable table = switch (experiment) {
            case "approx-vs-greedy" -> ScaleBenchmarks.approxVsGreedy(sizes, harness, seed);
            case "dsu" -> ScaleBenchmarks.dsuVsBfs(sizes, harness, seed);
            case "bloom" -> ScaleBenchmarks.bloomVsHashSet(sizes, harness, seed);
            default -> throw new IllegalArgumentException(
                    "Unknown experiment '" + request.experiment() + "'. Available: approx-vs-greedy, dsu, bloom");
        };
        return parse(experiment + " (live)", table.toCsv());
    }

    private static BenchmarkTable parse(String name, String csv) {
        List<List<String>> lines = new ArrayList<>();
        for (String line : csv.split("\\R")) {
            if (!line.isBlank()) {
                lines.add(List.of(line.split(",", -1)));
            }
        }
        if (lines.isEmpty()) {
            return new BenchmarkTable(name, List.of(), List.of());
        }
        return new BenchmarkTable(name, lines.get(0), lines.subList(1, lines.size()));
    }

    private static String read(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
