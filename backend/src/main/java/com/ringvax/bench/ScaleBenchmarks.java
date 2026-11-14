package com.ringvax.bench;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

import com.ringvax.data.SyntheticNetworkGenerator;
import com.ringvax.engine.bloom.BloomExperiment;
import com.ringvax.engine.bloom.BloomFilter;
import com.ringvax.engine.cover.ApproxVertexCover;
import com.ringvax.engine.cover.CoverProblem;
import com.ringvax.engine.cover.CoverResult;
import com.ringvax.engine.cover.GreedyVertexCover;
import com.ringvax.engine.dsu.DisjointSetUnion;
import com.ringvax.engine.graph.Contact;
import com.ringvax.engine.graph.ContactGraph;

/**
 * Benchmarks for the greedy method, the disjoint-set union and the Bloom filter,
 * each against its baseline. Used by {@link BenchmarkRunner} (full sizes, results
 * saved as CSV) and by the API's quick live run (small sizes).
 *
 * <ul>
 *   <li>{@link #approxVsGreedy}: run time and plan size on synthetic towns of growing size.</li>
 *   <li>{@link #dsuVsBfs}: one cluster update by union-find against recomputing every
 *       cluster with a breadth-first search.</li>
 *   <li>{@link #bloomVsHashSet}: memory, lookup time and false positive rate against a
 *       {@link HashSet} of the same keys.</li>
 * </ul>
 * All inputs are seeded.
 */
public final class ScaleBenchmarks {

    private ScaleBenchmarks() {
    }

    // ------------------------------------------------------------------ approximation against greedy

    /**
     * @param contactTargets approximate number of contacts per network (10 contacts per person on average)
     */
    public static CsvTable approxVsGreedy(int[] contactTargets, Harness harness, long seed) {
        CsvTable table = new CsvTable("people", "contacts", "approx_ms", "greedy_ms", "approx_size", "greedy_size",
                "lower_bound", "greedy_over_approx");
        ApproxVertexCover approx = new ApproxVertexCover();
        GreedyVertexCover greedy = new GreedyVertexCover();
        for (int target : contactTargets) {
            int people = Math.max(20, target / 5);
            ContactGraph g = SyntheticNetworkGenerator.generate(people, 10, seed + target);
            CoverProblem problem = CoverProblem.allContacts(g);
            CoverResult a = approx.solve(problem);
            CoverResult gr = greedy.solve(problem);
            check(problem.isValidCover(a.people()) && problem.isValidCover(gr.people()), "invalid cover");
            Harness.Measurement ta = harness.measure(() -> problem, approx::solve);
            Harness.Measurement tg = harness.measure(() -> problem, greedy::solve);
            table.add(people, g.contactCount(), ta.medianMs(), tg.medianMs(), a.size(), gr.size(), a.lowerBound(),
                    (double) gr.size() / a.size());
            log("  contacts=%,10d  approx %8.2f ms (%,d doses)  greedy %8.2f ms (%,d doses)", g.contactCount(),
                    ta.medianMs(), a.size(), tg.medianMs(), gr.size());
        }
        return table;
    }

    // ------------------------------------------------------------------ union-find against BFS

    /**
     * Builds a network, then measures (a) all contact insertions with union-find,
     * and (b) one full recomputation of the clusters by breadth-first search at the
     * final size, which is what the baseline pays after every new contact.
     */
    public static CsvTable dsuVsBfs(int[] contactTargets, Harness harness, long seed) {
        CsvTable table = new CsvTable("people", "insertions", "dsu_total_ms", "dsu_per_update_us", "bfs_per_update_ms",
                "bfs_total_estimate_ms", "speedup_per_update", "clusters");
        for (int target : contactTargets) {
            int people = Math.max(20, target / 4);
            ContactGraph g = SyntheticNetworkGenerator.generate(people, 8, seed + target);
            int e = g.contactCount();
            int[] a = new int[e];
            int[] b = new int[e];
            for (int c = 0; c < e; c++) {
                Contact link = g.contact(c);
                a[c] = link.a();
                b[c] = link.b();
            }
            int[] clusters = new int[1];
            Harness.Measurement dsu = harness.measure(() -> new DisjointSetUnion(people), d -> {
                for (int c = 0; c < e; c++) {
                    d.union(a[c], b[c]);
                }
                clusters[0] = d.setCount();
            });
            int[] bfsClusters = new int[1];
            Harness.Measurement bfs = harness.measure(() -> g, graph -> bfsClusters[0] = bfsComponents(graph));
            check(clusters[0] == bfsClusters[0], "union-find and BFS disagree on the number of clusters");
            double perUpdateUs = dsu.medianMs() * 1000 / e;
            table.add(people, e, dsu.medianMs(), perUpdateUs, bfs.medianMs(), bfs.medianMs() * e,
                    bfs.medianMs() * 1000 / perUpdateUs, clusters[0]);
            log("  insertions=%,10d  union-find %8.2f ms total (%.3f us each)  BFS %8.2f ms per update", e,
                    dsu.medianMs(), perUpdateUs, bfs.medianMs());
        }
        return table;
    }

