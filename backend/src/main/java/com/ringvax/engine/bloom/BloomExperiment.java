package com.ringvax.engine.bloom;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Measures a Bloom filter against the theory and against an exact
 * {@link HashSet}: inserts n random keys, checks that every one is found (no
 * false negatives), then asks about keys that were never added and counts how
 * many come back "possibly present".
 *
 * <p>The memory of the exact set is an estimate for a 64-bit JVM with
 * compressed references: per key, a hash table node (32 bytes), its share of the
 * table (4 bytes / 0.75 load factor), the String object (24 bytes) and its byte
 * array (16-byte header plus one byte per character, rounded up to 8).
 *
 * <p>Time complexity: O((n + q) (k + L)) for n keys and q queries of length L.
 * Space complexity: O(m) bits plus O(n) for the exact set used as a reference.
 */
public final class BloomExperiment {

    public record Result(long m, int k, int n, int queries, int optimalK, double bitsPerKey, double theoreticalRate,
                         double measuredRate, int falsePositives, int falseNegatives, double fillRatio,
                         long bloomBytes, long hashSetBytes, double memoryRatio, double insertNanosPerKey,
                         double queryNanosPerKey) {
    }

    private BloomExperiment() {
    }

    /**
     * @param m       bits
     * @param k       hash functions
     * @param n       keys to insert (at least 1)
     * @param queries non-member keys to ask about (at least 1)
     * @param seed    seed for the random keys
     */
    public static Result run(long m, int k, int n, int queries, long seed) {
        if (n < 1 || queries < 1) {
            throw new IllegalArgumentException("n and the number of queries must be at least 1");
        }
        BloomFilter filter = new BloomFilter(m, k);
        Random random = new Random(seed);
        String[] members = new String[n];
        Set<String> exact = new HashSet<>();
        long hashSetBytes = 0;
        for (int i = 0; i < n; i++) {
            // Shaped like the real keys: venue id plus an hour bucket.
            members[i] = "V" + random.nextInt(1_000_000) + "|2026-11-" + (1 + random.nextInt(28)) + "T" + i;
            exact.add(members[i]);
            hashSetBytes += 32 + Math.round(4 / 0.75) + 24 + roundUp8(16 + members[i].length());
        }

        long start = System.nanoTime();
        for (String key : members) {
            filter.add(key);
        }
        double insertNanos = (double) (System.nanoTime() - start) / n;

        int falseNegatives = 0;
        for (String key : members) {
            if (!filter.mightContain(key)) {
                falseNegatives++;
            }
        }

        int falsePositives = 0;
        start = System.nanoTime();
        for (int i = 0; i < queries; i++) {
            String probe = "Q" + random.nextInt(1_000_000) + "|probe|" + i;
            if (filter.mightContain(probe) && !exact.contains(probe)) {
                falsePositives++;
            }
        }
        double queryNanos = (double) (System.nanoTime() - start) / queries;

        return new Result(m, k, n, queries, BloomFilter.optimalHashCount(m, n), (double) m / n,
                BloomFilter.theoreticalRate(m, k, n), (double) falsePositives / queries, falsePositives,
                falseNegatives, filter.fillRatio(), filter.memoryBytes(), hashSetBytes,
                (double) hashSetBytes / filter.memoryBytes(), insertNanos, queryNanos);
    }

    private static long roundUp8(long bytes) {
        return (bytes + 7) / 8 * 8;
    }
}
