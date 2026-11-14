package com.ringvax.engine.heap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

import org.junit.jupiter.api.Test;

class IndexedMaxHeapTest {

    @Test
    void extractsInDecreasingKeyOrder() {
        IndexedMaxHeap heap = new IndexedMaxHeap(5);
        heap.insert(0, 3);
        heap.insert(1, 9);
        heap.insert(2, 1);
        heap.insert(3, 7);

        assertEquals(1, heap.peekMax());
        assertEquals(1, heap.extractMax());
        assertEquals(3, heap.extractMax());
        assertEquals(0, heap.extractMax());
        assertEquals(2, heap.extractMax());
        assertTrue(heap.isEmpty());
    }

    @Test
    void decreaseKeyMovesAnItemDown() {
        IndexedMaxHeap heap = new IndexedMaxHeap(4);
        heap.insert(0, 10);
        heap.insert(1, 8);
        heap.insert(2, 6);

        heap.decreaseKey(0, 5);

        assertEquals(5, heap.keyOf(0));
        assertEquals(1, heap.extractMax());
        assertEquals(2, heap.extractMax());
        assertEquals(0, heap.extractMax());
    }

    @Test
    void increaseKeyMovesAnItemUp() {
        IndexedMaxHeap heap = new IndexedMaxHeap(3);
        heap.insert(0, 1);
        heap.insert(1, 2);
        heap.insert(2, 3);

        heap.increaseKey(0, 100);

        assertEquals(0, heap.peekMax());
    }

    @Test
    void extractFromEmptyHeapThrows() {
        IndexedMaxHeap heap = new IndexedMaxHeap(3);

        assertThrows(NoSuchElementException.class, heap::extractMax);
        assertThrows(NoSuchElementException.class, heap::peekMax);
        assertThrows(NoSuchElementException.class, () -> heap.decreaseKey(0, 1));
    }

    @Test
    void invalidOperationsAreRejected() {
        IndexedMaxHeap heap = new IndexedMaxHeap(2);
        heap.insert(0, 5);

        assertThrows(IllegalArgumentException.class, () -> heap.insert(0, 1), "already in the heap");
        assertThrows(IllegalArgumentException.class, () -> heap.decreaseKey(0, 6), "a larger key is not a decrease");
        assertThrows(IllegalArgumentException.class, () -> heap.increaseKey(0, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> heap.insert(2, 1));
        assertThrows(IllegalArgumentException.class, () -> new IndexedMaxHeap(-1));
    }

    @Test
    void equalKeysComeOutBySmallerItemFirst() {
        IndexedMaxHeap heap = new IndexedMaxHeap(4);
        heap.insert(3, 7);
        heap.insert(1, 7);
        heap.insert(2, 7);

        assertEquals(1, heap.extractMax());
        assertEquals(2, heap.extractMax());
        assertEquals(3, heap.extractMax());
    }

    @Test
    void removeTakesAnItemOutFromTheMiddle() {
        IndexedMaxHeap heap = new IndexedMaxHeap(6);
        for (int i = 0; i < 6; i++) {
            heap.insert(i, i * 10);
        }

        heap.remove(3);

        assertFalse(heap.contains(3));
        assertEquals(5, heap.size());
        assertEquals(5, heap.extractMax());
        assertEquals(4, heap.extractMax());
        assertEquals(2, heap.extractMax());
    }

    @Test
    void longRandomSequenceMatchesASortedReference() {
        Random random = new Random(2026);
        int n = 2000;
        IndexedMaxHeap heap = new IndexedMaxHeap(n);
        long[] key = new long[n];
        for (int i = 0; i < n; i++) {
            key[i] = random.nextInt(500);
            heap.insert(i, key[i]);
        }
        // Lower a third of the keys and raise another third, as greedy and the deadline list do.
        for (int i = 0; i < n; i++) {
            if (i % 3 == 0) {
                key[i] -= random.nextInt(100);
                heap.decreaseKey(i, key[i]);
            } else if (i % 3 == 1) {
                key[i] += random.nextInt(100);
                heap.increaseKey(i, key[i]);
            }
        }
        List<Integer> expected = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            expected.add(i);
        }
        expected.sort(Comparator.comparingLong((Integer i) -> -key[i]).thenComparingInt(i -> i));

        for (int item : expected) {
            assertEquals(item, heap.extractMax());
        }
        assertTrue(heap.isEmpty());
    }
}
