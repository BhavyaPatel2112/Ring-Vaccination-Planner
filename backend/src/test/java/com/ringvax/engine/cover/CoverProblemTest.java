package com.ringvax.engine.cover;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.ringvax.engine.graph.ContactGraph;

class CoverProblemTest {

    @Test
    void linksAreSplitIntoUnprotectableForcedAndFree() {
        // 0-1 both excluded, 1-2 forces P2, 2-3 covered by P2, 3-4 free.
        ContactGraph g = TestGraphs.fromLinks(5, new int[][] {{0, 1}, {1, 2}, {2, 3}, {3, 4}});
        boolean[] excluded = {true, true, false, false, false};
        CoverProblem problem = CoverProblem.withExclusions(g, new int[] {0, 1, 2, 3}, excluded);

        assertArrayEquals(new int[] {0}, problem.unprotectableContacts());
        assertArrayEquals(new int[] {2}, problem.forcedPeople());
        assertArrayEquals(new int[] {3}, problem.freeContacts());
    }

    @Test
    void validCoverChecks() {
        ContactGraph g = TestGraphs.path(4);
        boolean[] excluded = {false, false, false, true};
        CoverProblem problem = CoverProblem.withExclusions(g, new int[] {0, 1, 2}, excluded);

        assertTrue(problem.isValidCover(new int[] {1, 2}));
        assertFalse(problem.isValidCover(new int[] {1}), "link 2-3 not covered");
        assertFalse(problem.isValidCover(new int[] {1, 3}), "P3 is excluded");
        assertFalse(problem.isValidCover(new int[] {1, 2, 2}), "duplicate person");
    }

    @Test
    void badTargetListsAreRejected() {
        ContactGraph g = TestGraphs.path(3);
        assertThrows(IllegalArgumentException.class, () -> CoverProblem.of(g, new int[] {0, 0}));
        assertThrows(IllegalArgumentException.class, () -> CoverProblem.of(g, new int[] {5}));
        assertThrows(IllegalArgumentException.class,
                () -> CoverProblem.withExclusions(g, new int[] {0}, new boolean[2]));
    }
}
