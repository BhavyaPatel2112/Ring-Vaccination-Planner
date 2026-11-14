package com.ringvax.api.dto;

import java.util.List;

import com.ringvax.engine.followup.ContactDefinition;

/**
 * Every method on the same rings, side by side.
 *
 * @param methods one entry per method; {@code error} explains a method that could not run
 * @param contactFilter how many contacts the contact settings left out, or null when none were given
 */
public record CompareResponse(int targetLinks, List<MethodResult> methods,
                              ContactDefinition.Summary contactFilter) {

    public record MethodResult(String method, Integer doses, Double runtimeMs, Double ratioToExact,
                               String guarantee, String error) {
    }
}
