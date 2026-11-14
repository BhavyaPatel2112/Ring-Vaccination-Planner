package com.ringvax.engine.followup;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import com.ringvax.engine.graph.Contact;
import com.ringvax.engine.graph.ContactGraph;
import com.ringvax.engine.graph.Person;
import com.ringvax.engine.heap.IndexedMaxHeap;
import com.ringvax.engine.ring.RingResult;

/**
 * Turns a vaccination plan into field work (features B and C):
 * <ul>
 *   <li><b>Vaccinate by:</b> post-exposure vaccination only helps within a short
 *       window (72 hours by default) after exposure. Each dose gets a deadline =
 *       last exposure + window, and the list is ordered by urgency with the
 *       {@link IndexedMaxHeap} (key = minus the deadline, so the earliest
 *       deadline comes out first). People already past the window are flagged
 *       "monitor instead".</li>
 *   <li><b>Monitoring list:</b> everyone in the rings who will not be protected by
 *       a dose today (not in the plan, cannot be vaccinated, or past the window)
 *       is watched for symptoms until last exposure + incubation period (21 days
 *       by default).</li>
 * </ul>
 *
 * <p><b>Last exposure</b> of a ring member is the most recent date of a counted
 * contact with someone one ring closer to a case (a case for ring 1). Dates are
 * days, so the exposure is taken as the start of that day: the earliest, most
 * cautious deadline. A person with no dated contact is listed first, as
 * "date unknown: vaccinate as soon as possible".
 *
 * <p>Time complexity: O(V + E) to find last exposures, O(D log D) to order D
 * doses with the heap, O(R log R) to sort R monitored people. Space: O(V).
 */
public class FollowUpPlanner {

    public static final int DEFAULT_WINDOW_HOURS = 72;
    public static final int DEFAULT_INCUBATION_DAYS = 21;
    /** A dose with this many hours or fewer left is urgent. */
    public static final int URGENT_HOURS = 24;

    public enum Status { DATE_UNKNOWN, URGENT, ON_TIME, PAST_WINDOW }

    public enum MonitorReason { PAST_WINDOW, CANNOT_BE_VACCINATED, NOT_IN_PLAN, LEFT_OUT_BY_BUDGET }

    /** One dose with its deadline. {@code hoursLeft} is negative past the deadline; null when the date is unknown. */
    public record Dose(int person, int ring, LocalDate lastExposure, LocalDateTime deadline, Long hoursLeft,
                       Status status) {
    }

    public record Monitor(int person, int ring, LocalDate lastExposure, LocalDate monitorUntil, MonitorReason reason) {
    }

    public record Result(List<Dose> doses, List<Monitor> monitoring) {
    }

    private final ContactGraph graph;
    private final Predicate<Contact> counts;

    public FollowUpPlanner(ContactGraph graph, Predicate<Contact> counts) {
        this.graph = graph;
        this.counts = counts;
    }

    /**
     * @param ring           the rings the plan was built on (with the same contact filter)
     * @param planPeople     people in the plan, in plan order
     * @param asOf           "now" for the deadlines
     * @param windowHours    post-exposure vaccination window
     * @param incubationDays how long to watch for symptoms after the last exposure
     */
    public Result plan(RingResult ring, int[] planPeople, LocalDateTime asOf, int windowHours, int incubationDays) {
        if (windowHours < 1 || windowHours > 720) {
            throw new IllegalArgumentException("The vaccination window must be between 1 and 720 hours");
        }
        if (incubationDays < 1 || incubationDays > 60) {
            throw new IllegalArgumentException("The incubation period must be between 1 and 60 days");
        }
        int n = graph.personCount();
        LocalDate[] last = lastExposures(ring);

        // Order the doses by urgency with the heap: key = -deadline (minutes), unknown dates first.
        IndexedMaxHeap heap = new IndexedMaxHeap(n);
        boolean[] inPlan = new boolean[n];
        for (int p : planPeople) {
            inPlan[p] = true;
            long key = last[p] == null ? Long.MAX_VALUE
                    : -deadline(last[p], windowHours).toEpochSecond(ZoneOffset.UTC) / 60;
            heap.insert(p, key);
        }
        List<Dose> doses = new ArrayList<>();
        List<Monitor> monitoring = new ArrayList<>();
        while (!heap.isEmpty()) {
            int p = heap.extractMax();
            int level = level(ring, p);
            if (last[p] == null) {
                doses.add(new Dose(p, level, null, null, null, Status.DATE_UNKNOWN));
                continue;
            }
            LocalDateTime due = deadline(last[p], windowHours);
            long hoursLeft = Duration.between(asOf, due).toHours();
            Status status = due.isBefore(asOf) ? Status.PAST_WINDOW
                    : hoursLeft <= URGENT_HOURS ? Status.URGENT : Status.ON_TIME;
            doses.add(new Dose(p, level, last[p], due, hoursLeft, status));
            if (status == Status.PAST_WINDOW) {
                monitoring.add(new Monitor(p, level, last[p], last[p].plusDays(incubationDays),
                        MonitorReason.PAST_WINDOW));
            }
        }

        for (int p = 0; p < n; p++) {
            int level = ring.level(p);
            if (level < 1 || inPlan[p]) {
                continue;
            }
            Person person = graph.person(p);
            if (person.vaccinated()) {
                continue;
            }
            MonitorReason reason = !person.canBeVaccinated() ? MonitorReason.CANNOT_BE_VACCINATED
                    : level == 1 ? MonitorReason.LEFT_OUT_BY_BUDGET : MonitorReason.NOT_IN_PLAN;
            LocalDate from = last[p] == null ? asOf.toLocalDate() : last[p];
            monitoring.add(new Monitor(p, level, last[p], from.plusDays(incubationDays), reason));
        }
        monitoring.sort(Comparator.comparingInt(Monitor::ring)
                .thenComparing(Monitor::monitorUntil, Comparator.reverseOrder())
                .thenComparingInt(Monitor::person));
        return new Result(doses, monitoring);
    }

    /**
     * Most recent counted contact with someone one ring closer, per person (null if none is dated).
     * People outside the rings only pick up exposures from the outermost ring.
     */
    LocalDate[] lastExposures(RingResult ring) {
        int n = graph.personCount();
        LocalDate[] last = new LocalDate[n];
        for (Contact c : graph.contacts()) {
            if (c.date() == null || !counts.test(c)) {
                continue;
            }
            int la = level(ring, c.a());
            int lb = level(ring, c.b());
            if (lb == la + 1) {
                last[c.b()] = later(last[c.b()], c.date());
            } else if (la == lb + 1) {
                last[c.a()] = later(last[c.a()], c.date());
            }
        }
        return last;
    }

    /**
     * Ring level, where someone just outside the rings counts as one ring further
     * out: the plan can include them when they share a link with the outermost ring.
     */
    private static int level(RingResult ring, int person) {
        int level = ring.level(person);
        return level == RingResult.OUTSIDE ? ring.depth() + 1 : level;
    }

    private static LocalDateTime deadline(LocalDate exposure, int windowHours) {
        return exposure.atStartOfDay().plusHours(windowHours);
    }

    private static LocalDate later(LocalDate a, LocalDate b) {
        return a == null || b.isAfter(a) ? b : a;
    }
}
