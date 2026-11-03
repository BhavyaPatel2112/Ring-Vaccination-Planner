package com.ringvax.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.ringvax.engine.graph.Contact;
import com.ringvax.engine.graph.ContactGraph;
import com.ringvax.engine.graph.Person;

class CsvParserTest {

    private static final String PEOPLE = """
            person_id,name,age,role,vaccinated
            P001,Asha,34,teacher,false
            P002,Ravi,41,driver,true
            P003,Meera,9,student,false
            P004,Kabir,67,retired,false
            """;

    private final CsvParser parser = new CsvParser();

    private ParseResult<ContactGraph> network(String people, String contacts) {
        return parser.parseNetwork(new StringReader(people), new StringReader(contacts));
    }

    private static long countCode(List<ValidationIssue> issues, String code) {
        return issues.stream().filter(i -> i.code().equals(code)).count();
    }

    @Nested
    class Network {

        @Test
        void validFilesBuildTheGraphWithoutWarnings() {
            ParseResult<ContactGraph> result = network(PEOPLE, """
                    person_a,person_b,date,duration_minutes
                    P001,P002,2026-11-01,45
                    P002,P003,2026-10-30,120
                    """);

            ContactGraph g = result.value();
            assertTrue(result.warnings().isEmpty(), result.warnings().toString());
            assertEquals(4, g.personCount());
            assertEquals(2, g.contactCount());
            assertTrue(g.hasContact(g.indexOf("P001"), g.indexOf("P002")));
            Person ravi = g.person(g.indexOf("P002"));
            assertEquals("Ravi", ravi.name());
            assertEquals(41, ravi.age());
            assertTrue(ravi.vaccinated());
            assertTrue(ravi.eligible());
            assertFalse(ravi.refused());
            Contact c = g.contact(1);
            assertEquals(LocalDate.of(2026, 10, 30), c.date());
            assertEquals(120, c.durationMinutes());
        }

        @Test
        void emptyPeopleFileIsAnError() {
            CsvFormatException e = assertThrows(CsvFormatException.class,
                    () -> network("", "person_a,person_b,date,duration_minutes\n"));
            assertEquals("EMPTY_FILE", e.issues().get(0).code());
        }

        @Test
        void headerOnlyPeopleFileIsAnError() {
            CsvFormatException e = assertThrows(CsvFormatException.class,
                    () -> network("person_id,name,age,role,vaccinated\n\n",
                            "person_a,person_b,date,duration_minutes\n"));
            assertEquals("NO_PEOPLE", e.issues().get(0).code());
        }

        @Test
        void emptyContactsFileIsAnErrorButHeaderOnlyIsFine() {
            CsvFormatException e = assertThrows(CsvFormatException.class, () -> network(PEOPLE, "   \n"));
            assertEquals("EMPTY_FILE", e.issues().get(0).code());
            assertEquals(CsvParser.CONTACTS_FILE, e.issues().get(0).file());

            ParseResult<ContactGraph> noContacts = network(PEOPLE, "person_a,person_b,date,duration_minutes\n");
            assertEquals(4, noContacts.value().personCount());
            assertEquals(0, noContacts.value().contactCount());
        }

        @Test
        void missingHeaderColumnIsAnErrorThatNamesTheColumn() {
            CsvFormatException e = assertThrows(CsvFormatException.class,
                    () -> network(PEOPLE, "person_a,person_b,duration_minutes\nP001,P002,10\n"));
            ValidationIssue issue = e.issues().get(0);
            assertEquals("BAD_HEADER", issue.code());
            assertTrue(issue.message().contains("date"), issue.message());
        }

        @Test
        void duplicateContactsInEitherDirectionAreMergedAndReported() {
            ParseResult<ContactGraph> result = network(PEOPLE, """
                    person_a,person_b,date,duration_minutes
                    P001,P002,2026-10-28,30
                    P002,P001,2026-11-01,10
                    P001,P002,2026-10-20,90
                    """);

            ContactGraph g = result.value();
            assertEquals(1, g.contactCount());
            Contact merged = g.contact(0);
            assertEquals(LocalDate.of(2026, 11, 1), merged.date(), "keeps the most recent date");
            assertEquals(90, merged.durationMinutes(), "keeps the longest duration");
            assertEquals(2, countCode(result.warnings(), "DUPLICATE_CONTACT"));
            assertEquals(3, result.warnings().get(0).line());
            assertTrue(result.warnings().get(0).message().contains("line 2"));
        }

