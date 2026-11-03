package com.ringvax.data;

import java.io.Reader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.ringvax.engine.graph.ContactGraph;
import com.ringvax.engine.graph.Person;

/**
 * Reads the four input files (people, contacts, check-ins, cases) and validates
 * every row.
 *
 * <p>Rules (see PROJECT_PLAN.md section 5):
 * <ul>
 *   <li>Empty file, wrong header or no valid people: {@link CsvFormatException}.</li>
 *   <li>Self-contact, unknown person, malformed row or invalid value: the row is
 *       rejected and reported with its line number.</li>
 *   <li>Duplicate contact (A-B twice, or A-B and B-A): merged into one link that
 *       keeps the most recent date and the longest duration, and reported.</li>
 *   <li>Confirmed cases are never dropped: any invalid row or unknown id in
 *       cases.csv is an error, not a warning.</li>
 * </ul>
 *
 * <p>Column order does not matter and extra columns are ignored with a warning.
 * Blank {@code date} or {@code duration_minutes} values are allowed and mean
 * "unknown". To keep responses readable, at most {@code maxWarningsPerCode}
 * warnings of each kind are listed, followed by a count of the rest.
 *
 * <p>Time complexity: O(R + V + E) for R input rows (duplicate detection uses a
 * hash set of packed person-index pairs). Space complexity: O(V + E).
 */
public class CsvParser {

    public static final String PEOPLE_FILE = "people.csv";
    public static final String CONTACTS_FILE = "contacts.csv";
    public static final String CHECKINS_FILE = "checkins.csv";
    public static final String CASES_FILE = "cases.csv";

    static final List<String> PEOPLE_COLUMNS = List.of("person_id", "name", "age", "role", "vaccinated");
    static final List<String> PEOPLE_OPTIONAL_COLUMNS = List.of("eligible", "refused");
    static final List<String> CONTACT_COLUMNS = List.of("person_a", "person_b", "date", "duration_minutes");
    static final List<String> CHECKIN_COLUMNS = List.of("person_id", "venue_id", "start_time", "end_time");
    static final List<String> CASE_COLUMNS = List.of("person_id", "confirmed_date");

    private static final int MAX_AGE = 120;

    private final int maxWarningsPerCode;

    public CsvParser() {
        this(200);
    }

    public CsvParser(int maxWarningsPerCode) {
        if (maxWarningsPerCode < 1) {
            throw new IllegalArgumentException("maxWarningsPerCode must be at least 1");
        }
        this.maxWarningsPerCode = maxWarningsPerCode;
    }

    // ------------------------------------------------------------------ network

    /** Parses people.csv and contacts.csv into a contact network. */
    public ParseResult<ContactGraph> parseNetwork(Reader peopleCsv, Reader contactsCsv) {
        Warnings warnings = new Warnings(maxWarningsPerCode);
        ContactGraph graph = new ContactGraph();
        readPeople(peopleCsv, graph, warnings);
        readContacts(contactsCsv, graph, warnings);
        return new ParseResult<>(graph, warnings.finish());
    }

