package com.ringvax.api.dto;

import java.util.List;

import com.ringvax.engine.bloom.BloomExperiment;

/**
 * A measured Bloom filter configuration, plus the theoretical false positive
 * rate for every k from 1 to 20 at the same m and n (for the chart).
 */
public record BloomConfigResponse(BloomExperiment.Result result, List<RateAtK> theoryByK, String summary) {

    public record RateAtK(int k, double rate) {
    }
}
