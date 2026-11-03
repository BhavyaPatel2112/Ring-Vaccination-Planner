package com.ringvax.engine.plan;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.ringvax.engine.cover.CoverChoice;
import com.ringvax.engine.cover.CoverProblem;
import com.ringvax.engine.cover.CoverResult;
import com.ringvax.engine.cover.VertexCoverSolver;
import com.ringvax.engine.graph.Contact;
import com.ringvax.engine.graph.ContactGraph;
import com.ringvax.engine.ring.RingResult;
import com.ringvax.engine.select.RandomizedQuickselect;

/**
 * Turns rings and a vertex cover method into a vaccination plan, with an
 * optional dose budget.
 *
 * <p><b>Full plan.</b> Every ring 1 member who can be vaccinated, plus the
 * solver's cover of the target links. Because ring 1 is vaccinated by policy,
 * the smallest full plan is |ring 1| + the minimum cover, so the approximation's
 * "at most 2 times the minimum" carries over to the whole plan.
 *
 * <p><b>Budget mode</b> (budget smaller than the full plan):
 * <ol>
 *   <li>Ring 1 first, most connected first, until the budget runs out.</li>
 *   <li>Then the top 10% most connected people: everyone whose number of
 *       unprotected target links is at or above the 90th percentile, found with
 *       randomized Quickselect in expected O(n) instead of sorting.</li>
 *   <li>Any doses left go to the rest of the full plan, most links first.</li>
 * </ol>
 * People are skipped once all their links are already protected. Links left
 * unprotected and ring 1 members left out are listed so nothing is hidden.
 * A budget of 0 gives an empty plan with every link listed; a negative budget
 * is rejected; a budget at or above the full plan returns the full plan.
 *
 * <p>Time complexity: the solver's time plus O(V log V + E) for budget mode
 * (the sort of the priority group; the percentile itself is expected O(V)).
 * Space complexity: O(V + E).
 */
public class VaccinationPlanner {

    /** People at or above this percentile of unprotected links are prioritised under a budget. */
    public static final double PRIORITY_PERCENTILE = 90;

    private final long seed;

    /** @param seed seed for Quickselect's random pivots, so plans are repeatable */
    public VaccinationPlanner(long seed) {
        this.seed = seed;
    }

    /**
     * Builds the plan.
     *
     * @param budget dose budget, or {@code null} for no limit
     * @throws IllegalArgumentException if the budget is negative
     */
    public VaccinationPlan plan(ContactGraph graph, RingResult ring, VertexCoverSolver solver, Integer budget) {
        if (budget != null && budget < 0) {
            throw new IllegalArgumentException("Budget must be 0 or more, got " + budget);
        }
        long start = System.nanoTime();

        List<CoverChoice> ring1 = ring1Choices(graph, ring);
        CoverProblem problem = CoverProblem.fromRing(graph, ring);
        CoverResult cover = solver.solve(problem);
        int fullSize = ring1.size() + cover.size();
        int lowerBound = cover.lowerBound() < 0 ? -1 : ring1.size() + cover.lowerBound();

        if (budget == null || budget >= fullSize) {
            List<CoverChoice> all = new ArrayList<>(ring1);
            all.addAll(cover.choices());
            return new VaccinationPlan(solver.name(), budget, fullSize, all, new int[0],
                    cover.unprotectableContacts(), new int[0], lowerBound, System.nanoTime() - start);
        }

        boolean[] chosen = new boolean[graph.personCount()];
        List<CoverChoice> picked = new ArrayList<>();
        int left = budget;

        // 1. Ring 1, most connected first.
        ring1.sort(Comparator.comparingInt((CoverChoice c) -> -graph.degree(c.person()))
                .thenComparingInt(CoverChoice::person));
        List<Integer> ring1Skipped = new ArrayList<>();
        for (CoverChoice choice : ring1) {
            if (left > 0) {
                picked.add(choice);
                chosen[choice.person()] = true;
                left--;
            } else {
                ring1Skipped.add(choice.person());
            }
        }

        // Unprotected target links per person who could be vaccinated.
        boolean[] isTarget = new boolean[graph.contactCount()];
        for (int c : problem.targetContacts()) {
            isTarget[c] = true;
        }
        int[] links = new int[graph.personCount()];
        List<Integer> candidates = new ArrayList<>();
        for (int c : problem.targetContacts()) {
            Contact link = graph.contact(c);
            for (int p : new int[] {link.a(), link.b()}) {
                if (!problem.isExcluded(p) && links[p]++ == 0) {
                    candidates.add(p);
                }
            }
        }

        if (left > 0 && !candidates.isEmpty()) {
            // 2. Top 10% most connected, threshold by Quickselect.
            int[] counts = new int[candidates.size()];
            for (int i = 0; i < counts.length; i++) {
                counts[i] = links[candidates.get(i)];
            }
            int threshold = new RandomizedQuickselect(seed).percentile(counts, PRIORITY_PERCENTILE);
            List<Integer> priority = new ArrayList<>();
            for (int p : candidates) {
                if (links[p] >= threshold) {
                    priority.add(p);
                }
            }
            priority.sort(Comparator.comparingInt((Integer p) -> -links[p]).thenComparingInt(p -> p));
            for (int p : priority) {
                if (left == 0) {
                    break;
                }
                int open = openLinks(graph, p, isTarget, chosen);
                if (!chosen[p] && open > 0) {
                    picked.add(new CoverChoice(p, CoverChoice.Reason.BUDGET_PRIORITY,
                            "Among the most connected 10%: " + open + " unprotected links"));
                    chosen[p] = true;
                    left--;
                }
            }

            // 3. The rest of the full plan, most links first.
            List<CoverChoice> rest = new ArrayList<>(cover.choices());
            rest.sort(Comparator.comparingInt((CoverChoice c) -> -links[c.person()])
                    .thenComparingInt(CoverChoice::person));
            for (CoverChoice choice : rest) {
                if (left == 0) {
                    break;
                }
                int p = choice.person();
                if (!chosen[p] && openLinks(graph, p, isTarget, chosen) > 0) {
                    picked.add(choice);
                    chosen[p] = true;
                    left--;
                }
            }
        }

        int[] unprotected = problem.uncoveredContacts(chosen);
        return new VaccinationPlan(solver.name(), budget, fullSize, picked, unprotected,
                cover.unprotectableContacts(), ring1Skipped.stream().mapToInt(Integer::intValue).toArray(),
                lowerBound, System.nanoTime() - start);
    }

    /** Ring 1 members who can be vaccinated, each linked to one of their cases. */
    private static List<CoverChoice> ring1Choices(ContactGraph graph, RingResult ring) {
        List<CoverChoice> choices = new ArrayList<>();
        for (int p : ring.mandatory()) {
            String caseId = "a confirmed case";
            for (int i = 0; i < graph.degree(p); i++) {
                int other = graph.neighbour(p, i);
                if (ring.level(other) == 0) {
                    caseId = "confirmed case " + graph.person(other).id();
                    break;
                }
            }
            choices.add(new CoverChoice(p, CoverChoice.Reason.RING_1, "Direct contact of " + caseId));
        }
        return choices;
    }

    /** Target links at {@code person} whose other end is not vaccinated in this plan. */
    private static int openLinks(ContactGraph graph, int person, boolean[] isTarget, boolean[] chosen) {
        int open = 0;
        for (int i = 0; i < graph.degree(person); i++) {
            if (isTarget[graph.contactAt(person, i)] && !chosen[graph.neighbour(person, i)]) {
                open++;
            }
        }
        return open;
    }
}
