package com.ringvax.service;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.ringvax.api.dto.NetworkSummary;
import com.ringvax.api.dto.NetworkView;
import com.ringvax.api.dto.SampleInfo;
import com.ringvax.data.CheckIn;
import com.ringvax.data.ConfirmedCase;
import com.ringvax.data.ContactQualityCheck;
import com.ringvax.data.CsvParser;
import com.ringvax.data.ParseResult;
import com.ringvax.data.QualityWarning;
import com.ringvax.data.SyntheticNetworkGenerator;
import com.ringvax.data.ValidationIssue;
import com.ringvax.engine.graph.Contact;
import com.ringvax.engine.graph.ContactGraph;
import com.ringvax.engine.graph.Person;

/**
 * Loads networks (upload, sample or synthetic), keeps them in memory and
 * prepares them for display.
 */
@Service
public class NetworkService {

    /** Above this many people the drawing is sampled around the cases. */
    public static final int MAX_VIEW_NODES = 1500;

    private static final Map<String, String[]> SAMPLE_LABELS = new LinkedHashMap<>();

    static {
        SAMPLE_LABELS.put("outbreak-small", new String[] {"Neighbourhood outbreak",
            "40 people in 10 households, a school, a bus route and a clinic, with 2 confirmed cases"});
        SAMPLE_LABELS.put("validation-demo", new String[] {"Messy data",
            "One example of every input problem, to show how bad rows are reported"});
        SAMPLE_LABELS.put("star", new String[] {"Star", "One hub with 7 contacts: the 2-approximation's worst case"});
        SAMPLE_LABELS.put("complete-bipartite-3-3", new String[] {"Two groups of three",
            "Everyone meets everyone in the other group (minimum 3)"});
        SAMPLE_LABELS.put("greedy-worst-case-12", new String[] {"Greedy trap (35 people)",
            "Built so 'most connected first' picks 23 people against a minimum of 12"});
        SAMPLE_LABELS.put("greedy-worst-case-18", new String[] {"Greedy trap (58 people)",
            "Greedy picks 40 against a minimum of 18, worse than 2 times"});
    }

    private final Map<String, StoredNetwork> networks = new ConcurrentHashMap<>();
    private final CsvParser parser = new CsvParser();
    private final Path sampleDir;
    private final Clock clock;

    @Autowired
    public NetworkService(@Value("${ringvax.sample-dir:../data/sample}") String sampleDir) {
        this(Path.of(sampleDir), Clock.systemDefaultZone());
    }

