package com.ringvax.bench;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.ringvax.engine.graph.ContactGraph;
import com.ringvax.engine.graph.Person;

/**
 * Seeded graph families for the vertex cover experiments.
 *
 * <ul>
 *   <li><b>Random</b> (Erdős–Rényi): every pair linked with the same probability,
 *       chosen to give a target average number of contacts.</li>
 *   <li><b>Random d-regular</b>: everyone has exactly d contacts. With no
 *       low-degree people, the exact solver's shortcuts (taking the neighbour of
 *       someone with one contact) never apply, so these are hard instances that
 *       show its exponential growth.</li>
 * </ul>
 */
public final class BenchmarkGraphs {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 1);

    private BenchmarkGraphs() {
    }

    public static ContactGraph random(int n, double averageContacts, long seed) {
        Random random = new Random(seed);
        ContactGraph g = people(n);
        double p = n > 1 ? averageContacts / (n - 1) : 0;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (random.nextDouble() < p) {
                    g.addContact(i, j, DAY, 30);
                }
            }
        }
        return g;
    }

    /**
     * Random d-regular graph by the configuration model: d "stubs" per person are
     * paired at random, retrying until there are no self-loops or repeated links.
     */
    public static ContactGraph regular(int n, int d, long seed) {
        if ((long) n * d % 2 != 0 || d >= n) {
            throw new IllegalArgumentException("n * d must be even and d < n");
        }
        Random random = new Random(seed);
        while (true) {
            List<Integer> stubs = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                for (int k = 0; k < d; k++) {
                    stubs.add(i);
                }
            }
            Collections.shuffle(stubs, random);
            Set<Long> seen = new HashSet<>();
            List<int[]> links = new ArrayList<>();
            boolean ok = true;
            for (int i = 0; i < stubs.size(); i += 2) {
                int a = stubs.get(i);
                int b = stubs.get(i + 1);
                if (a == b || !seen.add(((long) Math.min(a, b) << 32) | Math.max(a, b))) {
                    ok = false;
                    break;
                }
                links.add(new int[] {a, b});
            }
            if (ok) {
                ContactGraph g = people(n);
                for (int[] link : links) {
                    g.addContact(link[0], link[1], DAY, 30);
                }
                return g;
            }
        }
    }

    private static ContactGraph people(int n) {
        ContactGraph g = new ContactGraph();
        for (int i = 0; i < n; i++) {
            g.addPerson(new Person("P" + i, "", 30, "", false));
        }
        return g;
    }
}
