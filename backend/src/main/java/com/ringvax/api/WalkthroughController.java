package com.ringvax.api;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ringvax.api.dto.ApproxWalkthroughResponse;
import com.ringvax.api.dto.BloomDemoRequest;
import com.ringvax.api.dto.BloomDemoResponse;
import com.ringvax.api.dto.RingRequest;
import com.ringvax.service.WalkthroughService;

import jakarta.validation.Valid;

/** Step-by-step data for the "How it works" screen. */
@RestController
@RequestMapping("/api")
public class WalkthroughController {

    private final WalkthroughService walkthroughs;

    public WalkthroughController(WalkthroughService walkthroughs) {
        this.walkthroughs = walkthroughs;
    }

    @PostMapping("/networks/{networkId}/walkthrough/approx")
    public ApproxWalkthroughResponse approx(@PathVariable String networkId, @Valid @RequestBody RingRequest request) {
        return walkthroughs.approx(networkId, request);
    }

    @PostMapping("/bloom/demo")
    public BloomDemoResponse bloom(@Valid @RequestBody BloomDemoRequest request) {
        return walkthroughs.bloom(request);
    }
}
