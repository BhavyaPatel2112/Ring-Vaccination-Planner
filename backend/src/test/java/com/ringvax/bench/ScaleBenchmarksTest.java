package com.ringvax.bench;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.ringvax.data.SyntheticNetworkGenerator;
import com.ringvax.engine.dsu.DisjointSetUnion;
import com.ringvax.engine.graph.ContactGraph;

class ScaleBenchmarksTest {

    private final Harness quick = new Harness(0, 1);

    @Test
    void bfsBaselineCountsTheSameClustersAsUnionFind() {
        ContactGraph g = SyntheticNetworkGenerator.generate(500, 2, 9);
        DisjointSetUnion dsu = new DisjointSetUnion(g.personCount());
        for (int c = 0; c < g.contactCount(); c++) {
            dsu.union(g.contact(c).a(), g.contact(c).b());
        }

        assertEquals(dsu.setCount(), ScaleBenchmarks.bfsComponents(g));
    }

    @Test
    void everyExperimentProducesOneRowPerSize() {
        System.setProperty("ringvax.bench.quiet", "true");
        assertEquals(2, ScaleBenchmarks.approxVsGreedy(new int[] {500, 2000}, quick, 1).size());
        assertEquals(2, ScaleBenchmarks.dsuVsBfs(new int[] {500, 2000}, quick, 1).size());
        assertEquals(1, ScaleBenchmarks.bloomVsHashSet(new int[] {2000}, quick, 1).size());
    }

    @Test
    void bloomRowShowsTheMemorySaving() {
        System.setProperty("ringvax.bench.quiet", "true");
        String csv = ScaleBenchmarks.bloomVsHashSet(new int[] {5000}, quick, 1).toCsv();
        String[] row = csv.split("\n")[1].split(",");
        double ratio = Double.parseDouble(row[8]);

        assertTrue(ratio > 5, "memory ratio " + ratio);
    }
}
