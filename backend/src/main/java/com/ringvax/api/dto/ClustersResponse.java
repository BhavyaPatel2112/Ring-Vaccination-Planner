package com.ringvax.api.dto;

import java.util.List;

/**
 * Outbreak clusters of a network.
 *
 * @param clusterCount     number of clusters, counting people with no contacts
 * @param isolatedPeople   people with no contacts at all (left out of {@code clusters})
 * @param clusters         clusters of two or more people (cases first, then largest first), at most
 *                         {@code MAX_CLUSTERS}
 * @param clustersOmitted  clusters not listed because of the limit
 */
public record ClustersResponse(int clusterCount, int isolatedPeople, List<ClusterInfo> clusters, int clustersOmitted) {

    /**
     * @param clusterId  id of the cluster's representative person (changes when clusters merge)
     * @param members    person ids, or empty when the cluster is too large to list
     */
    public record ClusterInfo(String clusterId, int size, int cases, int vaccinated, List<String> caseIds,
                              List<String> members, boolean membersOmitted) {
    }
}
