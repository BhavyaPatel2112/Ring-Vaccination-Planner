package com.ringvax.engine.bloom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

/** Checks both hashes against the published test vectors. */
class HashFunctionsTest {

    @Test
    void fnv1aMatchesTheReferenceValues() {
        assertEquals(0xcbf29ce484222325L, HashFunctions.fnv1a64(""), "empty input gives the offset basis");
        assertEquals(0xaf63dc4c8601ec8cL, HashFunctions.fnv1a64("a"));
        assertEquals(0x85944171f73967e8L, HashFunctions.fnv1a64("foobar"));
    }

    @Test
    void murmur3MatchesTheReferenceValues() {
        assertEquals(0, HashFunctions.murmur3("", 0));
        assertEquals(0x514e28b7, HashFunctions.murmur3("", 1));
        assertEquals(0x248bfa47, HashFunctions.murmur3("hello", 0));
        assertEquals(0x2e4ff723, HashFunctions.murmur3("The quick brown fox jumps over the lazy dog", 0));
    }

    @Test
    void murmur3HandlesEveryTailLength() {
        // Inputs of length 1, 2, 3 and 4 exercise each branch of the tail switch.
        assertEquals(0x3c2569b2, HashFunctions.murmur3("a", 0));
        assertEquals(0x9bbfd75f, HashFunctions.murmur3("ab", 0));
        assertEquals(0xb3dd93fa, HashFunctions.murmur3("abc", 0));
        assertEquals(0x43ed676a, HashFunctions.murmur3("abcd", 0));
    }

    @Test
    void seedChangesTheMurmurHash() {
        assertNotEquals(HashFunctions.murmur3("V001|2026-11-01T16", 0), HashFunctions.murmur3("V001|2026-11-01T16", 42));
    }

    @Test
    void similarKeysGetDifferentHashes() {
        Set<Long> fnv = new HashSet<>();
        Set<Integer> murmur = new HashSet<>();
        for (int hour = 0; hour < 24; hour++) {
            String key = "V001|2026-11-01T" + hour;
            fnv.add(HashFunctions.fnv1a64(key));
            murmur.add(HashFunctions.murmur3(key, 0));
        }
        assertEquals(24, fnv.size());
        assertEquals(24, murmur.size());
        assertTrue(HashFunctions.fnv1a64("P001") != HashFunctions.fnv1a64("P010"));
    }
}
