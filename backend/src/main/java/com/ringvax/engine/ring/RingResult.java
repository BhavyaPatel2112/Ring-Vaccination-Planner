package com.ringvax.engine.ring;

import java.util.Arrays;
import java.util.List;

/**
 * Output of {@link RingBuilder}: who is in which ring around the confirmed cases,
 * and which contact links the vaccination plan still has to protect.
 *
 * <p>Arrays returned by the getters are copies, so callers cannot change the result.
 */
public final class RingResult {

    /** Ring level of a person who is not within the configured depth. */
    public static final int OUTSIDE = -1;

    private final int depth;
    private final int[] cases;
    private final int[] level;
    private final List<int[]> rings;
    private final int[] mandatory;
    private final int[] alreadyVaccinated;
    private final int[] cannotVaccinate;
    private final int[] targetContacts;
    private final int[] casesWithoutContacts;

    RingResult(int depth, int[] cases, int[] level, List<int[]> rings, int[] mandatory,
               int[] alreadyVaccinated, int[] cannotVaccinate, int[] targetContacts,
               int[] casesWithoutContacts) {
        this.depth = depth;
        this.cases = cases;
        this.level = level;
        this.rings = rings;
        this.mandatory = mandatory;
        this.alreadyVaccinated = alreadyVaccinated;
        this.cannotVaccinate = cannotVaccinate;
        this.targetContacts = targetContacts;
        this.casesWithoutContacts = casesWithoutContacts;
    }

    public int depth() {
        return depth;
    }

    /** Indices of the confirmed cases, sorted. */
    public int[] cases() {
        return cases.clone();
    }

    /** 0 for a case, 1 to depth for ring members, {@link #OUTSIDE} otherwise. */
    public int level(int person) {
        return level[person];
    }

    /** Members of ring {@code k} (1-based, 1 to depth), sorted by index. */
    public int[] ring(int k) {
        if (k < 1 || k > depth) {
            throw new IllegalArgumentException("Ring " + k + " is outside depth " + depth);
        }
        return rings.get(k - 1).clone();
    }

    /** Number of people in rings 1 to depth (cases not included). */
    public int peopleInRings() {
        int total = 0;
        for (int[] ring : rings) {
            total += ring.length;
        }
        return total;
    }

    /**
     * Ring 1 members who can be vaccinated. In ring vaccination every direct
     * contact of a case is vaccinated, so these are always in the plan.
     */
    public int[] mandatory() {
        return mandatory.clone();
    }

    /** Ring members who are already vaccinated and need no dose. */
    public int[] alreadyVaccinated() {
        return alreadyVaccinated.clone();
    }

    /** Ring members who are not vaccinated but are ineligible or refused. */
    public int[] cannotVaccinate() {
        return cannotVaccinate.clone();
    }

    /**
     * Contact links the vertex cover has to protect: at least one endpoint is in
     * ring 2 or beyond, and neither endpoint is a case, already vaccinated, or a
     * ring 1 member who will be vaccinated.
     */
    public int[] targetContacts() {
        return targetContacts.clone();
    }

    /** Cases with no (counted) contacts at all; their ring is empty. */
    public int[] casesWithoutContacts() {
        return casesWithoutContacts.clone();
    }

    @Override
    public String toString() {
        return "RingResult{depth=" + depth + ", cases=" + Arrays.toString(cases)
                + ", ringSizes=" + rings.stream().map(r -> r.length).toList()
                + ", mandatory=" + mandatory.length + ", targetContacts=" + targetContacts.length + "}";
    }
}
