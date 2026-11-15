package com.ringvax.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.ringvax.api.dto.ApproxWalkthroughResponse;
import com.ringvax.api.dto.BloomDemoRequest;
import com.ringvax.api.dto.BloomDemoResponse;
import com.ringvax.api.dto.RingRequest;
import com.ringvax.engine.bloom.BloomFilter;
import com.ringvax.engine.cover.ApproxVertexCover;
import com.ringvax.engine.cover.CoverProblem;
import com.ringvax.engine.cover.CoverResult;
import com.ringvax.engine.followup.ContactDefinition;
import com.ringvax.engine.graph.ContactGraph;
import com.ringvax.engine.ring.RingResult;

/**
 * Data for the "How it works" screen. Everything is computed by the real algorithms
 * in Java; the screen only replays the steps.
 */
@Service
public class WalkthroughService {

    /** Longer runs are not worth stepping through by hand. */
    public static final int MAX_STEPS = 400;

    private final NetworkService networks;

    public WalkthroughService(NetworkService networks) {
        this.networks = networks;
    }

    /** The 2-approximation's steps on the rings around the cases. */
    public ApproxWalkthroughResponse approx(String networkId, RingRequest request) {
        StoredNetwork network = networks.get(networkId);
        ContactGraph g = network.graph();
        ContactDefinition def = ContactSettings.resolve(network, request.minDurationMinutes(), request.windowDays());
        RingResult ring = PlanService.buildRing(g, request.caseIds(), request.depth(), def);
        CoverProblem problem = CoverProblem.fromRing(g, ring);
        int stepCount = problem.forcedPeople().length + problem.freeContacts().length;
        if (stepCount > MAX_STEPS) {
            throw new IllegalArgumentException("These rings have " + stepCount + " steps, too many to step through"
                    + " (at most " + MAX_STEPS + "). Use a smaller network or ring depth 1.");
        }

        List<String> ring1 = new ArrayList<>();
        for (int p : ring.mandatory()) {
            ring1.add(g.person(p).id());
        }
        List<ApproxWalkthroughResponse.Step> steps = new ArrayList<>();
        CoverResult result = new ApproxVertexCover().solve(problem, step -> {
            int index = steps.size() + 1;
            String a = step.a() >= 0 ? g.person(step.a()).id() : null;
            String b = step.b() >= 0 ? g.person(step.b()).id() : null;
            switch (step.kind()) {
                case FORCED -> steps.add(new ApproxWalkthroughResponse.Step(index, "FORCED", null, b, a,
                        a + " is the only one who can be vaccinated on the link with " + b + ", so " + a
                                + " must get a dose."));
                case MATCHED -> steps.add(new ApproxWalkthroughResponse.Step(index, "MATCHED", a, b, null,
                        "Neither " + a + " nor " + b + " has a dose yet: vaccinate both. This link joins the"
                                + " matching, so the minimum needs at least one dose here."));
                case ALREADY_PROTECTED -> steps.add(new ApproxWalkthroughResponse.Step(index, "ALREADY_PROTECTED",
                        a, b, null, "The link " + a + " - " + b + " is already protected by an earlier dose."
                                + " Skip it."));
                default -> throw new IllegalStateException();
            }
        });
        int doses = ring1.size() + result.size();
        int lowerBound = ring1.size() + result.lowerBound();
        return new ApproxWalkthroughResponse(ring1, steps, ring.targetContacts().length, doses, lowerBound,
                result.unprotectableContacts().length);
    }

    /** A tiny filter: the bits each key sets, and what one query finds. */
    public BloomDemoResponse bloom(BloomDemoRequest request) {
        BloomFilter filter = new BloomFilter(request.m(), request.k());
        List<BloomDemoResponse.KeyBits> keys = new ArrayList<>();
        List<List<Integer>> setBy = new ArrayList<>();
        for (int i = 0; i < request.m(); i++) {
            setBy.add(new ArrayList<>());
        }
        List<String> stored = new ArrayList<>();
        for (String raw : request.keys()) {
            String key = raw.trim();
            if (stored.contains(key)) {
                continue;
            }
            int index = stored.size();
            stored.add(key);
            filter.add(key);
            List<Long> positions = list(filter.positions(key));
            keys.add(new BloomDemoResponse.KeyBits(key, positions));
            for (long bit : positions) {
                if (!setBy.get((int) bit).contains(index)) {
                    setBy.get((int) bit).add(index);
                }
            }
        }
        String query = request.query().trim();
        List<Long> queryBits = list(filter.positions(query));
        boolean allSet = filter.mightContain(query);
        boolean member = stored.contains(query);
        String verdict;
        String explanation;
        if (!allSet) {
            long zero = queryBits.stream().filter(b -> setBy.get((int) (long) b).isEmpty()).count();
            verdict = "DEFINITELY_NOT";
            explanation = zero + " of its " + request.k() + " bits " + (zero == 1 ? "is" : "are")
                    + " still 0, so \"" + query + "\" was never added. A filter never gets this wrong.";
        } else if (member) {
            verdict = "PRESENT";
            explanation = "All " + request.k() + " bits are 1 and \"" + query + "\" really was added.";
        } else {
            verdict = "FALSE_POSITIVE";
            explanation = "All " + request.k() + " bits are 1, but only because other keys set them: \"" + query
                    + "\" was never added. This is a false positive, so the app confirms every hit exactly.";
        }
        return new BloomDemoResponse(request.m(), request.k(), keys, setBy,
                new BloomDemoResponse.KeyBits(query, queryBits), allSet, member, verdict, explanation,
                filter.expectedFalsePositiveRate());
    }

    private static List<Long> list(long[] values) {
        List<Long> out = new ArrayList<>();
        for (long v : values) {
            out.add(v);
        }
        return out;
    }

    static String percent(double share) {
        return String.format(Locale.ROOT, "%.0f%%", share * 100);
    }
}
