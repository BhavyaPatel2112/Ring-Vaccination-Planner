package com.ringvax.api.dto;

import java.util.List;

/**
 * Double-dose guard results. The Bloom filter answers first; every "possibly
 * vaccinated" answer is confirmed against the exact registry.
 */
public record RegistryCheckResponse(List<Entry> results, int alreadyVaccinated, int quickCheckHits,
                                    int falsePositives, FilterStats filter) {

    /**
     * @param known       false if the id is not in this network
     * @param quickCheck  "POSSIBLY_VACCINATED" or "NOT_VACCINATED" (Bloom filter)
     * @param vaccinated  confirmed from the registry
     * @param action      "SKIP" (already vaccinated), "GIVE_DOSE" or "UNKNOWN_PERSON"
     */
    public record Entry(String personId, boolean known, String quickCheck, boolean vaccinated, boolean falsePositive,
                        String action) {
    }
}
