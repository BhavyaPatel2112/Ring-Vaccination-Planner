package com.ringvax.engine.bloom;

import java.nio.charset.StandardCharsets;

/**
 * Bloom filter: a compact set that answers "definitely not present" or "possibly
 * present", using m bits and k hash functions.
 *
 * <pre>
 * add(x):          for i in 0..k-1: set bit  g_i(x)
 * mightContain(x): for i in 0..k-1: if bit g_i(x) is 0, return false   // definitely absent
 *                  return true                                         // possibly present
 *
 * g_i(x) = (h1(x) + i * h2(x)) mod m      (double hashing)
 * h1 = FNV-1a 64-bit, h2 = MurmurHash3 32-bit (made odd)
 * </pre>
 *
 * <p><b>No false negatives.</b> Adding x sets all k of its bits, and bits are
 * never cleared, so a later query for x always finds them set.
 *
 * <p><b>False positives.</b> A key that was never added can still find all k
 * bits set by other keys. After n insertions each bit is still 0 with
 * probability (1 - 1/m)^(kn) &asymp; e^(-kn/m), so the false positive rate is
 * <pre>p = (1 - e^(-kn/m))^k</pre>
 * It is smallest for k = (m/n) ln 2, which gives p &asymp; 0.6185^(m/n): about
 * 9.6 bits per key for 1%. That is why every "possibly present" answer in the
 * app is confirmed with an exact check before it is shown as final.
 *
 * <p><b>Double hashing</b> (Kirsch and Mitzenmacher, 2006): two good hashes are
 * enough to simulate k of them without losing accuracy, so each operation hashes
 * the key only twice.
 *
 * <p>Time complexity: O(k + L) for {@link #add} and {@link #mightContain} with a
 * key of L bytes. Space complexity: m bits (m / 8 bytes), whatever the keys are.
 */
public class BloomFilter {

    /** Largest filter allowed (about 250 MB of bits). */
    public static final long MAX_BITS = 2_000_000_000L;
    public static final int MAX_HASHES = 32;

    private final long[] words;
    private final long m;
    private final int k;
    private long inserted;
    private long setBits;

    /**
     * An empty filter with m bits and k hash functions.
     *
     * @throws IllegalArgumentException if m is not in 1..{@link #MAX_BITS} or k not in 1..{@link #MAX_HASHES}
     */
    public BloomFilter(long m, int k) {
        if (m < 1 || m > MAX_BITS) {
            throw new IllegalArgumentException("Number of bits m must be between 1 and " + MAX_BITS + ", got " + m);
        }
        if (k < 1 || k > MAX_HASHES) {
            throw new IllegalArgumentException("Number of hash functions k must be between 1 and " + MAX_HASHES
                    + ", got " + k);
        }
        this.m = m;
        this.k = k;
        this.words = new long[(int) ((m + 63) / 64)];
    }

    /**
     * A filter sized for n keys at the target false positive rate: m from
     * {@link #optimalBits(long, double)} and k from {@link #optimalHashCount(long, long)}.
     */
    public static BloomFilter forCapacity(long n, double falsePositiveRate) {
        long m = Math.max(64, optimalBits(Math.max(1, n), falsePositiveRate));
        return new BloomFilter(m, optimalHashCount(m, Math.max(1, n)));
    }

    /** Bits needed for n keys at false positive rate p: m = -n ln p / (ln 2)^2. */
    public static long optimalBits(long n, double p) {
        if (n < 1) {
            throw new IllegalArgumentException("n must be at least 1");
        }
        if (!(p > 0 && p < 1)) {
            throw new IllegalArgumentException("The false positive rate must be between 0 and 1");
        }
        return (long) Math.ceil(-n * Math.log(p) / (Math.log(2) * Math.log(2)));
    }

    /** Best number of hash functions for m bits and n keys: k = (m / n) ln 2, at least 1. */
    public static int optimalHashCount(long m, long n) {
        if (m < 1 || n < 1) {
            throw new IllegalArgumentException("m and n must be at least 1");
        }
        long k = Math.round((double) m / n * Math.log(2));
        return (int) Math.max(1, Math.min(MAX_HASHES, k));
    }

    /** Theoretical false positive rate (1 - e^(-kn/m))^k for k hashes, m bits and n keys. */
    public static double theoreticalRate(long m, int k, long n) {
        return Math.pow(1 - Math.exp(-(double) k * n / m), k);
    }

    /** Adds a key. Returns true if at least one bit changed (so the key was certainly new). */
    public boolean add(String key) {
        byte[] bytes = key.getBytes(StandardCharsets.UTF_8);
        long h1 = HashFunctions.fnv1a64(bytes);
        long h2 = (HashFunctions.murmur3(bytes, 0) & 0xffffffffL) | 1L;
        boolean changed = false;
        for (int i = 0; i < k; i++) {
            long bit = Math.floorMod(h1 + i * h2, m);
            int word = (int) (bit >>> 6);
            long mask = 1L << (bit & 63);
            if ((words[word] & mask) == 0) {
                words[word] |= mask;
                setBits++;
                changed = true;
            }
        }
        inserted++;
        return changed;
    }

    /**
     * The k bit positions of a key, in the order they are computed (they can repeat when
     * m is small). Used by the "How it works" screen to draw which bits a key touches.
     */
    public long[] positions(String key) {
        byte[] bytes = key.getBytes(StandardCharsets.UTF_8);
        long h1 = HashFunctions.fnv1a64(bytes);
        long h2 = (HashFunctions.murmur3(bytes, 0) & 0xffffffffL) | 1L;
        long[] positions = new long[k];
        for (int i = 0; i < k; i++) {
            positions[i] = Math.floorMod(h1 + i * h2, m);
        }
        return positions;
    }

    /** False means the key was definitely never added; true means it possibly was. */
    public boolean mightContain(String key) {
        byte[] bytes = key.getBytes(StandardCharsets.UTF_8);
        long h1 = HashFunctions.fnv1a64(bytes);
        long h2 = (HashFunctions.murmur3(bytes, 0) & 0xffffffffL) | 1L;
        for (int i = 0; i < k; i++) {
            long bit = Math.floorMod(h1 + i * h2, m);
            if ((words[(int) (bit >>> 6)] & (1L << (bit & 63))) == 0) {
                return false;
            }
        }
        return true;
    }

    /** Number of bits (m). */
    public long bitCount() {
        return m;
    }

    /** Number of hash functions (k). */
    public int hashCount() {
        return k;
    }

    /** Number of {@link #add} calls (n, counting repeats). */
    public long insertedCount() {
        return inserted;
    }

    /** Share of bits set to 1. */
    public double fillRatio() {
        return (double) setBits / m;
    }

    /** False positive rate predicted for the keys added so far. */
    public double expectedFalsePositiveRate() {
        return theoreticalRate(m, k, inserted);
    }

    /** Memory used by the bit array in bytes. */
    public long memoryBytes() {
        return (long) words.length * Long.BYTES;
    }
}
