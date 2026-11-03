package com.ringvax.api.dto;

import java.util.List;

/**
 * Every method on the same rings, side by side.
 *
 * @param methods one entry per method; {@code error} explains a method that could not run
 */
public record CompareResponse(int targetLinks, List<MethodResult> methods) {

    public record MethodResult(String method, Integer doses, Double runtimeMs, Double ratioToExact,
                               String guarantee, String error) {
    }
}
