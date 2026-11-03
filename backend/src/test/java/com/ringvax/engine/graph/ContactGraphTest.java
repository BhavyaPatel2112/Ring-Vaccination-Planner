package com.ringvax.engine.graph;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

class ContactGraphTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 1);

    private static ContactGraph graphWithPeople(int n) {
        ContactGraph g = new ContactGraph();
        for (int i = 0; i < n; i++) {
            g.addPerson(new Person("P" + i, "Person " + i, 30, "worker", false));
        }
        return g;
    }

    @Test
    void emptyGraphHasNoPeopleOrContacts() {
        ContactGraph g = new ContactGraph();
        assertEquals(0, g.personCount());
        assertEquals(0, g.contactCount());
        assertEquals(-1, g.indexOf("P1"));
        assertThrows(IndexOutOfBoundsException.class, () -> g.degree(0));
    }

    @Test
    void peopleGetIndicesInInsertionOrder() {
        ContactGraph g = graphWithPeople(3);
        assertEquals(0, g.indexOf("P0"));
        assertEquals(2, g.indexOf("P2"));
        assertTrue(g.containsPerson("P1"));
        assertEquals("Person 1", g.person(1).name());
    }

    @Test
    void contactIsStoredOnBothEndpoints() {
        ContactGraph g = graphWithPeople(3);
        int id = g.addContact(2, 0, DAY, 45);

        assertEquals(1, g.contactCount());
        assertEquals(1, g.degree(0));
        assertEquals(1, g.degree(2));
        assertEquals(0, g.degree(1));
        assertEquals(2, g.neighbour(0, 0));
        assertEquals(0, g.neighbour(2, 0));
        assertEquals(id, g.contactAt(0, 0));
        assertEquals(id, g.contactAt(2, 0));

        Contact c = g.contact(id);
        assertEquals(0, c.a(), "endpoints are normalised so a < b");
        assertEquals(2, c.b());
        assertEquals(2, c.other(0));
        assertEquals(DAY, c.date());
        assertEquals(45, c.durationMinutes());
    }

    @Test
    void hasContactWorksInBothDirections() {
        ContactGraph g = graphWithPeople(4);
        g.addContact(0, 1, DAY, 10);
        g.addContact(1, 2, DAY, 10);

        assertTrue(g.hasContact(0, 1));
        assertTrue(g.hasContact(1, 0));
        assertTrue(g.hasContact(2, 1));
        assertFalse(g.hasContact(0, 2));
        assertFalse(g.hasContact(3, 0));
    }

    @Test
    void duplicatePersonIdIsRejected() {
        ContactGraph g = graphWithPeople(1);
        assertThrows(IllegalArgumentException.class,
                () -> g.addPerson(new Person("P0", "Again", 40, "", false)));
    }

    @Test
    void selfContactAndUnknownIndicesAreRejected() {
        ContactGraph g = graphWithPeople(2);
        assertThrows(IllegalArgumentException.class, () -> g.addContact(1, 1, DAY, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> g.addContact(0, 2, DAY, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> g.addContact(-1, 0, DAY, 5));
        assertEquals(0, g.contactCount());
    }

    @Test
    void hubWithManyNeighboursGrowsItsAdjacencyList() {
        int leaves = 5000;
        ContactGraph g = graphWithPeople(leaves + 1);
        for (int i = 1; i <= leaves; i++) {
            g.addContact(0, i, DAY, 15);
        }

        assertEquals(leaves, g.degree(0));
        int[] neighbours = g.neighbours(0);
        Arrays.sort(neighbours);
        assertEquals(1, neighbours[0]);
        assertEquals(leaves, neighbours[leaves - 1]);
        for (int i = 1; i <= leaves; i++) {
            assertEquals(1, g.degree(i));
            assertTrue(g.hasContact(i, 0));
        }
    }

    @Test
    void neighboursReturnsACopy() {
        ContactGraph g = graphWithPeople(3);
        g.addContact(0, 1, DAY, 5);
        g.addContact(0, 2, DAY, 5);

        int[] copy = g.neighbours(0);
        assertArrayEquals(new int[] {1, 2}, copy);
        copy[0] = 99;
        assertEquals(1, g.neighbour(0, 0));
    }

    @Test
    void personVaccinationStatus() {
        assertTrue(new Person("A", "", 30, "", false, true, false).canBeVaccinated());
        assertFalse(new Person("B", "", 30, "", true, true, false).canBeVaccinated());
        assertFalse(new Person("C", "", 0, "", false, false, false).canBeVaccinated());
        assertFalse(new Person("D", "", 30, "", false, true, true).canBeVaccinated());
        assertThrows(IllegalArgumentException.class, () -> new Person(" ", "", 1, "", false));
    }
}
