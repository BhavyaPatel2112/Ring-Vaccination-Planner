package com.ringvax.api;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ringvax.api.dto.SimulateRequest;
import com.ringvax.api.dto.SimulateResponse;
import com.ringvax.service.SimulationService;

import jakarta.validation.Valid;

/** Outbreak simulation: infections under each vaccination strategy. */
@RestController
@RequestMapping("/api/networks/{networkId}")
public class SimulationController {

    private final SimulationService simulations;

    public SimulationController(SimulationService simulations) {
        this.simulations = simulations;
    }

    @PostMapping("/simulate")
    public SimulateResponse simulate(@PathVariable String networkId, @Valid @RequestBody SimulateRequest request) {
        return simulations.simulate(networkId, request);
    }
}
