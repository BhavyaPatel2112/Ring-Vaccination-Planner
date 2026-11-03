package com.ringvax.data;

import java.time.LocalDateTime;

/**
 * A visit by one person to one venue, from checkins.csv.
 *
 * @param personId person who visited
 * @param venueId  venue identifier, for example "V010"
 * @param start    arrival time
 * @param end      departure time (always after {@code start})
 */
public record CheckIn(String personId, String venueId, LocalDateTime start, LocalDateTime end) {
}