    /** Number of connected components by breadth-first search: the O(V + E) baseline. */
    static int bfsComponents(ContactGraph g) {
        int n = g.personCount();
        boolean[] seen = new boolean[n];
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        int components = 0;
        for (int s = 0; s < n; s++) {
            if (seen[s]) {
                continue;
            }
            components++;
            seen[s] = true;
            queue.add(s);
            while (!queue.isEmpty()) {
                int u = queue.poll();
                for (int i = 0; i < g.degree(u); i++) {
                    int v = g.neighbour(u, i);
                    if (!seen[v]) {
                        seen[v] = true;
                        queue.add(v);
                    }
                }
            }
        }
        return components;
    }

    // ------------------------------------------------------------------ Bloom filter against HashSet

    /**
     * Filters sized for a 1% false positive rate with the optimal k, against a HashSet of the same keys.
     * The HashSet memory is the per-key estimate from {@link BloomExperiment}; measuring heap use directly
     * depends on when the garbage collector runs and was too noisy to report.
     */
    public static CsvTable bloomVsHashSet(int[] keyCounts, Harness harness, long seed) {
        CsvTable table = new CsvTable("keys", "bits", "k", "theoretical_rate", "measured_rate", "relative_error",
                "bloom_bytes", "hashset_bytes_estimate", "memory_ratio", "bloom_query_ns",
                "hashset_query_ns");
        for (int n : keyCounts) {
            long m = BloomFilter.optimalBits(n, 0.01);
            int k = BloomFilter.optimalHashCount(m, n);
            BloomExperiment.Result r = BloomExperiment.run(m, k, n, 200_000, seed + n);

            String[] keys = new String[n];
            String[] probes = new String[100_000];
            Random random = new Random(seed);
            for (int i = 0; i < n; i++) {
                keys[i] = "V" + random.nextInt(1_000_000) + "|2026-11-" + (1 + random.nextInt(28)) + "T" + i;
            }
            for (int i = 0; i < probes.length; i++) {
                probes[i] = i % 2 == 0 ? keys[random.nextInt(n)] : "Q" + random.nextInt() + "|probe";
            }
            BloomFilter filter = new BloomFilter(m, k);
            Set<String> set = new HashSet<>();
            for (String key : keys) {
                set.add(key);
            }
            for (String key : keys) {
                filter.add(key);
            }
            int[] sink = new int[1];
            Harness.Measurement bloomQuery = harness.measure(() -> probes, p -> {
                for (String s : p) {
                    if (filter.mightContain(s)) {
                        sink[0]++;
                    }
                }
            });
            Harness.Measurement setQuery = harness.measure(() -> probes, p -> {
                for (String s : p) {
                    if (set.contains(s)) {
                        sink[0]++;
                    }
                }
            });
            double relative = r.theoreticalRate() == 0 ? 0 : Math.abs(r.measuredRate() - r.theoreticalRate())
                    / r.theoreticalRate();
            table.add(n, m, k, r.theoreticalRate(), r.measuredRate(), relative, r.bloomBytes(), r.hashSetBytes(),
                    (double) r.hashSetBytes() / r.bloomBytes(), bloomQuery.median() / (double) probes.length,
                    setQuery.median() / (double) probes.length);
            log("  keys=%,10d  theory %.4f%%  measured %.4f%%  bloom %,d B  hash set ~%,d B (%.1fx)", n,
                    r.theoreticalRate() * 100, r.measuredRate() * 100, r.bloomBytes(), r.hashSetBytes(),
                    (double) r.hashSetBytes() / r.bloomBytes());
        }
        return table;
    }

    // ------------------------------------------------------------------ helpers

    private static void check(boolean ok, String message) {
        if (!ok) {
            throw new IllegalStateException(message);
        }
    }

    private static void log(String format, Object... args) {
        if (!Boolean.getBoolean("ringvax.bench.quiet")) {
            System.out.println(String.format(Locale.ROOT, format, args));
        }
    }
}
