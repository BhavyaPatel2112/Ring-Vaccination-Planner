package com.ringvax.engine.cover;

import java.util.ArrayList;
import java.util.List;

import com.ringvax.engine.graph.Contact;
import com.ringvax.engine.graph.ContactGraph;
import com.ringvax.engine.heap.IndexedMaxHeap;

/**
 * Greedy "most connected first" vertex cover: the baseline the 2-approximation
 * is compared with.
 *
 * <pre>
 * cover = forced people
 * for each person p: uncovered[p] = number of free links at p
 * put every person with uncovered[p] &gt; 0 in a max-heap keyed by uncovered[p]
 * while the heap is not empty:
 *     p = extractMax()                 // most unprotected links right now
 *     add p to cover
 *     for each free link (p, q) still unprotected:
 *         mark it protected
 *         decreaseKey(q, uncovered[q] - 1)   // q now has one link fewer to protect
 *         if uncovered[q] == 0: remove q from the heap
 * </pre>
 *
 * <p><b>No constant guarantee.</b> Greedy often gives small plans, but it can be
 * fooled. On the layered "greedy trap" networks (sample
 * {@code greedy-worst-case-12} and {@code -18}) the most connected person is
 * always on the wrong side, and the ratio to the minimum grows roughly like
 * ln(n): it is 23 against 12 and 40 against 18 on the two samples, already worse
 * than the approximation's proven factor 2 on the second. Greedy is the
 * logarithmic-factor algorithm for set cover, and that bound is all it gets on
 * vertex cover too.
 *
 * <p>Ties (people with the same number of unprotected links) go to the person
 * with the smaller index, so the result is deterministic.
 *
 * <p>Time complexity: O((V + E) log V): every free link causes at most one
 * {@code decreaseKey}, and every person is extracted at most once. Space
 * complexity: O(V).
 */
public class GreedyVertexCover implements VertexCoverSolver {

    public static final String NAME = "GREEDY";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public CoverResult solve(CoverProblem problem) {
        long start = System.nanoTime();
        ContactGraph graph = problem.graph();
        int n = graph.personCount();
        boolean[] inCover = new boolean[n];
        List<CoverChoice> choices = new ArrayList<>();

        int[] forced = problem.forcedPeople();
        for (int i = 0; i < forced.length; i++) {
            int p = forced[i];
            inCover[p] = true;
            choices.add(new CoverChoice(p, CoverChoice.Reason.FORCED,
                    "Only person who can be vaccinated on the link with " + graph.person(problem.forcedBy(i)).id()));
        }

        // Free links per person, as a compact adjacency list (offsets into one array).
        int[] free = problem.freeContacts();
        int[] count = new int[n];
        for (int c : free) {
            Contact link = graph.contact(c);
            count[link.a()]++;
            count[link.b()]++;
        }
        int[] offset = new int[n + 1];
        for (int p = 0; p < n; p++) {
            offset[p + 1] = offset[p] + count[p];
        }
        int[] linkAt = new int[2 * free.length];
        int[] fill = offset.clone();
        for (int i = 0; i < free.length; i++) {
            Contact link = graph.contact(free[i]);
            linkAt[fill[link.a()]++] = i;
            linkAt[fill[link.b()]++] = i;
        }

        IndexedMaxHeap heap = new IndexedMaxHeap(n);
        for (int p = 0; p < n; p++) {
            if (count[p] > 0) {
                heap.insert(p, count[p]);
            }
        }
        boolean[] protectedLink = new boolean[free.length];
        while (!heap.isEmpty()) {
            int p = heap.extractMax();
            int covered = 0;
            for (int j = offset[p]; j < offset[p + 1]; j++) {
                int i = linkAt[j];
                if (protectedLink[i]) {
                    continue;
                }
                protectedLink[i] = true;
                covered++;
                int q = graph.contact(free[i]).other(p);
                long left = heap.keyOf(q) - 1;
                if (left == 0) {
                    heap.remove(q);
                } else {
                    heap.decreaseKey(q, left);
                }
            }
            inCover[p] = true;
            choices.add(new CoverChoice(p, CoverChoice.Reason.GREEDY,
                    "Covered " + covered + (covered == 1 ? " unprotected link" : " unprotected links")
                            + " when chosen (most connected first)"));
        }

        long elapsed = System.nanoTime() - start;
        return new CoverResult(NAME, choices, problem.unprotectableContacts(), -1, elapsed, 0);
    }
}
