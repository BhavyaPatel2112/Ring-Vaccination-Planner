package com.ringvax.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.ringvax.api.dto.BloomConfigRequest;
import com.ringvax.api.dto.BloomConfigResponse;
import com.ringvax.api.dto.ExposureCheckRequest;
import com.ringvax.api.dto.ExposureCheckResponse;
import com.ringvax.api.dto.FilterStats;
import com.ringvax.api.dto.RegistryCheckRequest;
import com.ringvax.api.dto.RegistryCheckResponse;
import com.ringvax.data.CheckIn;
import com.ringvax.data.ConfirmedCase;
import com.ringvax.engine.bloom.BloomExperiment;
import com.ringvax.engine.bloom.BloomFilter;
import com.ringvax.engine.graph.ContactGraph;
import com.ringvax.engine.graph.Person;
import com.ringvax.engine.graph.UnknownPersonException;

/**
 * The three Bloom filter features:
 * <ul>
 *   <li><b>Exposure self-check (6)</b>: one filter per network holds a key
 *       {@code venueId|yyyy-MM-ddTHH} for every hour a confirmed case spent at a
 *       venue during their infectious window. A visit is looked up hour by hour;
 *       a hit is only "possibly exposed" until the exact check-in records confirm
 *       an overlap.</li>
 *   <li><b>Double-dose guard (9)</b>: one filter per network holds the ids of
 *       people already vaccinated; every hit is confirmed against the registry
 *       (the people list).</li>
 *   <li><b>Tuning panel (10)</b>: runs {@link BloomExperiment} for chosen m and k.</li>
 * </ul>
 * Filters are sized for a 1% false positive rate with the optimal k.
 */
@Service
public class BloomService {

    public static final int DEFAULT_INFECTIOUS_DAYS = 14;
    public static final double TARGET_RATE = 0.01;
    public static final int TUNING_QUERIES = 100_000;
    private static final int MAX_VISIT_HOURS = 7 * 24;

    /** Filter of case venue-hours plus the exact case check-ins used to confirm hits. */
    private record ExposureIndex(BloomFilter filter, Map<String, List<CaseVisit>> caseVisitsByVenue, long hashSetBytes,
                                 int cases) {
    }

    /** A case's visit, trimmed to the infectious window. The case is deliberately not stored. */
    private record CaseVisit(LocalDateTime start, LocalDateTime end) {
    }

    private record Registry(BloomFilter filter, long hashSetBytes, int people) {
    }

    private final NetworkService networks;
    private final Map<String, ExposureIndex> exposureIndexes = new ConcurrentHashMap<>();
    private final Map<String, Registry> registries = new ConcurrentHashMap<>();

    public BloomService(NetworkService networks) {
        this.networks = networks;
    }

    // ------------------------------------------------------------------ exposure check (feature 6)

