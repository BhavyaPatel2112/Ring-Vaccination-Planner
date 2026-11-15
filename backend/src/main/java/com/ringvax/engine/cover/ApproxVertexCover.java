package com.ringvax.engine.cover;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

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
 *
 * <p>{@link #solve(CoverProblem, Consumer)} can also report every step (each forced
 * person, and each free link with whether it was matched or already protected),
 * which the "How it works" screen replays.
 */
public class ApproxVertexCover implements VertexCoverSolver {

    public static final String NAME = "APPROX";

    /** What happened at one step of the algorithm. */
    public enum StepKind { FORCED, MATCHED, ALREADY_PROTECTED }

    /**
     * One step. For FORCED, {@code person} is the forced person and {@code contact} the link that
     * forced them; for links, {@code a} and {@code b} are its two people and {@code person} is -1.
     */
    public record Step(StepKind kind, int contact, int a, int b, int person) {
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public CoverResult solve(CoverProblem problem) {
        return solve(problem, null);
    }

    /**
     * Same as {@link #solve(CoverProblem)}, and reports every step to {@code steps}
     * (when it is not null) in the order the algorithm takes them.
     */
    public CoverResult solve(CoverProblem problem, Consumer<Step> steps) {
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
            if (steps != null) {
                steps.accept(new Step(StepKind.FORCED, -1, p, problem.forcedBy(i), p));
            }
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
                if (steps != null) {
                    steps.accept(new Step(StepKind.MATCHED, c, u, v, -1));
                }
            } else if (steps != null) {
                steps.accept(new Step(StepKind.ALREADY_PROTECTED, c, u, v, -1));
            }
        }

        long elapsed = System.nanoTime() - start;
        return new CoverResult(NAME, choices, problem.unprotectableContacts(), forced.length + matched, elapsed, 0);
    }

    private static String id(ContactGraph graph, int person) {
        return graph.person(person).id();
    }
}
