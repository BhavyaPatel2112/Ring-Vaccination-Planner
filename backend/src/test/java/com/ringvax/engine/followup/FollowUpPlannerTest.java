package com.ringvax.engine.followup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.ringvax.engine.graph.ContactGraph;
import com.ringvax.engine.graph.Person;
import com.ringvax.engine.ring.RingBuilder;
import com.ringvax.engine.ring.RingResult;

class FollowUpPlannerTest {

    private static final LocalDate NOV = LocalDate.of(2026, 11, 1);

    /**
     * Case P0. Ring 1: P1 (met 1 Nov), P2 (met 30 Oct, and again 25 Oct), P3 (no date).
     * Ring 2: P4 met P1 on 2 Nov. P5 is ring 1 but refused.
     */
    private static ContactGraph network() {
        ContactGraph g = new ContactGraph();
        for (int i = 0; i < 6; i++) {
            g.addPerson(new Person("P" + i, "", null, "", false, true, i == 5));
        }
        g.addContact(0, 1, NOV, 60);
        g.addContact(0, 2, NOV.minusDays(2), 60);
        g.addContact(0, 3, null, 60);
        g.addContact(1, 4, NOV.plusDays(1), 60);
        g.addContact(0, 5, NOV, 60);
        return g;
    }

    private static FollowUpPlanner.Result run(int[] plan, LocalDateTime asOf) {
        ContactGraph g = network();
        RingResult ring = new RingBuilder(g).build(List.of("P0"), 2);
        return new FollowUpPlanner(g, c -> true).plan(ring, plan, asOf, 72, 21);
    }

    @Test
    void lastExposureIsTheLatestContactWithTheCloserRing() {
        ContactGraph g = network();
        RingResult ring = new RingBuilder(g).build(List.of("P0"), 2);
        LocalDate[] last = new FollowUpPlanner(g, c -> true).lastExposures(ring);

        assertEquals(NOV, last[1]);
        assertEquals(NOV.minusDays(2), last[2]);
        assertNull(last[3], "no dated contact");
        assertEquals(NOV.plusDays(1), last[4], "ring 2: exposed to P1");
        assertNull(last[0], "the case itself has no exposure");
    }

    @Test
    void dosesAreOrderedByDeadlineWithUnknownDatesFirst() {
        FollowUpPlanner.Result r = run(new int[] {4, 1, 2, 3}, NOV.atStartOfDay());
        List<FollowUpPlanner.Dose> d = r.doses();

        assertEquals(3, d.get(0).person(), "date unknown: as soon as possible");
        assertEquals(FollowUpPlanner.Status.DATE_UNKNOWN, d.get(0).status());
        assertEquals(2, d.get(1).person(), "exposed 30 Oct: due 2 Nov 00:00");
        assertEquals(1, d.get(2).person());
        assertEquals(4, d.get(3).person());
        assertEquals(LocalDateTime.of(2026, 11, 2, 0, 0), d.get(1).deadline());
        assertEquals(24, d.get(1).hoursLeft());
        assertEquals(FollowUpPlanner.Status.URGENT, d.get(1).status());
        assertEquals(FollowUpPlanner.Status.ON_TIME, d.get(2).status());
    }

    @Test
    void peoplePastTheWindowAreMonitoredInstead() {
        FollowUpPlanner.Result r = run(new int[] {1, 2}, LocalDateTime.of(2026, 11, 3, 12, 0));

        FollowUpPlanner.Dose p2 = r.doses().stream().filter(x -> x.person() == 2).findFirst().orElseThrow();
        assertEquals(FollowUpPlanner.Status.PAST_WINDOW, p2.status());
        assertTrue(p2.hoursLeft() < 0);
        assertTrue(r.monitoring().stream().anyMatch(m -> m.person() == 2
                && m.reason() == FollowUpPlanner.MonitorReason.PAST_WINDOW
                && m.monitorUntil().equals(NOV.minusDays(2).plusDays(21))));
    }

    @Test
    void monitoringListsRingMembersWithoutADose() {
        FollowUpPlanner.Result r = run(new int[] {1, 2, 3}, NOV.atStartOfDay());

        // P5 refused, P4 is ring 2 and not in the plan; the case and the dosed people are not listed.
        assertEquals(2, r.monitoring().size());
        FollowUpPlanner.Monitor refused = r.monitoring().get(0);
        assertEquals(5, refused.person());
        assertEquals(FollowUpPlanner.MonitorReason.CANNOT_BE_VACCINATED, refused.reason());
        assertEquals(NOV.plusDays(21), refused.monitorUntil());
        assertEquals(FollowUpPlanner.MonitorReason.NOT_IN_PLAN, r.monitoring().get(1).reason());
    }

    @Test
    void emptyPlanGivesNoDoses() {
        FollowUpPlanner.Result r = run(new int[0], NOV.atStartOfDay());

        assertTrue(r.doses().isEmpty());
        assertEquals(FollowUpPlanner.MonitorReason.LEFT_OUT_BY_BUDGET, r.monitoring().stream()
                .filter(m -> m.person() == 1).findFirst().orElseThrow().reason());
    }

    @Test
    void invalidWindowsAreRejected() {
        ContactGraph g = network();
        RingResult ring = new RingBuilder(g).build(List.of("P0"), 1);
        FollowUpPlanner planner = new FollowUpPlanner(g, c -> true);

        assertThrows(IllegalArgumentException.class, () -> planner.plan(ring, new int[0], NOV.atStartOfDay(), 0, 21));
        assertThrows(IllegalArgumentException.class, () -> planner.plan(ring, new int[0], NOV.atStartOfDay(), 72, 0));
    }
}
