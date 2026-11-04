package com.ringvax.bench;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import com.ringvax.engine.cover.ApproxVertexCover;
import com.ringvax.engine.cover.CoverProblem;
import com.ringvax.engine.cover.CoverResult;
import com.ringvax.engine.cover.ExactVertexCover;
import com.ringvax.engine.graph.ContactGraph;
import com.ringvax.engine.select.MedianOfMedians;
import com.ringvax.engine.select.RandomizedQuickselect;

/**
 * Runs the benchmark experiments and writes raw results as CSV.
 *
 * <pre>
 * cd backend
 * ./mvnw -q compile
 * java -cp target/classes com.ringvax.bench.BenchmarkRunner all ../docs/benchmarks
 * </pre>
 *
 * Experiments: {@code quality} (approximation against exact), {@code exact-growth}
 * (exact solver run time and search size on hard networks, against brute force and
 * the approximation) and {@code selection} (Quickselect against median-of-medians
 * and a full sort). All inputs are seeded, so every run produces the same inputs.
 */
public final class BenchmarkRunner {

    private static final long SEED = 2026;

    private BenchmarkRunner() {
    }

    public static void main(String[] args) {
        String which = args.length > 0 ? args[0] : "all";
        Path out = Path.of(args.length > 1 ? args[1] : "../docs/benchmarks");
        writeEnvironment(out);
        if (which.equals("all") || which.equals("quality")) {
            quality(out);
        }
        if (which.equals("all") || which.equals("exact-growth")) {
            exactGrowth(out);
        }
        if (which.equals("all") || which.equals("selection")) {
            selection(out);
        }
    }

    // ------------------------------------------------------------- experiment 1

    /**
     * How close is the 2-approximation to the true minimum? 30 seeded random
     * networks per size and density, each solved by both methods.
     */
    static void quality(Path out) {
        System.out.println("Approximation quality against the exact minimum");
        CsvTable raw = new CsvTable("family", "people", "seed", "contacts", "approx", "exact", "lower_bound", "ratio");
        CsvTable summary = new CsvTable("family", "people", "networks", "mean_ratio", "max_ratio", "share_optimal",
                "mean_approx", "mean_exact");
        ApproxVertexCover approx = new ApproxVertexCover();
        ExactVertexCover exact = new ExactVertexCover(64, Duration.ofSeconds(30));
        int[] sizes = {10, 15, 20, 25, 30, 35, 40};
        double[] densities = {3, 6};
        int networks = 30;

        for (double density : densities) {
            String family = String.format(Locale.ROOT, "random avg %.0f contacts", density);
            for (int n : sizes) {
                double sumRatio = 0;
                double maxRatio = 0;
                int optimal = 0;
                int counted = 0;
                long sumApprox = 0;
                long sumExact = 0;
                for (int s = 0; s < networks; s++) {
                    ContactGraph g = BenchmarkGraphs.random(n, density, SEED * 1000 + n * 37L + s);
                    CoverProblem problem = CoverProblem.allContacts(g);
                    CoverResult a = approx.solve(problem);
                    CoverResult e = exact.solve(problem);
                    Double ratio = e.size() == 0 ? null : (double) a.size() / e.size();
                    raw.add(family, n, s, g.contactCount(), a.size(), e.size(), a.lowerBound(), ratio);
                    sumApprox += a.size();
                    sumExact += e.size();
                    if (ratio != null) {
                        counted++;
                        sumRatio += ratio;
                        maxRatio = Math.max(maxRatio, ratio);
                        if (a.size() == e.size()) {
                            optimal++;
                        }
                    }
                }
                summary.add(family, n, networks, sumRatio / counted, maxRatio, (double) optimal / counted,
                        (double) sumApprox / networks, (double) sumExact / networks);
                System.out.printf(Locale.ROOT, "  %-24s n=%2d  mean ratio %.3f  max %.3f%n", family, n,
                        sumRatio / counted, maxRatio);
            }
        }
        raw.write(out.resolve("approx-vs-exact.csv"));
        summary.write(out.resolve("approx-vs-exact-summary.csv"));
    }

    // ------------------------------------------------------------- experiment 2

