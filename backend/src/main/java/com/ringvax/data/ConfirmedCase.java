package com.ringvax.data;

import java.time.LocalDate;

/**
 * A confirmed case from cases.csv.
 *
 * @param personId      person who tested positive
 * @param confirmedDate date the case was confirmed
 */
public record ConfirmedCase(String personId, LocalDate confirmedDate) {
}