    NetworkService(Path sampleDir, Clock clock) {
        this.sampleDir = sampleDir;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ loading

    /** Reads uploaded CSV text. Check-ins and cases are optional (null). */
    public NetworkSummary load(String name, String peopleCsv, String contactsCsv, String checkInsCsv, String casesCsv,
                               String source) {
        ParseResult<ContactGraph> network = parser.parseNetwork(new StringReader(peopleCsv),
                new StringReader(contactsCsv));
        ContactGraph graph = network.value();
        List<ValidationIssue> warnings = new ArrayList<>(network.warnings());

        List<CheckIn> checkIns = List.of();
        if (checkInsCsv != null) {
            ParseResult<List<CheckIn>> parsed = parser.parseCheckIns(new StringReader(checkInsCsv), graph);
            checkIns = parsed.value();
            warnings.addAll(parsed.warnings());
        }
        List<ConfirmedCase> cases = List.of();
        if (casesCsv != null) {
            ParseResult<List<ConfirmedCase>> parsed = parser.parseCases(new StringReader(casesCsv), graph);
            cases = parsed.value();
            warnings.addAll(parsed.warnings());
        }
        List<QualityWarning> quality = new ContactQualityCheck(2026).check(graph, cases, LocalDate.now(clock));

        String id = newId();
        StoredNetwork stored = new StoredNetwork(id, blankToDefault(name, "Uploaded network"), source, graph, cases,
                checkIns, warnings, quality, LocalDateTime.now(clock));
        networks.put(id, stored);
        return summary(stored);
    }

    /** Builds a seeded synthetic network. */
    public NetworkSummary generate(int population, double avgContacts, long seed, String name) {
        ContactGraph graph = SyntheticNetworkGenerator.generate(population, avgContacts, seed);
        List<QualityWarning> quality = new ContactQualityCheck(seed).check(graph, List.of(), LocalDate.now(clock));
        String label = blankToDefault(name, "Synthetic town of " + population + " people");
        String id = newId();
        StoredNetwork stored = new StoredNetwork(id, label, "generated", graph, List.of(), List.of(), List.of(),
                quality, LocalDateTime.now(clock));
        networks.put(id, stored);
        return summary(stored);
    }

    /** Sample networks available in the sample folder. */
    public List<SampleInfo> samples() {
        if (!Files.isDirectory(sampleDir)) {
            return List.of();
        }
        try (Stream<Path> dirs = Files.list(sampleDir)) {
            return dirs.filter(d -> Files.exists(d.resolve("people.csv")) && Files.exists(d.resolve("contacts.csv")))
                    .map(d -> d.getFileName().toString())
                    .sorted(Comparator.comparingInt(NetworkService::sampleOrder).thenComparing(s -> s))
                    .map(id -> {
                        String[] label = SAMPLE_LABELS.getOrDefault(id, new String[] {id, ""});
                        return new SampleInfo(id, label[0], label[1]);
                    })
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Loads one of the sample networks. */
    public NetworkSummary loadSample(String sampleId) {
        Path dir = sampleDir.resolve(sampleId).normalize();
        if (!sampleId.matches("[a-z0-9-]+") || !Files.exists(dir.resolve("people.csv"))) {
            throw new NetworkNotFoundException("No sample network called '" + sampleId + "'");
        }
        String label = SAMPLE_LABELS.getOrDefault(sampleId, new String[] {sampleId})[0];
        return load(label, read(dir.resolve("people.csv")), read(dir.resolve("contacts.csv")),
                readIfExists(dir.resolve("checkins.csv")), readIfExists(dir.resolve("cases.csv")), "sample");
    }

    // ------------------------------------------------------------------ reading

    public StoredNetwork get(String networkId) {
        StoredNetwork network = networks.get(networkId);
        if (network == null) {
            throw new NetworkNotFoundException("No network with id '" + networkId + "'. Load or upload it first.");
        }
        return network;
    }

    public NetworkSummary summary(String networkId) {
        return summary(get(networkId));
    }

    /** All loaded networks, newest first. */
    public List<NetworkSummary> list() {
        return networks.values().stream()
                .sorted(Comparator.comparing(StoredNetwork::createdAt).reversed())
                .map(this::summary)
                .toList();
    }

    /**
     * The network for drawing. Up to {@link #MAX_VIEW_NODES} people are included,
     * found by breadth-first search outwards from the cases (or from the most
     * connected person when there are none).
     */
    public NetworkView view(String networkId) {
        StoredNetwork network = get(networkId);
        ContactGraph g = network.graph();
        Set<Integer> caseIndices = new HashSet<>();
        for (ConfirmedCase c : network.cases()) {
            caseIndices.add(g.indexOf(c.personId()));
        }

        boolean sampled = g.personCount() > MAX_VIEW_NODES;
        boolean[] include = new boolean[g.personCount()];
        if (!sampled) {
            Arrays.fill(include, true);
        } else {
            ArrayDeque<Integer> queue = new ArrayDeque<>(caseIndices);
            if (queue.isEmpty()) {
                int hub = 0;
                for (int p = 1; p < g.personCount(); p++) {
                    if (g.degree(p) > g.degree(hub)) {
                        hub = p;
                    }
                }
                queue.add(hub);
            }
            int count = 0;
            for (int p : queue) {
                include[p] = true;
                count++;
            }
            while (!queue.isEmpty() && count < MAX_VIEW_NODES) {
                int u = queue.poll();
                for (int i = 0; i < g.degree(u) && count < MAX_VIEW_NODES; i++) {
                    int v = g.neighbour(u, i);
                    if (!include[v]) {
                        include[v] = true;
                        count++;
                        queue.add(v);
                    }
                }
            }
        }

        List<NetworkView.Node> nodes = new ArrayList<>();
        for (int p = 0; p < g.personCount(); p++) {
            if (include[p]) {
                Person person = g.person(p);
                nodes.add(new NetworkView.Node(person.id(), person.name(), person.age(), person.role(),
                        person.vaccinated(), person.eligible(), person.refused(), caseIndices.contains(p),
                        g.degree(p)));
            }
        }
        List<NetworkView.Link> links = new ArrayList<>();
        for (Contact c : g.contacts()) {
            if (include[c.a()] && include[c.b()]) {
                links.add(new NetworkView.Link(g.person(c.a()).id(), g.person(c.b()).id(), c.date(),
                        c.durationMinutes()));
            }
        }
        return new NetworkView(network.id(), g.personCount(), g.contactCount(), sampled, nodes, links);
    }

    // ------------------------------------------------------------------ helpers

    private NetworkSummary summary(StoredNetwork n) {
        return new NetworkSummary(n.id(), n.name(), n.source(), n.graph().personCount(), n.graph().contactCount(),
                n.cases().stream().map(ConfirmedCase::personId).toList(), n.checkIns().size(), n.warnings(),
                n.qualityWarnings(), n.createdAt());
    }

    private static int sampleOrder(String id) {
        int i = 0;
        for (String known : SAMPLE_LABELS.keySet()) {
            if (known.equals(id)) {
                return i;
            }
            i++;
        }
        return i;
    }

    private static String newId() {
        return "net-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String read(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String readIfExists(Path file) {
        return Files.exists(file) ? read(file) : null;
    }
}
