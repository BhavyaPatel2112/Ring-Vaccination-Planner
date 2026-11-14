package com.ringvax.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ringvax.api.dto.BloomConfigRequest;
import com.ringvax.api.dto.BloomConfigResponse;
import com.ringvax.api.dto.ExposureCheckRequest;
import com.ringvax.api.dto.ExposureCheckResponse;
import com.ringvax.api.dto.FilterStats;
import com.ringvax.api.dto.RegistryCheckRequest;
import com.ringvax.api.dto.RegistryCheckResponse;
import com.ringvax.service.BloomService;

import jakarta.validation.Valid;

/** Exposure self-check, double-dose guard and Bloom filter tuning. */
@RestController
@RequestMapping("/api")
public class BloomController {

    private final BloomService bloom;

    public BloomController(BloomService bloom) {
        this.bloom = bloom;
    }

    @PostMapping("/exposure/check")
    public ExposureCheckResponse exposure(@Valid @RequestBody ExposureCheckRequest request) {
        return bloom.checkExposure(request);
    }

    @PostMapping("/registry/check")
    public RegistryCheckResponse registry(@Valid @RequestBody RegistryCheckRequest request) {
        return bloom.checkRegistry(request);
    }

    @PostMapping("/bloom/config")
    public BloomConfigResponse config(@Valid @RequestBody BloomConfigRequest request) {
        return bloom.tune(request);
    }

    @GetMapping("/bloom/stats")
    public List<FilterStats> stats(@RequestParam String networkId) {
        return bloom.stats(networkId);
    }
}
