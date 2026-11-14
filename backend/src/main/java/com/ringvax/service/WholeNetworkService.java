package com.ringvax.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.ringvax.api.dto.CompareResponse;
import com.ringvax.engine.cover.ApproxVertexCover;
import com.ringvax.engine.cover.CoverProblem;
import com.ringvax.engine.cover.CoverResult;
import com.ringvax.engine.cover.ExactSolverLimitException;
import com.ringvax.engine.cover.ExactVertexCover;
import com.ringvax.engine.cover.GreedyVertexCover;
import com.ringvax.engine.cover.VertexCoverSolver;
import com.ringvax.engine.graph.ContactGraph;

/**
 * Compares the vertex cover methods on every link of the network, without rings
 * or ring 1 doses. This is the plain textbook problem, used to show the greedy
 * trap samples (greedy 23 against a minimum of 12) and the exact solver refusing
 * large inputs.
 */
@Service
public class WholeNetworkService {

    private final NetworkService networks;

    public WholeNetworkService(NetworkService networks) {
        this.networks = networks;
    }

    public CompareResponse compareAllContacts(String networkId) {
        ContactGraph g = networks.get(networkId).graph();
        CoverProblem problem = CoverProblem.allContacts(g);
        Map<String, VertexCoverSolver> solvers = Map.of(ApproxVertexCover.NAME, new ApproxVertexCover(),
                ExactVertexCover.NAME, new ExactVertexCover(), GreedyVertexCover.NAME, new GreedyVertexCover());
        CoverResult exact = null;
        String exactError = null;
        try {
            exact = solvers.get(ExactVertexCover.NAME).solve(problem);
        } catch (ExactSolverLimitException e) {
            exactError = e.getMessage();
        }
        List<CompareResponse.MethodResult> results = new ArrayList<>();
        for (String name : List.of(ApproxVertexCover.NAME, ExactVertexCover.NAME, GreedyVertexCover.NAME)) {
            if (name.equals(ExactVertexCover.NAME) && exact == null) {
                results.add(new CompareResponse.MethodResult(name, null, null, null, null, exactError));
                continue;
            }
            CoverResult r = name.equals(ExactVertexCover.NAME) ? exact : solvers.get(name).solve(problem);
            Double ratio = null;
            if (exact != null && exact.size() > 0) {
                ratio = (double) r.size() / exact.size();
            } else if (exact != null && r.size() == 0) {
                ratio = 1.0;
            }
            String guarantee = switch (name) {
                case ApproxVertexCover.NAME -> "At most 2 times the minimum. The minimum is at least " + r.lowerBound()
                        + " (the size of the matching it found).";
                case ExactVertexCover.NAME -> "The smallest possible set of people covering every link.";
                default -> "No fixed guarantee: it can be fooled into using many more people than needed.";
            };
            results.add(new CompareResponse.MethodResult(name, r.size(), r.runtimeNanos() / 1e6, ratio, guarantee,
                    null));
        }
        return new CompareResponse(g.contactCount(), results, null);
    }
}
