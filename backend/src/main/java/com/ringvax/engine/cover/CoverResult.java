package com.ringvax.engine.cover;

import java.util.List;

/**
 * Result of a {@link VertexCoverSolver}.
 *
 * @param method                name of the solver that produced it
 * @param choices               chosen people in the order they were picked, each with a reason
 * @param unprotectableContacts target links where neither person can be vaccinated; they need
 *                              quarantine or monitoring instead
 * @param lowerBound            proven lower bound on the minimum cover size for this problem,
 *                              or -1 if the method does not provide one
 * @param runtimeNanos          time spent inside the solver
 */
public record CoverResult(String method, List<CoverChoice> choices, int[] unprotectableContacts,
                          int lowerBound, long runtimeNanos) {

    public CoverResult {
        choices = List.copyOf(choices);
        unprotectableContacts = unprotectableContacts.clone();
    }

    /** Number of doses in the plan. */
    public int size() {
        return choices.size();
    }

    /** Chosen person indices in pick order. */
    public int[] people() {
        return choices.stream().mapToInt(CoverChoice::person).toArray();
    }

    @Override
    public int[] unprotectableContacts() {
        return unprotectableContacts.clone();
    }

    /**
     * Upper bound on how far this plan can be from the minimum, from the lower
     * bound alone: size / lowerBound. Returns NaN when no lower bound is known,
     * and 1.0 for an empty problem.
     */
    public double provenRatioBound() {
        if (lowerBound < 0) {
            return Double.NaN;
        }
        if (lowerBound == 0) {
            return size() == 0 ? 1.0 : Double.POSITIVE_INFINITY;
        }
        return (double) size() / lowerBound;
    }
}
