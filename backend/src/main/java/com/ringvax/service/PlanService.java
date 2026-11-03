package com.ringvax.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import org.springframework.stereotype.Service;

import com.ringvax.api.dto.CompareResponse;
import com.ringvax.api.dto.LinkRef;
import com.ringvax.api.dto.PlanResponse;
import com.ringvax.api.dto.RingResponse;
import com.ringvax.engine.cover.ApproxVertexCover;
import com.ringvax.engine.cover.CoverChoice;
import com.ringvax.engine.cover.ExactSolverLimitException;
import com.ringvax.engine.cover.ExactVertexCover;
import com.ringvax.engine.cover.VertexCoverSolver;
import com.ringvax.engine.graph.Contact;
import com.ringvax.engine.graph.ContactGraph;
import com.ringvax.engine.plan.VaccinationPlan;
import com.ringvax.engine.plan.VaccinationPlanner;
import com.ringvax.engine.ring.RingBuilder;
import com.ringvax.engine.ring.RingResult;

/**
 * Builds rings, vaccination plans and method comparisons for a loaded network.
 */
@Service
public class PlanService {

    public static final long DEFAULT_SEED = 2026;

    private final NetworkService networks;
    /** Vertex cover methods by API name. The greedy baseline is added here when it is ready. */
    private final Map<String, Supplier<VertexCoverSolver>> methods = new LinkedHashMap<>();

    public PlanService(NetworkService networks) {
        this.networks = networks;
        methods.put(ApproxVertexCover.NAME, ApproxVertexCover::new);
        methods.put(ExactVertexCover.NAME, ExactVertexCover::new);
    }

    public List<String> availableMethods() {
        return List.copyOf(methods.keySet());
    }

    public RingResponse ring(String networkId, List<String> caseIds, Integer depth) {
        ContactGraph g = networks.get(networkId).graph();
        RingResult ring = buildRing(g, caseIds, depth);
        List<List<String>> rings = new ArrayList<>();
        for (int k = 1; k <= ring.depth(); k++) {
            rings.add(ids(g, ring.ring(k)));
        }
        return new RingResponse(ring.depth(), ids(g, ring.cases()), rings, ids(g, ring.mandatory()),
                ids(g, ring.alreadyVaccinated()), ids(g, ring.cannotVaccinate()), links(g, ring.targetContacts()),
                ids(g, ring.casesWithoutContacts()));
    }

    /**
     * @throws IllegalArgumentException  for an unknown method or a negative budget
     * @throws ExactSolverLimitException if the exact method refuses the problem
     */
    public PlanResponse plan(String networkId, List<String> caseIds, Integer depth, String method, Integer budget,
                             Long seed) {
        ContactGraph g = networks.get(networkId).graph();
        VertexCoverSolver solver = solver(method);
        RingResult ring = buildRing(g, caseIds, depth);
        VaccinationPlan plan = new VaccinationPlanner(seed == null ? DEFAULT_SEED : seed)
                .plan(g, ring, solver, budget);

        List<PlanResponse.Dose> doses = new ArrayList<>();
        for (CoverChoice choice : plan.choices()) {
            doses.add(new PlanResponse.Dose(g.person(choice.person()).id(), g.person(choice.person()).name(),
                    choice.reason().name(), choice.explanation()));
        }
        Double ratio = plan.lowerBound() <= 0 ? (plan.doses() == 0 ? 1.0 : null)
                : (double) plan.dosesForFullPlan() / plan.lowerBound();
        return new PlanResponse(plan.method(), plan.doses(), plan.dosesForFullPlan(), plan.budget(), plan.complete(),
                plan.budgetLimited(), ring.targetContacts().length, doses, links(g, plan.unprotectedContacts()),
                links(g, plan.unprotectableContacts()), ids(g, plan.ring1NotVaccinated()), plan.lowerBound(), ratio,
                guarantee(plan, ring.mandatory().length, ratio), plan.runtimeNanos() / 1e6);
    }