    public ExposureCheckResponse checkExposure(ExposureCheckRequest request) {
        StoredNetwork network = networks.get(request.networkId());
        List<ExposureCheckRequest.Visit> visits = visitsToCheck(network, request);
        int days = request.infectiousDays() == null ? DEFAULT_INFECTIOUS_DAYS : request.infectiousDays();
        ExposureIndex index = exposureIndex(network, request.caseIds(), days);

        List<ExposureCheckResponse.VisitResult> results = new ArrayList<>();
        int hits = 0;
        int confirmedCount = 0;
        int falseAlarms = 0;
        for (ExposureCheckRequest.Visit visit : visits) {
            int checked = 0;
            int flagged = 0;
            for (LocalDateTime hour = visit.start().truncatedTo(ChronoUnit.HOURS); hour.isBefore(visit.end());
                    hour = hour.plusHours(1)) {
                checked++;
                if (index.filter().mightContain(key(visit.venueId().trim(), hour))) {
                    flagged++;
                }
            }
            boolean possibly = flagged > 0;

            // Exact confirmation: same venue, overlapping time. Only run when the filter says "possibly".
            LocalDate exposureDate = null;
            long overlap = 0;
            if (possibly) {
                for (CaseVisit c : index.caseVisitsByVenue().getOrDefault(visit.venueId().trim(), List.of())) {
                    LocalDateTime from = later(visit.start(), c.start());
                    LocalDateTime to = earlier(visit.end(), c.end());
                    if (from.isBefore(to)) {
                        overlap += ChronoUnit.MINUTES.between(from, to);
                        if (exposureDate == null) {
                            exposureDate = from.toLocalDate();
                        }
                    }
                }
            }
            boolean confirmed = overlap > 0;
            String explanation;
            if (!possibly) {
                explanation = "Quick check: no confirmed case was at this venue in any of these hours. Not exposed.";
            } else if (confirmed) {
                explanation = "Quick check said possibly exposed; the records confirm a confirmed case was here at the"
                        + " same time (" + overlap + " minutes of overlap). Please follow the advice for contacts.";
            } else {
                explanation = "Quick check said possibly exposed, but the exact records show no overlapping time. Not"
                        + " exposed. (A Bloom filter can say 'possibly' when the answer is no; it never misses a"
                        + " real exposure.)";
            }
            if (possibly) {
                hits++;
            }
            if (confirmed) {
                confirmedCount++;
            } else if (possibly) {
                falseAlarms++;
            }
            results.add(new ExposureCheckResponse.VisitResult(visit.venueId().trim(), visit.start(), visit.end(),
                    possibly ? "POSSIBLY_EXPOSED" : "NOT_EXPOSED", checked, flagged, confirmed, exposureDate,
                    confirmed ? overlap : null, explanation));
        }

        String message;
        if (index.cases() == 0) {
            message = "There are no confirmed cases in this network yet, so none of these visits is an exposure.";
        } else if (confirmedCount > 0) {
            message = "Exposed: " + confirmedCount + (confirmedCount == 1 ? " visit overlaps" : " visits overlap")
                    + " a confirmed case's visit. Watch for symptoms and contact the outbreak team.";
        } else if (hits > 0) {
            message = "Not exposed. The quick check flagged " + hits + (hits == 1 ? " visit" : " visits")
                    + ", but the exact records show no overlap with a confirmed case.";
        } else {
            message = "Not exposed: no confirmed case was at these venues at these times.";
        }
        return new ExposureCheckResponse(hits > 0 ? "POSSIBLY_EXPOSED" : "NOT_EXPOSED",
                confirmedCount > 0 ? "EXPOSED" : "NOT_EXPOSED", message, results, hits, confirmedCount, falseAlarms,
                FilterStats.of("Exposure (venue-hours of confirmed cases)", index.filter(), index.hashSetBytes()));
    }

    private static List<ExposureCheckRequest.Visit> visitsToCheck(StoredNetwork network, ExposureCheckRequest request) {
        List<ExposureCheckRequest.Visit> visits = new ArrayList<>();
        if (request.visits() != null) {
            visits.addAll(request.visits());
        }
        if (request.personId() != null && !request.personId().isBlank()) {
            String id = request.personId().trim();
            if (!network.graph().containsPerson(id)) {
                throw new UnknownPersonException(id);
            }
            for (CheckIn c : network.checkIns()) {
                if (c.personId().equals(id)) {
                    visits.add(new ExposureCheckRequest.Visit(c.venueId(), c.start(), c.end()));
                }
            }
            if (visits.isEmpty()) {
                throw new IllegalArgumentException("There are no recorded visits for " + id
                        + ". Enter the visits to check instead.");
            }
        }
        if (visits.isEmpty()) {
            throw new IllegalArgumentException("Give at least one visit (venue, start and end), or a person id with"
                    + " recorded check-ins");
        }
        for (ExposureCheckRequest.Visit v : visits) {
            if (v.venueId() == null || v.venueId().isBlank() || v.start() == null || v.end() == null) {
                throw new IllegalArgumentException("Every visit needs a venue, a start and an end");
            }
            if (!v.end().isAfter(v.start())) {
                throw new IllegalArgumentException("The visit to " + v.venueId() + " must end after it starts");
            }
            if (ChronoUnit.HOURS.between(v.start(), v.end()) > MAX_VISIT_HOURS) {
                throw new IllegalArgumentException("A single visit can be at most 7 days long");
            }
        }
        return visits;
    }

