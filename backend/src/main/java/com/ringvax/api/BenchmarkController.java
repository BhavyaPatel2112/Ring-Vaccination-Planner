package com.ringvax.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ringvax.api.dto.BenchmarkRunRequest;
import com.ringvax.api.dto.BenchmarkTable;
import com.ringvax.service.BenchmarkService;

import jakarta.validation.Valid;

/** Saved benchmark results and quick live runs. */
@RestController
@RequestMapping("/api/benchmarks")
public class BenchmarkController {

    private final BenchmarkService benchmarks;

    public BenchmarkController(BenchmarkService benchmarks) {
        this.benchmarks = benchmarks;
    }

    @GetMapping
    public List<BenchmarkTable> saved() {
        return benchmarks.saved();
    }

    @PostMapping("/run")
    public BenchmarkTable run(@Valid @RequestBody BenchmarkRunRequest request) {
        return benchmarks.run(request);
    }
}
