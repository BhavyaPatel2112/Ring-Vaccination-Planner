package com.ringvax.engine.ring;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.ringvax.data.CsvParser;
import com.ringvax.engine.graph.Contact;
import com.ringvax.engine.graph.ContactGraph;
import com.ringvax.engine.graph.Person;
import com.ringvax.engine.graph.UnknownPersonException;

class RingBuilderTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 1);

    /** People P0..P(n-1); {@code special} marks "v" (vaccinated) or "x" (cannot be vaccinated). */
    private static ContactGraph graph(int n, int[][] links, String... special) {
        ContactGraph g = new ContactGraph();
        for (int i = 0; i < n; i++) {
            boolean vaccinated = false;
            boolean eligible = true;
            for (String s : special) {
                if (s.equals("v" + i)) {
                    vaccinated = true;
                }
                if (s.equals("x" + i)) {
                    eligible = false;
                }
            }
            g.addPerson(new Person("P" + i, "", 30, "", vaccinated, eligible, false));
        }
        for (int[] l : links) {
            g.addContact(l[0], l[1], DAY, l.length > 2 ? l[2] : 60);
        }
        return g;
    }

    /** Path 0-1-2-3-4-5 with the case at 0. */
    private static final int[][] PATH = {{0, 1}, {1, 2}, {2, 3}, {3, 4}, {4, 5}};

    @Test
    void caseWithNoContactsHasAnEmptyRing() {
        ContactGraph g = graph(3, new int[][] {{1, 2}});
        RingResult r = new RingBuilder(g).build(List.of("P0"), 2);

        assertEquals(0, r.peopleInRings());
        assertArrayEquals(new int[] {0}, r.casesWithoutContacts());
        assertEquals(0, r.targetContacts().length);
    }

    @Test
    void depthOneVersusDepthTwo() {
        ContactGraph g = graph(6, PATH);

        RingResult d1 = new RingBuilder(g).build(List.of("P0"), 1);
        assertArrayEquals(new int[] {1}, d1.ring(1));
        assertArrayEquals(new int[] {1}, d1.mandatory());
        assertEquals(0, d1.targetContacts().length, "with depth 1 there is no ring 2 to protect");
        assertEquals(RingResult.OUTSIDE, d1.level(2));

        RingResult d2 = new RingBuilder(g).build(List.of("P0"), 2);
        assertArrayEquals(new int[] {1}, d2.ring(1));
        assertArrayEquals(new int[] {2}, d2.ring(2));
        // 1-2 is protected because P1 is vaccinated as ring 1; 2-3 still needs protecting.
        assertArrayEquals(new int[] {2}, d2.targetContacts());
        assertEquals(2, d2.level(2));
    }

    @Test
    void alreadyVaccinatedPeopleProtectTheirLinks() {
        // Case 0, ring 1 = {1}, ring 2 = {2, 3}; P2 already vaccinated.
        ContactGraph g = graph(5, new int[][] {{0, 1}, {1, 2}, {1, 3}, {2, 4}, {3, 4}}, "v2");
        RingResult r = new RingBuilder(g).build(List.of("P0"), 2);

        assertArrayEquals(new int[] {2}, r.alreadyVaccinated());
        assertArrayEquals(new int[] {4}, r.targetContacts(), "only link 3-4; link 2-4 touches a vaccinated person");
    }

    @Test
    void unknownCaseIsRejectedByName() {
        ContactGraph g = graph(3, new int[][] {{0, 1}});
        UnknownPersonException e = assertThrows(UnknownPersonException.class,
                () -> new RingBuilder(g).build(List.of("P0", "P42"), 2));
        assertEquals("P42", e.personId());
    }

    @Test
    void invalidRequestsAreRejected() {
        ContactGraph g = graph(3, new int[][] {{0, 1}});
        RingBuilder builder = new RingBuilder(g);
        assertThrows(IllegalArgumentException.class, () -> builder.build(List.of(), 2));
        assertThrows(IllegalArgumentException.class, () -> builder.build(List.of("P0"), 0));
        assertThrows(IllegalArgumentException.class, () -> builder.build(List.of("P0"), RingBuilder.MAX_DEPTH + 1));
        assertThrows(IllegalArgumentException.class, () -> builder.buildFromIndices(new int[] {7}, 2));
    }

    @Test
    void overlappingRingsFromTwoCasesAreMerged() {
        // Cases 0 and 4 share contact 2; P4 is also a contact of P3 but stays a case.
        ContactGraph g = graph(6, new int[][] {{0, 1}, {0, 2}, {4, 2}, {4, 3}, {3, 5}, {1, 5}});
        RingResult r = new RingBuilder(g).build(List.of("P4", "P0", "P0"), 2);

        assertArrayEquals(new int[] {0, 4}, r.cases(), "duplicates removed, sorted");
        assertArrayEquals(new int[] {1, 2, 3}, r.ring(1));
        assertArrayEquals(new int[] {5}, r.ring(2));
        assertEquals(0, r.level(4));
        assertEquals(4, r.peopleInRings());
        assertEquals(0, r.targetContacts().length, "P5's links all go to vaccinated ring 1 members");
    }

    @Test
    void ringOneMemberWhoCannotBeVaccinatedLeavesLinksToProtect() {
        // Case 0, ring 1 = {1 (infant), 2}, ring 2 = {3, 4}.
        ContactGraph g = graph(5, new int[][] {{0, 1}, {0, 2}, {1, 3}, {2, 4}}, "x1");
        RingResult r = new RingBuilder(g).build(List.of("P0"), 2);

        assertArrayEquals(new int[] {2}, r.mandatory());
        assertArrayEquals(new int[] {1}, r.cannotVaccinate());
        assertArrayEquals(new int[] {2}, r.targetContacts(), "link 1-3 must be protected through P3");
    }

    @Test
    void contactFilterIgnoresShortContacts() {
        // 0-1 is a 5 minute contact, 0-2 is 60 minutes.
        ContactGraph g = graph(4, new int[][] {{0, 1, 5}, {0, 2, 60}, {2, 3, 60}});
        RingResult r = new RingBuilder(g, c -> c.durationMinutes() >= 15).build(List.of("P0"), 2);

        assertArrayEquals(new int[] {2}, r.ring(1));
        assertArrayEquals(new int[] {3}, r.ring(2));
        assertEquals(RingResult.OUTSIDE, r.level(1));
    }

    @Test
    void matchesAPlainReferenceOnRandomNetworks() {
        Random random = new Random(11);
        for (int trial = 0; trial < 60; trial++) {
            int n = 10 + random.nextInt(40);
            ContactGraph g = new ContactGraph();
            for (int i = 0; i < n; i++) {
                boolean vaccinated = random.nextInt(8) == 0;
                boolean eligible = random.nextInt(10) != 0;
                g.addPerson(new Person("P" + i, "", 30, "", vaccinated, eligible, false));
            }
            for (int i = 0; i < n * 2; i++) {
                int a = random.nextInt(n);
                int b = random.nextInt(n);
                if (a != b && !g.hasContact(a, b)) {
                    g.addContact(a, b, DAY, 30);
                }
            }
            int[] cases = random.ints(1 + random.nextInt(3), 0, n).toArray();
            int depth = 1 + random.nextInt(3);

            RingResult r = new RingBuilder(g).buildFromIndices(cases, depth);
            int[] expectedLevel = referenceLevels(g, cases, depth);
            for (int p = 0; p < n; p++) {
                assertEquals(expectedLevel[p], r.level(p), "level of P" + p + " in trial " + trial);
            }
            assertArrayEquals(referenceTargets(g, expectedLevel), r.targetContacts(), "targets in trial " + trial);
        }
    }

    private static int[] referenceLevels(ContactGraph g, int[] cases, int depth) {
        int[] level = new int[g.personCount()];
        Arrays.fill(level, -1);
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        for (int c : cases) {
            level[c] = 0;
            queue.add(c);
        }
        while (!queue.isEmpty()) {
            int u = queue.poll();
            for (int v : g.neighbours(u)) {
                if (level[v] == -1 && level[u] < depth) {
                    level[v] = level[u] + 1;
                    queue.add(v);
                }
            }
        }
        return level;
    }

    private static int[] referenceTargets(ContactGraph g, int[] level) {
        return IntStream.range(0, g.contactCount()).filter(i -> {
            Contact c = g.contact(i);
            boolean deep = level[c.a()] >= 2 || level[c.b()] >= 2;
            return deep && open(g, c.a(), level) && open(g, c.b(), level);
        }).toArray();
    }

    private static boolean open(ContactGraph g, int p, int[] level) {
        Person person = g.person(p);
        if (level[p] == 0 || person.vaccinated()) {
            return false;
        }
        return !(level[p] == 1 && person.canBeVaccinated());
    }

    @Test
    void sampleOutbreakRingsAreConsistent() throws IOException {
        Path dir = Path.of("..", "data", "sample", "outbreak-small");
        ContactGraph g = new CsvParser().parseNetwork(
                Files.newBufferedReader(dir.resolve("people.csv")),
                Files.newBufferedReader(dir.resolve("contacts.csv"))).value();

        RingResult r = new RingBuilder(g).build(List.of("P003", "P023"), 2);
        String ring1 = Arrays.toString(Arrays.stream(r.ring(1)).mapToObj(p -> g.person(p).id()).toArray());
        assertEquals("[P001, P002, P004, P008, P011, P018, P024, P025, P029, P030, P035, P037, P040]", ring1);
        assertTrue(Arrays.stream(r.cannotVaccinate()).anyMatch(p -> g.person(p).id().equals("P004")));
        assertTrue(Arrays.stream(r.cannotVaccinate()).anyMatch(p -> g.person(p).id().equals("P011")));
        assertTrue(r.targetContacts().length > 0);
    }
}
