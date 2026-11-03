package com.ringvax.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.ringvax.data.SyntheticNetworkGenerator.Config;
import com.ringvax.engine.graph.Contact;
import com.ringvax.engine.graph.ContactGraph;
import com.ringvax.engine.graph.Person;

class SyntheticNetworkGeneratorTest {

    @Test
    void sameSeedGivesExactlyTheSameNetwork() {
        ContactGraph first = SyntheticNetworkGenerator.generate(2000, 12, 42);
        ContactGraph second = SyntheticNetworkGenerator.generate(2000, 12, 42);

        assertEquals(first.people(), second.people());
        assertEquals(first.contacts(), second.contacts());
    }

    @Test
    void differentSeedsGiveDifferentNetworks() {
        ContactGraph a = SyntheticNetworkGenerator.generate(2000, 12, 1);
        ContactGraph b = SyntheticNetworkGenerator.generate(2000, 12, 2);
        assertNotEquals(a.contacts(), b.contacts());
    }

    @Test
    void averageContactsIsCloseToTheTarget() {
        for (double target : new double[] {5, 10, 25}) {
            ContactGraph g = SyntheticNetworkGenerator.generate(20_000, target, 7);
            double average = 2.0 * g.contactCount() / g.personCount();
            assertEquals(20_000, g.personCount());
            assertEquals(target, average, target * 0.05, "average contacts for target " + target);
        }
    }

    @Test
    void noSelfContactsOrDuplicateLinks() {
        ContactGraph g = SyntheticNetworkGenerator.generate(5000, 15, 99);
        Set<Long> seen = new HashSet<>();
        for (Contact c : g.contacts()) {
            assertTrue(c.a() < c.b());
            assertTrue(seen.add(((long) c.a() << 32) | c.b()), "duplicate link " + c);
        }
    }

    @Test
    void everyoneHasAContactAndRealisticAttributes() {
        ContactGraph g = SyntheticNetworkGenerator.generate(5000, 8, 3);
        LocalDate latest = SyntheticNetworkGenerator.DEFAULT_REFERENCE_DATE;
        int vaccinated = 0;
        for (int p = 0; p < g.personCount(); p++) {
            Person person = g.person(p);
            assertTrue(g.degree(p) >= 1, "household members always meet each other");
            assertTrue(person.age() >= 0 && person.age() <= 90);
            if (person.age() < 1) {
                assertFalse(person.eligible(), "infants cannot take a live vaccine");
            }
            if (person.vaccinated()) {
                vaccinated++;
                assertTrue(person.eligible() && !person.refused());
            }
        }
        assertTrue(vaccinated > 300 && vaccinated < 700, "about 10% vaccinated, got " + vaccinated);
        for (Contact c : g.contacts()) {
            assertFalse(c.date().isAfter(latest));
            assertTrue(c.date().isAfter(latest.minusDays(14)));
            assertTrue(c.durationMinutes() >= 5 && c.durationMinutes() <= 720);
        }
        assertEquals("P0001", g.person(0).id());
    }

    @Test
    void smallestPopulationIsOneHousehold() {
        ContactGraph g = SyntheticNetworkGenerator.generate(2, 1, 5);
        assertEquals(2, g.personCount());
        assertEquals(1, g.contactCount());
    }

    @Test
    void invalidSettingsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> SyntheticNetworkGenerator.generate(1, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> SyntheticNetworkGenerator.generate(2_000_001, 5, 0));
        assertThrows(IllegalArgumentException.class, () -> SyntheticNetworkGenerator.generate(100, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> SyntheticNetworkGenerator.generate(100, -4, 0));
        assertThrows(IllegalArgumentException.class, () -> SyntheticNetworkGenerator.generate(100, Double.NaN, 0));
        assertThrows(IllegalArgumentException.class, () -> SyntheticNetworkGenerator.generate(10, 10, 0));
        assertThrows(IllegalArgumentException.class, () -> SyntheticNetworkGenerator.generate(1000, 500, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new Config(100, 5, 0, 1.5, 0, LocalDate.of(2026, 11, 1)));
    }

    @Test
    void millionContactNetworkIsGeneratedQuickly() {
        long start = System.nanoTime();
        ContactGraph g = SyntheticNetworkGenerator.generate(100_000, 20, 2026);
        double seconds = (System.nanoTime() - start) / 1e9;

        assertEquals(1_000_000, g.contactCount(), 20_000);
        assertTrue(seconds < 15, "took " + seconds + " s");
    }
}
