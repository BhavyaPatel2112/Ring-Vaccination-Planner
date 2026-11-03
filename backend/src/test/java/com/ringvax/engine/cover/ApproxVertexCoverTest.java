package com.ringvax.engine.cover;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.ringvax.data.CsvParser;
import com.ringvax.data.SyntheticNetworkGenerator;
import com.ringvax.engine.graph.ContactGraph;
import com.ringvax.engine.ring.RingBuilder;
import com.ringvax.engine.ring.RingResult;

class ApproxVertexCoverTest {

    private final ApproxVertexCover approx = new ApproxVertexCover();

    @Test
    void emptyProblemGivesAnEmptyPlan() {
        ContactGraph g = TestGraphs.people(3);
        CoverResult result = approx.solve(CoverProblem.allContacts(g));

        assertEquals(0, result.size());
        assertEquals(0, result.lowerBound());
        assertEquals(1.0, result.provenRatioBound());
        assertEquals("APPROX", result.method());
    }

    @Test
    void singleLinkTakesBothPeople() {
        CoverResult result = approx.solve(CoverProblem.allContacts(TestGraphs.path(2)));

        assertArrayEquals(new int[] {0, 1}, result.people());
        assertEquals(1, result.lowerBound());
        assertEquals(CoverChoice.Reason.MATCHED, result.choices().get(0).reason());
        assertEquals("Matched link P0 - P1", result.choices().get(0).explanation());
    }

    @Test
    void starIsTheTightCaseForFactorTwo() {
        CoverProblem problem = CoverProblem.allContacts(TestGraphs.star(8));
        CoverResult result = approx.solve(problem);

        assertEquals(2, result.size(), "hub plus one leaf");
        assertEquals(1, TestGraphs.bruteForceMinimum(problem));
        assertEquals(1, result.lowerBound());
        assertTrue(problem.isValidCover(result.people()));
    }

    @Test
    void completeBipartiteTakesBothSides() {
        CoverProblem problem = CoverProblem.allContacts(TestGraphs.completeBipartite(3, 3));
        CoverResult result = approx.solve(problem);

        assertEquals(6, result.size());
        assertEquals(3, TestGraphs.bruteForceMinimum(problem));
        assertEquals(2.0, result.provenRatioBound());
        assertTrue(problem.isValidCover(result.people()));
    }

    @Test
    void alwaysAValidCoverOnRandomGraphs() {
        for (long seed = 0; seed < 100; seed++) {
            int n = 20 + (int) (seed % 7) * 25;
            ContactGraph g = TestGraphs.random(n, 0.08, seed);
            boolean[] excluded = TestGraphs.randomExclusions(n, seed % 2 == 0 ? 0.0 : 0.15, seed);
            CoverProblem problem = CoverProblem.withExclusions(g, CoverProblem.allContacts(g).targetContacts(), excluded);
            CoverResult result = approx.solve(problem);

            assertTrue(problem.isValidCover(result.people()), "invalid cover for seed " + seed);
        }
    }

    @Test
    void neverMoreThanTwiceTheMinimum() {
        double worst = 0;
        for (long seed = 0; seed < 200; seed++) {
            int n = 6 + (int) (seed % 9);
            ContactGraph g = TestGraphs.random(n, 0.35, seed);
            boolean[] excluded = TestGraphs.randomExclusions(n, seed % 3 == 0 ? 0.2 : 0.0, seed + 1);
            CoverProblem problem = CoverProblem.withExclusions(g, CoverProblem.allContacts(g).targetContacts(), excluded);
            CoverResult result = approx.solve(problem);
            int optimum = TestGraphs.bruteForceMinimum(problem);

            assertTrue(result.size() <= 2 * optimum, "seed " + seed + ": " + result.size() + " > 2 x " + optimum);
            assertTrue(result.lowerBound() <= optimum, "lower bound must not exceed the optimum");
            if (optimum > 0) {
                worst = Math.max(worst, (double) result.size() / optimum);
            }
        }
        assertTrue(worst <= 2.0);
    }

    @Test
    void excludedHubForcesEveryLeaf() {
        ContactGraph g = TestGraphs.star(8);
        boolean[] excluded = new boolean[8];
        excluded[0] = true;
        CoverProblem problem = CoverProblem.withExclusions(g, CoverProblem.allContacts(g).targetContacts(), excluded);
        CoverResult result = approx.solve(problem);

        assertEquals(7, result.size(), "the hub cannot be vaccinated, so all 7 leaves must be");
        assertEquals(TestGraphs.bruteForceMinimum(problem), result.size());
        assertEquals(7, result.lowerBound());
        for (CoverChoice choice : result.choices()) {
            assertEquals(CoverChoice.Reason.FORCED, choice.reason());
            assertTrue(choice.explanation().endsWith("P0"), choice.explanation());
        }
    }

    @Test
    void linksWhereNobodyCanBeVaccinatedAreReported() {
        ContactGraph g = TestGraphs.path(4);
        boolean[] excluded = {true, true, false, false};
        CoverProblem problem = CoverProblem.withExclusions(g, new int[] {0, 1, 2}, excluded);
        CoverResult result = approx.solve(problem);

        assertArrayEquals(new int[] {0}, result.unprotectableContacts());
        assertArrayEquals(new int[] {2}, result.people(), "P2 covers 1-2 (forced) and 2-3");
        assertTrue(problem.isValidCover(result.people()));
    }

    @Test
    void everyChosenPersonHasOneReason() {
        ContactGraph g = TestGraphs.random(60, 0.1, 5);
        boolean[] excluded = TestGraphs.randomExclusions(60, 0.1, 6);
        CoverProblem problem = CoverProblem.withExclusions(g, CoverProblem.allContacts(g).targetContacts(), excluded);
        CoverResult result = approx.solve(problem);

        Set<Integer> seen = new HashSet<>();
        for (CoverChoice choice : result.choices()) {
            assertTrue(seen.add(choice.person()), "person listed twice");
            assertFalse(problem.isExcluded(choice.person()));
            assertFalse(choice.explanation().isBlank());
        }
    }

    @Test
    void sampleOutbreakPlanSkipsPeopleWhoCannotBeVaccinated() throws IOException {
        Path dir = Path.of("..", "data", "sample", "outbreak-small");
        ContactGraph g = new CsvParser().parseNetwork(
                Files.newBufferedReader(dir.resolve("people.csv")),
                Files.newBufferedReader(dir.resolve("contacts.csv"))).value();
        RingResult ring = new RingBuilder(g).build(List.of("P003", "P023"), 2);
        CoverProblem problem = CoverProblem.fromRing(g, ring);
        CoverResult result = approx.solve(problem);

        assertTrue(problem.isValidCover(result.people()));
        for (int p : result.people()) {
            assertTrue(g.person(p).canBeVaccinated(), g.person(p).id() + " cannot be vaccinated");
            assertTrue(ring.level(p) >= 2 || ring.level(p) == RingResult.OUTSIDE, "ring 1 is handled separately");
        }
        assertTrue(result.provenRatioBound() <= 2.0);
    }

    @Test
    void millionContactNetworkIsFast() {
        ContactGraph g = SyntheticNetworkGenerator.generate(100_000, 20, 1);
        CoverProblem problem = CoverProblem.allContacts(g);
        CoverResult result = approx.solve(problem);

        assertTrue(problem.isValidCover(result.people()));
        double seconds = result.runtimeNanos() / 1e9;
        assertTrue(seconds < 2.0, "took " + seconds + " s for " + g.contactCount() + " contacts");
    }
}
