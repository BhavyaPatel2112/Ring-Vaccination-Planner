package com.ringvax.engine.select;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Random;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for both selection algorithms. Each is checked against a sorted copy,
 * which is the obvious correct answer.
 */
class SelectionTest {

    private static int[] randomArray(Random random, int n, int maxValue) {
        int[] a = new int[n];
        for (int i = 0; i < n; i++) {
            a[i] = random.nextInt(maxValue);
        }
        return a;
    }

    @Nested
    class Quickselect {

        private final RandomizedQuickselect quickselect = new RandomizedQuickselect(42);

        @Test
        void singleElement() {
            assertEquals(7, quickselect.select(new int[] {7}, 0));
        }

        @Test
        void smallestAndLargest() {
            int[] a = {9, 3, 7, 1, 8, 2};
            assertEquals(1, quickselect.select(a.clone(), 0));
            assertEquals(9, quickselect.select(a.clone(), a.length - 1));
        }

        @Test
        void allValuesEqual() {
            int[] a = new int[10_000];
            Arrays.fill(a, 4);
            assertEquals(4, quickselect.select(a, 5_000));
        }

        @Test
        void matchesSortingOnRandomInputs() {
            Random random = new Random(1);
            for (int trial = 0; trial < 300; trial++) {
                int n = 1 + random.nextInt(500);
                int[] a = randomArray(random, n, trial % 2 == 0 ? 10 : 1_000_000);
                int k = random.nextInt(n);
                int[] sorted = a.clone();
                Arrays.sort(sorted);
                assertEquals(sorted[k], quickselect.select(a, k), "trial " + trial);
            }
        }

        @Test
        void invalidArgumentsAreRejected() {
            assertThrows(IllegalArgumentException.class, () -> quickselect.select(new int[0], 0));
            assertThrows(IllegalArgumentException.class, () -> quickselect.select(new int[] {1, 2}, 2));
            assertThrows(IllegalArgumentException.class, () -> quickselect.select(new int[] {1, 2}, -1));
            assertThrows(IllegalArgumentException.class, () -> quickselect.percentile(new int[] {1}, 101));
            assertThrows(IllegalArgumentException.class, () -> RandomizedQuickselect.percentileRank(0, 50));
        }

        @Test
        void percentilesUseNearestRank() {
            int[] values = new int[100];
            for (int i = 0; i < 100; i++) {
                values[i] = 100 - i; // 100 down to 1
            }
            assertEquals(90, quickselect.percentile(values.clone(), 90));
            assertEquals(50, quickselect.percentile(values.clone(), 50));
            assertEquals(1, quickselect.percentile(values.clone(), 0));
            assertEquals(100, quickselect.percentile(values.clone(), 100));
            assertEquals(0, RandomizedQuickselect.percentileRank(10, 5));
            assertEquals(8, RandomizedQuickselect.percentileRank(10, 90));
        }

        @Test
        void longVersionAndMedian() {
            long[] times = {500L, 120L, 9_000_000_000L, 300L, 200L};
            assertEquals(300L, quickselect.median(times));
            assertEquals(9_000_000_000L, quickselect.select(new long[] {1L, 9_000_000_000L}, 1));
        }

        @Test
        void sameSeedGivesSameAnswerAndWorkOnLargeInput() {
            int[] a = randomArray(new Random(3), 1_000_000, Integer.MAX_VALUE);
            int[] b = a.clone();
            int[] sorted = a.clone();
            Arrays.sort(sorted);
            assertEquals(sorted[500_000], new RandomizedQuickselect(9).select(a, 500_000));
            assertEquals(sorted[500_000], new RandomizedQuickselect(9).select(b, 500_000));
            assertEquals(Arrays.toString(Arrays.copyOf(a, 20)), Arrays.toString(Arrays.copyOf(b, 20)),
                    "same seed reorders the array the same way");
        }

        @Test
        void sortedInputIsNotAProblem() {
            int[] a = new int[200_000];
            for (int i = 0; i < a.length; i++) {
                a[i] = i;
            }
            assertEquals(123_456, quickselect.select(a, 123_456));
        }
    }

    @Nested
    class MedianOfMediansTests {

        @Test
        void singleElement() {
            assertEquals(7, MedianOfMedians.select(new int[] {7}, 0));
        }

        @Test
        void smallestAndLargest() {
            int[] a = {9, 3, 7, 1, 8, 2, 11, 4, 6, 0, 5};
            assertEquals(0, MedianOfMedians.select(a.clone(), 0));
            assertEquals(11, MedianOfMedians.select(a.clone(), a.length - 1));
        }

        @Test
        void allValuesEqual() {
            int[] a = new int[10_000];
            Arrays.fill(a, 4);
            assertEquals(4, MedianOfMedians.select(a, 9_999));
        }

        @Test
        void matchesSortingOnRandomInputs() {
            Random random = new Random(2);
            for (int trial = 0; trial < 300; trial++) {
                int n = 1 + random.nextInt(500);
                int[] a = randomArray(random, n, trial % 2 == 0 ? 10 : 1_000_000);
                int k = random.nextInt(n);
                int[] sorted = a.clone();
                Arrays.sort(sorted);
                assertEquals(sorted[k], MedianOfMedians.select(a, k), "trial " + trial);
            }
        }

        @Test
        void invalidArgumentsAreRejected() {
            assertThrows(IllegalArgumentException.class, () -> MedianOfMedians.select(new int[0], 0));
            assertThrows(IllegalArgumentException.class, () -> MedianOfMedians.select(new int[] {1}, 1));
        }

        @Test
        void agreesWithQuickselect() {
            Random random = new Random(4);
            RandomizedQuickselect quickselect = new RandomizedQuickselect(4);
            for (int trial = 0; trial < 50; trial++) {
                int n = 1_000 + random.nextInt(50_000);
                int[] a = randomArray(random, n, 1_000);
                int k = random.nextInt(n);
                assertEquals(quickselect.select(a.clone(), k), MedianOfMedians.select(a.clone(), k), "trial " + trial);
            }
        }

        @Test
        void largeSortedAndReversedInputs() {
            int[] up = new int[1_000_000];
            int[] down = new int[1_000_000];
            for (int i = 0; i < up.length; i++) {
                up[i] = i;
                down[i] = up.length - 1 - i;
            }
            assertEquals(500_000, MedianOfMedians.select(up, 500_000));
            assertEquals(999_999, MedianOfMedians.select(down, 999_999));
        }
    }
}
