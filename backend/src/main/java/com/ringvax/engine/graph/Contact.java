package com.ringvax.engine.graph;

import java.time.LocalDate;

/**
 * An undirected contact link between two people, stored by their index in the
 * {@link ContactGraph}. The endpoints are normalised so that {@code a < b}.
 *
 * @param a               index of the first person (always the smaller index)
 * @param b               index of the second person
 * @param date            day of the (most recent) contact, or {@code null} if unknown
 * @param durationMinutes length of the (longest) contact in minutes, or -1 if unknown
 */
public record Contact(int a, int b, LocalDate date, int durationMinutes) {

    public Contact {
        if (a == b) {
            throw new IllegalArgumentException("A contact needs two different people");
        }
        if (a > b) {
            int t = a;
            a = b;
            b = t;
        }
    }

    /** Returns the endpoint that is not {@code person}. */
    public int other(int person) {
        if (person == a) {
            return b;
        }
        if (person == b) {
            return a;
        }
        throw new IllegalArgumentException("Person " + person + " is not on this contact");
    }
}
