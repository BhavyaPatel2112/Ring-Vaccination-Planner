package com.ringvax.engine.cover;

import java.util.ArrayList;
import java.util.List;

import com.ringvax.engine.graph.Contact;
import com.ringvax.engine.graph.ContactGraph;
import com.ringvax.engine.ring.RingResult;

/**
 * A vertex cover instance: the links to protect, plus the people who cannot be
 * chosen (ineligible, refused).
 *
 * <p>Exclusions are handled once here, so every solver treats them the same way.
 * Each target link falls into exactly one group:
 * <ul>
 *   <li><b>Unprotectable</b>: both people are excluded. No vaccination plan can
 *       protect it; it is reported for quarantine or monitoring.</li>
 *   <li><b>Forced</b>: exactly one person is excluded, so any valid plan must
 *       vaccinate the other one. Those people are {@link #forcedPeople()}.</li>
 *   <li><b>Free</b>: neither person is excluded and neither is forced. This is
 *       the part a solver actually has to decide: {@link #freeContacts()}.</li>
 * </ul>
 * Every valid plan contains all forced people, so the minimum plan size is
 * |forced| plus the minimum cover of the free links. That is why the
 * 2-approximation guarantee still holds with exclusions.
 *
 * <p>Time complexity: O(V + T) to build for T target links. Space: O(V + T).
 */
public final class CoverProblem {

    private final ContactGraph graph;
    private final int[] targetContacts;
    private final boolean[] excluded;
    private final int[] forcedPeople;
    private final int[] forcedBy;
    private final int[] freeContacts;
    private final int[] unprotectableContacts;

    private CoverProblem(ContactGraph graph, int[] targetContacts, boolean[] excluded) {
        if (excluded.length != graph.personCount()) {
            throw new IllegalArgumentException("excluded must have one entry per person");
        }
        this.graph = graph;
        this.targetContacts = targetContacts.clone();
        this.excluded = excluded.clone();

        boolean[] seen = new boolean[graph.contactCount()];
        boolean[] forced = new boolean[graph.personCount()];
        List<Integer> forcedList = new ArrayList<>();
        List<Integer> forcedByList = new ArrayList<>();
        List<Integer> unprotectable = new ArrayList<>();
        for (int c : this.targetContacts) {
            if (c < 0 || c >= graph.contactCount()) {
                throw new IllegalArgumentException("Contact index " + c + " is out of range");
            }
            if (seen[c]) {
                throw new IllegalArgumentException("Contact " + c + " is listed twice");
            }
            seen[c] = true;
            Contact contact = graph.contact(c);
            boolean exA = excluded[contact.a()];
            boolean exB = excluded[contact.b()];
            if (exA && exB) {
                unprotectable.add(c);
            } else if (exA || exB) {
                int must = exA ? contact.b() : contact.a();
                if (!forced[must]) {
                    forced[must] = true;
                    forcedList.add(must);
                    forcedByList.add(exA ? contact.a() : contact.b());
                }
            }
        }

        List<Integer> free = new ArrayList<>();
        for (int c : this.targetContacts) {
            Contact contact = graph.contact(c);
            if (!excluded[contact.a()] && !excluded[contact.b()] && !forced[contact.a()] && !forced[contact.b()]) {
                free.add(c);
            }
        }
        this.forcedPeople = toArray(forcedList);
        this.forcedBy = toArray(forcedByList);
        this.freeContacts = toArray(free);
        this.unprotectableContacts = toArray(unprotectable);
    }

    /** Protect the given links; anyone may be chosen. */
    public static CoverProblem of(ContactGraph graph, int[] targetContacts) {
        return new CoverProblem(graph, targetContacts, new boolean[graph.personCount()]);
    }

    /** Protect the given links; people with {@code excluded[p] == true} may not be chosen. */
    public static CoverProblem withExclusions(ContactGraph graph, int[] targetContacts, boolean[] excluded) {
        return new CoverProblem(graph, targetContacts, excluded);
    }

    /** Protect every link in the graph; anyone may be chosen. Used for tests and benchmarks. */
    public static CoverProblem allContacts(ContactGraph graph) {
        int[] all = new int[graph.contactCount()];
        for (int i = 0; i < all.length; i++) {
            all[i] = i;
        }
        return of(graph, all);
    }

    /**
     * Protect the ring's target links. People who cannot be vaccinated
     * (ineligible or refused) are excluded.
     */
    public static CoverProblem fromRing(ContactGraph graph, RingResult ring) {
        boolean[] excluded = new boolean[graph.personCount()];
        for (int p = 0; p < excluded.length; p++) {
            excluded[p] = !graph.person(p).canBeVaccinated();
        }
        return new CoverProblem(graph, ring.targetContacts(), excluded);
    }

    public ContactGraph graph() {
        return graph;
    }

    public int[] targetContacts() {
        return targetContacts.clone();
    }

    public boolean isExcluded(int person) {
        return excluded[person];
    }

    /** People every valid plan must include, in the order they were found. */
    public int[] forcedPeople() {
        return forcedPeople.clone();
    }

    /** For the i-th forced person, the excluded person on the link that forced them. */
    public int forcedBy(int i) {
        return forcedBy[i];
    }

    /** Target links still to be decided after exclusions and forced people. */
    public int[] freeContacts() {
        return freeContacts.clone();
    }

    /** Target links where both people are excluded. */
    public int[] unprotectableContacts() {
        return unprotectableContacts.clone();
    }

    /** Target links not covered by {@code inCover}, ignoring unprotectable ones. */
    public int[] uncoveredContacts(boolean[] inCover) {
        List<Integer> uncovered = new ArrayList<>();
        for (int c : targetContacts) {
            Contact contact = graph.contact(c);
            boolean unprotectable = excluded[contact.a()] && excluded[contact.b()];
            if (!unprotectable && !inCover[contact.a()] && !inCover[contact.b()]) {
                uncovered.add(c);
            }
        }
        return toArray(uncovered);
    }

    /**
     * True if {@code people} protects every target link that can be protected and
     * contains no excluded person and no duplicates.
     */
    public boolean isValidCover(int[] people) {
        boolean[] inCover = new boolean[graph.personCount()];
        for (int p : people) {
            if (p < 0 || p >= inCover.length || excluded[p] || inCover[p]) {
                return false;
            }
            inCover[p] = true;
        }
        return uncoveredContacts(inCover).length == 0;
    }

    private static int[] toArray(List<Integer> list) {
        int[] array = new int[list.size()];
        for (int i = 0; i < array.length; i++) {
            array[i] = list.get(i);
        }
        return array;
    }
}
