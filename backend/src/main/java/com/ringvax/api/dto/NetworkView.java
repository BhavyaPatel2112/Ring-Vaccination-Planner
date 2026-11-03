package com.ringvax.api.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * The network for drawing. Large networks are sampled around the cases so the
 * browser stays fast; the algorithms always run on the full network.
 */
public record NetworkView(String networkId, int totalPeople, int totalContacts, boolean sampled,
                          List<Node> nodes, List<Link> links) {

    public record Node(String id, String name, Integer age, String role, boolean vaccinated, boolean eligible,
                       boolean refused, boolean confirmedCase, int contacts) {
    }

    public record Link(String source, String target, LocalDate date, int durationMinutes) {
    }
}
