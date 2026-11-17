package com.ringvax.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import org.springframework.stereotype.Service;

import com.ringvax.api.dto.RobustnessRequest;
import com.ringvax.api.dto.RobustnessResponse;
import com.ringvax.api.dto.SimulateRequest;
import com.ringvax.api.dto.SimulateResponse;
import com.ringvax.engine.cover.ApproxVertexCover;
import com.ringvax.engine.cover.GreedyVertexCover;
import com.ringvax.engine.followup.ContactDefinition;
import com.ringvax.engine.graph.Contact;
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

        StoredNetwork network = networks.get(networkId);
        ContactDefinition def = ContactSettings.resolve(network, request.minDurationMinutes(), request.windowDays());
        RingResult ring = PlanService.buildRing(g, request.caseIds(), request.depth(), def);
        int[] cases = ring.cases();
        VaccinationPlanner planner = new VaccinationPlanner(seed);
        VaccinationPlan approx = planner.plan(g, ring, new ApproxVertexCover(), request.budget());
        VaccinationPlan greedy = new VaccinationPlanner(seed).plan(g, ring, new GreedyVertexCover(), request.budget());

        OutbreakSimulator sim = simulator(g, p, recovery, efficacy, trials, seed, request.durationWeighted());
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

    /**
     * Robustness to incomplete contact tracing: for each share s, a seeded random s of
     * the contacts is hidden from the planner (the hidden sets are nested, so a larger
     * share hides everything a smaller one hid). The plan is made from what is left, and
     * the outbreak spreads along every contact. Shows how fast the plan's benefit fades
     * when tracing misses people.
     */
    public RobustnessResponse robustness(String networkId, RobustnessRequest request) {
        long start = System.nanoTime();
        ContactGraph g = networks.get(networkId).graph();
        double p = request.transmission() == null ? 0.08 : request.transmission();
        int recovery = request.recoverySteps() == null ? 4 : request.recoverySteps();
        int trials = request.trials() == null ? 100 : request.trials();
        double efficacy = request.efficacy() == null ? 1.0 : request.efficacy();
        long seed = request.seed() == null ? PlanService.DEFAULT_SEED : request.seed();
        List<Double> shares = request.missingShares() == null || request.missingShares().isEmpty()
                ? List.of(0.0, 0.1, 0.2, 0.3, 0.4, 0.5)
                : request.missingShares().stream().sorted().toList();
        long size = (long) g.personCount() + g.contactCount();
        if (trials * size * (shares.size() + 1) > 4 * MAX_WORK) {
            throw new IllegalArgumentException("Too much work for a network this size: use at most "
                    + Math.max(1, 4 * MAX_WORK / (size * (shares.size() + 1))) + " trials");
        }
        int depth = request.depth() == null ? RingBuilder.DEFAULT_DEPTH : request.depth();
        ContactDefinition def = ContactSettings.resolve(networks.get(networkId), request.minDurationMinutes(),
                request.windowDays());
        // Fails fast with UNKNOWN_PERSON before any work.
        int[] cases = new RingBuilder(g).build(request.caseIds(), depth).cases();

        // One seeded order of the contacts; the first share x E of it are hidden.
        int e = g.contactCount();
        int[] order = new int[e];
        for (int i = 0; i < e; i++) {
            order[i] = i;
        }
        Random random = new Random(seed);
        for (int i = e - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int t = order[i];
            order[i] = order[j];
            order[j] = t;
        }

        OutbreakSimulator sim = simulator(g, p, recovery, efficacy, trials, seed, request.durationWeighted());
        boolean[] already = new boolean[g.personCount()];
        for (int q = 0; q < g.personCount(); q++) {
            already[q] = g.person(q).vaccinated();
        }
        OutbreakSimulator.Result none = sim.run(cases, already);

        List<RobustnessResponse.Row> rows = new ArrayList<>();
        for (double share : shares) {
            int hidden = (int) Math.round(share * e);
            boolean[] isHidden = new boolean[e];
            for (int i = 0; i < hidden; i++) {
                isHidden[order[i]] = true;
            }
            ContactGraph visible = new ContactGraph();
            for (int q = 0; q < g.personCount(); q++) {
                visible.addPerson(g.person(q));
            }
            for (int c = 0; c < e; c++) {
                if (!isHidden[c]) {
                    Contact link = g.contact(c);
                    visible.addContact(link.a(), link.b(), link.date(), link.durationMinutes());
                }
            }
            RingResult ring = PlanService.buildRing(visible, request.caseIds(), depth, def);
            VaccinationPlan plan = new VaccinationPlanner(seed).plan(visible, ring, new ApproxVertexCover(), null);
            OutbreakSimulator.Result r = sim.run(cases, with(already, plan.people()));
            rows.add(new RobustnessResponse.Row(share, hidden, plan.doses(), r.mean(), r.p90(),
                    none.mean() == 0 ? null : reduction(r.mean(), none.mean())));
        }

        String headline;
        if (none.mean() == 0) {
            headline = "With these settings the outbreak does not spread even without vaccination.";
        } else {
            RobustnessResponse.Row full = rows.get(0);
            RobustnessResponse.Row worst = rows.get(rows.size() - 1);
            // "Still" only when at least half of the benefit survives; otherwise say plainly that it falls.
            boolean holds = worst.reductionVsNone() >= full.reductionVsNone() / 2;
            headline = String.format(java.util.Locale.ROOT,
                    "With %s of contacts traced, the plan cuts infections by %.0f%%. If tracing misses %s of"
                            + " contacts, %s %.0f%%, with %d doses instead of %d.",
                    WalkthroughService.percent(1 - full.missingShare()), full.reductionVsNone(),
                    WalkthroughService.percent(worst.missingShare()),
                    holds ? "it still cuts them by" : "the cut falls to", worst.reductionVsNone(), worst.doses(),
                    full.doses());
        }
        return new RobustnessResponse(none.mean(), rows, headline, trials, seed, (System.nanoTime() - start) / 1e6);
    }

    /**
     * The simulator for these settings. With durationWeighted, p is the chance of infection
     * per 15 minutes of contact and each contact's risk grows with its duration; otherwise
     * every contact carries p.
     */
    private static OutbreakSimulator simulator(ContactGraph g, double p, int recovery, double efficacy, int trials,
                                               long seed, Boolean durationWeighted) {
        OutbreakSimulator.Settings settings =
                new OutbreakSimulator.Settings(p, recovery, efficacy, trials, OutbreakSimulator.MAX_STEPS, seed);
        return Boolean.TRUE.equals(durationWeighted)
                ? new OutbreakSimulator(g, settings, OutbreakSimulator.durationWeighted(g, p))
                : new OutbreakSimulator(g, settings);
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
