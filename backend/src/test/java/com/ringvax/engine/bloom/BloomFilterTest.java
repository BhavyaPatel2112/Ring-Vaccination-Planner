package com.ringvax.engine.bloom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BloomFilterTest {

    @Test
    void emptyFilterContainsNothing() {
        BloomFilter filter = new BloomFilter(1024, 3);

        assertFalse(filter.mightContain("P001"));
        assertEquals(0.0, filter.fillRatio());
        assertEquals(0.0, filter.expectedFalsePositiveRate());
    }

    @Test
    void everyInsertedKeyIsFound() {
        BloomFilter filter = BloomFilter.forCapacity(50_000, 0.01);
        for (int i = 0; i < 50_000; i++) {
            filter.add("P" + i);
        }
        for (int i = 0; i < 50_000; i++) {
            assertTrue(filter.mightContain("P" + i), "no false negatives: P" + i);
        }
    }

    @Test
    void measuredFalsePositiveRateIsWithin20PercentOfTheory() {
        // 10 bits per key and k = 7: the theory says about 0.82%.
        BloomExperiment.Result r = BloomExperiment.run(100_000, 7, 10_000, 100_000, 2026);

        assertEquals(0, r.falseNegatives());
        double relativeError = Math.abs(r.measuredRate() - r.theoreticalRate()) / r.theoreticalRate();
        assertTrue(relativeError < 0.20, "measured " + r.measuredRate() + " vs theory " + r.theoreticalRate());
    }

    @Test
    void tooFewBitsGivesManyFalsePositives() {
        // Overloaded filter: 1 bit per key. Theory and measurement both say most answers are "possibly".
        BloomExperiment.Result r = BloomExperiment.run(5_000, 3, 5_000, 20_000, 7);

        assertTrue(r.theoreticalRate() > 0.5);
        assertTrue(r.measuredRate() > 0.5);
        assertEquals(0, r.falseNegatives(), "still never a false negative");
    }

    @Test
    void invalidParametersAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new BloomFilter(0, 3));
        assertThrows(IllegalArgumentException.class, () -> new BloomFilter(1024, 0));
        assertThrows(IllegalArgumentException.class, () -> new BloomFilter(1024, 33));
        assertThrows(IllegalArgumentException.class, () -> new BloomFilter(BloomFilter.MAX_BITS + 1, 3));
        assertThrows(IllegalArgumentException.class, () -> BloomFilter.optimalBits(10, 0));
    }

    @Test
    void sizingFormulasMatchTheTextbook() {
        // 1% needs about 9.59 bits per key and k = 7.
        assertEquals(9586, BloomFilter.optimalBits(1000, 0.01));
        assertEquals(7, BloomFilter.optimalHashCount(9586, 1000));
        assertEquals(1, BloomFilter.optimalHashCount(10, 1000), "at least one hash");
        BloomFilter filter = BloomFilter.forCapacity(1000, 0.01);
        assertEquals(9586, filter.bitCount());
        assertEquals(7, filter.hashCount());
        assertEquals(1200, filter.memoryBytes(), "150 words of 64 bits");
    }

    @Test
    void addingTheSameKeyTwiceChangesNoBits() {
        BloomFilter filter = new BloomFilter(4096, 4);

        assertTrue(filter.add("V001|2026-11-01T16"));
        assertFalse(filter.add("V001|2026-11-01T16"));
        assertEquals(2, filter.insertedCount());
    }

    @Test
    void usesMuchLessMemoryThanAHashSet() {
        BloomExperiment.Result r = BloomExperiment.run(BloomFilter.optimalBits(20_000, 0.01), 7, 20_000, 1_000, 1);

        assertTrue(r.memoryRatio() > 5, "a hash set needs " + r.memoryRatio() + " times more memory");
    }
}
