package com.ringvax.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.ringvax.engine.graph.ContactGraph;

/**
 * Makes sure every sample network in data/sample still parses the way the
 * README in that folder describes. Tests run from the backend folder.
 */
class SampleDataTest {

    private static final Path SAMPLES = Path.of("..", "data", "sample");

    private final CsvParser parser = new CsvParser();

    private static Reader open(String folder, String file) {
        try {
            return Files.newBufferedReader(SAMPLES.resolve(folder).resolve(file));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private ParseResult<ContactGraph> network(String folder) {
        return parser.parseNetwork(open(folder, "people.csv"), open(folder, "contacts.csv"));
    }

    private static long countCode(List<ValidationIssue> issues, String code) {
        return issues.stream().filter(i -> i.code().equals(code)).count();
    }

    @ParameterizedTest
    @CsvSource({
            "outbreak-small, 40, 73",
            "star, 8, 7",
            "complete-bipartite-3-3, 6, 9",
            "greedy-worst-case-12, 35, 115",
            "greedy-worst-case-18, 58, 259",
    })
    void cleanSamplesParseWithoutWarnings(String folder, int people, int contacts) {
        ParseResult<ContactGraph> result = network(folder);
        assertTrue(result.warnings().isEmpty(), result.warnings().toString());
        assertEquals(people, result.value().personCount());
        assertEquals(contacts, result.value().contactCount());
    }

    @Test
    void outbreakSampleHasCasesCheckInsAndSpecialPeople() {
        ContactGraph g = network("outbreak-small").value();

        ParseResult<List<ConfirmedCase>> cases = parser.parseCases(open("outbreak-small", "cases.csv"), g);
        assertEquals(List.of("P003", "P023"), cases.value().stream().map(ConfirmedCase::personId).toList());

        ParseResult<List<CheckIn>> checkIns = parser.parseCheckIns(open("outbreak-small", "checkins.csv"), g);
        assertTrue(checkIns.warnings().isEmpty(), checkIns.warnings().toString());
        assertEquals(13, checkIns.value().size());

        assertFalse(g.person(g.indexOf("P004")).eligible(), "infant cannot take a live vaccine");
        assertTrue(g.person(g.indexOf("P011")).refused());
        assertTrue(g.person(g.indexOf("P005")).vaccinated());
    }

    @Test
    void validationDemoReportsEveryProblem() {
        ParseResult<ContactGraph> result = network("validation-demo");
        ContactGraph g = result.value();
        List<ValidationIssue> w = result.warnings();

        assertEquals(5, g.personCount());
        assertEquals(2, g.contactCount());
        assertEquals(1, countCode(w, "DUPLICATE_PERSON"));
        assertEquals(1, countCode(w, "ASSUMED_VALUE"));
        assertEquals(4, countCode(w, "INVALID_VALUE"), "age 140, vaccinated 'maybe', bad date, negative duration");
        assertEquals(2, countCode(w, "DUPLICATE_CONTACT"));
        assertEquals(1, countCode(w, "SELF_CONTACT"));
        assertEquals(1, countCode(w, "UNKNOWN_PERSON"));
        assertEquals(1, countCode(w, "MALFORMED_ROW"));
        assertEquals("Shah, Dev", g.person(g.indexOf("P007")).name());

        CsvFormatException e = assertThrows(CsvFormatException.class,
                () -> parser.parseCases(open("validation-demo", "cases-unknown-id.csv"), g));
        assertEquals("UNKNOWN_CASE", e.issues().get(0).code());
        assertTrue(e.getMessage().contains("P404"));
    }
}
