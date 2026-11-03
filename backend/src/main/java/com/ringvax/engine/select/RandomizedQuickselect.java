package com.ringvax.engine.select;

import java.util.Random;

/**
 * Randomized Quickselect: finds the k-th smallest value without sorting.
 *
 * <pre>
 * select(a, k):
 *     lo = 0, hi = n - 1
 *     loop:
 *         pivot = a[random index in lo..hi]
 *         three-way partition lo..hi into  &lt; pivot | == pivot | &gt; pivot
 *         if k is in the "&lt;" part: hi = end of "&lt;" part
 *         else if k is in the "&gt;" part: lo = start of "&gt;" part
 *         else: return pivot
 * </pre>
 *
 * <p>Used for the budget mode (90th percentile of uncovered-link counts), the
 * contact tracing quality warnings (99th percentile of contacts), simulation
 * percentiles and median benchmark times.
 *
 * <p>The three-way partition keeps it fast when many values are equal, which is
 * common for contact counts. The pivot is chosen with a seeded {@link Random},
 * so runs are repeatable.
 *
 * <p>Time complexity: expected O(n). With a random pivot, each round keeps on
 * average at most 3/4 of the range, so the expected total work is a geometric
 * series bounded by about 4n comparisons. Worst case O(n^2) if every pivot is
 * the smallest or largest value, which is very unlikely. Space complexity: O(1)
 * extra; the loop is iterative and the array is reordered in place.
 */
public class RandomizedQuickselect {

    private final Random random;

    public RandomizedQuickselect(long seed) {
        this.random = new Random(seed);
    }

    /**
     * Returns the k-th smallest value (k = 0 is the minimum). The array is
     * reordered in place.
     *
     * @throws IllegalArgumentException if the array is empty or k is out of range
     */
    public int select(int[] a, int k) {
        checkArguments(a.length, k);
        int lo = 0;
        int hi = a.length - 1;
        while (true) {
            if (lo == hi) {
                return a[lo];
            }
            int pivot = a[lo + random.nextInt(hi - lo + 1)];
            // Invariant: a[lo..lt-1] < pivot, a[lt..i-1] == pivot, a[gt+1..hi] > pivot.
            int lt = lo;
            int gt = hi;
            int i = lo;
            while (i <= gt) {
                if (a[i] < pivot) {
                    swap(a, lt++, i++);
                } else if (a[i] > pivot) {
                    swap(a, i, gt--);
                } else {
                    i++;
                }
            }
            if (k < lt) {
                hi = lt - 1;
            } else if (k > gt) {
                lo = gt + 1;
            } else {
                return pivot;
            }
        }
    }

    /** Same as {@link #select(int[], int)} for {@code long} values, such as run times in nanoseconds. */
    public long select(long[] a, int k) {
        checkArguments(a.length, k);
        int lo = 0;
        int hi = a.length - 1;
        while (true) {
            if (lo == hi) {
                return a[lo];
            }
            long pivot = a[lo + random.nextInt(hi - lo + 1)];
            int lt = lo;
            int gt = hi;
            int i = lo;
            while (i <= gt) {
                if (a[i] < pivot) {
                    swap(a, lt++, i++);
                } else if (a[i] > pivot) {
                    swap(a, i, gt--);
                } else {
                    i++;
                }
            }
            if (k < lt) {
                hi = lt - 1;
            } else if (k > gt) {
                lo = gt + 1;
            } else {
                return pivot;
            }
        }
    }

    /**
     * Returns the p-th percentile (0 to 100) using the nearest-rank method, for
     * example p = 90 gives the value that at least 90% of entries are at or below.
     * The array is reordered in place.
     */
    public int percentile(int[] a, double p) {
        return select(a, percentileRank(a.length, p));
    }

    /** Median (lower median for an even length). The array is reordered in place. */
    public long median(long[] a) {
        return select(a, (a.length - 1) / 2);
    }

    /**
     * 0-based index of the p-th percentile in a sorted array of length n, by the
     * nearest-rank method: ceil(p / 100 * n) - 1, clamped to 0..n-1.
     */
    public static int percentileRank(int n, double p) {
        if (n <= 0) {
            throw new IllegalArgumentException("Cannot take a percentile of an empty array");
        }
        if (!(p >= 0 && p <= 100)) {
            throw new IllegalArgumentException("Percentile must be between 0 and 100, got " + p);
        }
        int rank = (int) Math.ceil(p / 100.0 * n) - 1;
        return Math.max(0, Math.min(n - 1, rank));
    }

    static void checkArguments(int length, int k) {
        if (length == 0) {
            throw new IllegalArgumentException("Cannot select from an empty array");
        }
        if (k < 0 || k >= length) {
            throw new IllegalArgumentException("k must be between 0 and " + (length - 1) + ", got " + k);
        }
    }

    private static void swap(int[] a, int i, int j) {
        int t = a[i];
        a[i] = a[j];
        a[j] = t;
    }

    private static void swap(long[] a, int i, int j) {
        long t = a[i];
        a[i] = a[j];
        a[j] = t;
    }
}
