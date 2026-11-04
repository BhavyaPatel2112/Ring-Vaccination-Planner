package com.ringvax.bench;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.ringvax.engine.cover.CoverProblem;
import com.ringvax.engine.cover.ExactVertexCover;
import com.ringvax.engine.graph.ContactGraph;

class HarnessTest {

    @Test
    void runsWarmupPlusRepeatsAndPreparesFreshInput() {
        AtomicInteger prepared = new AtomicInteger();
        AtomicInteger ran = new AtomicInteger();
        Harness.Measurement m = new Harness(2, 5).measure(prepared::incrementAndGet, x -> ran.incrementAndGet());

        assertEquals(7, prepared.get());
        assertEquals(7, ran.get());
        assertEquals(5, m.runs().length);
    }

    @Test
    void medianMinAndMax() {
        Harness.Measurement m = Harness.summarise(new long[] {50, 10, 40, 20, 30});
        assertEquals(30, m.median());
        assertEquals(10, m.min());
        assertEquals(50, m.max());
        assertEquals(0.00003, m.medianMs(), 1e-12);
    }

    @Test
    void invalidSettingsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new Harness(-1, 5));
        assertThrows(IllegalArgumentException.class, () -> new Harness(0, 0));
    }

    @Test
    void csvEscapesAndFormats() {
        CsvTable t = new CsvTable("name", "value", "note").add("a,b", 0.5, null).add("plain", 3, "say \"hi\"");
        assertEquals("name,value,note\n\"a,b\",0.5000,\nplain,3,\"say \"\"hi\"\"\"\n", t.toCsv());
        assertThrows(IllegalArgumentException.class, () -> t.add("too few"));
    }

    @Test
    void regularGraphsHaveTheRightDegreeAndBruteForceMatchesExact() {
        for (long seed = 0; seed < 5; seed++) {
            ContactGraph g = BenchmarkGraphs.regular(16, 4, seed);
            for (int p = 0; p < 16; p++) {
                assertEquals(4, g.degree(p));
            }
            assertEquals(32, g.contactCount());
            int exact = new ExactVertexCover().solve(CoverProblem.allContacts(g)).size();
            assertEquals(exact, BruteForceCover.minimumSize(g), "seed " + seed);
        }
        assertThrows(IllegalArgumentException.class, () -> BenchmarkGraphs.regular(5, 3, 1));
    }

    @Test
    void randomGraphsAreSeeded() {
        ContactGraph a = BenchmarkGraphs.random(30, 4, 7);
        ContactGraph b = BenchmarkGraphs.random(30, 4, 7);
        assertEquals(a.contacts(), b.contacts());
        assertTrue(a.contactCount() > 20 && a.contactCount() < 100);
    }
}