        @Test
        void selfContactIsRejectedWithItsLineNumber() {
            ParseResult<ContactGraph> result = network(PEOPLE, """
                    person_a,person_b,date,duration_minutes
                    P001,P002,2026-11-01,45
                    P003,P003,2026-11-01,45
                    """);

            assertEquals(1, result.value().contactCount());
            ValidationIssue issue = result.warnings().get(0);
            assertEquals("SELF_CONTACT", issue.code());
            assertEquals(3, issue.line());
        }

        @Test
        void unknownPersonIsRejectedWithItsLineNumberAndId() {
            ParseResult<ContactGraph> result = network(PEOPLE, """
                    person_a,person_b,date,duration_minutes

                    P001,P999,2026-11-01,45
                    P001,P002,2026-11-01,45
                    """);

            assertEquals(1, result.value().contactCount());
            ValidationIssue issue = result.warnings().get(0);
            assertEquals("UNKNOWN_PERSON", issue.code());
            assertEquals(3, issue.line(), "blank lines still count towards line numbers");
            assertTrue(issue.message().contains("P999"));
        }

        @Test
        void invalidValuesRejectTheRow() {
            ParseResult<ContactGraph> result = network(PEOPLE, """
                    person_a,person_b,date,duration_minutes
                    P001,P002,01/11/2026,45
                    P001,P003,2026-11-01,-5
                    P001,P004,2026-11-01,ten
                    P002,P003,2026-11-01
                    P003,P004,,
                    """);

            ContactGraph g = result.value();
            assertEquals(1, g.contactCount(), "only the row with blank date and duration is kept");
            assertNull(g.contact(0).date());
            assertEquals(-1, g.contact(0).durationMinutes());
            assertEquals(3, countCode(result.warnings(), "INVALID_VALUE"));
            assertEquals(1, countCode(result.warnings(), "MALFORMED_ROW"));
        }

        @Test
        void duplicatePeopleAndBadPeopleRowsAreReported() {
            ParseResult<ContactGraph> result = network("""
                    person_id,name,age,role,vaccinated
                    P001,Asha,34,teacher,false
                    P001,Asha again,35,teacher,false
                    P002,Ravi,two hundred,driver,false
                    P003,Meera,9,student,maybe
                    P004,Kabir,,retired,
                    """, "person_a,person_b,date,duration_minutes\n");

            ContactGraph g = result.value();
            assertEquals(2, g.personCount());
            assertEquals("Asha", g.person(0).name(), "first definition wins");
            assertNull(g.person(g.indexOf("P004")).age(), "blank age means unknown");
            assertFalse(g.person(g.indexOf("P004")).vaccinated());
            assertEquals(1, countCode(result.warnings(), "DUPLICATE_PERSON"));
            assertEquals(2, countCode(result.warnings(), "INVALID_VALUE"));
            assertEquals(1, countCode(result.warnings(), "ASSUMED_VALUE"));
        }

        @Test
        void noValidPeopleIsAnErrorThatListsTheReasons() {
            CsvFormatException e = assertThrows(CsvFormatException.class, () -> network("""
                    person_id,name,age,role,vaccinated
                    P001,Asha,-3,teacher,false
                    """, "person_a,person_b,date,duration_minutes\n"));
            assertEquals("NO_VALID_PEOPLE", e.issues().get(0).code());
            assertEquals("INVALID_VALUE", e.issues().get(1).code());
        }

        @Test
        void optionalEligibilityColumnsAndQuotedNames() {
            ParseResult<ContactGraph> result = network("""
                    ﻿vaccinated,person_id,name,age,role,eligible,refused,ward
                    false,P001,"Shah, Asha",34,teacher,yes,no,4
                    false,P002,Baby Anya,0,infant,false,,
                    false,P003,Dev,29,nurse,,true,4
                    """, "person_a,person_b,date,duration_minutes\n");

            ContactGraph g = result.value();
            assertEquals("Shah, Asha", g.person(0).name());
            assertTrue(g.person(0).canBeVaccinated());
            assertFalse(g.person(1).eligible());
            assertFalse(g.person(1).canBeVaccinated());
            assertTrue(g.person(2).eligible(), "blank eligible defaults to true");
            assertTrue(g.person(2).refused());
            assertEquals(1, countCode(result.warnings(), "UNKNOWN_COLUMN"), "the extra 'ward' column");
        }

