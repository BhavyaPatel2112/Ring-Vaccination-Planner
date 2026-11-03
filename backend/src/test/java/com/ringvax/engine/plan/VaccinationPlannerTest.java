package com.ringvax.engine.plan;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.ringvax.engine.cover.ApproxVertexCover;
import com.ringvax.engine.cover.CoverChoice;
import com.ringvax.engine.cover.CoverProblem;
import com.ringvax.engine.cover.ExactVertexCover;
import com.ringvax.engine.graph.ContactGraph;
import com.ringvax.engine.graph.Person;
import com.ringvax.engine.ring.RingBuilder;
import com.ringvax.engine.ring.RingResult;

class VaccinationPlannerTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 1);

    /**
     * Case P0. Ring 1 = {P1, P2}. Ring 2 = {P3, P10}. P3 is a hub linked to
     * P4..P9; P10 is linked to P11. The 7 target links are 3-4 .. 3-9 and 10-11.
     * The approximation takes {3, 4, 10, 11}, so the full plan has 6 doses.
     */
    private static ContactGraph outbreak() {
        ContactGraph g = new ContactGraph();
        for (int i = 0; i < 12; i++) {
            g.addPerson(new Person("P" + i, "", 30, "", false));
        }
        int[][] links = {{0, 1}, {0, 2}, {1, 3}, {2, 10}, {3, 4}, {3, 5}, {3, 6}, {3, 7}, {3, 8}, {3, 9}, {10, 11}};
        for (int[] l : links) {
            g.addContact(l[0], l[1], DAY, 60);
        }
        return g;
    }

    private final ContactGraph graph = outbreak();
    private final RingResult ring = new RingBuilder(graph).build(List.of("P0"), 2);
    private final VaccinationPlanner planner = new VaccinationPlanner(1);
    private final ApproxVertexCover approx = new ApproxVertexCover();

    @Test
    void noBudgetGivesTheFullPlanWithRingOneFirst() {
        VaccinationPlan plan = planner.plan(graph, ring, approx, null);

        assertEquals(6, plan.doses());
        assertEquals(6, plan.dosesForFullPlan());
        assertTrue(plan.complete());
        assertFalse(plan.budgetLimited());
        assertEquals(CoverChoice.Reason.RING_1, plan.choices().get(0).reason());
        assertEquals("Direct contact of confirmed case P0", plan.choices().get(0).explanation());
        assertEquals(4, plan.lowerBound(), "2 ring 1 doses + matching of size 2");
    }

    @Test
    void budgetLargerThanNeededReturnsTheFullPlan() {
        VaccinationPlan plan = planner.plan(graph, ring, approx, 50);
        assertEquals(6, plan.doses());
        assertTrue(plan.complete());
        assertFalse(plan.budgetLimited());
    }

    @Test
    void budgetZeroListsEverythingAsUnprotected() {
        VaccinationPlan plan = planner.plan(graph, ring, approx, 0);

        assertEquals(0, plan.doses());
        assertEquals(7, plan.unprotectedContacts().length);
        assertArrayEquals(new int[] {1, 2}, plan.ring1NotVaccinated());
        assertFalse(plan.complete());
        assertTrue(plan.budgetLimited());
    }

    @Test
    void negativeBudgetIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> planner.plan(graph, ring, approx, -1));
    }

    @Test
    void budgetSmallerThanRingOneTakesTheMostConnectedFirst() {
        VaccinationPlan plan = planner.plan(graph, ring, approx, 1);

        assertArrayEquals(new int[] {1}, plan.people(), "P1 and P2 both have 2 links; lower index wins");
        assertArrayEquals(new int[] {2}, plan.ring1NotVaccinated());
        assertEquals(7, plan.unprotectedContacts().length);
    }

    @Test
    void budgetGoesToTheHubAfterRingOne() {
        VaccinationPlan plan = planner.plan(graph, ring, approx, 3);

        assertArrayEquals(new int[] {1, 2, 3}, plan.people());
        CoverChoice hub = plan.choices().get(2);
        assertEquals(CoverChoice.Reason.BUDGET_PRIORITY, hub.reason());
        assertEquals("Among the most connected 10%: 6 unprotected links", hub.explanation());
        assertEquals(1, plan.unprotectedContacts().length, "only 10-11 is left");
        assertEquals(10, plan.unprotectedContacts()[0]);
    }

    @Test
    void leftoverBudgetSkipsPeopleWhoseLinksAreAlreadySafe() {
        VaccinationPlan plan = planner.plan(graph, ring, approx, 4);

        // P4 is in the approximation's plan but all its links are covered by P3.
        assertArrayEquals(new int[] {1, 2, 3, 10}, plan.people());
        assertTrue(plan.complete(), "4 doses are enough, fewer than the approximation's 6");
        assertTrue(plan.budgetLimited());
    }

    @Test
    void neverExceedsTheBudgetAndReportsExactlyTheUncoveredLinks() {
        Random random = new Random(8);
        for (int trial = 0; trial < 40; trial++) {
            int n = 60;
            ContactGraph g = new ContactGraph();
            for (int i = 0; i < n; i++) {
                g.addPerson(new Person("P" + i, "", 30, "", random.nextInt(10) == 0,
                        random.nextInt(12) != 0, random.nextInt(15) == 0));
            }
            for (int i = 0; i < 150; i++) {
                int a = random.nextInt(n);
                int b = random.nextInt(n);
                if (a != b && !g.hasContact(a, b)) {
                    g.addContact(a, b, DAY, 30);
                }
            }
            RingResult r = new RingBuilder(g).buildFromIndices(new int[] {random.nextInt(n)}, 2);
            VaccinationPlan full = planner.plan(g, r, approx, null);
            int budget = random.nextInt(full.doses() + 2);
            VaccinationPlan plan = planner.plan(g, r, approx, budget);

            assertTrue(plan.doses() <= budget, "trial " + trial);
            boolean[] chosen = new boolean[n];
            for (int p : plan.people()) {
                assertFalse(chosen[p], "person listed twice");
                assertTrue(g.person(p).canBeVaccinated());
                chosen[p] = true;
            }
            assertArrayEquals(CoverProblem.fromRing(g, r).uncoveredContacts(chosen), plan.unprotectedContacts());
        }
    }

    @Test
    void worksWithTheExactSolverToo() {
        VaccinationPlan plan = planner.plan(graph, ring, new ExactVertexCover(), null);
        assertEquals("EXACT", plan.method());
        assertEquals(4, plan.doses(), "ring 1 (2) + minimum cover {P3, P10} (2)");
        assertEquals(4, plan.lowerBound());
    }
}
