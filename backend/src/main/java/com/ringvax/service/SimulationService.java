package com.ringvax.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import org.springframework.stereotype.Service;

import com.ringvax.api.dto.SimulateRequest;
import com.ringvax.api.dto.SimulateResponse;
import com.ringvax.engine.cover.ApproxVertexCover;
import com.ringvax.engine.cover.GreedyVertexCover;
import com.ringvax.engine.graph.ContactGraph;
import com.ringvax.engine.plan.VaccinationPlan;
import com.ringvax.engine.plan.VaccinationPlanner;
import com.ringvax.engine.ring.RingBuilder;
import com.ringvax.engine.ring.RingResult;
import com.ringvax.engine.sim.OutbreakSimulator;

/**
 * Outbreak simulation (feature 11): runs the same seeded outbreaks with no
 * vaccination, random vaccination, the approximation plan and the greedy plan.
 * Random vaccination gets exactly as many doses as the approximation plan, so
 * the comparison shows the value of choosing who gets the doses.
 */
@Service
public class SimulationService {

    /** Limit on trials x (people + contacts) per strategy, to keep a request under a few seconds. */
    public static final long MAX_WORK = 30_000_000L;

    private final NetworkService networks;

    public SimulationService(NetworkService networks) {
        this.networks = networks;
    }

    public SimulateResponse simulate(String networkId, SimulateRequest request) {
        long start = System.nanoTime();
        ContactGraph g = networks.get(networkId).graph();
        double p = request.transmission() == null ? 0.08 : request.transmission();
        int recovery = request.recoverySteps() == null ? 4 : request.recoverySteps();
        int trials = request.trials() == null ? 200 : request.trials();
        double efficacy = request.efficacy() == null ? 1.0 : request.efficacy();
        long seed = request.seed() == null ? PlanService.DEFAULT_SEED : request.seed();
        long size = (long) g.personCount() + g.contactCount();
        if (trials * size > MAX_WORK) {
            throw new IllegalArgumentException("Too many trials for a network this size: use at most "
                    + Math.max(1, MAX_WORK / size) + " trials");
        }

        RingResult ring = new RingBuilder(g).build(request.caseIds(),
                request.depth() == null ? RingBuilder.DEFAULT_DEPTH : request.depth());
        int[] cases = ring.cases();
        VaccinationPlanner planner = new VaccinationPlanner(seed);
        VaccinationPlan approx = planner.plan(g, ring, new ApproxVertexCover(), request.budget());
        VaccinationPlan greedy = new VaccinationPlanner(seed).plan(g, ring, new GreedyVertexCover(), request.budget());

        OutbreakSimulator sim = new OutbreakSimulator(g,
                new OutbreakSimulator.Settings(p, recovery, efficacy, trials, OutbreakSimulator.MAX_STEPS, seed));
        boolean[] already = new boolean[g.personCount()];
        for (int q = 0; q < g.personCount(); q++) {
            already[q] = g.person(q).vaccinated();
        }

        OutbreakSimulator.Result none = sim.run(cases, already);
        OutbreakSimulator.Result random = sim.run(cases, withRandom(g, already, cases, approx.doses(), seed));
        OutbreakSimulator.Result approxResult = sim.run(cases, with(already, approx.people()));
        OutbreakSimulator.Result greedyResult = sim.run(cases, with(already, greedy.people()));

        List<SimulateResponse.Scenario> scenarios = new ArrayList<>();
        scenarios.add(scenario("NONE", "No vaccination", 0, none, none));
        scenarios.add(scenario("RANDOM", "Random people, same doses as the plan", approx.doses(), random, none));
        scenarios.add(scenario("APPROX", "Ring plan (2-approximation)", approx.doses(), approxResult, none));
        scenarios.add(scenario("GREEDY", "Ring plan (greedy)", greedy.doses(), greedyResult, none));

        String headline;
        if (none.mean() == 0) {
            headline = "With these settings the outbreak does not spread even without vaccination.";
        } else {
            double vsNone = reduction(approxResult.mean(), none.mean());
            double vsRandom = random.mean() == 0 ? 0 : reduction(approxResult.mean(), random.mean());
            headline = String.format(Locale.ROOT,
                    "The ring plan (%d doses) reduces average new infections by %.0f%% compared with no vaccination"
                            + " (%.1f to %.1f), and by %.0f%% compared with giving the same %d doses to random people.",
                    approx.doses(), vsNone, none.mean(), approxResult.mean(), vsRandom, approx.doses());
        }
        return new SimulateResponse(p, recovery, trials, efficacy, seed, scenarios, headline,
                (System.nanoTime() - start) / 1e6);
    }

    private static SimulateResponse.Scenario scenario(String name, String label, int doses,
                                                      OutbreakSimulator.Result r, OutbreakSimulator.Result none) {
        Double vsNone = none.mean() == 0 ? null : reduction(r.mean(), none.mean());
        return new SimulateResponse.Scenario(name, label, doses, r.mean(), r.median(), r.p90(), r.min(), r.max(),
                vsNone, r.curve());
    }

    private static double reduction(double value, double baseline) {
        return 100.0 * (baseline - value) / baseline;
    }

    private static boolean[] with(boolean[] already, int[] people) {
        boolean[] v = already.clone();
        for (int p : people) {
            v[p] = true;
        }
        return v;
    }

    /** Already vaccinated people plus `doses` people picked uniformly (seeded) from those who can be vaccinated. */
    private static boolean[] withRandom(ContactGraph g, boolean[] already, int[] cases, int doses, long seed) {
        boolean[] isCase = new boolean[g.personCount()];
        for (int c : cases) {
            isCase[c] = true;
        }
        List<Integer> candidates = new ArrayList<>();
        for (int q = 0; q < g.personCount(); q++) {
            if (!isCase[q] && g.person(q).canBeVaccinated()) {
                candidates.add(q);
            }
        }
        boolean[] v = already.clone();
        Random random = new Random(seed);
        // Partial Fisher-Yates shuffle: the first `doses` candidates after shuffling are a uniform sample.
        int take = Math.min(doses, candidates.size());
        for (int i = 0; i < take; i++) {
            int j = i + random.nextInt(candidates.size() - i);
            int t = candidates.get(i);
            candidates.set(i, candidates.get(j));
            candidates.set(j, t);
            v[candidates.get(i)] = true;
        }
        return v;
    }
}
