package com.ringvax.engine.graph;

import java.util.Objects;

/**
 * A person in the contact network.
 *
 * @param id         unique identifier from people.csv, for example "P001"
 * @param name       display name (may be empty)
 * @param age        age in years, or {@code null} when unknown
 * @param role       free-text role such as "teacher" or "student" (may be empty)
 * @param vaccinated already vaccinated, so already immune
 * @param eligible   medically able to receive the vaccine (false for infants,
 *                   pregnant or immunocompromised people when the vaccine is live)
 * @param refused    declined vaccination
 */
public record Person(String id, String name, Integer age, String role,
                     boolean vaccinated, boolean eligible, boolean refused) {

    public Person {
        Objects.requireNonNull(id, "id");
        if (id.isBlank()) {
            throw new IllegalArgumentException("Person id must not be blank");
        }
        name = name == null ? "" : name;
        role = role == null ? "" : role;
    }

    /** Convenience constructor for an eligible person who has not refused. */
    public Person(String id, String name, Integer age, String role, boolean vaccinated) {
        this(id, name, age, role, vaccinated, true, false);
    }

    /**
     * True when this person can be given a dose today: not already vaccinated,
     * medically eligible and has not refused.
     */
    public boolean canBeVaccinated() {
        return !vaccinated && eligible && !refused;
    }
}
