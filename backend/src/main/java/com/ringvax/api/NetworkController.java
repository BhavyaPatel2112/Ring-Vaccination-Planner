package com.ringvax.api;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.ringvax.api.dto.GenerateRequest;
import com.ringvax.api.dto.NetworkSummary;
import com.ringvax.api.dto.NetworkView;
import com.ringvax.api.dto.SampleInfo;
import com.ringvax.service.NetworkService;
import com.ringvax.service.PlanService;

import jakarta.validation.Valid;

/** Loading networks and reading them back. */
@RestController
@RequestMapping("/api")
public class NetworkController {

    private final NetworkService networks;

    public NetworkController(NetworkService networks) {
        this.networks = networks;
    }

    /** Upload people.csv and contacts.csv, with optional checkins.csv and cases.csv. */
    @PostMapping(path = "/networks", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public NetworkSummary upload(@RequestPart("people") MultipartFile people,
                                 @RequestPart("contacts") MultipartFile contacts,
                                 @RequestPart(value = "checkins", required = false) MultipartFile checkins,
                                 @RequestPart(value = "cases", required = false) MultipartFile cases,
                                 @RequestParam(value = "name", required = false) String name) throws IOException {
        return networks.load(name, text(people), text(contacts), textOrNull(checkins), textOrNull(cases), "upload");
    }

    @PostMapping("/networks/generate")
    @ResponseStatus(HttpStatus.CREATED)
    public NetworkSummary generate(@Valid @RequestBody GenerateRequest request) {
        long seed = request.seed() == null ? PlanService.DEFAULT_SEED : request.seed();
        return networks.generate(request.population(), request.avgContacts(), seed, request.name());
    }

    @GetMapping("/samples")
    public List<SampleInfo> samples() {
        return networks.samples();
    }

    @PostMapping("/networks/samples/{sampleId}")
    @ResponseStatus(HttpStatus.CREATED)
    public NetworkSummary loadSample(@PathVariable String sampleId) {
        return networks.loadSample(sampleId);
    }

    @GetMapping("/networks")
    public List<NetworkSummary> list() {
        return networks.list();
    }

    @GetMapping("/networks/{networkId}/summary")
    public NetworkSummary summary(@PathVariable String networkId) {
        return networks.summary(networkId);
    }

    /** Nodes and links for drawing (sampled around the cases when the network is large). */
    @GetMapping("/networks/{networkId}")
    public NetworkView view(@PathVariable String networkId) {
        return networks.view(networkId);
    }

    private static String text(MultipartFile file) throws IOException {
        return new String(file.getBytes(), StandardCharsets.UTF_8);
    }

    private static String textOrNull(MultipartFile file) throws IOException {
        return file == null ? null : text(file);
    }
}
