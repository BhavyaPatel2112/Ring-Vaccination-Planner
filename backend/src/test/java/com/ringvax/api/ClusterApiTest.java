package com.ringvax.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

/** Outbreak clusters (feature 2) and live contact updates (feature 12). */
@SpringBootTest
@AutoConfigureMockMvc
class ClusterApiTest {

    @Autowired
    private MockMvc mvc;

    private String loadSample(String id) throws Exception {
        String body = mvc.perform(post("/api/networks/samples/" + id))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.networkId");
    }

    private ResultActions postJson(String path, String json) throws Exception {
        return mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void clustersListSizesCasesAndVaccinated() throws Exception {
        String id = loadSample("outbreak-small");
        mvc.perform(get("/api/networks/" + id + "/clusters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clusters[0].cases").value(2))
                .andExpect(jsonPath("$.clusters[0].caseIds", hasItem("P003")))
                .andExpect(jsonPath("$.clusters[0].vaccinated").value(3))
                .andExpect(jsonPath("$.clusters[0].members", hasItem("P023")));
    }

    @Test
    void caseIdsInTheQueryReplaceTheFileCases() throws Exception {
        String id = loadSample("star");
        mvc.perform(get("/api/networks/" + id + "/clusters").param("caseIds", "P001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clusterCount").value(1))
                .andExpect(jsonPath("$.clusters[0].size").value(8))
                .andExpect(jsonPath("$.clusters[0].caseIds[0]").value("P001"));
    }

    @Test
    void liveContactMergesTwoClusters() throws Exception {
        String body = postJson("/api/networks/generate", "{\"population\": 300, \"avgContacts\": 1, \"seed\": 7}")
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.networkId");
        int before = JsonPath.read(mvc.perform(get("/api/networks/" + id + "/clusters"))
                .andReturn().getResponse().getContentAsString(), "$.clusterCount");

        // Find two people in different clusters: the representatives of the first two listed clusters.
        String clusters = mvc.perform(get("/api/networks/" + id + "/clusters")).andReturn().getResponse()
                .getContentAsString();
        String a = JsonPath.read(clusters, "$.clusters[0].clusterId");
        String b = JsonPath.read(clusters, "$.clusters[1].clusterId");

        postJson("/api/networks/" + id + "/contacts",
                "{\"personA\": \"" + a + "\", \"personB\": \"" + b + "\", \"date\": \"2026-11-10\", \"durationMinutes\": 30}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.merged").value(true))
                .andExpect(jsonPath("$.clusterCount").value(before - 1));
    }

    @Test
    void contactInsideOneClusterIsAcceptedWithoutMerging() throws Exception {
        String id = loadSample("outbreak-small");
        postJson("/api/networks/" + id + "/contacts", "{\"personA\": \"P001\", \"personB\": \"P040\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.merged").value(false))
                .andExpect(jsonPath("$.contacts").value(74));
        // The new link is visible to the rest of the app straight away.
        mvc.perform(get("/api/networks/" + id + "/summary")).andExpect(jsonPath("$.contacts").value(74));
    }

    @Test
    void selfContactDuplicateAndUnknownPeopleAreRejected() throws Exception {
        String id = loadSample("outbreak-small");
        postJson("/api/networks/" + id + "/contacts", "{\"personA\": \"P001\", \"personB\": \"P001\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("two different people")));
        postJson("/api/networks/" + id + "/contacts", "{\"personA\": \"P001\", \"personB\": \"P002\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already have a contact")));
        postJson("/api/networks/" + id + "/contacts", "{\"personA\": \"P001\", \"personB\": \"P999\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("UNKNOWN_PERSON"));
        postJson("/api/networks/" + id + "/contacts", "{\"personA\": \"P001\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));
        mvc.perform(get("/api/networks/" + id + "/clusters").param("caseIds", "P999"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownNetworkIs404() throws Exception {
        mvc.perform(get("/api/networks/net-missing/clusters")).andExpect(status().isNotFound());
        postJson("/api/networks/net-missing/contacts", "{\"personA\": \"A\", \"personB\": \"B\"}")
                .andExpect(status().isNotFound());
    }
}
