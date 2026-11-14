package com.ringvax.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.ringvax.api.dto.ClustersResponse;
import com.ringvax.api.dto.LiveContactRequest;
import com.ringvax.api.dto.LiveContactResponse;
import com.ringvax.data.ConfirmedCase;
import com.ringvax.engine.dsu.OutbreakClusters;
import com.ringvax.engine.graph.ContactGraph;
import com.ringvax.engine.graph.UnknownPersonException;

/**
 * Outbreak clusters (feature 2) and live contact updates (feature 12). One
 * {@link OutbreakClusters} is kept per network and built the first time it is
 * needed; after that each new contact is a single union.
 */
@Service
public class ClusterService {

    /** At most this many clusters are listed. */
    public static final int MAX_CLUSTERS = 200;
    /** Members are listed only for clusters up to this size (enough to highlight on the map). */
    public static final int MAX_MEMBERS = 1500;

    private final NetworkService networks;
    private final Map<String, OutbreakClusters> byNetwork = new ConcurrentHashMap<>();

    public ClusterService(NetworkService networks) {
        this.networks = networks;
    }

    /** Clusters, with cases taken from {@code caseIds} or, when that is empty, from the network's cases.csv. */
    public ClustersResponse clusters(String networkId, List<String> caseIds) {
        StoredNetwork network = networks.get(networkId);
        ContactGraph g = network.graph();
        boolean[] isCase = new boolean[g.personCount()];
        List<String> ids = caseIds == null || caseIds.isEmpty()
                ? network.cases().stream().map(ConfirmedCase::personId).toList()
                : caseIds;
        for (String id : ids) {
            int p = g.indexOf(id.trim());
            if (p < 0) {
                throw new UnknownPersonException(id.trim());
            }
            isCase[p] = true;
        }

        List<OutbreakClusters.Cluster> all;
        int count;
        synchronized (g) {
            OutbreakClusters clusters = forNetwork(networkId, g);
            all = clusters.summarize(isCase);
            count = clusters.clusterCount();
        }

        int isolated = 0;
        List<ClustersResponse.ClusterInfo> listed = new ArrayList<>();
        int omitted = 0;
        for (OutbreakClusters.Cluster c : all) {
            if (c.size() == 1 && c.cases() == 0) {
                isolated++;
                continue;
            }
            if (listed.size() >= MAX_CLUSTERS) {
                omitted++;
                continue;
            }
            List<String> cases = new ArrayList<>();
            List<String> members = new ArrayList<>();
            boolean tooBig = c.size() > MAX_MEMBERS;
            for (int p : c.members()) {
                String id = g.person(p).id();
                if (isCase[p]) {
                    cases.add(id);
                }
                if (!tooBig) {
                    members.add(id);
                }
            }
            listed.add(new ClustersResponse.ClusterInfo(g.person(c.root()).id(), c.size(), c.cases(),
                    c.vaccinated(), cases, members, tooBig));
        }
        return new ClustersResponse(count, isolated, listed, omitted);
    }

    /**
     * Adds a contact reported in the field to the stored network and merges the two
     * people's clusters. The rings, plans and the network view see it straight away.
     *
     * @throws UnknownPersonException   if either person is not in the network
     * @throws IllegalArgumentException for a self-contact or a contact that already exists
     */
    public LiveContactResponse addContact(String networkId, LiveContactRequest request) {
        StoredNetwork network = networks.get(networkId);
        ContactGraph g = network.graph();
        String idA = request.personA().trim();
        String idB = request.personB().trim();
        int a = g.indexOf(idA);
        if (a < 0) {
            throw new UnknownPersonException(idA);
        }
        int b = g.indexOf(idB);
        if (b < 0) {
            throw new UnknownPersonException(idB);
        }
        if (a == b) {
            throw new IllegalArgumentException("A contact needs two different people (got " + idA + " twice)");
        }
        int minutes = request.durationMinutes() == null ? -1 : request.durationMinutes();
        synchronized (g) {
            if (g.hasContact(a, b)) {
                throw new IllegalArgumentException(idA + " and " + idB + " already have a contact in this network");
            }
            OutbreakClusters clusters = forNetwork(networkId, g);
            g.addContact(a, b, request.date(), minutes);
            long start = System.nanoTime();
            boolean merged = clusters.addContact(a, b);
            double micros = (System.nanoTime() - start) / 1e3;
            int size = clusters.clusterSize(a);
            String message = merged
                    ? "Two clusters merged into one of " + size + " people"
                    : idA + " and " + idB + " were already in the same cluster (" + size + " people)";
            return new LiveContactResponse(idA, idB, merged, size, clusters.clusterCount(), g.contactCount(), message,
                    micros);
        }
    }

    private OutbreakClusters forNetwork(String networkId, ContactGraph g) {
        return byNetwork.computeIfAbsent(networkId, id -> new OutbreakClusters(g));
    }
}
