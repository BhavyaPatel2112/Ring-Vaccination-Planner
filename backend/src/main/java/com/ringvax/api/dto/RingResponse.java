package com.ringvax.api.dto;

import java.util.List;

import com.ringvax.engine.followup.ContactDefinition;

/**
 * Rings around the cases.
 *
 * @param rings       ring 1, ring 2, ... as lists of person ids
 * @param targetLinks links the plan still has to protect
 * @param contactFilter how many contacts the contact settings left out, or null when none were given
 */
public record RingResponse(int depth, List<String> cases, List<List<String>> rings, List<String> mandatory,
                           List<String> alreadyVaccinated, List<String> cannotVaccinate, List<LinkRef> targetLinks,
                           List<String> casesWithoutContacts,
                           ContactDefinition.Summary contactFilter) {
}
