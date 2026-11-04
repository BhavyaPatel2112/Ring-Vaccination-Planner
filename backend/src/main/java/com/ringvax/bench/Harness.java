package com.ringvax.bench;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.ringvax.engine.select.RandomizedQuickselect;

/**
 * Times a task the same way for every experiment: a few warm-up runs so the JIT
 * compiler has optimised the code, then several measured runs, reporting the
 * median (robust against one slow run caused by garbage collection).
 *
 * <p>Input preparation is not timed: {@code prepare} builds a fresh input for
 * each run (selection reorders its array in place, so every run needs its own copy).
 */
public final class Harness {

    /** Run times of one measurement, in nanoseconds. */
    public record Measurement(long[] runs, long median, long min, long max) {

        public double medianMs() {
            return median / 1e6;
        }

        public double minMs() {
            return min / 1e6;
        }

        public double maxMs() {
            return max / 1e6;
        }
    }

    private final int warmup;
    private final int repeats;

    /**
     * @param warmup  untimed runs before measuring (0 or more)
     * @param repeats measured runs (at least 1; the report uses 5 or more)
     */
    public Harness(int warmup, int repeats) {
        if (warmup < 0 || repeats < 1) {
            throw new IllegalArgumentException("warmup must be >= 0 and repeats >= 1");
        }
        this.warmup = warmup;
        this.repeats = repeats;
    }

    public <T> Measurement measure(Supplier<T> prepare, Consumer<T> task) {
        for (int i = 0; i < warmup; i++) {
            task.accept(prepare.get());
        }
        long[] runs = new long[repeats];
        for (int i = 0; i < repeats; i++) {
            T input = prepare.get();
            long start = System.nanoTime();
            task.accept(input);
            runs[i] = System.nanoTime() - start;
        }
        return summarise(runs);
    }

    /** Median (with our own Quickselect), minimum and maximum of a set of run times. */
    public static Measurement summarise(long[] runs) {
        long[] copy = runs.clone();
        long median = new RandomizedQuickselect(1).median(copy);
        long min = Arrays.stream(runs).min().orElseThrow();
        long max = Arrays.stream(runs).max().orElseThrow();
        return new Measurement(runs.clone(), median, min, max);
    }
}
