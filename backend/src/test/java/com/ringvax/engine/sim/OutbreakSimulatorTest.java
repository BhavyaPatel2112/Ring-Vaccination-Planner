package com.ringvax.engine.sim;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.ringvax.data.SyntheticNetworkGenerator;
import com.ringvax.engine.cover.ApproxVertexCover;
import com.ringvax.engine.graph.ContactGraph;
import com.ringvax.engine.graph.Person;
import com.ringvax.engine.plan.VaccinationPlan;
import com.ringvax.engine.plan.VaccinationPlanner;
import com.ringvax.engine.ring.RingBuilder;
import com.ringvax.engine.ring.RingResult;

class OutbreakSimulatorTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 1);

    private static OutbreakSimulator.Settings settings(double p, int recovery, double efficacy, int trials, long seed) {
        return new OutbreakSimulator.Settings(p, recovery, efficacy, trials, 365, seed);
    }

    private static ContactGraph path(int n) {
        ContactGraph g = new ContactGraph();
        for (int i = 0; i < n; i++) {
            g.addPerson(new Person("P" + i, "", null, "", false));
        }
        for (int i = 0; i + 1 < n; i++) {
            g.addContact(i, i + 1, DAY, 30);
        }
        return g;
    }

    @Test
    void sameSeedGivesTheSameResult() {
        ContactGraph g = SyntheticNetworkGenerator.generate(500, 6, 3);
        boolean[] none = new boolean[g.personCount()];

        OutbreakSimulator.Result a = new OutbreakSimulator(g, settings(0.1, 4, 1, 50, 2026)).run(new int[] {0}, none);
        OutbreakSimulator.Result b = new OutbreakSimulator(g, settings(0.1, 4, 1, 50, 2026)).run(new int[] {0}, none);

        assertArrayEquals(a.newInfections(), b.newInfections());
        assertEquals(a.mean(), b.mean());
    }

    @Test
    void noTransmissionMeansNoNewInfections() {
        ContactGraph g = path(10);
        OutbreakSimulator.Result r = new OutbreakSimulator(g, settings(0, 3, 1, 20, 1)).run(new int[] {0}, new boolean[10]);

        assertEquals(0, r.max());
        assertEquals(0.0, r.mean());
    }

    @Test
    void certainTransmissionInfectsTheWholeChain() {
        ContactGraph g = path(10);
        OutbreakSimulator.Result r = new OutbreakSimulator(g, settings(1, 1, 1, 5, 1)).run(new int[] {0}, new boolean[10]);

        assertEquals(9, r.min(), "everyone except the case");
        // With p = 1 and one step of infectiousness, one new person per step: the curve is flat at 1.
        assertEquals(1.0, r.curve()[3]);
    }

    @Test
    void vaccinatedPersonBlocksTheChain() {
        ContactGraph g = path(10);
        boolean[] vaccinated = new boolean[10];
        vaccinated[3] = true;

        OutbreakSimulator.Result full = new OutbreakSimulator(g, settings(1, 1, 1, 5, 1)).run(new int[] {0}, vaccinated);
        OutbreakSimulator.Result useless = new OutbreakSimulator(g, settings(1, 1, 0, 5, 1)).run(new int[] {0}, vaccinated);

        assertEquals(2, full.max(), "only P1 and P2 can be reached");
        assertEquals(9, useless.min(), "efficacy 0 protects nobody");
    }

    @Test
    void percentilesAreOrdered() {
        ContactGraph g = SyntheticNetworkGenerator.generate(400, 8, 5);
        OutbreakSimulator.Result r = new OutbreakSimulator(g, settings(0.06, 4, 1, 101, 9))
                .run(new int[] {0, 1}, new boolean[g.personCount()]);
        int[] sorted = r.newInfections().clone();
        Arrays.sort(sorted);

        assertEquals(sorted[50], r.median());
        assertEquals(sorted[90], r.p90(), "nearest-rank 90th percentile of 101 values");
        assertTrue(r.min() <= r.median() && r.median() <= r.p90() && r.p90() <= r.max());
    }

    @Test
    void ringVaccinationPlanBeatsRandomVaccinationWithTheSameDoses() {
        ContactGraph g = SyntheticNetworkGenerator.generate(2000, 8, 11);
        int[] cases = {0, 500};
        RingResult ring = new RingBuilder(g).buildFromIndices(cases, 2);
        VaccinationPlan plan = new VaccinationPlanner(2026).plan(g, ring, new ApproxVertexCover(), null);
        boolean[] planned = new boolean[g.personCount()];
        for (int p : plan.people()) {
            planned[p] = true;
        }
        boolean[] random = new boolean[g.personCount()];
        Random pick = new Random(4);
        for (int given = 0; given < plan.doses(); ) {
            int p = pick.nextInt(g.personCount());
            if (!random[p] && p != 0 && p != 500) {
                random[p] = true;
                given++;
            }
        }
        OutbreakSimulator sim = new OutbreakSimulator(g, settings(0.08, 4, 1, 200, 2026));

        double withPlan = sim.run(cases, planned).mean();
        double withRandom = sim.run(cases, random).mean();

        assertTrue(withPlan < withRandom * 0.7, "plan " + withPlan + " vs random " + withRandom);
    }

    @Test
    void invalidSettingsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> settings(1.5, 3, 1, 10, 1));
        assertThrows(IllegalArgumentException.class, () -> settings(0.1, 0, 1, 10, 1));
        assertThrows(IllegalArgumentException.class, () -> settings(0.1, 3, -0.1, 10, 1));
        assertThrows(IllegalArgumentException.class, () -> settings(0.1, 3, 1, 0, 1));
        ContactGraph g = path(3);
        OutbreakSimulator sim = new OutbreakSimulator(g, settings(0.1, 3, 1, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> sim.run(new int[] {7}, new boolean[3]));
    }

    // ------------------------------------------------------------------ per-contact probabilities

    @Test
    void sameProbabilityOnEveryContactMatchesTheSettings() {
        ContactGraph g = SyntheticNetworkGenerator.generate(500, 6, 3);
        boolean[] none = new boolean[g.personCount()];
        double[] uniform = new double[g.contactCount()];
        Arrays.fill(uniform, 0.1);

        OutbreakSimulator.Result plain = new OutbreakSimulator(g, settings(0.1, 4, 1, 50, 2026)).run(new int[] {0}, none);
        OutbreakSimulator.Result perContact =
                new OutbreakSimulator(g, settings(0.1, 4, 1, 50, 2026), uniform).run(new int[] {0}, none);

        assertArrayEquals(plain.newInfections(), perContact.newInfections(), "same draws, same outbreaks");
    }

    @Test
    void zeroProbabilityContactsNeverTransmit() {
        ContactGraph g = path(6);
        double[] q = {1, 1, 0, 1, 1};    // contact 2 links P2 and P3
        OutbreakSimulator.Result r = new OutbreakSimulator(g, settings(0.5, 10, 1, 20, 1), q)
                .run(new int[] {0}, new boolean[6]);

        assertEquals(2, r.max(), "the chain stops at the zero-risk contact: only P1 and P2 are infected");
    }

    @Test
    void durationWeightedRiskGrowsWithContactTime() {
        ContactGraph g = new ContactGraph();
        for (int i = 0; i < 5; i++) {
            g.addPerson(new Person("P" + i, "", null, "", false));
        }
        g.addContact(0, 1, DAY, 15);
        g.addContact(0, 2, DAY, 30);
        g.addContact(0, 3, DAY, 1);
        g.addContact(0, 4, DAY, -1);    // unknown duration counts as 15 minutes

        double[] q = OutbreakSimulator.durationWeighted(g, 0.2);

        assertEquals(0.2, q[0], 1e-12, "15 minutes = one exposure");
        assertEquals(1 - 0.8 * 0.8, q[1], 1e-12, "30 minutes = two exposures");
        assertEquals(1 - Math.pow(0.8, 1 / 15.0), q[2], 1e-12);
        assertTrue(q[2] < q[0] && q[0] < q[1]);
        assertEquals(0.2, q[3], 1e-12);
    }

    @Test
    void invalidContactProbabilitiesAreRejected() {
        ContactGraph g = path(3);
        assertThrows(IllegalArgumentException.class,
                () -> new OutbreakSimulator(g, settings(0.1, 3, 1, 1, 1), new double[] {0.5}));
        assertThrows(IllegalArgumentException.class,
                () -> new OutbreakSimulator(g, settings(0.1, 3, 1, 1, 1), new double[] {0.5, 1.5}));
        assertThrows(IllegalArgumentException.class, () -> OutbreakSimulator.durationWeighted(g, -0.1));
    }
}
