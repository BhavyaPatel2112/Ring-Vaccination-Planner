package com.ringvax.engine.select;

/**
 * Deterministic selection with the median-of-medians pivot (Blum, Floyd, Pratt,
 * Rivest and Tarjan): the worst-case linear baseline for {@link RandomizedQuickselect}.
 *
 * <pre>
 * select(a, lo, hi, k):
 *     loop:
 *         if hi - lo &lt; 5: insertion sort a[lo..hi] and return a[k]
 *         split a[lo..hi] into groups of 5, sort each group, and move each
 *             group's median to the front of the range
 *         pivot = select(medians, middle)          // recursive call
 *         three-way partition a[lo..hi] around pivot
 *         continue in the part that contains k, or return pivot
 * </pre>
 *
 * <p><b>Why it is linear.</b> At least half of the n/5 group medians are &ge; the
 * pivot, and each of those groups has at least 3 values &ge; the pivot, so at
 * least about 3n/10 values are &ge; the pivot (and likewise &le; it). Each round
 * therefore keeps at most about 7n/10 values. With the recursive call on n/5
 * medians, T(n) &le; T(n/5) + T(7n/10) + O(n), and because 1/5 + 7/10 &lt; 1
 * this solves to O(n) in the worst case.
 *
 * <p>The constant factor is much larger than Quickselect's, which is what the
 * benchmarks show. Space complexity: O(log n) for the recursion on the medians;
 * the array is reordered in place.
 */
public final class MedianOfMedians {

    private static final int GROUP = 5;

    private MedianOfMedians() {
    }

    /**
     * Returns the k-th smallest value (k = 0 is the minimum). The array is
     * reordered in place.
     *
     * @throws IllegalArgumentException if the array is empty or k is out of range
     */
    public static int select(int[] a, int k) {
        RandomizedQuickselect.checkArguments(a.length, k);
        return select(a, 0, a.length - 1, k);
    }

    private static int select(int[] a, int lo, int hi, int k) {
        while (true) {
            if (hi - lo < GROUP) {
                insertionSort(a, lo, hi);
                return a[k];
            }
            int pivot = pivot(a, lo, hi);
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

    /** Median of the group medians of a[lo..hi]. */
    private static int pivot(int[] a, int lo, int hi) {
        int medians = 0;
        for (int start = lo; start <= hi; start += GROUP) {
            int end = Math.min(start + GROUP - 1, hi);
            insertionSort(a, start, end);
            int median = start + (end - start) / 2;
            swap(a, lo + medians, median);
            medians++;
        }
        int last = lo + medians - 1;
        return select(a, lo, last, lo + (medians - 1) / 2);
    }

    private static void insertionSort(int[] a, int lo, int hi) {
        for (int i = lo + 1; i <= hi; i++) {
            int value = a[i];
            int j = i - 1;
            while (j >= lo && a[j] > value) {
                a[j + 1] = a[j];
                j--;
            }
            a[j + 1] = value;
        }
    }

    private static void swap(int[] a, int i, int j) {
        int t = a[i];
        a[i] = a[j];
        a[j] = t;
    }
}
