package com.ringvax.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.ringvax.api.dto.FollowUpRequest;
import com.ringvax.api.dto.FollowUpResponse;
import com.ringvax.engine.cover.CoverChoice;
import com.ringvax.engine.followup.ContactDefinition;
import com.ringvax.engine.followup.FollowUpPlanner;
import com.ringvax.engine.graph.ContactGraph;
import com.ringvax.engine.graph.Person;
import com.ringvax.engine.plan.VaccinationPlan;
import com.ringvax.engine.plan.VaccinationPlanner;
import com.ringvax.engine.ring.RingResult;

/** "Vaccinate by" deadlines (B), the symptom monitoring list (C), with the contact settings (E). */
@Service
public class FollowUpService {

    private final NetworkService networks;
    private final PlanService plans;

    public FollowUpService(NetworkService networks, PlanService plans) {
        this.networks = networks;
        this.plans = plans;
    }

    public FollowUpResponse followUp(String networkId, FollowUpRequest request) {
        StoredNetwork network = networks.get(networkId);
        ContactGraph g = network.graph();
        ContactDefinition def = ContactSettings.resolve(network, request.minDurationMinutes(), request.windowDays());
        RingResult ring = PlanService.buildRing(g, request.caseIds(), request.depth(), def);
        long seed = request.seed() == null ? PlanService.DEFAULT_SEED : request.seed();
        VaccinationPlan plan = new VaccinationPlanner(seed).plan(g, ring, plans.solver(request.method()),
                request.budget());

        int windowHours = request.windowHours() == null ? FollowUpPlanner.DEFAULT_WINDOW_HOURS : request.windowHours();
        int incubation = request.incubationDays() == null ? FollowUpPlanner.DEFAULT_INCUBATION_DAYS
                : request.incubationDays();
        LocalDateTime asOf = request.asOf() != null ? request.asOf()
                : ContactSettings.referenceDate(network).atStartOfDay();
        FollowUpPlanner.Result result = new FollowUpPlanner(g, def).plan(ring, plan.people(), asOf, windowHours,
                incubation);

        Map<Integer, String> reasons = new HashMap<>();
        for (CoverChoice choice : plan.choices()) {
            reasons.put(choice.person(), choice.reason().name());
        }
        List<FollowUpResponse.Dose> doses = new ArrayList<>();
        int urgent = 0;
        int onTime = 0;
        int past = 0;
        int unknown = 0;
        for (FollowUpPlanner.Dose d : result.doses()) {
            Person p = g.person(d.person());
            String advice = switch (d.status()) {
                case DATE_UNKNOWN -> {
                    unknown++;
                    yield "Exposure date unknown: vaccinate as soon as possible";
                }
                case URGENT -> {
                    urgent++;
                    yield "Vaccinate today: " + d.hoursLeft() + " hours left";
                }
                case ON_TIME -> {
                    onTime++;
                    yield "Vaccinate within " + d.hoursLeft() + " hours";
                }
                case PAST_WINDOW -> {
                    past++;
                    yield "Past the " + windowHours + "-hour window: monitor for symptoms instead";
                }
            };
            doses.add(new FollowUpResponse.Dose(doses.size() + 1, p.id(), p.name(), d.ring(), d.lastExposure(),
                    d.deadline(), d.hoursLeft(), d.status().name(), reasons.get(d.person()), advice));
        }

        List<FollowUpResponse.Monitor> monitoring = new ArrayList<>();
        for (FollowUpPlanner.Monitor m : result.monitoring()) {
            Person p = g.person(m.person());
            String explanation = switch (m.reason()) {
                case PAST_WINDOW -> "Too late for a post-exposure dose";
                case CANNOT_BE_VACCINATED -> p.refused() ? "Refused vaccination" : "Cannot be vaccinated (ineligible)";
                case NOT_IN_PLAN -> "Not in the plan: their links are protected through other people";
                case LEFT_OUT_BY_BUDGET -> "Ring 1, but left out because of the dose budget";
            };
            if (m.lastExposure() == null) {
                explanation += " (exposure date unknown, counted from today)";
            }
            monitoring.add(new FollowUpResponse.Monitor(p.id(), p.name(), m.ring(), m.lastExposure(),
                    m.monitorUntil(), m.reason().name(), explanation));
        }
        return new FollowUpResponse(asOf, windowHours, incubation, plan.method(), doses, urgent, onTime, past, unknown,
                monitoring, def.isActive() ? def.summarize(g) : null);
    }
}