    private ExposureIndex exposureIndex(StoredNetwork network, List<String> caseIds, int days) {
        ContactGraph g = network.graph();
        Map<String, LocalDate> confirmed = new HashMap<>();
        for (ConfirmedCase c : network.cases()) {
            confirmed.put(c.personId(), c.confirmedDate());
        }
        // Cases marked in the app (not in cases.csv) have no confirmation date: all their visits count.
        TreeSet<String> cases = new TreeSet<>();
        if (caseIds == null || caseIds.isEmpty()) {
            cases.addAll(confirmed.keySet());
        } else {
            for (String id : caseIds) {
                String trimmed = id.trim();
                if (!g.containsPerson(trimmed)) {
                    throw new UnknownPersonException(trimmed);
                }
                cases.add(trimmed);
            }
        }
        String cacheKey = network.id() + "|" + days + "|" + String.join(",", cases);
        return exposureIndexes.computeIfAbsent(cacheKey, k -> buildExposureIndex(network, cases, confirmed, days));
    }

    private static ExposureIndex buildExposureIndex(StoredNetwork network, TreeSet<String> cases,
                                                    Map<String, LocalDate> confirmed, int days) {
        List<String> keys = new ArrayList<>();
        Map<String, List<CaseVisit>> byVenue = new HashMap<>();
        for (CheckIn c : network.checkIns()) {
            if (!cases.contains(c.personId())) {
                continue;
            }
            LocalDate date = confirmed.get(c.personId());
            LocalDateTime from = c.start();
            LocalDateTime to = c.end();
            if (date != null) {
                // Infectious window: from `days` days before confirmation to the end of the confirmation day.
                from = later(from, date.minusDays(days).atStartOfDay());
                to = earlier(to, date.plusDays(1).atStartOfDay());
            }
            if (!from.isBefore(to)) {
                continue;
            }
            byVenue.computeIfAbsent(c.venueId(), v -> new ArrayList<>()).add(new CaseVisit(from, to));
            for (LocalDateTime hour = from.truncatedTo(ChronoUnit.HOURS); hour.isBefore(to); hour = hour.plusHours(1)) {
                keys.add(key(c.venueId(), hour));
            }
        }
        BloomFilter filter = BloomFilter.forCapacity(Math.max(1, keys.size()), TARGET_RATE);
        long hashSetBytes = 0;
        for (String key : keys) {
            filter.add(key);
            hashSetBytes += hashSetBytes(key);
        }
        return new ExposureIndex(filter, byVenue, hashSetBytes, cases.size());
    }

    /** Filter key for one venue and one clock hour, for example "V002|2026-11-01T18". */
    static String key(String venueId, LocalDateTime hour) {
        return venueId + "|" + hour.toLocalDate() + "T" + String.format(Locale.ROOT, "%02d", hour.getHour());
    }

    // ------------------------------------------------------------------ double-dose guard (feature 9)

