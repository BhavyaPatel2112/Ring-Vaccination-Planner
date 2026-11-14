package com.ringvax.engine.heap;

import java.util.NoSuchElementException;

/**
 * Indexed binary max-heap: a priority queue of items 0 to capacity - 1, each with
 * a {@code long} key, that can change an item's key while it is in the heap.
 *
 * <p>Three arrays do the work:
 * <ul>
 *   <li>{@code heap[i]}: the item at position i of the binary heap (children of i
 *       are at 2i + 1 and 2i + 2);</li>
 *   <li>{@code pos[item]}: where the item currently sits in {@code heap}, or -1
 *       if it is not in the heap. This index is what makes
 *       {@link #decreaseKey} possible without searching;</li>
 *   <li>{@code key[item]}: the item's priority.</li>
 * </ul>
 *
 * <pre>
 * insert(item, k):   put item at the end, key[item] = k, sift it up
 * extractMax():      take heap[0], move the last item to the top, sift it down
 * decreaseKey(i, k): key[i] = k (k &lt;= old key), sift i down from pos[i]
 * increaseKey(i, k): key[i] = k (k &gt;= old key), sift i up from pos[i]
 * </pre>
 *
 * <p>Ties are broken by the smaller item number, so the order is fully
 * deterministic (the greedy plan and the deadline list never depend on chance).
 *
 * <p>Used by the greedy "most connected first" vertex cover (keys are the number
 * of unprotected links a person would cover) and by the "vaccinate by" deadline
 * list (keys are minus the deadline, so the earliest deadline comes out first).
 *
 * <p>Time complexity: O(log n) for {@link #insert}, {@link #extractMax},
 * {@link #decreaseKey} and {@link #increaseKey}; O(1) for {@link #peekMax},
 * {@link #contains} and {@link #keyOf}. Space complexity: O(capacity).
 */
public class IndexedMaxHeap {

    private final int[] heap;
    private final int[] pos;
    private final long[] key;
    private int size;

    /** An empty heap for items 0 to capacity - 1. */
    public IndexedMaxHeap(int capacity) {
        if (capacity < 0) {
            throw new IllegalArgumentException("Capacity must be 0 or more, got " + capacity);
        }
        heap = new int[capacity];
        pos = new int[capacity];
        key = new long[capacity];
        java.util.Arrays.fill(pos, -1);
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean contains(int item) {
        checkItem(item);
        return pos[item] >= 0;
    }

    /** Current key of an item in the heap. */
    public long keyOf(int item) {
        checkInHeap(item);
        return key[item];
    }

    /**
     * Adds an item with the given key.
     *
     * @throws IllegalArgumentException if the item is already in the heap
     */
    public void insert(int item, long k) {
        checkItem(item);
        if (pos[item] >= 0) {
            throw new IllegalArgumentException("Item " + item + " is already in the heap");
        }
        heap[size] = item;
        pos[item] = size;
        key[item] = k;
        size++;
        siftUp(size - 1);
    }

    /** The item with the largest key, without removing it. */
    public int peekMax() {
        if (size == 0) {
            throw new NoSuchElementException("The heap is empty");
        }
        return heap[0];
    }

    /** Removes and returns the item with the largest key. */
    public int extractMax() {
        int top = peekMax();
        size--;
        if (size > 0) {
            move(heap[size], 0);
            siftDown(0);
        }
        pos[top] = -1;
        return top;
    }

    /**
     * Lowers an item's key and restores the heap order.
     *
     * @throws IllegalArgumentException if the new key is larger than the current one
     */
    public void decreaseKey(int item, long newKey) {
        checkInHeap(item);
        if (newKey > key[item]) {
            throw new IllegalArgumentException("decreaseKey: new key " + newKey + " is larger than " + key[item]);
        }
        key[item] = newKey;
        siftDown(pos[item]);
    }

    /**
     * Raises an item's key and restores the heap order.
     *
     * @throws IllegalArgumentException if the new key is smaller than the current one
     */
    public void increaseKey(int item, long newKey) {
        checkInHeap(item);
        if (newKey < key[item]) {
            throw new IllegalArgumentException("increaseKey: new key " + newKey + " is smaller than " + key[item]);
        }
        key[item] = newKey;
        siftUp(pos[item]);
    }

    /** Removes an item wherever it is in the heap. */
    public void remove(int item) {
        checkInHeap(item);
        int i = pos[item];
        size--;
        if (i != size) {
            int last = heap[size];
            move(last, i);
            siftUp(i);
            siftDown(pos[last]);
        }
        pos[item] = -1;
    }

    // ------------------------------------------------------------------ internals

    /** True if the item at heap position i should be above the item at position j. */
    private boolean above(int i, int j) {
        int a = heap[i];
        int b = heap[j];
        return key[a] > key[b] || (key[a] == key[b] && a < b);
    }

    private void siftUp(int i) {
        while (i > 0) {
            int parent = (i - 1) / 2;
            if (!above(i, parent)) {
                return;
            }
            swap(i, parent);
            i = parent;
        }
    }

    private void siftDown(int i) {
        while (true) {
            int left = 2 * i + 1;
            if (left >= size) {
                return;
            }
            int right = left + 1;
            int best = right < size && above(right, left) ? right : left;
            if (!above(best, i)) {
                return;
            }
            swap(i, best);
            i = best;
        }
    }

    private void swap(int i, int j) {
        int a = heap[i];
        int b = heap[j];
        move(a, j);
        move(b, i);
    }

    private void move(int item, int position) {
        heap[position] = item;
        pos[item] = position;
    }

    private void checkItem(int item) {
        if (item < 0 || item >= pos.length) {
            throw new IndexOutOfBoundsException("Item " + item + " out of range 0.." + (pos.length - 1));
        }
    }

    private void checkInHeap(int item) {
        checkItem(item);
        if (pos[item] < 0) {
            throw new NoSuchElementException("Item " + item + " is not in the heap");
        }
    }
}
