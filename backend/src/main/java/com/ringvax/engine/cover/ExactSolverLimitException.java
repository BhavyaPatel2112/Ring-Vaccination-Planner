package com.ringvax.engine.cover;

/**
 * Thrown when the exact solver will not or could not finish: the problem has too
 * many people, or the search ran past its time limit. The request was valid, so
 * the API answers 422 and suggests the approximation instead.
 */
public class ExactSolverLimitException extends RuntimeException {

    public enum Kind {
        TOO_LARGE,
        TIMED_OUT
    }

    private final Kind kind;

    private ExactSolverLimitException(Kind kind, String message) {
        super(message);
        this.kind = kind;
    }

    static ExactSolverLimitException tooLarge(int people, int limit) {
        return new ExactSolverLimitException(Kind.TOO_LARGE,
                "The exact solver handles at most " + limit + " people on the links to protect, but this plan has "
                        + people + ". Use the approximation, which is guaranteed to be at most 2 times the minimum.");
    }

    static ExactSolverLimitException timedOut(long millis) {
        return new ExactSolverLimitException(Kind.TIMED_OUT,
                "The exact solver could not prove the minimum within " + millis + " ms. "
                        + "Use the approximation, which is guaranteed to be at most 2 times the minimum.");
    }

    public Kind kind() {
        return kind;
    }
}