    private void readPeople(Reader source, ContactGraph graph, Warnings warnings) {
        CsvReader reader = new CsvReader(source);
        Header header = readHeader(reader, PEOPLE_FILE, PEOPLE_COLUMNS, PEOPLE_OPTIONAL_COLUMNS, warnings);
        Map<String, Integer> firstLineById = new HashMap<>();
        int rows = 0;

        for (CsvReader.Row row = reader.next(); row != null; row = reader.next()) {
            rows++;
            List<String> f = fields(row, header, PEOPLE_FILE, warnings);
            if (f == null) {
                continue;
            }
            String id = header.get(f, "person_id");
            if (id.isEmpty()) {
                warnings.add(PEOPLE_FILE, row.line(), "MISSING_VALUE", "person_id is empty; row skipped");
                continue;
            }
            Integer firstLine = firstLineById.get(id);
            if (firstLine != null) {
                warnings.add(PEOPLE_FILE, row.line(), "DUPLICATE_PERSON",
                        "person " + id + " already defined on line " + firstLine + "; row skipped");
                continue;
            }

            Integer age;
            String ageText = header.get(f, "age");
            if (ageText.isEmpty()) {
                age = null;
            } else {
                age = parseIntOrNull(ageText);
                if (age == null || age < 0 || age > MAX_AGE) {
                    warnings.add(PEOPLE_FILE, row.line(), "INVALID_VALUE",
                            "age '" + ageText + "' for " + id + " must be a whole number from 0 to "
                                    + MAX_AGE + "; row skipped");
                    continue;
                }
            }

            String vaccinatedText = header.get(f, "vaccinated");
            Boolean vaccinated;
            if (vaccinatedText.isEmpty()) {
                vaccinated = false;
                warnings.add(PEOPLE_FILE, row.line(), "ASSUMED_VALUE",
                        "vaccinated is empty for " + id + "; assumed not vaccinated");
            } else {
                vaccinated = parseBoolean(vaccinatedText);
                if (vaccinated == null) {
                    warnings.add(PEOPLE_FILE, row.line(), "INVALID_VALUE",
                            "vaccinated '" + vaccinatedText + "' for " + id
                                    + " must be true or false; row skipped");
                    continue;
                }
            }

            Boolean eligible = optionalBoolean(header, f, "eligible", true);
            Boolean refused = optionalBoolean(header, f, "refused", false);
            if (eligible == null || refused == null) {
                warnings.add(PEOPLE_FILE, row.line(), "INVALID_VALUE",
                        "eligible and refused for " + id + " must be true or false; row skipped");
                continue;
            }

            graph.addPerson(new Person(id, header.get(f, "name"), age, header.get(f, "role"),
                    vaccinated, eligible, refused));
            firstLineById.put(id, row.line());
        }

        if (rows == 0) {
            throw new CsvFormatException(new ValidationIssue(PEOPLE_FILE, 0, "NO_PEOPLE",
                    "people.csv has a header but no people"));
        }
        if (graph.personCount() == 0) {
            List<ValidationIssue> issues = new ArrayList<>();
            issues.add(new ValidationIssue(PEOPLE_FILE, 0, "NO_VALID_PEOPLE",
                    "none of the " + rows + " rows in people.csv are valid"));
            issues.addAll(warnings.finish());
            throw new CsvFormatException(issues);
        }
    }

    private void readContacts(Reader source, ContactGraph graph, Warnings warnings) {
        CsvReader reader = new CsvReader(source);
        Header header = readHeader(reader, CONTACTS_FILE, CONTACT_COLUMNS, List.of(), warnings);
        Map<Long, PendingContact> byPair = new LinkedHashMap<>();

        for (CsvReader.Row row = reader.next(); row != null; row = reader.next()) {
            List<String> f = fields(row, header, CONTACTS_FILE, warnings);
            if (f == null) {
                continue;
            }
            String idA = header.get(f, "person_a");
            String idB = header.get(f, "person_b");
            if (idA.isEmpty() || idB.isEmpty()) {
                warnings.add(CONTACTS_FILE, row.line(), "MISSING_VALUE",
                        "person_a and person_b are both required; row skipped");
                continue;
            }
            if (idA.equals(idB)) {
                warnings.add(CONTACTS_FILE, row.line(), "SELF_CONTACT",
                        "contact from " + idA + " to themselves; row skipped");
                continue;
            }
            int a = graph.indexOf(idA);
            int b = graph.indexOf(idB);
            if (a < 0 || b < 0) {
                String unknown = a < 0 && b < 0 ? idA + " and " + idB : (a < 0 ? idA : idB);
                warnings.add(CONTACTS_FILE, row.line(), "UNKNOWN_PERSON",
                        "unknown person " + unknown + " (not in people.csv, or that row was skipped); row skipped");
                continue;
            }

            String dateText = header.get(f, "date");
            LocalDate date = null;
            if (!dateText.isEmpty()) {
                date = parseDateOrNull(dateText);
                if (date == null) {
                    warnings.add(CONTACTS_FILE, row.line(), "INVALID_VALUE",
                            "date '" + dateText + "' must look like 2026-11-01; row skipped");
                    continue;
                }
            }
            String durationText = header.get(f, "duration_minutes");
            int duration = -1;
            if (!durationText.isEmpty()) {
                Integer parsed = parseIntOrNull(durationText);
                if (parsed == null || parsed < 0) {
                    warnings.add(CONTACTS_FILE, row.line(), "INVALID_VALUE",
                            "duration_minutes '" + durationText
                                    + "' must be a whole number of minutes, 0 or more; row skipped");
                    continue;
                }
                duration = parsed;
            }

            long key = pairKey(a, b);
            PendingContact existing = byPair.get(key);
            if (existing == null) {
                byPair.put(key, new PendingContact(a, b, date, duration, row.line()));
            } else {
                existing.merge(date, duration);
                warnings.add(CONTACTS_FILE, row.line(), "DUPLICATE_CONTACT",
                        "contact " + idA + " - " + idB + " already listed on line " + existing.firstLine
                                + "; merged into one link");
            }
        }

        for (PendingContact c : byPair.values()) {
            graph.addContact(c.a, c.b, c.date, c.duration);
        }
    }

