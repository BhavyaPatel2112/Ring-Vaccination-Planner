package com.ringvax.api.dto;

import java.util.List;

/**
 * Body of every error response.
 *
 * @param error   short machine-readable code, for example {@code INVALID_FILE}
 * @param message one sentence the frontend can show as is
 * @param details individual problems (rejected rows, invalid fields), possibly empty
 */
public record ApiError(String error, String message, List<?> details) {
}
