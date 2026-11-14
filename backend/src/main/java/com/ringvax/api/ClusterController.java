package com.ringvax.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ringvax.api.dto.ClustersResponse;
import com.ringvax.api.dto.LiveContactRequest;
import com.ringvax.api.dto.LiveContactResponse;
import com.ringvax.service.ClusterService;

import jakarta.validation.Valid;

/** Outbreak clusters and live contact reports. */
@RestController
@RequestMapping("/api/networks/{networkId}")
public class ClusterController {

    private final ClusterService clusters;

    public ClusterController(ClusterService clusters) {
        this.clusters = clusters;
    }

    /** Clusters; {@code caseIds} (comma separated) overrides the cases from cases.csv. */
    @GetMapping("/clusters")
    public ClustersResponse clusters(@PathVariable String networkId,
                                     @RequestParam(value = "caseIds", required = false) List<String> caseIds) {
        return clusters.clusters(networkId, caseIds);
    }

    @PostMapping("/contacts")
    @ResponseStatus(HttpStatus.CREATED)
    public LiveContactResponse addContact(@PathVariable String networkId,
                                          @Valid @RequestBody LiveContactRequest request) {
        return clusters.addContact(networkId, request);
    }
}
