package com.ringvax.engine.cover;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.ringvax.engine.graph.Contact;
import com.ringvax.engine.graph.ContactGraph;

/**
 * Exact minimum vertex cover by a bounded search tree with branch and bound.
 * Used as ground truth for the approximation on small networks.
 *
 * <pre>
 * cover = forced people
 * for each connected group of the remaining (free) links:
 *     best = endpoints of a maximal matching          // a valid cover to start from
 *     search(group, chosen = {})
 *
 * search(R, chosen):
 *     repeat until nothing changes:
 *         drop people in R with no links left
 *         if someone in R has exactly one link left: take their neighbour   // always safe
 *     if R has no links: best = min(best, chosen); return
 *     if |chosen| + (maximal matching size in R) &gt;= |best|: return    // prune
 *     u = person in R with the most links left
 *     search(R - u, chosen + u)                         // u is vaccinated
 *     search(R - u - N(u), chosen + N(u))               // u is not, so all of u's neighbours must be
 * </pre>
 *
 * <p><b>Correctness.</b> Every cover either contains u or, if it does not,
 * contains every neighbour of u (otherwise a link at u is unprotected), so the
 * two branches together never miss the optimum. The degree-1 rule is safe because
 * taking the neighbour covers everything taking the leaf would, and more.
 * Pruning only discards branches that provably cannot beat the best cover found:
 * the links of any matching need distinct people, so a matching in R is a lower
 * bound on what R still needs. Independent groups are solved separately, which
 * turns 2^(k1 + k2) into 2^k1 + 2^k2.
 *
 * <p><b>Why it is exponential.</b> Vertex cover is NP-complete (one of Karp's
 * 21 problems): a graph has an independent set of size s exactly when it has a
 * vertex cover of size V - s, and independent set is NP-complete. So no
 * polynomial algorithm is known, and this solver refuses inputs above a size
 * limit (default 40 people) or after a time limit (default 5 seconds).
 *
 * <p>Each person in the free part is one bit of a {@code long}, so neighbour
 * sets, removals and degree counts are single machine instructions.
 *
 * <p>Time complexity: O(2^k * (V + E)) in the worst case for a minimum cover of
 * size k (each branch adds at least one person); branching on the highest-degree
 * person and pruning make it far smaller in practice. Space: O(V) plus recursion
 * depth at most V.
 */
public class ExactVertexCover implements VertexCoverSolver {

