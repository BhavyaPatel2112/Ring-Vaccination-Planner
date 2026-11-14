package com.ringvax.api.dto;

import java.util.List;

/**
 * Infections under each vaccination strategy, on the same simulated outbreaks.
 *
 * @param headline one plain-language sentence with the main result
 */
public record SimulateResponse(double transmission, int recoverySteps, int trials, double efficacy, long seed,
                               List<Scenario> scenarios, String headline, double runtimeMs) {

    /**
     * @param name            NONE, RANDOM, APPROX or GREEDY
     * @param doses           doses given by this strategy (on top of people already vaccinated)
     * @param reductionVsNone percentage fewer mean new infections than with no vaccination
     * @param curve           average number of infectious people at each step
     */
    public record Scenario(String name, String label, int doses, double mean, int median, int p90, int min, int max,
                           Double reductionVsNone, double[] curve) {
    }
}
