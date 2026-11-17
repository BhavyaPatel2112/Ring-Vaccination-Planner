package com.ringvax.engine.sim;

import java.util.Arrays;
import java.util.Random;

import com.ringvax.engine.graph.ContactGraph;
import com.ringvax.engine.select.RandomizedQuickselect;

/**
 * Seeded outbreak simulation on the contact network (a discrete-time SIR
 * model), used to check that a vaccination plan actually prevents infections.
 *
 * <pre>
 * for each trial t (its own seed, derived from the main seed):
 *     everyone is Susceptible; the cases are Infected at step 0
 *     each vaccinated person is Immune with probability = efficacy
 *     repeat until nobody is infected (or maxSteps):
 *         for each infected person u, for each contact v of u:
 *             if v is susceptible and not immune: infect v with probability p
 *         people infected for recoverySteps steps become Recovered
 *     record the number of new infections in trial t
 * mean, median and 90th percentile over the trials (percentiles with randomized Quickselect)
 * </pre>
 *
 * <p>People infected during a step only start spreading on the next step, so
 * the order in which infected people are processed does not matter.
 *
 * <p><b>Fair comparisons.</b> Trial t uses the same seed for every plan
 * (common random numbers), so differences between plans come from the plans,
 * not from luck.
 *
 * <p>Time complexity: O(trials &times; steps &times; (V + E)) in the worst case;
 * in practice each person is infected at most once per trial, so a trial costs
 * O(V + sum of the degrees of the infected people &times; recoverySteps). Space
 * complexity: O(V + maxSteps).
 */
public class OutbreakSimulator {

    public static final int MAX_TRIALS = 2000;
    public static final int MAX_STEPS = 365;

    /**
     * @param transmissionProbability chance that an infected person infects a susceptible contact in one step
     * @param recoverySteps           steps a person stays infectious
     * @param efficacy                chance a vaccinated person is fully protected (1 = always)
     * @param trials                  number of simulated outbreaks
     * @param maxSteps                safety limit on the length of one outbreak
     * @param seed                    main seed; trial t uses a seed derived from it
     */
    public record Settings(double transmissionProbability, int recoverySteps, double efficacy, int trials,
                           int maxSteps, long seed) {

        public Settings {
            if (!(transmissionProbability >= 0 && transmissionProbability <= 1)) {
                throw new IllegalArgumentException("The transmission probability must be between 0 and 1");
            }
            if (recoverySteps < 1 || recoverySteps > 60) {
                throw new IllegalArgumentException("Recovery steps must be between 1 and 60");
            }
            if (!(efficacy >= 0 && efficacy <= 1)) {
                throw new IllegalArgumentException("Vaccine efficacy must be between 0 and 1");
            }
            if (trials < 1 || trials > MAX_TRIALS) {
                throw new IllegalArgumentException("Trials must be between 1 and " + MAX_TRIALS);
            }
            if (maxSteps < 1 || maxSteps > MAX_STEPS) {
                throw new IllegalArgumentException("Steps must be between 1 and " + MAX_STEPS);
            }
        }
    }

    /**
     * @param newInfections new infections in each trial (the cases themselves are not counted)
     * @param curve         average number of people infectious at each step
     */
    public record Result(int[] newInfections, double mean, int median, int p90, int min, int max, double[] curve,
                         long runtimeNanos) {
    }

    /** Minutes of contact that one "unit" of exposure stands for in {@link #durationWeighted}. */
    public static final double MINUTES_PER_EXPOSURE = 15.0;

    private final ContactGraph graph;
    private final Settings settings;
    private final double[] contactProbability;

    /** Every contact carries the same transmission probability, from the settings. */
    public OutbreakSimulator(ContactGraph graph, Settings settings) {
        this(graph, settings, null);
    }

    /**
     * Each contact c carries its own transmission probability {@code contactProbability[c]}
     * (indexed like {@link ContactGraph#contact(int)}); null means "use the settings' probability
     * for every contact". The settings' probability is then not used.
     */
    public OutbreakSimulator(ContactGraph graph, Settings settings, double[] contactProbability) {
        if (contactProbability != null) {
            if (contactProbability.length != graph.contactCount()) {
                throw new IllegalArgumentException("contactProbability must have one entry per contact");
            }
            for (double q : contactProbability) {
                if (!(q >= 0 && q <= 1)) {
                    throw new IllegalArgumentException("Contact probabilities must be between 0 and 1");
                }
            }
        }
        this.graph = graph;
        this.settings = settings;
        this.contactProbability = contactProbability;
    }

