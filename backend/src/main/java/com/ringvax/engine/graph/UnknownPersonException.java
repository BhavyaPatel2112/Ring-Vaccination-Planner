package com.ringvax.engine.graph;

/**
 * Thrown when a request names a person who is not in the network, for example
 * an unknown confirmed case. The API turns this into a 400 response.
 */
public class UnknownPersonException extends IllegalArgumentException {

    private final String personId;

    public UnknownPersonException(String personId) {
        super("Unknown person: " + personId);
        this.personId = personId;
    }

    public String personId() {
        return personId;
    }
}
