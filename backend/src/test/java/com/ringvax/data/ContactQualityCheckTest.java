package com.ringvax.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.ringvax.engine.graph.ContactGraph;
import com.ringvax.engine.graph.Person;
import com.ringvax.engine.graph.UnknownPersonException;

class ContactQualityCheckTest {

    private static final LocalDate NOV_1 = LocalDate.of(2026, 11, 1);
    private static final LocalDate NOV_3 = LocalDate.of(2026, 11, 3);

    private final ContactQualityCheck check = new ContactQualityCheck(5);

    private static ContactGraph people(int n) {
        ContactGraph g = new ContactGraph();
        for (int i = 0; i < n; i++) {
            g.addPerson(new Person("P" + i, "", 30, "", false));
        }
        return g;
    }

    private static long count(List<QualityWarning> warnings, String code) {
        return warnings.stream().filter(w -> w.code().equals(code)).count();
    }

    @Test
    void emptyNetworkHasNoWarnings() {
        assertTrue(check.check(new ContactGraph(), List.of(), NOV_3).isEmpty());
    }

    @Test
    void caseWithoutContactsIsFlagged() {
        ContactGraph g = people(3);
        g.addContact(1, 2, NOV_1, 30);
        List<QualityWarning> warnings = check.check(g, List.of(new ConfirmedCase("P0", NOV_1)), NOV_3);

        assertEquals(1, warnings.size());
        assertEquals("CASE_WITHOUT_CONTACTS", warnings.get(0).code());
        assertEquals("P0", warnings.get(0).personId());
    }

    @Test
    void personWithFarMoreContactsThanEveryoneElseIsFlagged() {
        // A ring of 300 people (2 contacts each) plus P0 who met 80 of them.
        int n = 301;
        ContactGraph g = people(n);
        for (int i = 1; i < n; i++) {
            g.addContact(i, i == n - 1 ? 1 : i + 1, NOV_1, 30);
        }
        for (int i = 1; i <= 80; i++) {
            g.addContact(0, i * 3, NOV_1, 30);
        }
        List<QualityWarning> warnings = check.check(g, List.of(), NOV_3);

        assertEquals(1, count(warnings, "UNUSUALLY_MANY_CONTACTS"));
        assertEquals("P0", warnings.get(0).personId());
        assertTrue(warnings.get(0).message().contains("80 contacts"), warnings.get(0).message());
    }

    @Test
    void evenlyConnectedNetworkHasNoOutliers() {
        ContactGraph g = people(200);
        for (int i = 0; i < 200; i++) {
            g.addContact(i, (i + 1) % 200, NOV_1, 30);
            g.addContact(i, (i + 7) % 200, NOV_1, 30);
        }
        assertEquals(0, count(check.check(g, List.of(), NOV_3), "UNUSUALLY_MANY_CONTACTS"));
    }

    @Test
    void futureContactsAndContactsAfterConfirmationAreFlagged() {
        ContactGraph g = people(4);
        g.addContact(0, 1, LocalDate.of(2026, 11, 2), 30);  // after P0 was confirmed on 1 Nov
        g.addContact(2, 3, LocalDate.of(2026, 11, 9), 30);  // in the future
        g.addContact(0, 2, LocalDate.of(2026, 10, 30), 30); // fine
        List<QualityWarning> warnings = check.check(g, List.of(new ConfirmedCase("P0", NOV_1)), NOV_3);

        assertEquals(1, count(warnings, "CONTACT_AFTER_CONFIRMATION"));
        assertEquals(1, count(warnings, "CONTACT_IN_FUTURE"));
        assertEquals(2, warnings.size());
    }

    @Test
    void missingDetailsAreSummarisedOnce() {
        ContactGraph g = people(4);
        g.addContact(0, 1, null, 30);
        g.addContact(1, 2, NOV_1, -1);
        g.addContact(2, 3, NOV_1, 30);
        List<QualityWarning> warnings = check.check(g, List.of(), NOV_3);

        assertEquals(1, warnings.size());
        assertEquals("MISSING_CONTACT_DETAILS", warnings.get(0).code());
        assertTrue(warnings.get(0).message().startsWith("2 of 3"));
    }

    @Test
    void manyWarningsOfOneKindAreCapped() {
        ContactGraph g = people(20);
        for (int i = 0; i < 19; i++) {
            g.addContact(i, i + 1, LocalDate.of(2027, 1, 1), 30);
        }
        List<QualityWarning> warnings = new ContactQualityCheck(5, 4).check(g, List.of(), NOV_3);

        assertEquals(5, warnings.size());
        assertTrue(warnings.get(4).message().startsWith("15 more"));
    }

    @Test
    void unknownCaseIsRejected() {
        assertThrows(UnknownPersonException.class,
                () -> check.check(people(2), List.of(new ConfirmedCase("P9", NOV_1)), NOV_3));
    }

    @Test
    void sampleOutbreakIsClean() throws IOException {
        Path dir = Path.of("..", "data", "sample", "outbreak-small");
        CsvParser parser = new CsvParser();
        ContactGraph g = parser.parseNetwork(Files.newBufferedReader(dir.resolve("people.csv")),
                Files.newBufferedReader(dir.resolve("contacts.csv"))).value();
        List<ConfirmedCase> cases = parser.parseCases(Files.newBufferedReader(dir.resolve("cases.csv")), g).value();

        assertTrue(check.check(g, cases, NOV_3).isEmpty());
    }
}
