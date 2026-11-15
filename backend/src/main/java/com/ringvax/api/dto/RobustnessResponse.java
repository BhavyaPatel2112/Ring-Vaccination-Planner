package com.ringvax.api.dto;

import java.util.List;

/**
 * Infections when the plan was made from incomplete contact data.
 *
 * @param noVaccinationMean average new infections with no vaccination at all
 * @param rows              one row per missing share, from the full data upwards
 * @param headline          the main result in one sentence
 */
public record RobustnessResponse(double noVaccinationMean, List<Row> rows, String headline, int trials, long seed,
                                 double runtimeMs) {

    /**
     * @param hiddenContacts  contacts the planner did not see
     * @param doses           doses in the plan made from the visible contacts
     * @param reductionVsNone percentage fewer infections than no vaccination (null when none spread)
     */
    public record Row(double missingShare, int hiddenContacts, int doses, double mean, int p90,
                      Double reductionVsNone) {
    }
}
