package com.ringvax.engine.cover;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.ringvax.data.CsvParser;
import com.ringvax.engine.graph.ContactGraph;

class GreedyVertexCoverTest {

    private final GreedyVertexCover greedy = new GreedyVertexCover();

    private static ContactGraph sample(String folder) throws IOException {
        Path dir = Path.of("..", "data", "sample", folder);
        return new CsvParser().parseNetwork(Files.newBufferedReader(dir.resolve("people.csv")),
                Files.newBufferedReader(dir.resolve("contacts.csv"))).value();
    }

    @Test
    void emptyProblemGivesAnEmptyPlan() {
        CoverResult result = greedy.solve(CoverProblem.allContacts(TestGraphs.people(4)));

        assertEquals(0, result.size());
        assertEquals("GREEDY", result.method());
        assertEquals(-1, result.lowerBound(), "greedy has no guarantee, so no lower bound");
    }

    @Test
    void starTakesOnlyTheHub() {
        CoverProblem problem = CoverProblem.allContacts(TestGraphs.star(8));
        CoverResult result = greedy.solve(problem);

        assertArrayEquals(new int[] {0}, result.people());
        assertEquals(CoverChoice.Reason.GREEDY, result.choices().get(0).reason());
        assertEquals("Covered 7 unprotected links when chosen (most connected first)",
                result.choices().get(0).explanation());
    }

    @Test
    void pathOfFourTakesTheTwoMiddlePeople() {
        CoverResult result = greedy.solve(CoverProblem.allContacts(TestGraphs.path(4)));

        assertArrayEquals(new int[] {1, 2}, result.people());
    }

    @Test
    void validCoverOnRandomGraphsWithAndWithoutExclusions() {
        for (long seed = 1; seed <= 150; seed++) {
            ContactGraph g = TestGraphs.random(14, 0.25, seed);
            CoverProblem plain = CoverProblem.allContacts(g);
            CoverResult result = greedy.solve(plain);
            assertTrue(plain.isValidCover(result.people()), "seed " + seed);
            assertTrue(result.size() >= TestGraphs.bruteForceMinimum(plain), "never below the minimum");

            int[] all = new int[g.contactCount()];
            for (int i = 0; i < all.length; i++) {
                all[i] = i;
            }
            CoverProblem constrained = CoverProblem.withExclusions(g, all, TestGraphs.randomExclusions(14, 0.2, seed));
            CoverResult r2 = greedy.solve(constrained);
            assertTrue(constrained.isValidCover(r2.people()), "seed " + seed + " with exclusions");
            assertArrayEquals(constrained.unprotectableContacts(), r2.unprotectableContacts());
        }
    }

    @Test
    void forcedPeopleComeFirstWithTheirReason() {
        ContactGraph g = TestGraphs.path(3);
        int[] all = {0, 1};
        boolean[] excluded = {true, false, false};
        CoverResult result = greedy.solve(CoverProblem.withExclusions(g, all, excluded));

        assertArrayEquals(new int[] {1}, result.people(), "person 1 is forced and also covers the second link");
        assertEquals(CoverChoice.Reason.FORCED, result.choices().get(0).reason());
    }

    @Test
    void greedyTrapPicks23AgainstAMinimumOf12() throws IOException {
        CoverProblem problem = CoverProblem.allContacts(sample("greedy-worst-case-12"));
        CoverResult result = greedy.solve(problem);
        CoverResult exact = new ExactVertexCover().solve(problem);

        assertEquals(23, result.size());
        assertEquals(12, exact.size());
        assertTrue(problem.isValidCover(result.people()));
    }

    @Test
    void largerTrapIsWorseThanTheApproximationGuarantee() throws IOException {
        CoverProblem problem = CoverProblem.allContacts(sample("greedy-worst-case-18"));

        int greedySize = greedy.solve(problem).size();

        assertEquals(40, greedySize);
        assertTrue(greedySize > 2 * 18, "40 > 2 x 18: greedy breaks the factor the approximation guarantees");
    }
}
