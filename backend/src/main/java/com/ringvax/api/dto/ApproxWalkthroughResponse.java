package com.ringvax.api.dto;

import java.util.List;

/**
 * Every step of the 2-approximation on the current rings, for the "How it works"
 * screen. Ring 1 is vaccinated first; then the forced people; then the free links in
 * the order the algorithm scans them.
 *
 * @param ring1      people vaccinated first (ring 1, who can be vaccinated)
 * @param steps      FORCED, MATCHED or ALREADY_PROTECTED, in order
 * @param doses      doses in the finished plan (ring 1 + forced + 2 per matched link)
 * @param lowerBound the minimum is at least this (ring 1 + forced + matched links)
 */
public record ApproxWalkthroughResponse(List<String> ring1, List<Step> steps, int targetLinks, int doses,
                                        int lowerBound, int unprotectableLinks) {

    /** One step: {@code person} is set for FORCED, {@code a} and {@code b} for links. */
    public record Step(int index, String kind, String a, String b, String person, String explanation) {
    }
}
