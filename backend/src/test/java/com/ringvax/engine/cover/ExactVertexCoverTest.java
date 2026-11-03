package com.ringvax.engine.cover;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.ringvax.data.CsvParser;
import com.ringvax.engine.graph.ContactGraph;

class ExactVertexCoverTest {

    private final ExactVertexCover exact = new ExactVertexCover();

    private int minimum(ContactGraph g) {
        CoverProblem problem = CoverProblem.allContacts(g);
        CoverResult result = exact.solve(problem);
        assertTrue(problem.isValidCover(result.people()), "exact result must be a valid cover");
        return result.size();
    }

    @Test
    void emptyGraphNeedsNobody() {
        CoverResult result = exact.solve(CoverProblem.allContacts(TestGraphs.people(4)));
        assertEquals(0, result.size());
        assertEquals(1.0, result.provenRatioBound());
    }

    @Test
    void triangleNeedsTwo() {
        assertEquals(2, minimum(TestGraphs.fromLinks(3, new int[][] {{0, 1}, {1, 2}, {0, 2}})));
    }

    @Test
    void pathOfFourNeedsTwo() {
        assertEquals(2, minimum(TestGraphs.path(4)));
    }

    @Test
    void knownMinimumsOnTightExamples() {
        assertEquals(1, minimum(TestGraphs.star(10)));
        assertEquals(3, minimum(TestGraphs.completeBipartite(3, 3)));
        assertEquals(4, minimum(TestGraphs.completeBipartite(4, 9)));
        assertEquals(4, minimum(TestGraphs.fromLinks(6, new int[][] {{0, 1}, {1, 2}, {0, 2}, {3, 4}, {4, 5}, {3, 5}})),
                "two separate triangles");
    }

    @Test
    void matchesBruteForceOnRandomGraphs() {
        for (long seed = 0; seed < 120; seed++) {
            int n = 5 + (int) (seed % 11);
            ContactGraph g = TestGraphs.random(n, 0.2 + (seed % 5) * 0.1, seed);
            boolean[] excluded = TestGraphs.randomExclusions(n, seed % 4 == 0 ? 0.25 : 0.0, seed + 7);
            CoverProblem problem = CoverProblem.withExclusions(g, CoverProblem.allContacts(g).targetContacts(), excluded);
            CoverResult result = exact.solve(problem);

            assertTrue(problem.isValidCover(result.people()), "seed " + seed);
            assertEquals(TestGraphs.bruteForceMinimum(problem), result.size(), "seed " + seed);
        }
    }

    @Test
    void neverWorseThanTheApproximation() {
        ApproxVertexCover approx = new ApproxVertexCover();
        for (long seed = 0; seed < 40; seed++) {
            ContactGraph g = TestGraphs.random(35, 0.12, seed);
            CoverProblem problem = CoverProblem.allContacts(g);
            CoverResult exactResult = exact.solve(problem);
            CoverResult approxResult = approx.solve(problem);

            assertTrue(exactResult.size() <= approxResult.size());
            assertTrue(approxResult.lowerBound() <= exactResult.size());
            assertTrue(approxResult.size() <= 2 * exactResult.size());
        }
    }

    @Test
    void greedyWorstCaseSampleHasMinimumTwelve() throws IOException {
        Path dir = Path.of("..", "data", "sample", "greedy-worst-case-12");
        ContactGraph g = new CsvParser().parseNetwork(
                Files.newBufferedReader(dir.resolve("people.csv")),
                Files.newBufferedReader(dir.resolve("contacts.csv"))).value();
        assertEquals(35, g.personCount());
        assertEquals(12, new ExactVertexCover(64, Duration.ofSeconds(10)).solve(CoverProblem.allContacts(g)).size());
    }

    @Test
    void refusesProblemsAboveTheLimit() {
        ContactGraph g = TestGraphs.path(42);
        ExactSolverLimitException e = assertThrows(ExactSolverLimitException.class,
                () -> exact.solve(CoverProblem.allContacts(g)));
        assertEquals(ExactSolverLimitException.Kind.TOO_LARGE, e.kind());
        assertTrue(e.getMessage().contains("42"), e.getMessage());
        assertTrue(e.getMessage().contains("approximation"));
    }

    @Test
    void forcedPeopleDoNotCountTowardsTheLimit() {
        // Hub P0 excluded: all 41 leaves are forced, nothing is left to search.
        ContactGraph g = TestGraphs.star(42);
        boolean[] excluded = new boolean[42];
        excluded[0] = true;
        CoverProblem problem = CoverProblem.withExclusions(g, CoverProblem.allContacts(g).targetContacts(), excluded);
        assertEquals(41, exact.solve(problem).size());
    }

    @Test
    void stopsAtTheTimeLimit() {
        ContactGraph g = TestGraphs.random(40, 0.5, 3);
        ExactVertexCover impatient = new ExactVertexCover(40, Duration.ofNanos(1));
        ExactSolverLimitException e = assertThrows(ExactSolverLimitException.class,
                () -> impatient.solve(CoverProblem.allContacts(g)));
        assertEquals(ExactSolverLimitException.Kind.TIMED_OUT, e.kind());
    }

    @Test
    void invalidSettingsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ExactVertexCover(0, Duration.ofSeconds(1)));
        assertThrows(IllegalArgumentException.class, () -> new ExactVertexCover(65, Duration.ofSeconds(1)));
        assertThrows(IllegalArgumentException.class, () -> new ExactVertexCover(10, Duration.ZERO));
    }

    @Test
    void fortyPersonRandomNetworkFinishesInTime() {
        ContactGraph g = TestGraphs.random(40, 0.15, 21);
        CoverResult result = exact.solve(CoverProblem.allContacts(g));
        assertTrue(result.runtimeNanos() < ExactVertexCover.DEFAULT_TIMEOUT.toNanos());
        assertEquals(CoverChoice.Reason.EXACT, result.choices().get(0).reason());
    }
}
