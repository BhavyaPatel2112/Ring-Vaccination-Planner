package com.ringvax.engine.followup;

import java.time.LocalDate;
import java.util.function.Predicate;

import com.ringvax.engine.graph.Contact;
import com.ringvax.engine.graph.ContactGraph;

/**
 * Which contacts count as risky (field feature E): at least a minimum duration,
 * and within a window of days before a reference date (usually the latest case
 * confirmation). Contacts that fail are left out before the rings are built.
 *
 * <p><b>Unknown values are kept.</b> A contact with no recorded duration or no
 * recorded date might still be risky, so it is kept rather than silently
 * dropped; the data quality warnings already list such contacts.
 *
 * <p>Time complexity: O(1) per contact to test, O(E) for {@link #summarize}.
 * Space complexity: O(1).
 *
 * @param minDurationMinutes contacts shorter than this are left out; null for no limit
 * @param windowDays         contacts more than this many days before the reference date, or after it, are left
 *                           out; null for no limit
 * @param referenceDate      end of the window (needed only when windowDays is set)
 */
public record ContactDefinition(Integer minDurationMinutes, Integer windowDays, LocalDate referenceDate)
        implements Predicate<Contact> {

    /** Every contact counts. */
    public static final ContactDefinition ALL = new ContactDefinition(null, null, null);

    public ContactDefinition {
        if (minDurationMinutes != null && (minDurationMinutes < 0 || minDurationMinutes > 1440)) {
            throw new IllegalArgumentException("Minimum duration must be between 0 and 1440 minutes");
        }
        if (windowDays != null && (windowDays < 1 || windowDays > 365)) {
            throw new IllegalArgumentException("The contact window must be between 1 and 365 days");
        }
        if (windowDays != null && referenceDate == null) {
            throw new IllegalArgumentException("A contact window needs a reference date");
        }
    }

    /** Why a contact was left out, or KEPT. */
    public enum Verdict { KEPT, TOO_SHORT, BEFORE_WINDOW, AFTER_REFERENCE }

    /** How many contacts were kept and left out, and why. */
    public record Summary(Integer minDurationMinutes, Integer windowDays, LocalDate referenceDate, int total, int kept,
                          int tooShort, int beforeWindow, int afterReference, int keptUnknownDuration,
                          int keptUnknownDate) {

        public int excluded() {
            return total - kept;
        }
    }

    public boolean isActive() {
        return minDurationMinutes != null || windowDays != null;
    }

    public Verdict verdict(Contact c) {
        if (minDurationMinutes != null && c.durationMinutes() >= 0 && c.durationMinutes() < minDurationMinutes) {
            return Verdict.TOO_SHORT;
        }
        if (windowDays != null && c.date() != null) {
            if (c.date().isBefore(referenceDate.minusDays(windowDays))) {
                return Verdict.BEFORE_WINDOW;
            }
            if (c.date().isAfter(referenceDate)) {
                return Verdict.AFTER_REFERENCE;
            }
        }
        return Verdict.KEPT;
    }

    @Override
    public boolean test(Contact c) {
        return verdict(c) == Verdict.KEPT;
    }

    /** Counts the verdicts over every contact in the graph. */
    public Summary summarize(ContactGraph graph) {
        int kept = 0;
        int shortCount = 0;
        int before = 0;
        int after = 0;
        int unknownDuration = 0;
        int unknownDate = 0;
        for (Contact c : graph.contacts()) {
            switch (verdict(c)) {
                case KEPT -> {
                    kept++;
                    if (minDurationMinutes != null && c.durationMinutes() < 0) {
                        unknownDuration++;
                    }
                    if (windowDays != null && c.date() == null) {
                        unknownDate++;
                    }
                }
                case TOO_SHORT -> shortCount++;
                case BEFORE_WINDOW -> before++;
                case AFTER_REFERENCE -> after++;
                default -> throw new IllegalStateException();
            }
        }
        return new Summary(minDurationMinutes, windowDays, referenceDate, graph.contactCount(), kept, shortCount,
                before, after, unknownDuration, unknownDate);
    }
}