    public RegistryCheckResponse checkRegistry(RegistryCheckRequest request) {
        StoredNetwork network = networks.get(request.networkId());
        ContactGraph g = network.graph();
        Registry registry = registry(network);
        List<RegistryCheckResponse.Entry> results = new ArrayList<>();
        int vaccinatedCount = 0;
        int hits = 0;
        int falsePositives = 0;
        for (String raw : request.personIds()) {
            String id = raw.trim();
            boolean possibly = registry.filter().mightContain(id);
            int p = g.indexOf(id);
            boolean known = p >= 0;
            // Exact confirmation only when the filter says "possibly"; a "no" is always correct.
            boolean vaccinated = possibly && known && g.person(p).vaccinated();
            boolean falsePositive = possibly && !vaccinated;
            if (possibly) {
                hits++;
            }
            if (vaccinated) {
                vaccinatedCount++;
            }
            if (falsePositive) {
                falsePositives++;
            }
            String action = !known ? "UNKNOWN_PERSON" : vaccinated ? "SKIP" : "GIVE_DOSE";
            results.add(new RegistryCheckResponse.Entry(id, known, possibly ? "POSSIBLY_VACCINATED" : "NOT_VACCINATED",
                    vaccinated, falsePositive, action));
        }
        return new RegistryCheckResponse(results, vaccinatedCount, hits, falsePositives,
                FilterStats.of("Vaccination registry (person ids)", registry.filter(), registry.hashSetBytes()));
    }

    private Registry registry(StoredNetwork network) {
        return registries.computeIfAbsent(network.id(), id -> {
            ContactGraph g = network.graph();
            List<String> ids = new ArrayList<>();
            for (Person p : g.people()) {
                if (p.vaccinated()) {
                    ids.add(p.id());
                }
            }
            BloomFilter filter = BloomFilter.forCapacity(Math.max(1, ids.size()), TARGET_RATE);
            long bytes = 0;
            for (String pid : ids) {
                filter.add(pid);
                bytes += hashSetBytes(pid);
            }
            return new Registry(filter, bytes, ids.size());
        });
    }

    // ------------------------------------------------------------------ tuning (feature 10) and stats

    public BloomConfigResponse tune(BloomConfigRequest request) {
        int n = request.n() == null ? 10_000 : request.n();
        long seed = request.seed() == null ? PlanService.DEFAULT_SEED : request.seed();
        BloomExperiment.Result r = BloomExperiment.run(request.m(), request.k(), n, TUNING_QUERIES, seed);
        List<BloomConfigResponse.RateAtK> curve = new ArrayList<>();
        for (int k = 1; k <= 20; k++) {
            curve.add(new BloomConfigResponse.RateAtK(k, BloomFilter.theoreticalRate(request.m(), k, n)));
        }
        String summary = String.format(Locale.ROOT,
                "%,d keys in %,d bits (%.1f bits per key) with k = %d: theory %.3f%%, measured %.3f%% over %,d"
                        + " non-member queries, no false negatives. Best k here is %d. The filter uses %.1f times"
                        + " less memory than a hash set of the same keys.",
                n, r.m(), r.bitsPerKey(), r.k(), r.theoreticalRate() * 100, r.measuredRate() * 100, r.queries(),
                r.optimalK(), r.memoryRatio());
        return new BloomConfigResponse(r, curve, summary);
    }

    /** Stats of the live filters of a network (built if needed). */
    public List<FilterStats> stats(String networkId) {
        StoredNetwork network = networks.get(networkId);
        ExposureIndex exposure = exposureIndex(network, null, DEFAULT_INFECTIOUS_DAYS);
        Registry registry = registry(network);
        return List.of(
                FilterStats.of("Exposure (venue-hours of confirmed cases)", exposure.filter(), exposure.hashSetBytes()),
                FilterStats.of("Vaccination registry (person ids)", registry.filter(), registry.hashSetBytes()));
    }

    // ------------------------------------------------------------------ helpers

    /** Estimated bytes for one String in a HashSet (same estimate as {@link BloomExperiment}). */
    private static long hashSetBytes(String key) {
        return 32 + Math.round(4 / 0.75) + 24 + (16 + key.length() + 7) / 8 * 8;
    }

    private static LocalDateTime later(LocalDateTime a, LocalDateTime b) {
        return a.isAfter(b) ? a : b;
    }

    private static LocalDateTime earlier(LocalDateTime a, LocalDateTime b) {
        return a.isBefore(b) ? a : b;
    }
}
