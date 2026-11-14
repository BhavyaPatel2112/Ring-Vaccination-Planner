package com.ringvax.engine.dsu;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.ringvax.engine.graph.ContactGraph;

/**
 * Outbreak clusters of a contact network: groups of people connected by a chain
 * of contacts, kept in a {@link DisjointSetUnion} so that a new contact report
 * merges clusters in near-constant time.
 *
 * <p>Built once from the graph with one {@code union} per contact. After that,
 * {@link #addContact(int, int)} keeps it up to date; nothing is recomputed.
 * Cluster statistics (size, cases, already vaccinated) are read with one pass
 * over the people.
 *
 * <p>Time complexity: O(V + E &alpha;(V)) to build, O(&alpha;(V)) per new contact,
 * O(V &alpha;(V) + C log C) for {@link #summarize(boolean[])} with C clusters.
 * Space complexity: O(V).
 */
public class OutbreakClusters {

    /** One cluster with its statistics. {@code members} are person indices in index order. */
    public record Cluster(int root, int size, int cases, int vaccinated, int[] members) {
    }

    private final ContactGraph graph;
    private final DisjointSetUnion dsu;

    public OutbreakClusters(ContactGraph graph) {
        this.graph = graph;
        this.dsu = new DisjointSetUnion(graph.personCount());
        for (int c = 0; c < graph.contactCount(); c++) {
            dsu.union(graph.contact(c).a(), graph.contact(c).b());
        }
    }

    /**
     * Records a new contact between two people that is already in the graph.
     *
     * @return true if it joined two clusters that were separate before
     */
    public boolean addContact(int a, int b) {
        return dsu.union(a, b);
    }

    public boolean sameCluster(int a, int b) {
        return dsu.connected(a, b);
    }

    public int clusterSize(int person) {
        return dsu.size(person);
    }

    /** Root of the person's cluster; stable until the cluster is merged with another. */
    public int clusterOf(int person) {
        return dsu.find(person);
    }

    /** Number of clusters, counting people with no contacts. */
    public int clusterCount() {
        return dsu.setCount();
    }

    /**
     * All clusters with their statistics: clusters containing a case first, then
     * by size (largest first), then by root.
     *
     * @param isCase one entry per person, true for confirmed cases
     */
    public List<Cluster> summarize(boolean[] isCase) {
        int n = graph.personCount();
        if (isCase.length != n) {
            throw new IllegalArgumentException("isCase must have one entry per person");
        }
        int[] slot = new int[n];
        java.util.Arrays.fill(slot, -1);
        List<int[]> stats = new ArrayList<>(); // root, size, cases, vaccinated
        List<List<Integer>> members = new ArrayList<>();
        for (int p = 0; p < n; p++) {
            int root = dsu.find(p);
            if (slot[root] < 0) {
                slot[root] = stats.size();
                stats.add(new int[] {root, 0, 0, 0});
                members.add(new ArrayList<>());
            }
            int[] s = stats.get(slot[root]);
            s[1]++;
            if (isCase[p]) {
                s[2]++;
            }
            if (graph.person(p).vaccinated()) {
                s[3]++;
            }
            members.get(slot[root]).add(p);
        }
        List<Cluster> clusters = new ArrayList<>();
        for (int i = 0; i < stats.size(); i++) {
            int[] s = stats.get(i);
            int[] m = members.get(i).stream().mapToInt(Integer::intValue).toArray();
            clusters.add(new Cluster(s[0], s[1], s[2], s[3], m));
        }
        clusters.sort(Comparator.comparing((Cluster c) -> c.cases() == 0)
                .thenComparing(Comparator.comparingInt(Cluster::size).reversed())
                .thenComparingInt(Cluster::root));
        return clusters;
    }
}