    // ---------------------------------------------------------------- check-ins

    /** Parses checkins.csv. Every person must already exist in {@code graph}. */
    public ParseResult<List<CheckIn>> parseCheckIns(Reader checkinsCsv, ContactGraph graph) {
        Warnings warnings = new Warnings(maxWarningsPerCode);
        CsvReader reader = new CsvReader(checkinsCsv);
        Header header = readHeader(reader, CHECKINS_FILE, CHECKIN_COLUMNS, List.of(), warnings);
        List<CheckIn> result = new ArrayList<>();
        Map<CheckIn, Integer> firstLine = new HashMap<>();

        for (CsvReader.Row row = reader.next(); row != null; row = reader.next()) {
            List<String> f = fields(row, header, CHECKINS_FILE, warnings);
            if (f == null) {
                continue;
            }
            String personId = header.get(f, "person_id");
            String venueId = header.get(f, "venue_id");
            if (personId.isEmpty() || venueId.isEmpty()) {
                warnings.add(CHECKINS_FILE, row.line(), "MISSING_VALUE",
                        "person_id and venue_id are both required; row skipped");
                continue;
            }
            if (!graph.containsPerson(personId)) {
                warnings.add(CHECKINS_FILE, row.line(), "UNKNOWN_PERSON",
                        "unknown person " + personId + "; row skipped");
                continue;
            }
            LocalDateTime start = parseDateTimeOrNull(header.get(f, "start_time"));
            LocalDateTime end = parseDateTimeOrNull(header.get(f, "end_time"));
            if (start == null || end == null) {
                warnings.add(CHECKINS_FILE, row.line(), "INVALID_VALUE",
                        "start_time and end_time must look like 2026-11-01T09:00; row skipped");
                continue;
            }
            if (!end.isAfter(start)) {
                warnings.add(CHECKINS_FILE, row.line(), "INVALID_VALUE",
                        "end_time is not after start_time; row skipped");
                continue;
            }
            CheckIn checkIn = new CheckIn(personId, venueId, start, end);
            Integer seen = firstLine.putIfAbsent(checkIn, row.line());
            if (seen != null) {
                warnings.add(CHECKINS_FILE, row.line(), "DUPLICATE_CHECKIN",
                        "same visit already listed on line " + seen + "; merged");
                continue;
            }
            result.add(checkIn);
        }
        return new ParseResult<>(result, warnings.finish());
    }

    // -------------------------------------------------------------------- cases

