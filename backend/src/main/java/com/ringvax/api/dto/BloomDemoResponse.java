package com.ringvax.api.dto;

import java.util.List;

/**
 * The drawing data for a tiny Bloom filter.
 *
 * @param setBy   for each bit, the indexes of the keys that set it (empty: the bit is 0)
 * @param verdict DEFINITELY_NOT, PRESENT (possibly, and it really is) or FALSE_POSITIVE
 */
public record BloomDemoResponse(int m, int k, List<KeyBits> keys, List<List<Integer>> setBy, KeyBits query,
                                boolean allQueryBitsSet, boolean queryIsMember, String verdict, String explanation,
                                double expectedFalsePositiveRate) {

    public record KeyBits(String key, List<Long> positions) {
    }
}
