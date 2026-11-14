package com.ringvax.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ringvax.api.dto.CompareRequest;
import com.ringvax.api.dto.CompareResponse;
import com.ringvax.api.dto.FollowUpRequest;
import com.ringvax.api.dto.FollowUpResponse;
import com.ringvax.api.dto.PlanRequest;
import com.ringvax.api.dto.PlanResponse;
import com.ringvax.api.dto.RingRequest;
import com.ringvax.api.dto.RingResponse;
import com.ringvax.service.FollowUpService;
import com.ringvax.service.PlanService;

import jakarta.validation.Valid;

/** Rings, vaccination plans and method comparison. */
@RestController
@RequestMapping("/api")
public class PlanController {

    private final PlanService plans;
    private final FollowUpService followUps;

    public PlanController(PlanService plans, FollowUpService followUps) {
        this.plans = plans;
        this.followUps = followUps;
    }

    @GetMapping("/methods")
    public List<String> methods() {
        return plans.availableMethods();
    }

    @PostMapping("/networks/{networkId}/ring")
    public RingResponse ring(@PathVariable String networkId, @Valid @RequestBody RingRequest request) {
        return plans.ring(networkId, request.caseIds(), request.depth(), request.minDurationMinutes(),
                request.windowDays());
    }

    @PostMapping("/networks/{networkId}/plan")
    public PlanResponse plan(@PathVariable String networkId, @Valid @RequestBody PlanRequest request) {
        return plans.plan(networkId, request.caseIds(), request.depth(), request.method(), request.budget(),
                request.seed(), request.minDurationMinutes(), request.windowDays());
    }

    @PostMapping("/networks/{networkId}/compare")
    public CompareResponse compare(@PathVariable String networkId, @Valid @RequestBody CompareRequest request) {
        return plans.compare(networkId, request.caseIds(), request.depth(), request.minDurationMinutes(),
                request.windowDays());
    }

    /** Vaccinate-by deadlines and the symptom monitoring list for a plan. */
    @PostMapping("/networks/{networkId}/follow-up")
    public FollowUpResponse followUp(@PathVariable String networkId, @Valid @RequestBody FollowUpRequest request) {
        return followUps.followUp(networkId, request);
    }
}
