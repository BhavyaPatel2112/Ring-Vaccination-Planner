package com.ringvax.engine.dsu;

import java.util.Arrays;

/**
 * Disjoint-set union (union-find): keeps track of which people are linked by a
 * chain of contacts, so outbreak clusters can be read and updated instantly as
 * new contact reports arrive.
 *
 * <pre>
 * find(x):                      // root of x's set, with path compression
 *     root = x
 *     while parent[root] != root: root = parent[root]
 *     while parent[x] != root:   // second pass: point everyone on the path at the root
 *         next = parent[x]; parent[x] = root; x = next
 *     return root
 *
 * union(a, b):                  // union by rank
 *     ra = find(a), rb = find(b)
 *     if ra == rb: return false  // already in the same cluster
 *     attach the root with the smaller rank under the other one
 *     if the ranks were equal: rank of the new root += 1
 *     size[new root] += size[other root]
 *     return true
 * </pre>
 *
 * <p><b>Why it is fast.</b> Union by rank keeps every tree at most log2(n) high,
 * because a root's rank only grows when two trees of equal rank meet, and a tree
 * of rank r has at least 2^r members. Path compression then flattens each path
 * it walks. Together they give an amortised cost of O(&alpha;(n)) per operation,
 * where &alpha; is the inverse Ackermann function: at most 4 for any n that fits
 * in memory, so in practice constant time.
 *
 * <p>Baseline: recomputing connected components with a breadth-first search
 * after every new contact costs O(V + E) per update.
 *
 * <p>Time complexity: O(&alpha;(n)) amortised per {@link #find}, {@link #union}
 * and {@link #connected}; O(n) to create. Space complexity: O(n) (three int
 * arrays).
 */
public class DisjointSetUnion {

    private final int[] parent;
    private final int[] rank;
    private final int[] size;
    private int sets;

    /** Creates n single-person sets, numbered 0 to n - 1. */
    public DisjointSetUnion(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("Size must be 0 or more, got " + n);
        }
        parent = new int[n];
        rank = new int[n];
        size = new int[n];
        for (int i = 0; i < n; i++) {
            parent[i] = i;
        }
        Arrays.fill(size, 1);
        sets = n;
    }

    /** Number of elements. */
    public int elementCount() {
        return parent.length;
    }

    /** Number of separate sets (clusters, counting people with no contacts as their own cluster). */
    public int setCount() {
        return sets;
    }

    /** Root (representative) of x's set. Two elements are in the same set exactly when their roots are equal. */
    public int find(int x) {
        check(x);
        int root = x;
        while (parent[root] != root) {
            root = parent[root];
        }
        // Path compression: every element on the path now points straight at the root.
        while (parent[x] != root) {
            int next = parent[x];
            parent[x] = root;
            x = next;
        }
        return root;
    }

    /**
     * Joins the sets of a and b.
     *
     * @return true if two different sets were merged, false if a and b were already together
     */
    public boolean union(int a, int b) {
        int ra = find(a);
        int rb = find(b);
        if (ra == rb) {
            return false;
        }
        // Union by rank: the shallower tree goes under the deeper one, so heights stay logarithmic.
        if (rank[ra] < rank[rb]) {
            int t = ra;
            ra = rb;
            rb = t;
        }
        parent[rb] = ra;
        if (rank[ra] == rank[rb]) {
            rank[ra]++;
        }
        size[ra] += size[rb];
        sets--;
        return true;
    }

    /** True if a and b are in the same set. */
    public boolean connected(int a, int b) {
        return find(a) == find(b);
    }

    /** Number of elements in x's set. */
    public int size(int x) {
        return size[find(x)];
    }

    /** Rank of a root (an upper bound on its tree height). Exposed for tests and the viva demo. */
    int rankOf(int root) {
        check(root);
        return rank[root];
    }

    private void check(int x) {
        if (x < 0 || x >= parent.length) {
            throw new IndexOutOfBoundsException("Element " + x + " out of range 0.." + (parent.length - 1));
        }
    }
}
