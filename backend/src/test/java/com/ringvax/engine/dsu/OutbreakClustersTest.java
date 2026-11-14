package com.ringvax.engine.dsu;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.ringvax.engine.graph.ContactGraph;
import com.ringvax.engine.graph.Person;

class OutbreakClustersTest {

    private static final LocalDate DAY = LocalDate.of(2026, 11, 1);

    /** Two groups {0,1,2} and {3,4}, plus 5 with no contacts. Person 4 is vaccinated. */
    private static ContactGraph twoGroups() {
        ContactGraph g = new ContactGraph();
        for (int i = 0; i < 6; i++) {
            g.addPerson(new Person("P" + i, "", null, "", i == 4));
        }
        g.addContact(0, 1, DAY, 30);
        g.addContact(1, 2, DAY, 30);
        g.addContact(3, 4, DAY, 30);
        return g;
    }

    @Test
    void summaryCountsSizeCasesAndVaccinated() {
        OutbreakClusters clusters = new OutbreakClusters(twoGroups());
        boolean[] isCase = {false, false, false, true, false, false};

        List<OutbreakClusters.Cluster> list = clusters.summarize(isCase);

        assertEquals(3, list.size());
        OutbreakClusters.Cluster first = list.get(0);
        assertEquals(2, first.size(), "the cluster with the case comes first, even though it is smaller");
        assertEquals(1, first.cases());
        assertEquals(1, first.vaccinated());
        assertArrayEquals(new int[] {3, 4}, first.members());
        assertEquals(3, list.get(1).size());
        assertEquals(0, list.get(1).cases());
        assertEquals(1, list.get(2).size(), "person 5 has no contacts");
    }

    @Test
    void liveContactMergesClusters() {
        ContactGraph g = twoGroups();
        OutbreakClusters clusters = new OutbreakClusters(g);
        assertFalse(clusters.sameCluster(2, 3));

        g.addContact(2, 3, DAY, 20);
        assertTrue(clusters.addContact(2, 3));

        assertTrue(clusters.sameCluster(0, 4));
        assertEquals(5, clusters.clusterSize(0));
        assertEquals(2, clusters.clusterCount());
    }

    @Test
    void contactInsideAClusterChangesNothing() {
        OutbreakClusters clusters = new OutbreakClusters(twoGroups());

        assertFalse(clusters.addContact(0, 2));
        assertEquals(3, clusters.clusterCount());
    }

    @Test
    void networkWithoutContactsHasOneClusterPerPerson() {
        ContactGraph g = new ContactGraph();
        g.addPerson(new Person("A", "", null, "", false));
        g.addPerson(new Person("B", "", null, "", false));

        List<OutbreakClusters.Cluster> list = new OutbreakClusters(g).summarize(new boolean[2]);

        assertEquals(2, list.size());
        assertEquals(1, list.get(0).size());
    }

    @Test
    void caseFlagsMustMatchThePeople() {
        OutbreakClusters clusters = new OutbreakClusters(twoGroups());

        assertThrows(IllegalArgumentException.class, () -> clusters.summarize(new boolean[2]));
    }
}
