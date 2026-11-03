package com.ringvax.api.dto;

import java.util.List;

/**
 * Rings around the cases.
 *
 * @param rings       ring 1, ring 2, ... as lists of person ids
 * @param targetLinks links the plan still has to protect
 */
public record RingResponse(int depth, List<String> cases, List<List<String>> rings, List<String> mandatory,
                           List<String> alreadyVaccinated, List<String> cannotVaccinate, List<LinkRef> targetLinks,
                           List<String> casesWithoutContacts) {
}
