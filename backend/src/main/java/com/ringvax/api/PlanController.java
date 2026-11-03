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
import com.ringvax.api.dto.PlanRequest;
import com.ringvax.api.dto.PlanResponse;
import com.ringvax.api.dto.RingRequest;
import com.ringvax.api.dto.RingResponse;
import com.ringvax.service.PlanService;

import jakarta.validation.Valid;

/** Rings, vaccination plans and method comparison. */
@RestController
@RequestMapping("/api")
public class PlanController {

    private final PlanService plans;

    public PlanController(PlanService plans) {
        this.plans = plans;
    }

    @GetMapping("/methods")
    public List<String> methods() {
        return plans.availableMethods();
    }

    @PostMapping("/networks/{networkId}/ring")
    public RingResponse ring(@PathVariable String networkId, @Valid @RequestBody RingRequest request) {
        return plans.ring(networkId, request.caseIds(), request.depth());
    }

    @PostMapping("/networks/{networkId}/plan")
    public PlanResponse plan(@PathVariable String networkId, @Valid @RequestBody PlanRequest request) {
        return plans.plan(networkId, request.caseIds(), request.depth(), request.method(), request.budget(),
                request.seed());
    }

    @PostMapping("/networks/{networkId}/compare")
    public CompareResponse compare(@PathVariable String networkId, @Valid @RequestBody CompareRequest request) {
        return plans.compare(networkId, request.caseIds(), request.depth());
    }
}