    /** Runs every available method on the same rings. A method that refuses is reported, not thrown. */
    public CompareResponse compare(String networkId, List<String> caseIds, Integer depth) {
        ContactGraph g = networks.get(networkId).graph();
        RingResult ring = buildRing(g, caseIds, depth);

        Map<String, VaccinationPlan> plans = new LinkedHashMap<>();
        Map<String, String> errors = new LinkedHashMap<>();
        for (Map.Entry<String, Supplier<VertexCoverSolver>> entry : methods.entrySet()) {
            try {
                plans.put(entry.getKey(), new VaccinationPlanner(DEFAULT_SEED).plan(g, ring, entry.getValue().get(), null));
            } catch (ExactSolverLimitException e) {
                errors.put(entry.getKey(), e.getMessage());
            }
        }
        VaccinationPlan exact = plans.get(ExactVertexCover.NAME);

        List<CompareResponse.MethodResult> results = new ArrayList<>();
        for (String name : methods.keySet()) {
            VaccinationPlan plan = plans.get(name);
            if (plan == null) {
                results.add(new CompareResponse.MethodResult(name, null, null, null, null, errors.get(name)));
                continue;
            }
            Double ratio = exact == null ? null
                    : exact.doses() == 0 ? (plan.doses() == 0 ? 1.0 : null) : (double) plan.doses() / exact.doses();
            Double proven = plan.lowerBound() <= 0 ? null : (double) plan.doses() / plan.lowerBound();
            results.add(new CompareResponse.MethodResult(name, plan.doses(), plan.runtimeNanos() / 1e6, ratio,
                    guarantee(plan, ring.mandatory().length, proven), null));
        }
        return new CompareResponse(ring.targetContacts().length, results);
    }

    // ------------------------------------------------------------------ helpers

    private VertexCoverSolver solver(String method) {
        String name = method == null || method.isBlank() ? ApproxVertexCover.NAME : method.trim().toUpperCase(Locale.ROOT);
        Supplier<VertexCoverSolver> supplier = methods.get(name);
        if (supplier == null) {
            throw new IllegalArgumentException("Unknown method '" + method + "'. Available: "
                    + String.join(", ", methods.keySet()));
        }
        return supplier.get();
    }

    private static RingResult buildRing(ContactGraph g, List<String> caseIds, Integer depth) {
        return new RingBuilder(g).build(caseIds, depth == null ? RingBuilder.DEFAULT_DEPTH : depth);
    }

    /** Plain-language statement of how good the plan is. Never claims the approximation is optimal. */
    static String guarantee(VaccinationPlan plan, int ring1, Double ratio) {
        StringBuilder text = new StringBuilder();
        switch (plan.method()) {
            case ApproxVertexCover.NAME -> {
                text.append("At most 2 times the minimum number of doses (ring 1 is always included).");
                if (ratio != null && plan.lowerBound() > 0) {
                    text.append(String.format(Locale.ROOT,
                            " For this plan the minimum is at least %d, so it is at most %.2f times the minimum.",
                            plan.lowerBound(), ratio));
                }
            }
            case ExactVertexCover.NAME -> text.append("The smallest possible plan for these rings (exact solver, "
                    + ring1 + " ring 1 doses plus a minimum cover).");
            default -> text.append("No fixed guarantee for this method.");
        }
        if (plan.budgetLimited()) {
            text.append(" Budget mode: ").append(plan.doses()).append(" of ").append(plan.dosesForFullPlan())
                    .append(" doses used");
            int open = plan.unprotectedContacts().length;
            text.append(open == 0 ? "; every link is still protected." : "; " + open + " links are left unprotected.");
        }
        return text.toString();
    }

    private static List<String> ids(ContactGraph g, int[] people) {
        return Arrays.stream(people).mapToObj(p -> g.person(p).id()).toList();
    }

    private static List<LinkRef> links(ContactGraph g, int[] contacts) {
        List<LinkRef> result = new ArrayList<>();
        for (int c : contacts) {
            Contact link = g.contact(c);
            result.add(new LinkRef(g.person(link.a()).id(), g.person(link.b()).id()));
        }
        return result;
    }
}
