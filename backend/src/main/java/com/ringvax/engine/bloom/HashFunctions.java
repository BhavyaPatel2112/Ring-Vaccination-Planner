package com.ringvax.engine.bloom;

import java.nio.charset.StandardCharsets;

/**
 * The two hash functions the Bloom filter combines, written from their
 * published definitions.
 *
 * <ul>
 *   <li><b>FNV-1a, 64-bit</b>: start from the offset basis, then for every byte
 *       XOR it in and multiply by the FNV prime. Simple and fast; each byte
 *       changes the whole state.</li>
 *   <li><b>MurmurHash3, x86 32-bit</b> (Austin Appleby): mixes the input four
 *       bytes at a time with multiply, rotate and XOR steps, then a final
 *       "avalanche" so every input bit affects every output bit.</li>
 * </ul>
 *
 * <p>The two are built differently, so they behave like independent hashes,
 * which is what double hashing in {@link BloomFilter} needs.
 *
 * <p>Time complexity: O(L) for an input of L bytes. Space complexity: O(1).
 */
public final class HashFunctions {

    static final long FNV_OFFSET_BASIS = 0xcbf29ce484222325L;
    static final long FNV_PRIME = 0x100000001b3L;

    private static final int C1 = 0xcc9e2d51;
    private static final int C2 = 0x1b873593;

    private HashFunctions() {
    }

    /** FNV-1a 64-bit hash of the UTF-8 bytes of a string. */
    public static long fnv1a64(String text) {
        return fnv1a64(text.getBytes(StandardCharsets.UTF_8));
    }

    /** FNV-1a 64-bit hash. */
    public static long fnv1a64(byte[] data) {
        long hash = FNV_OFFSET_BASIS;
        for (byte b : data) {
            hash ^= (b & 0xff);
            hash *= FNV_PRIME;
        }
        return hash;
    }

    /** MurmurHash3 (x86, 32-bit) of the UTF-8 bytes of a string. */
    public static int murmur3(String text, int seed) {
        return murmur3(text.getBytes(StandardCharsets.UTF_8), seed);
    }

    /** MurmurHash3 (x86, 32-bit). */
    public static int murmur3(byte[] data, int seed) {
        int h = seed;
        int length = data.length;
        int blocks = length / 4;

        // Body: four bytes at a time, little-endian.
        for (int i = 0; i < blocks; i++) {
            int j = i * 4;
            int k = (data[j] & 0xff) | (data[j + 1] & 0xff) << 8 | (data[j + 2] & 0xff) << 16 | (data[j + 3] & 0xff) << 24;
            k *= C1;
            k = Integer.rotateLeft(k, 15);
            k *= C2;
            h ^= k;
            h = Integer.rotateLeft(h, 13);
            h = h * 5 + 0xe6546b64;
        }

        // Tail: the last 1 to 3 bytes.
        int tail = blocks * 4;
        int k = 0;
        switch (length & 3) {
            case 3:
                k ^= (data[tail + 2] & 0xff) << 16;
                // fall through
            case 2:
                k ^= (data[tail + 1] & 0xff) << 8;
                // fall through
            case 1:
                k ^= data[tail] & 0xff;
                k *= C1;
                k = Integer.rotateLeft(k, 15);
                k *= C2;
                h ^= k;
                break;
            default:
                break;
        }

        // Finalisation ("fmix32"): makes every input bit affect every output bit.
        h ^= length;
        h ^= h >>> 16;
        h *= 0x85ebca6b;
        h ^= h >>> 13;
        h *= 0xc2b2ae35;
        h ^= h >>> 16;
        return h;
    }
}
