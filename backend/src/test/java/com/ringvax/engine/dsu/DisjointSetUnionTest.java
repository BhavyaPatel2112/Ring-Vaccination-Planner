package com.ringvax.engine.dsu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

class DisjointSetUnionTest {

    @Test
    void singleElementIsItsOwnSet() {
        DisjointSetUnion dsu = new DisjointSetUnion(1);

        assertEquals(0, dsu.find(0));
        assertEquals(1, dsu.size(0));
        assertEquals(1, dsu.setCount());
    }

    @Test
    void emptyStructureIsAllowedButHasNoElements() {
        DisjointSetUnion dsu = new DisjointSetUnion(0);

        assertEquals(0, dsu.setCount());
        assertThrows(IndexOutOfBoundsException.class, () -> dsu.find(0));
        assertThrows(IllegalArgumentException.class, () -> new DisjointSetUnion(-1));
    }

    @Test
    void unionThenFindPutsBothInTheSameSet() {
        DisjointSetUnion dsu = new DisjointSetUnion(5);

        assertTrue(dsu.union(1, 3));

        assertEquals(dsu.find(1), dsu.find(3));
        assertTrue(dsu.connected(3, 1));
        assertFalse(dsu.connected(1, 2));
        assertEquals(2, dsu.size(1));
        assertEquals(4, dsu.setCount());
    }

    @Test
    void unionOfAnAlreadyJoinedPairIsANoOp() {
        DisjointSetUnion dsu = new DisjointSetUnion(4);
        dsu.union(0, 1);
        dsu.union(1, 2);
        int root = dsu.find(0);

        assertFalse(dsu.union(2, 0), "0 and 2 are already connected through 1");
        assertFalse(dsu.union(1, 1), "a person is always in their own set");
        assertEquals(root, dsu.find(2));
        assertEquals(3, dsu.size(0));
        assertEquals(2, dsu.setCount());
    }

    @Test
    void chainOfUnionsGivesOneCluster() {
        int n = 1000;
        DisjointSetUnion dsu = new DisjointSetUnion(n);
        for (int i = 0; i + 1 < n; i++) {
            assertTrue(dsu.union(i, i + 1));
        }

        assertEquals(1, dsu.setCount());
        assertEquals(n, dsu.size(0));
        assertTrue(dsu.connected(0, n - 1));
    }

    @Test
    void unionByRankKeepsTreesLogarithmic() {
        // Merging equal-sized sets pairwise is the case that grows rank fastest.
        int n = 1 << 10;
        DisjointSetUnion dsu = new DisjointSetUnion(n);
        for (int step = 1; step < n; step *= 2) {
            for (int i = 0; i + step < n; i += 2 * step) {
                dsu.union(i, i + step);
            }
        }
        int root = dsu.find(0);

        assertEquals(n, dsu.size(root));
        assertTrue(dsu.rankOf(root) <= 10, "rank is at most log2(n)");
    }

    @Test
    void outOfRangeElementsAreRejected() {
        DisjointSetUnion dsu = new DisjointSetUnion(3);

        assertThrows(IndexOutOfBoundsException.class, () -> dsu.find(3));
        assertThrows(IndexOutOfBoundsException.class, () -> dsu.union(-1, 0));
    }

    @Test
    void clusterSizesMatchABreadthFirstSearchReferenceOnRandomGraphs() {
        for (long seed = 1; seed <= 40; seed++) {
            Random random = new Random(seed);
            int n = 5 + random.nextInt(80);
            int m = random.nextInt(n * 2);
            List<List<Integer>> adj = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                adj.add(new ArrayList<>());
            }
            DisjointSetUnion dsu = new DisjointSetUnion(n);
            for (int e = 0; e < m; e++) {
                int a = random.nextInt(n);
                int b = random.nextInt(n);
                adj.get(a).add(b);
                adj.get(b).add(a);
                dsu.union(a, b);
            }

            int[] component = bfsComponents(adj);
            int[] componentSize = new int[n];
            int components = 0;
            for (int c : component) {
                if (componentSize[c]++ == 0) {
                    components++;
                }
            }
            assertEquals(components, dsu.setCount(), "seed " + seed);
            for (int a = 0; a < n; a++) {
                assertEquals(componentSize[component[a]], dsu.size(a), "seed " + seed);
                for (int b = 0; b < n; b++) {
                    assertEquals(component[a] == component[b], dsu.connected(a, b), "seed " + seed);
                }
            }
        }
    }

    private static int[] bfsComponents(List<List<Integer>> adj) {
        int n = adj.size();
        int[] component = new int[n];
        java.util.Arrays.fill(component, -1);
        int next = 0;
        for (int s = 0; s < n; s++) {
            if (component[s] >= 0) {
                continue;
            }
            ArrayDeque<Integer> queue = new ArrayDeque<>();
            queue.add(s);
            component[s] = next;
            while (!queue.isEmpty()) {
                int u = queue.poll();
                for (int v : adj.get(u)) {
                    if (component[v] < 0) {
                        component[v] = next;
                        queue.add(v);
                    }
                }
            }
            next++;
        }
        return component;
    }
}