    /**
     * Exact solver on random 4-regular networks (no shortcuts apply), from 16 to
     * 64 people: median run time, search tree size, the approximation's time on
     * the same network, and brute force over all subsets where it is feasible.
     */
    static void exactGrowth(Path out) {
        System.out.println("Exact solver growth on random 4-regular networks");
        CsvTable raw = new CsvTable("people", "seed", "contacts", "minimum", "exact_ms", "exact_nodes", "approx_ms",
                "approx_size", "brute_force_ms");
        CsvTable summary = new CsvTable("people", "networks", "median_exact_ms", "median_exact_nodes",
                "median_approx_ms", "median_brute_force_ms");
        Harness harness = new Harness(3, 5);
        Harness slow = new Harness(1, 3);
        ExactVertexCover exact = new ExactVertexCover(64, Duration.ofSeconds(60));
        ApproxVertexCover approx = new ApproxVertexCover();
        int networks = 5;
        int bruteLimit = 24;

        for (int n = 16; n <= 64; n += 4) {
            long[] exactTimes = new long[networks];
            long[] nodes = new long[networks];
            long[] approxTimes = new long[networks];
            List<Long> bruteTimes = new ArrayList<>();
            for (int s = 0; s < networks; s++) {
                ContactGraph g = BenchmarkGraphs.regular(n, 4, SEED + n * 101L + s);
                CoverProblem problem = CoverProblem.allContacts(g);
                CoverResult once = exact.solve(problem);
                Harness.Measurement e = harness.measure(() -> problem, exact::solve);
                Harness.Measurement a = harness.measure(() -> problem, approx::solve);
                Harness.Measurement b = null;
                if (n <= bruteLimit) {
                    b = slow.measure(() -> g, BruteForceCover::minimumSize);
                    bruteTimes.add(b.median());
                }
                exactTimes[s] = e.median();
                nodes[s] = once.searchNodes();
                approxTimes[s] = a.median();
                raw.add(n, s, g.contactCount(), once.size(), e.medianMs(), once.searchNodes(), a.medianMs(),
                        approx.solve(problem).size(), b == null ? null : b.medianMs());
            }
            Double brute = bruteTimes.isEmpty() ? null
                    : Harness.summarise(bruteTimes.stream().mapToLong(Long::longValue).toArray()).medianMs();
            Harness.Measurement e = Harness.summarise(exactTimes);
            long medianNodes = Harness.summarise(nodes).median();
            Harness.Measurement a = Harness.summarise(approxTimes);
            summary.add(n, networks, e.medianMs(), medianNodes, a.medianMs(), brute);
            System.out.printf(Locale.ROOT, "  n=%2d  exact %8.3f ms  %8d nodes  approx %.4f ms  brute %s%n", n,
                    e.medianMs(), medianNodes, a.medianMs(), brute == null ? "-" : String.format(Locale.ROOT, "%.1f ms", brute));
        }
        raw.write(out.resolve("exact-growth.csv"));
        summary.write(out.resolve("exact-growth-summary.csv"));
    }

    // ------------------------------------------------------------- experiment 3

    /**
     * Median of n integers by randomized Quickselect, median-of-medians and a full
     * sort, on random and already-sorted input, from 10,000 to 10,000,000 values.
     */
    static void selection(Path out) {
        System.out.println("Selection: Quickselect against median-of-medians and sorting");
        CsvTable table = new CsvTable("input", "size", "algorithm", "median_ms", "min_ms", "max_ms", "repeats");
        Harness harness = new Harness(3, 7);
        int[] sizes = {10_000, 100_000, 1_000_000, 10_000_000};

        for (String input : new String[] {"random", "sorted"}) {
            for (int n : sizes) {
                int[] base = input.equals("random") ? randomInts(n, SEED + n) : sortedInts(n);
                int k = (n - 1) / 2;
                int expected = sortedCopy(base)[k];
                RandomizedQuickselect quickselect = new RandomizedQuickselect(SEED);

                Harness.Measurement q = harness.measure(base::clone, a -> check(quickselect.select(a, k), expected));
                Harness.Measurement m = harness.measure(base::clone, a -> check(MedianOfMedians.select(a, k), expected));
                Harness.Measurement s = harness.measure(base::clone, a -> {
                    Arrays.sort(a);
                    check(a[k], expected);
                });
                table.add(input, n, "randomized quickselect", q.medianMs(), q.minMs(), q.maxMs(), 7);
                table.add(input, n, "median of medians", m.medianMs(), m.minMs(), m.maxMs(), 7);
                table.add(input, n, "full sort", s.medianMs(), s.minMs(), s.maxMs(), 7);
                System.out.printf(Locale.ROOT, "  %-6s n=%,11d  quickselect %8.2f ms  median-of-medians %8.2f ms  sort %8.2f ms%n",
                        input, n, q.medianMs(), m.medianMs(), s.medianMs());
            }
        }
        table.write(out.resolve("selection.csv"));
    }

    // ------------------------------------------------------------------ helpers

    private static void check(int actual, int expected) {
        if (actual != expected) {
            throw new IllegalStateException("Selection returned " + actual + ", expected " + expected);
        }
    }

    private static int[] randomInts(int n, long seed) {
        Random random = new Random(seed);
        int[] a = new int[n];
        for (int i = 0; i < n; i++) {
            a[i] = random.nextInt();
        }
        return a;
    }

    private static int[] sortedInts(int n) {
        int[] a = new int[n];
        for (int i = 0; i < n; i++) {
            a[i] = i;
        }
        return a;
    }

    private static int[] sortedCopy(int[] a) {
        int[] copy = a.clone();
        Arrays.sort(copy);
        return copy;
    }

    private static void writeEnvironment(Path out) {
        Runtime runtime = Runtime.getRuntime();
        String text = String.join("\n",
                "Benchmark run " + LocalDateTime.now().withNano(0),
                "Java " + System.getProperty("java.version") + " (" + System.getProperty("java.vm.name") + ")",
                "OS " + System.getProperty("os.name") + " " + System.getProperty("os.version") + " " + System.getProperty("os.arch"),
                "CPU cores " + runtime.availableProcessors(),
                "Max heap " + runtime.maxMemory() / (1024 * 1024) + " MB",
                "JVM arguments " + ManagementFactory.getRuntimeMXBean().getInputArguments(),
                "Seed " + SEED,
                "");
        try {
            Files.createDirectories(out);
            Files.writeString(out.resolve("environment.txt"), text);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
