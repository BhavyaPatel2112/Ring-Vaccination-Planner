package com.ringvax.engine.followup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.ringvax.engine.graph.Contact;
import com.ringvax.engine.graph.ContactGraph;
import com.ringvax.engine.graph.Person;
import com.ringvax.engine.ring.RingBuilder;
import com.ringvax.engine.ring.RingResult;

class ContactDefinitionTest {

    private static final LocalDate REF = LocalDate.of(2026, 11, 2);

    @Test
    void shortContactsAreLeftOutAndUnknownDurationsKept() {
        ContactDefinition def = new ContactDefinition(15, null, null);

        assertEquals(ContactDefinition.Verdict.TOO_SHORT, def.verdict(new Contact(0, 1, REF, 5)));
        assertTrue(def.test(new Contact(0, 1, REF, 15)), "exactly the minimum counts");
        assertTrue(def.test(new Contact(0, 1, REF, -1)), "unknown duration is kept to be safe");
    }

    @Test
    void windowLeavesOutOldAndLaterContacts() {
        ContactDefinition def = new ContactDefinition(null, 14, REF);

        assertTrue(def.test(new Contact(0, 1, REF.minusDays(14), 30)), "the first day of the window counts");
        assertEquals(ContactDefinition.Verdict.BEFORE_WINDOW, def.verdict(new Contact(0, 1, REF.minusDays(15), 30)));
        assertEquals(ContactDefinition.Verdict.AFTER_REFERENCE, def.verdict(new Contact(0, 1, REF.plusDays(1), 30)));
        assertTrue(def.test(new Contact(0, 1, null, 30)), "unknown date is kept");
    }

    @Test
    void noSettingsKeepsEverything() {
        assertFalse(ContactDefinition.ALL.isActive());
        assertTrue(ContactDefinition.ALL.test(new Contact(0, 1, LocalDate.of(2000, 1, 1), 1)));
    }

    @Test
    void invalidSettingsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ContactDefinition(-1, null, null));
        assertThrows(IllegalArgumentException.class, () -> new ContactDefinition(null, 0, REF));
        assertThrows(IllegalArgumentException.class, () -> new ContactDefinition(null, 14, null));
    }

    @Test
    void summaryCountsEveryReasonAndTheRingsShrink() {
        ContactGraph g = new ContactGraph();
        for (int i = 0; i < 5; i++) {
            g.addPerson(new Person("P" + i, "", null, "", false));
        }
        g.addContact(0, 1, REF, 60);              // kept
        g.addContact(0, 2, REF, 4);               // too short
        g.addContact(0, 3, REF.minusDays(30), 60); // too old
        g.addContact(0, 4, null, -1);             // unknown, kept
        ContactDefinition def = new ContactDefinition(15, 14, REF);

        ContactDefinition.Summary s = def.summarize(g);
        RingResult ring = new RingBuilder(g, def).build(java.util.List.of("P0"), 1);

        assertEquals(4, s.total());
        assertEquals(2, s.kept());
        assertEquals(1, s.tooShort());
        assertEquals(1, s.beforeWindow());
        assertEquals(1, s.keptUnknownDuration());
        assertEquals(1, s.keptUnknownDate());
        assertEquals(2, s.excluded());
        assertEquals(2, ring.ring(1).length, "only P1 and P4 are ring 1");
    }
}