        @Test
        void warningsAreCappedPerCode() {
            StringBuilder contacts = new StringBuilder("person_a,person_b,date,duration_minutes\n");
            for (int i = 0; i < 10; i++) {
                contacts.append("P001,P001,2026-11-01,5\n");
            }
            ParseResult<ContactGraph> result = new CsvParser(3)
                    .parseNetwork(new StringReader(PEOPLE), new StringReader(contacts.toString()));

            assertEquals(4, result.warnings().size());
            ValidationIssue summary = result.warnings().get(3);
            assertEquals(0, summary.line());
            assertTrue(summary.message().startsWith("7 more"), summary.message());
        }
    }

    @Nested
    class CheckIns {

        private final ContactGraph graph = network(PEOPLE, "person_a,person_b,date,duration_minutes\n").value();

        private ParseResult<List<CheckIn>> parse(String csv) {
            return parser.parseCheckIns(new StringReader(csv), graph);
        }

        @Test
        void validCheckInsAreParsed() {
            ParseResult<List<CheckIn>> result = parse("""
                    person_id,venue_id,start_time,end_time
                    P001,V010,2026-11-01T09:00,2026-11-01T11:30
                    P002,V010,2026-11-01 10:15,2026-11-01 10:45
                    """);
            assertTrue(result.warnings().isEmpty(), result.warnings().toString());
            assertEquals(2, result.value().size());
            CheckIn first = result.value().get(0);
            assertEquals("V010", first.venueId());
            assertEquals(LocalDateTime.of(2026, 11, 1, 11, 30), first.end());
        }

        @Test
        void badCheckInsAreRejectedAndDuplicatesMerged() {
            ParseResult<List<CheckIn>> result = parse("""
                    person_id,venue_id,start_time,end_time
                    P001,V010,2026-11-01T09:00,2026-11-01T11:30
                    P001,V010,2026-11-01T09:00,2026-11-01T11:30
                    P009,V010,2026-11-01T09:00,2026-11-01T11:30
                    P002,V011,2026-11-01T12:00,2026-11-01T11:00
                    P003,,2026-11-01T09:00,2026-11-01T10:00
                    P004,V012,yesterday,today
                    """);
            assertEquals(1, result.value().size());
            assertEquals(1, countCode(result.warnings(), "DUPLICATE_CHECKIN"));
            assertEquals(1, countCode(result.warnings(), "UNKNOWN_PERSON"));
            assertEquals(2, countCode(result.warnings(), "INVALID_VALUE"));
            assertEquals(1, countCode(result.warnings(), "MISSING_VALUE"));
        }
    }

    @Nested
    class Cases {

        private final ContactGraph graph = network(PEOPLE, "person_a,person_b,date,duration_minutes\n").value();

        private ParseResult<List<ConfirmedCase>> parse(String csv) {
            return parser.parseCases(new StringReader(csv), graph);
        }

        @Test
        void validCasesAreParsedAndDuplicatesKeepTheEarliestDate() {
            ParseResult<List<ConfirmedCase>> result = parse("""
                    person_id,confirmed_date
                    P002,2026-11-02
                    P003,2026-11-01
                    P002,2026-10-31
                    """);
            assertEquals(2, result.value().size());
            assertEquals(LocalDate.of(2026, 10, 31), result.value().get(0).confirmedDate());
            assertEquals(1, countCode(result.warnings(), "DUPLICATE_CASE"));
        }

        @Test
        void headerOnlyMeansNoCasesYet() {
            assertTrue(parse("person_id,confirmed_date\n").value().isEmpty());
        }

        @Test
        void unknownCaseIsAnErrorNamingTheIdentifier() {
            CsvFormatException e = assertThrows(CsvFormatException.class, () -> parse("""
                    person_id,confirmed_date
                    P002,2026-11-02
                    P404,2026-11-02
                    P003,02-11-2026
                    """));
            assertEquals(2, e.issues().size());
            assertEquals("UNKNOWN_CASE", e.issues().get(0).code());
            assertTrue(e.getMessage().contains("P404"), e.getMessage());
            assertEquals("INVALID_VALUE", e.issues().get(1).code());
        }
    }

    @Test
    void csvSplitHandlesQuotesAndEmptyFields() {
        assertEquals(List.of("a", "b, c", "", "say \"hi\""), CsvReader.split("a,\"b, c\",,\"say \"\"hi\"\"\""));
        assertThrows(IllegalArgumentException.class, () -> CsvReader.split("a,\"open"));
    }
}
