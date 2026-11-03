package com.ringvax.engine.cover;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Random;

import com.ringvax.engine.graph.Contact;
import com.ringvax.engine.graph.ContactGraph;
import com.ringvax.engine.graph.Person;

/**
 * Small graphs with known minimum covers, plus a brute-force solver to check
 * the real solvers against. Shared by all vertex cover tests.
 */
final class TestGraphs {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 1);

    private TestGraphs() {
    }

    static ContactGraph people(int n) {
        ContactGraph g = new ContactGraph();
        for (int i = 0; i < n; i++) {
            g.addPerson(new Person("P" + i, "", 30, "", false));
        }
        return g;
    }

    static ContactGraph fromLinks(int n, int[][] links) {
        ContactGraph g = people(n);
        for (int[] l : links) {
            g.addContact(l[0], l[1], DAY, 30);
        }
        return g;
    }

    /** Hub P0 linked to P1..P(n-1). Minimum cover: 1. */
    static ContactGraph star(int n) {
        ContactGraph g = people(n);
        for (int i = 1; i < n; i++) {
            g.addContact(0, i, DAY, 30);
        }
        return g;
    }

    /** Everyone in P0..P(a-1) linked to everyone in Pa..P(a+b-1). Minimum cover: min(a, b). */
    static ContactGraph completeBipartite(int a, int b) {
        ContactGraph g = people(a + b);
        for (int i = 0; i < a; i++) {
            for (int j = 0; j < b; j++) {
                g.addContact(i, a + j, DAY, 30);
            }
        }
        return g;
    }

    /** P0 - P1 - ... - P(n-1). Minimum cover: floor(n / 2). */
    static ContactGraph path(int n) {
        ContactGraph g = people(n);
        for (int i = 0; i + 1 < n; i++) {
            g.addContact(i, i + 1, DAY, 30);
        }
        return g;
    }

    /** Random graph where each pair is linked with probability p. */
    static ContactGraph random(int n, double p, long seed) {
        Random random = new Random(seed);
        ContactGraph g = people(n);
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (random.nextDouble() < p) {
                    g.addContact(i, j, DAY, 30);
                }
            }
        }
        return g;
    }

    /** Random exclusion flags, each person excluded with probability p. */
    static boolean[] randomExclusions(int n, double p, long seed) {
        Random random = new Random(seed);
        boolean[] excluded = new boolean[n];
        for (int i = 0; i < n; i++) {
            excluded[i] = random.nextDouble() < p;
        }
        return excluded;
    }

    /**
     * Size of a minimum valid plan, by trying every subset of the people who
     * could be chosen. Written independently of {@link CoverProblem}'s forced and
     * free split so it can check that logic too. Only for up to about 20 candidates.
     */
    static int bruteForceMinimum(CoverProblem problem) {
        ContactGraph g = problem.graph();
        int[] targets = problem.targetContacts();
        int[] candidateIndex = new int[g.personCount()];
        Arrays.fill(candidateIndex, -1);
        int candidates = 0;
        for (int c : targets) {
            Contact link = g.contact(c);
            for (int p : new int[] {link.a(), link.b()}) {
                if (!problem.isExcluded(p) && candidateIndex[p] < 0) {
                    candidateIndex[p] = candidates++;
                }
            }
        }
        if (candidates > 22) {
            throw new IllegalArgumentException("Too many candidates for brute force: " + candidates);
        }
        int best = Integer.MAX_VALUE;
        for (int mask = 0; mask < (1 << candidates); mask++) {
            int size = Integer.bitCount(mask);
            if (size >= best) {
                continue;
            }
            boolean ok = true;
            for (int c : targets) {
                Contact link = g.contact(c);
                int ia = candidateIndex[link.a()];
                int ib = candidateIndex[link.b()];
                if (ia < 0 && ib < 0) {
                    continue; // both excluded: nothing can protect this link
                }
                boolean covered = (ia >= 0 && (mask >> ia & 1) == 1) || (ib >= 0 && (mask >> ib & 1) == 1);
                if (!covered) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                best = size;
            }
        }
        return best;
    }
}
