package com.ringvax.api.dto;

import com.ringvax.engine.bloom.BloomFilter;

/**
 * Size and expected accuracy of one live Bloom filter.
 *
 * @param hashSetBytes estimated memory an exact hash set of the same keys would need
 */
public record FilterStats(String name, long bits, int hashes, long keys, double fillRatio,
                          double expectedFalsePositiveRate, long memoryBytes, long hashSetBytes) {

    public static FilterStats of(String name, BloomFilter filter, long hashSetBytes) {
        return new FilterStats(name, filter.bitCount(), filter.hashCount(), filter.insertedCount(),
                filter.fillRatio(), filter.expectedFalsePositiveRate(), filter.memoryBytes(), hashSetBytes);
    }
}