    /**
     * Transmission probabilities that grow with how long two people were together.
     * With p the chance of infection per 15 minutes of contact, a contact of d minutes has
     *
     * <pre>
     * q = 1 - (1 - p)^(d / 15)
     * </pre>
     *
     * the chance that at least one of d/15 independent 15-minute exposures transmits. A
     * 1-minute contact gets about p/15, a 15-minute one exactly p, and a full day close
     * to 1. A contact of unknown duration counts as 15 minutes. O(E) time and space.
     */
    public static double[] durationWeighted(ContactGraph graph, double pPer15Minutes) {
        if (!(pPer15Minutes >= 0 && pPer15Minutes <= 1)) {
            throw new IllegalArgumentException("The transmission probability must be between 0 and 1");
        }
        double[] q = new double[graph.contactCount()];
        for (int c = 0; c < q.length; c++) {
            int minutes = graph.contact(c).durationMinutes();
            double units = minutes > 0 ? minutes / MINUTES_PER_EXPOSURE : 1.0;
            q[c] = 1 - Math.pow(1 - pPer15Minutes, units);
        }
        return q;
    }

    /**
     * Runs all trials.
     *
     * @param cases      indices of the people infected at the start
     * @param vaccinated one entry per person: true if vaccinated (already, or by the plan)
     */
    public Result run(int[] cases, boolean[] vaccinated) {
        int n = graph.personCount();
        if (vaccinated.length != n) {
            throw new IllegalArgumentException("vaccinated must have one entry per person");
        }
        for (int c : cases) {
            if (c < 0 || c >= n) {
                throw new IllegalArgumentException("Case index " + c + " is out of range");
            }
        }
        long start = System.nanoTime();
        int[] totals = new int[settings.trials()];
        double[] curve = new double[settings.maxSteps() + 1];
        int longest = 0;

        byte[] state = new byte[n];
        int[] infectedAt = new int[n];
        boolean[] immune = new boolean[n];
        int[] current = new int[n];
        int[] next = new int[n];
        int[] born = new int[n];

        for (int t = 0; t < settings.trials(); t++) {
            long trialSeed = settings.seed() * 1_000_003L + t;
            Random spread = new Random(trialSeed);
            Random protection = new Random(~trialSeed);
            Arrays.fill(state, SUSCEPTIBLE);
            for (int p = 0; p < n; p++) {
                immune[p] = vaccinated[p] && (settings.efficacy() >= 1 || protection.nextDouble() < settings.efficacy());
            }

            int size = 0;
            for (int c : cases) {
                if (state[c] == SUSCEPTIBLE) {
                    state[c] = INFECTED;
                    infectedAt[c] = 0;
                    current[size++] = c;
                }
            }
            int infections = 0;
            int step = 0;
            curve[0] += size;
            while (size > 0 && step < settings.maxSteps()) {
                // Spread: people infected now go to `born` and only start spreading on the next step.
                int newly = 0;
                for (int i = 0; i < size; i++) {
                    int u = current[i];
                    for (int j = 0; j < graph.degree(u); j++) {
                        int v = graph.neighbour(u, j);
                        double q = contactProbability == null
                                ? settings.transmissionProbability()
                                : contactProbability[graph.contactAt(u, j)];
                        if (state[v] == SUSCEPTIBLE && !immune[v] && spread.nextDouble() < q) {
                            state[v] = INFECTED;
                            infectedAt[v] = step + 1;
                            born[newly++] = v;
                        }
                    }
                }
                // Recovery: keep the people who are still infectious, then add the new ones.
                int nextSize = 0;
                for (int i = 0; i < size; i++) {
                    int u = current[i];
                    if (step + 1 - infectedAt[u] >= settings.recoverySteps()) {
                        state[u] = RECOVERED;
                    } else {
                        next[nextSize++] = u;
                    }
                }
                System.arraycopy(born, 0, next, nextSize, newly);
                nextSize += newly;
                infections += newly;
                int[] swap = current;
                current = next;
                next = swap;
                size = nextSize;
                step++;
                curve[step] += size;
            }
            longest = Math.max(longest, step);
            totals[t] = infections;
        }

        double[] meanCurve = new double[longest + 1];
        for (int s = 0; s <= longest; s++) {
            meanCurve[s] = curve[s] / settings.trials();
        }
        double mean = Arrays.stream(totals).average().orElse(0);
        RandomizedQuickselect select = new RandomizedQuickselect(settings.seed());
        int median = select.percentile(totals.clone(), 50);
        int p90 = select.percentile(totals.clone(), 90);
        int min = Arrays.stream(totals).min().orElse(0);
        int max = Arrays.stream(totals).max().orElse(0);
        return new Result(totals, mean, median, p90, min, max, meanCurve, System.nanoTime() - start);
    }

    private static final byte SUSCEPTIBLE = 0;
    private static final byte INFECTED = 1;
    private static final byte RECOVERED = 2;
}