    /**
     * Parses cases.csv. Unlike the other files, any bad row or unknown person is
     * an error, because silently dropping a confirmed case would make the ring
     * and the plan wrong. Duplicates keep the earliest confirmation date.
     */
    public ParseResult<List<ConfirmedCase>> parseCases(Reader casesCsv, ContactGraph graph) {
        Warnings warnings = new Warnings(maxWarningsPerCode);
        CsvReader reader = new CsvReader(casesCsv);
        Header header = readHeader(reader, CASES_FILE, CASE_COLUMNS, List.of(), warnings);
        Map<String, ConfirmedCase> byId = new LinkedHashMap<>();
        Map<String, Integer> firstLine = new HashMap<>();
        List<ValidationIssue> errors = new ArrayList<>();

        for (CsvReader.Row row = reader.next(); row != null; row = reader.next()) {
            List<String> f;
            try {
                f = header.fieldsOf(row);
            } catch (IllegalArgumentException e) {
                errors.add(new ValidationIssue(CASES_FILE, row.line(), "MALFORMED_ROW", e.getMessage()));
                continue;
            }
            String id = header.get(f, "person_id");
            String dateText = header.get(f, "confirmed_date");
            if (id.isEmpty()) {
                errors.add(new ValidationIssue(CASES_FILE, row.line(), "MISSING_VALUE", "person_id is empty"));
                continue;
            }
            if (!graph.containsPerson(id)) {
                errors.add(new ValidationIssue(CASES_FILE, row.line(), "UNKNOWN_CASE",
                        "confirmed case " + id + " is not in people.csv"));
                continue;
            }
            LocalDate date = parseDateOrNull(dateText);
            if (date == null) {
                errors.add(new ValidationIssue(CASES_FILE, row.line(), "INVALID_VALUE",
                        "confirmed_date '" + dateText + "' for " + id + " must look like 2026-11-02"));
                continue;
            }
            ConfirmedCase existing = byId.get(id);
            if (existing != null) {
                warnings.add(CASES_FILE, row.line(), "DUPLICATE_CASE",
                        "case " + id + " already listed on line " + firstLine.get(id)
                                + "; kept the earlier confirmation date");
                if (date.isBefore(existing.confirmedDate())) {
                    byId.put(id, new ConfirmedCase(id, date));
                }
                continue;
            }
            byId.put(id, new ConfirmedCase(id, date));
            firstLine.put(id, row.line());
        }

        if (!errors.isEmpty()) {
            throw new CsvFormatException(errors);
        }
        return new ParseResult<>(new ArrayList<>(byId.values()), warnings.finish());
    }

    // ------------------------------------------------------------------ helpers

