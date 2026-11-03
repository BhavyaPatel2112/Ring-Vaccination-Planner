package com.ringvax.service;

/** No network with this id has been loaded (the API answers 404). */
public class NetworkNotFoundException extends RuntimeException {

    public NetworkNotFoundException(String what) {
        super(what);
    }
}
