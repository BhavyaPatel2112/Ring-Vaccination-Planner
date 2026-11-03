package com.ringvax.data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ringvax.engine.graph.Contact;
import com.ringvax.engine.graph.ContactGraph;
import com.ringvax.engine.graph.UnknownPersonException;
import com.ringvax.engine.select.RandomizedQuickselect;

/**
 * Flags contact tracing data that is probably incomplete or wrong, so the team
 * can check it before trusting the plan.
 *
 * <ul>
 *   <li>{@code CASE_WITHOUT_CONTACTS}: a confirmed case with no recorded
 *       contacts. Real cases almost always have contacts, so this usually means
 *       the tracing interview is unfinished.</li>
 *   <li>{@code UNUSUALLY_MANY_CONTACTS}: more contacts than the 99th percentile
 *       and more than 3 times the median. This is a likely data entry error or
 *       a possible super-spreader setting. Both percentiles come from randomized
 *       Quickselect in expected O(V) time, instead of sorting all degrees.</li>
 *   <li>{@code CONTACT_IN_FUTURE}: a contact dated after the check date.</li>
 *   <li>{@code CONTACT_AFTER_CONFIRMATION}: a contact with a case dated after the
 *       case was confirmed, when the case should already be isolating.</li>
 *   <li>{@code MISSING_CONTACT_DETAILS}: one summary of how many contacts have no
 *       date or no duration, since the contact definition settings cannot judge them.</li>
 * </ul>
 * At most {@code maxPerCode} warnings of each kind are listed, plus a summary of the rest.
 *
 * <p>Time complexity: O(V + E) expected. Space complexity: O(V).
 */
public class ContactQualityCheck {

    public static final double HIGH_PERCENTILE = 99;
    public static final int MEDIAN_MULTIPLE = 3;

    private final int maxPerCode;
    private final long seed;

    public ContactQualityCheck(long seed) {
        this(seed, 50);
    }

    public ContactQualityCheck(long seed, int maxPerCode) {
        if (maxPerCode < 1) {
            throw new IllegalArgumentException("maxPerCode must be at least 1");
        }
        this.seed = seed;
        this.maxPerCode = maxPerCode;
    }

    /**
     * Runs every check.
     *
     * @param asOf date the data is checked on; contacts after it are "in the future"
     * @throws UnknownPersonException if a case is not in the network
     */
    public List<QualityWarning> check(ContactGraph graph, List<ConfirmedCase> cases, LocalDate asOf) {
        Collector out = new Collector(maxPerCode);
        Map<Integer, LocalDate> confirmedOn = new HashMap<>();
        for (ConfirmedCase c : cases) {
            int index = graph.indexOf(c.personId());
            if (index < 0) {
                throw new UnknownPersonException(c.personId());
            }
            confirmedOn.merge(index, c.confirmedDate(), (x, y) -> x.isBefore(y) ? x : y);
            if (graph.degree(index) == 0) {
                out.add("CASE_WITHOUT_CONTACTS", c.personId(), "Confirmed case " + c.personId()
                        + " has no recorded contacts; contact tracing may be incomplete");
            }
        }

        int n = graph.personCount();
        if (n > 0) {
            RandomizedQuickselect quickselect = new RandomizedQuickselect(seed);
            int[] degrees = new int[n];
            for (int p = 0; p < n; p++) {
                degrees[p] = graph.degree(p);
            }
            int median = quickselect.select(degrees, (n - 1) / 2);
            int high = quickselect.percentile(degrees, HIGH_PERCENTILE);
            for (int p = 0; p < n; p++) {
                int degree = graph.degree(p);
                if (degree > high && degree > MEDIAN_MULTIPLE * median) {
                    String id = graph.person(p).id();
                    out.add("UNUSUALLY_MANY_CONTACTS", id, id + " has " + degree + " contacts, against a median of "
                            + median + " and a 99th percentile of " + high
                            + "; check for a data entry error or a crowded setting");
                }
            }
        }

        int missing = 0;
        for (Contact c : graph.contacts()) {
            if (c.date() == null || c.durationMinutes() < 0) {
                missing++;
            }
            if (c.date() == null) {
                continue;
            }
            String a = graph.person(c.a()).id();
            String b = graph.person(c.b()).id();
            if (c.date().isAfter(asOf)) {
                out.add("CONTACT_IN_FUTURE", a, "Contact " + a + " - " + b + " is dated " + c.date()
                        + ", after " + asOf);
            }
            for (int end : new int[] {c.a(), c.b()}) {
                LocalDate confirmed = confirmedOn.get(end);
                if (confirmed != null && c.date().isAfter(confirmed)) {
                    String caseId = graph.person(end).id();
                    String other = graph.person(c.other(end)).id();
                    out.add("CONTACT_AFTER_CONFIRMATION", caseId, "Case " + caseId + " met " + other + " on "
                            + c.date() + ", after being confirmed on " + confirmed + "; check isolation or the date");
                }
            }
        }
        if (missing > 0) {
            out.add("MISSING_CONTACT_DETAILS", null, missing + " of " + graph.contactCount()
                    + " contacts have no date or no duration, so the contact definition settings cannot judge them");
        }
        return out.finish();
    }

    /** Keeps at most {@code limit} warnings per code and counts the rest. */
    private static final class Collector {
        private final int limit;
        private final List<QualityWarning> listed = new ArrayList<>();
        private final Map<String, Integer> counts = new LinkedHashMap<>();

        Collector(int limit) {
            this.limit = limit;
        }

        void add(String code, String personId, String message) {
            int count = counts.merge(code, 1, Integer::sum);
            if (count <= limit) {
                listed.add(new QualityWarning(code, personId, message));
            }
        }

        List<QualityWarning> finish() {
            List<QualityWarning> result = new ArrayList<>(listed);
            counts.forEach((code, count) -> {
                if (count > limit) {
                    result.add(new QualityWarning(code, null, (count - limit) + " more " + code + " warnings not listed"));
                }
            });
            return result;
        }
    }
}
