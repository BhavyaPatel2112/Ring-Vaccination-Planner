package com.ringvax.api.dto;

/**
 * Result of adding a contact.
 *
 * @param merged       true if the contact joined two clusters that were separate
 * @param clusterSize  size of the (merged) cluster the two people are now in
 * @param clusterCount number of clusters after the update
 * @param contacts     contacts in the network after the update
 * @param runtimeMicros time taken by the cluster update itself
 */
public record LiveContactResponse(String personA, String personB, boolean merged, int clusterSize, int clusterCount,
                                  int contacts, String message, double runtimeMicros) {
}
