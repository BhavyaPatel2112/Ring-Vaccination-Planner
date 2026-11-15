package com.ringvax.engine.cover;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.ringvax.engine.graph.ContactGraph;

/** The recorded steps of the 2-approximation describe exactly what it did. */
class ApproxStepsTest {

    private final ApproxVertexCover approx = new ApproxVertexCover();

    @Test
    void recordingDoesNotChangeTheResult() {
        for (long seed = 1; seed <= 50; seed++) {
            ContactGraph g = TestGraphs.random(15, 0.3, seed);
            CoverProblem problem = CoverProblem.allContacts(g);
            List<ApproxVertexCover.Step> steps = new ArrayList<>();

            assertArrayEquals(approx.solve(problem).people(), approx.solve(problem, steps::add).people());
        }
    }

    @Test
    void oneStepPerForcedPersonAndPerFreeLink() {
        ContactGraph g = TestGraphs.random(14, 0.3, 7);
        int[] all = new int[g.contactCount()];
        for (int i = 0; i < all.length; i++) {
            all[i] = i;
        }
        CoverProblem problem = CoverProblem.withExclusions(g, all, TestGraphs.randomExclusions(14, 0.2, 7));
        List<ApproxVertexCover.Step> steps = new ArrayList<>();

        approx.solve(problem, steps::add);

        assertEquals(problem.forcedPeople().length + problem.freeContacts().length, steps.size());
        assertEquals(problem.forcedPeople().length,
                steps.stream().filter(s -> s.kind() == ApproxVertexCover.StepKind.FORCED).count());
    }

    @Test
    void matchedStepsFormAMatchingAndGiveTheLowerBound() {
        ContactGraph g = TestGraphs.random(20, 0.25, 3);
        CoverProblem problem = CoverProblem.allContacts(g);
        List<ApproxVertexCover.Step> steps = new ArrayList<>();

        CoverResult result = approx.solve(problem, steps::add);

        boolean[] used = new boolean[g.personCount()];
        int matched = 0;
        for (ApproxVertexCover.Step s : steps) {
            if (s.kind() == ApproxVertexCover.StepKind.MATCHED) {
                assertFalse(used[s.a()] || used[s.b()], "no two matched links share a person");
                used[s.a()] = true;
                used[s.b()] = true;
                matched++;
            } else {
                assertTrue(used[s.a()] || used[s.b()], "a skipped link is already protected");
            }
        }
        assertEquals(result.lowerBound(), matched);
        assertEquals(result.size(), 2 * matched);
    }

    @Test
    void starTakesTheHubAndOneLeafInTheFirstStep() {
        List<ApproxVertexCover.Step> steps = new ArrayList<>();
        approx.solve(CoverProblem.allContacts(TestGraphs.star(5)), steps::add);

        assertEquals(ApproxVertexCover.StepKind.MATCHED, steps.get(0).kind());
        assertTrue(steps.subList(1, steps.size()).stream()
                .allMatch(s -> s.kind() == ApproxVertexCover.StepKind.ALREADY_PROTECTED));
    }

    @Test
    void emptyProblemHasNoSteps() {
        List<ApproxVertexCover.Step> steps = new ArrayList<>();
        approx.solve(CoverProblem.allContacts(TestGraphs.people(3)), steps::add);

        assertTrue(steps.isEmpty());
    }
}