    private static Header readHeader(CsvReader reader, String file, List<String> required,
                                     List<String> optional, Warnings warnings) {
        CsvReader.Row first = reader.next();
        if (first == null) {
            throw new CsvFormatException(new ValidationIssue(file, 0, "EMPTY_FILE",
                    file + " is empty; expected the header " + String.join(",", required)));
        }
        List<String> names;
        try {
            names = CsvReader.split(first.raw());
        } catch (IllegalArgumentException e) {
            throw new CsvFormatException(new ValidationIssue(file, first.line(), "BAD_HEADER",
                    "header could not be read (" + e.getMessage() + ")"));
        }
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i).toLowerCase(Locale.ROOT);
            if (index.putIfAbsent(name, i) != null) {
                throw new CsvFormatException(new ValidationIssue(file, first.line(), "BAD_HEADER",
                        "column '" + name + "' appears twice in the header"));
            }
        }
        List<String> missing = new ArrayList<>();
        for (String column : required) {
            if (!index.containsKey(column)) {
                missing.add(column);
            }
        }
        if (!missing.isEmpty()) {
            throw new CsvFormatException(new ValidationIssue(file, first.line(), "BAD_HEADER",
                    "missing column(s) " + String.join(", ", missing) + "; expected the header "
                            + String.join(",", required)));
        }
        for (String name : index.keySet().stream().sorted().toList()) {
            if (!required.contains(name) && !optional.contains(name)) {
                warnings.add(file, first.line(), "UNKNOWN_COLUMN", "column '" + name + "' is not used and was ignored");
            }
        }
        return new Header(index, names.size());
    }

    private static List<String> fields(CsvReader.Row row, Header header, String file, Warnings warnings) {
        try {
            return header.fieldsOf(row);
        } catch (IllegalArgumentException e) {
            warnings.add(file, row.line(), "MALFORMED_ROW", e.getMessage() + "; row skipped");
            return null;
        }
    }

    private static Boolean optionalBoolean(Header header, List<String> f, String column, boolean defaultValue) {
        if (!header.has(column)) {
            return defaultValue;
        }
        String text = header.get(f, column);
        return text.isEmpty() ? Boolean.valueOf(defaultValue) : parseBoolean(text);
    }

    static Boolean parseBoolean(String text) {
        switch (text.toLowerCase(Locale.ROOT)) {
            case "true", "yes", "y", "1":
                return Boolean.TRUE;
            case "false", "no", "n", "0":
                return Boolean.FALSE;
            default:
                return null;
        }
    }

    private static Integer parseIntOrNull(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static LocalDate parseDateOrNull(String text) {
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static LocalDateTime parseDateTimeOrNull(String text) {
        try {
            return LocalDateTime.parse(text.replace(' ', 'T'));
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** Packs an unordered pair of person indices into one long. */
    private static long pairKey(int a, int b) {
        int lo = Math.min(a, b);
        int hi = Math.max(a, b);
        return ((long) lo << 32) | (hi & 0xffffffffL);
    }

    /** Column positions taken from the header row. */
    private record Header(Map<String, Integer> index, int width) {

        boolean has(String column) {
            return index.containsKey(column);
        }

        String get(List<String> fields, String column) {
            Integer i = index.get(column);
            return i == null || i >= fields.size() ? "" : fields.get(i);
        }

        List<String> fieldsOf(CsvReader.Row row) {
            List<String> fields = CsvReader.split(row.raw());
            if (fields.size() < width) {
                throw new IllegalArgumentException("expected " + width + " values but found " + fields.size());
            }
            if (fields.size() > width) {
                throw new IllegalArgumentException("expected " + width + " values but found " + fields.size()
                        + " (put values containing commas in double quotes)");
            }
            return fields;
        }
    }

    /** A contact waiting to be added once all duplicates have been merged. */
    private static final class PendingContact {
        final int a;
        final int b;
        final int firstLine;
        LocalDate date;
        int duration;

        PendingContact(int a, int b, LocalDate date, int duration, int firstLine) {
            this.a = a;
            this.b = b;
            this.date = date;
            this.duration = duration;
            this.firstLine = firstLine;
        }

        void merge(LocalDate otherDate, int otherDuration) {
            if (otherDate != null && (date == null || otherDate.isAfter(date))) {
                date = otherDate;
            }
            duration = Math.max(duration, otherDuration);
        }
    }

    /** Collects warnings, listing at most {@code limit} per file and code. */
    private static final class Warnings {
        private final int limit;
        private final List<ValidationIssue> listed = new ArrayList<>();
        private final Map<List<String>, Integer> counts = new LinkedHashMap<>();

        Warnings(int limit) {
            this.limit = limit;
        }

        void add(String file, int line, String code, String message) {
            int count = counts.merge(List.of(file, code), 1, Integer::sum);
            if (count <= limit) {
                listed.add(new ValidationIssue(file, line, code, message));
            }
        }

        List<ValidationIssue> finish() {
            List<ValidationIssue> result = new ArrayList<>(listed);
            for (Map.Entry<List<String>, Integer> e : counts.entrySet()) {
                int hidden = e.getValue() - limit;
                if (hidden > 0) {
                    String file = e.getKey().get(0);
                    String code = e.getKey().get(1);
                    result.add(new ValidationIssue(file, 0, code, hidden + " more " + code + " warnings not listed"));
                }
            }
            return result;
        }
    }
}
