package com.ringvax.engine.cover;

/**
 * A method that picks people to vaccinate so that every target link has at least
 * one vaccinated endpoint (a vertex cover of the target links).
 *
 * <p>Implementations: {@link ApproxVertexCover} (2-approximation), the greedy
 * "most connected first" baseline and the exact solver. All of them must:
 * <ul>
 *   <li>never choose a person the problem excludes;</li>
 *   <li>include every person in {@link CoverProblem#forcedPeople()};</li>
 *   <li>cover every link in {@link CoverProblem#freeContacts()};</li>
 *   <li>give each chosen person a {@link CoverChoice} with a reason.</li>
 * </ul>
 * {@link CoverProblem#isValidCover(int[])} checks the first three.
 */
public interface VertexCoverSolver {

    /** Short method name used in the API, for example "APPROX". */
    String name();

    CoverResult solve(CoverProblem problem);
}
