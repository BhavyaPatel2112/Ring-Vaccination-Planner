package com.ringvax.bench;

import com.ringvax.engine.graph.Contact;
import com.ringvax.engine.graph.ContactGraph;

/**
 * Naive exact baseline: tries every subset of people and keeps the smallest one
 * that covers every link. Used only in the benchmarks, to show how much the
 * branch and bound solver saves.
 *
 * <p>Time complexity: O(2^V * V) (each subset is checked with bit operations, one
 * neighbour mask per person). Space complexity: O(V). Limited to 30 people.
 */
public final class BruteForceCover {

    public static final int MAX_PEOPLE = 30;

    private BruteForceCover() {
    }

    /** Size of a minimum vertex cover of all links in {@code g}. */
    public static int minimumSize(ContactGraph g) {
        int n = g.personCount();
        if (n > MAX_PEOPLE) {
            throw new IllegalArgumentException("Brute force handles at most " + MAX_PEOPLE + " people");
        }
        long[] neighbours = new long[n];
        for (Contact c : g.contacts()) {
            neighbours[c.a()] |= 1L << c.b();
            neighbours[c.b()] |= 1L << c.a();
        }
        int best = n;
        long all = (1L << n) - 1;
        for (long mask = 0; mask <= all; mask++) {
            int size = Long.bitCount(mask);
            if (size >= best) {
                continue;
            }
            // Valid if every person left out has all their neighbours inside the cover.
            boolean valid = true;
            for (long out = ~mask & all; out != 0 && valid; out &= out - 1) {
                int person = Long.numberOfTrailingZeros(out);
                valid = (neighbours[person] & ~mask) == 0;
            }
            if (valid) {
                best = size;
            }
        }
        return best;
    }
}