    public static final String NAME = "EXACT";
    public static final int DEFAULT_MAX_PEOPLE = 40;
    /** One bit per person in a {@code long}. */
    public static final int HARD_MAX_PEOPLE = 64;
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);

    private final int maxPeople;
    private final Duration timeout;

    public ExactVertexCover() {
        this(DEFAULT_MAX_PEOPLE, DEFAULT_TIMEOUT);
    }

    /**
     * @param maxPeople largest number of people on the free links to accept (1 to 64)
     * @param timeout   search time limit (positive)
     */
    public ExactVertexCover(int maxPeople, Duration timeout) {
        if (maxPeople < 1 || maxPeople > HARD_MAX_PEOPLE) {
            throw new IllegalArgumentException("maxPeople must be between 1 and " + HARD_MAX_PEOPLE);
        }
        if (timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        this.maxPeople = maxPeople;
        this.timeout = timeout;
    }

    @Override
    public String name() {
        return NAME;
    }

    /**
     * @throws ExactSolverLimitException if the problem is too large or the time limit is reached
     */
    @Override
    public CoverResult solve(CoverProblem problem) {
        long start = System.nanoTime();
        ContactGraph graph = problem.graph();
        List<CoverChoice> choices = new ArrayList<>();

        int[] forced = problem.forcedPeople();
        for (int i = 0; i < forced.length; i++) {
            choices.add(new CoverChoice(forced[i], CoverChoice.Reason.FORCED,
                    "Only person who can be vaccinated on the link with "
                            + graph.person(problem.forcedBy(i)).id()));
        }

        // Give each person on a free link a bit position.
        int[] localId = new int[graph.personCount()];
        Arrays.fill(localId, -1);
        int[] person = new int[HARD_MAX_PEOPLE];
        int m = 0;
        int[] free = problem.freeContacts();
        for (int c : free) {
            Contact link = graph.contact(c);
            for (int p : new int[] {link.a(), link.b()}) {
                if (localId[p] < 0) {
                    if (m == maxPeople) {
                        throw ExactSolverLimitException.tooLarge(countPeople(graph, free), maxPeople);
                    }
                    localId[p] = m;
                    person[m++] = p;
                }
            }
        }
        long[] adj = new long[m];
        for (int c : free) {
            Contact link = graph.contact(c);
            int u = localId[link.a()];
            int v = localId[link.b()];
            adj[u] |= 1L << v;
            adj[v] |= 1L << u;
        }

        Search search = new Search(adj, start + timeout.toNanos(), timeout.toMillis());
        long all = m == 64 ? -1L : (1L << m) - 1;
        long cover = search.solveAll(all);
        for (long bits = cover; bits != 0; bits &= bits - 1) {
            int p = person[Long.numberOfTrailingZeros(bits)];
            choices.add(new CoverChoice(p, CoverChoice.Reason.EXACT, "Part of a smallest possible plan for these links"));
        }

        long elapsed = System.nanoTime() - start;
        return new CoverResult(NAME, choices, problem.unprotectableContacts(), choices.size(), elapsed);
    }

    private static int countPeople(ContactGraph graph, int[] contacts) {
        boolean[] seen = new boolean[graph.personCount()];
        int count = 0;
        for (int c : contacts) {
            Contact link = graph.contact(c);
            for (int p : new int[] {link.a(), link.b()}) {
                if (!seen[p]) {
                    seen[p] = true;
                    count++;
                }
            }
        }
        return count;
    }

    /** The branch and bound search over bitmask neighbour sets. */
    private static final class Search {
        private final long[] adj;
        private final long deadline;
        private final long timeoutMillis;
        private long nodes;
        private long best;
        private int bestSize;

        Search(long[] adj, long deadline, long timeoutMillis) {
            this.adj = adj;
            this.deadline = deadline;
            this.timeoutMillis = timeoutMillis;
        }

        long solveAll(long all) {
            long result = 0;
            long remaining = all;
            while (remaining != 0) {
                long group = component(Long.numberOfTrailingZeros(remaining), all);
                remaining &= ~group;
                best = matchingCover(group);
                bestSize = Long.bitCount(best);
                branch(group, 0L);
                result |= best;
            }
            return result;
        }

        private void branch(long r, long chosen) {
            if ((nodes++ & 255) == 0 && System.nanoTime() > deadline) {
                throw ExactSolverLimitException.timedOut(timeoutMillis);
            }
            boolean changed = true;
            while (changed) {
                changed = false;
                for (long it = r; it != 0; it &= it - 1) {
                    int u = Long.numberOfTrailingZeros(it);
                    if ((r & (1L << u)) == 0) {
                        continue;
                    }
                    long neighbours = adj[u] & r;
                    int degree = Long.bitCount(neighbours);
                    if (degree == 0) {
                        r &= ~(1L << u);
                    } else if (degree == 1) {
                        chosen |= neighbours;
                        r &= ~(neighbours | (1L << u));
                        changed = true;
                    }
                }
            }

            int size = Long.bitCount(chosen);
            if (r == 0) {
                if (size < bestSize) {
                    best = chosen;
                    bestSize = size;
                }
                return;
            }
            if (size + matchingSize(r) >= bestSize) {
                return;
            }

            int u = -1;
            int maxDegree = -1;
            for (long it = r; it != 0; it &= it - 1) {
                int v = Long.numberOfTrailingZeros(it);
                int degree = Long.bitCount(adj[v] & r);
                if (degree > maxDegree) {
                    maxDegree = degree;
                    u = v;
                }
            }
            long uBit = 1L << u;
            long neighbours = adj[u] & r;
            branch(r & ~uBit, chosen | uBit);
            branch(r & ~uBit & ~neighbours, chosen | neighbours);
        }

        /** People reachable from {@code start} inside {@code within}. */
        private long component(int start, long within) {
            long group = 1L << start;
            long frontier = group;
            while (frontier != 0) {
                long next = 0;
                for (long it = frontier; it != 0; it &= it - 1) {
                    next |= adj[Long.numberOfTrailingZeros(it)] & within;
                }
                frontier = next & ~group;
                group |= frontier;
            }
            return group;
        }

        /** Number of links in a greedy maximal matching inside r: a lower bound. */
        private int matchingSize(long r) {
            return Long.bitCount(matchingCover(r)) / 2;
        }

        /** Both endpoints of every link in a greedy maximal matching inside r: a valid cover. */
        private long matchingCover(long r) {
            long unmatched = r;
            long matched = 0;
            while (unmatched != 0) {
                int u = Long.numberOfTrailingZeros(unmatched);
                unmatched &= ~(1L << u);
                long options = adj[u] & unmatched;
                if (options != 0) {
                    long vBit = Long.lowestOneBit(options);
                    unmatched &= ~vBit;
                    matched |= (1L << u) | vBit;
                }
            }
            return matched;
        }
    }
}
