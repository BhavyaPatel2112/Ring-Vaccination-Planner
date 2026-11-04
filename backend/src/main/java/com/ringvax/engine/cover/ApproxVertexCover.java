package com.ringvax.engine.cover;

import java.util.ArrayList;
import java.util.List;

import com.ringvax.engine.graph.Contact;
import com.ringvax.engine.graph.ContactGraph;

/**
 * Vertex cover 2-approximation by maximal matching: the core vaccination plan.
 *
 * <pre>
 * cover = forced people (only eligible person on a link)
 * for each free link (u, v):
 *     if neither u nor v is in cover:
 *         add u and v to cover          // (u, v) joins the matching M
 * </pre>
 *
 * <p><b>Guarantee.</b> The links that triggered an addition form a matching M:
 * no two of them share a person, because both endpoints are added at once.
 * Any valid plan must vaccinate at least one person per link of M, and none of
 * those people is forced (free links never touch forced people), so
 * OPT &ge; |forced| + |M|. The plan has exactly |forced| + 2|M| people, so it is
 * at most 2 &times; OPT. {@code |forced| + |M|} is returned as the lower bound,
 * which lets the app show a proven ratio even without the exact solver.
 *
 * <p>The factor 2 is tight: on a star the plan takes the hub and one leaf
 * (optimum 1), and on K(n, n) it takes all 2n people (optimum n).
 *
 * <p>Time complexity: O(V + E) for the target links. Space complexity: O(V).
 */
public class ApproxVertexCover implements VertexCoverSolver {

    public static final String NAME = "APPROX";

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public CoverResult solve(CoverProblem problem) {
        long start = System.nanoTime();
        ContactGraph graph = problem.graph();
        boolean[] inCover = new boolean[graph.personCount()];
        List<CoverChoice> choices = new ArrayList<>();

        int[] forced = problem.forcedPeople();
        for (int i = 0; i < forced.length; i++) {
            int p = forced[i];
            inCover[p] = true;
            choices.add(new CoverChoice(p, CoverChoice.Reason.FORCED,
                    "Only person who can be vaccinated on the link with " + id(graph, problem.forcedBy(i))));
        }

        int matched = 0;
        for (int c : problem.freeContacts()) {
            Contact link = graph.contact(c);
            int u = link.a();
            int v = link.b();
            if (!inCover[u] && !inCover[v]) {
                inCover[u] = true;
                inCover[v] = true;
                matched++;
                String text = "Matched link " + id(graph, u) + " - " + id(graph, v);
                choices.add(new CoverChoice(u, CoverChoice.Reason.MATCHED, text));
                choices.add(new CoverChoice(v, CoverChoice.Reason.MATCHED, text));
            }
        }

        long elapsed = System.nanoTime() - start;
        return new CoverResult(NAME, choices, problem.unprotectableContacts(), forced.length + matched, elapsed, 0);
    }

    private static String id(ContactGraph graph, int person) {
        return graph.person(person).id();
    }
}
