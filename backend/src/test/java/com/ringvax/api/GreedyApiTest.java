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

/** The greedy method through the plan and compare endpoints (feature 5). */
@SpringBootTest
@AutoConfigureMockMvc
class GreedyApiTest {

    @Autowired
    private MockMvc mvc;

    private String loadSample(String id) throws Exception {
        String body = mvc.perform(post("/api/networks/samples/" + id)).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.networkId");
    }

    private ResultActions postJson(String path, String json) throws Exception {
        return mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void methodsListIncludesGreedy() throws Exception {
        mvc.perform(get("/api/methods")).andExpect(status().isOk()).andExpect(jsonPath("$", hasItem("GREEDY")));
    }

    @Test
    void greedyPlanHasReasonsAndNoGuarantee() throws Exception {
        String id = loadSample("outbreak-small");
        postJson("/api/networks/" + id + "/plan", "{\"caseIds\": [\"P003\", \"P023\"], \"method\": \"greedy\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.method").value("GREEDY"))
                .andExpect(jsonPath("$.lowerBound").value(-1))
                .andExpect(jsonPath("$.guarantee", containsString("No fixed guarantee")))
                .andExpect(jsonPath("$.people[0].reason").value("RING_1"));
    }

    @Test
    void compareHasAThirdRowForGreedy() throws Exception {
        String id = loadSample("outbreak-small");
        postJson("/api/networks/" + id + "/compare", "{\"caseIds\": [\"P003\", \"P023\"]}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.methods[2].method").value("GREEDY"))
                .andExpect(jsonPath("$.methods[2].error").doesNotExist());
    }

    @Test
    void greedyBudgetModeWorks() throws Exception {
        String id = loadSample("outbreak-small");
        postJson("/api/networks/" + id + "/plan",
                "{\"caseIds\": [\"P003\"], \"method\": \"GREEDY\", \"budget\": 3}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.doses").value(3))
                .andExpect(jsonPath("$.budgetLimited").value(true));
    }

    @Test
    void greedyOnUnknownNetworkIs404() throws Exception {
        postJson("/api/networks/net-missing/plan", "{\"caseIds\": [\"P1\"], \"method\": \"GREEDY\"}")
                .andExpect(status().isNotFound());
    }

    @Test
    void wholeNetworkComparisonShowsTheGreedyTrap() throws Exception {
        String id = loadSample("greedy-worst-case-12");
        mvc.perform(post("/api/networks/" + id + "/compare-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetLinks").value(115))
                .andExpect(jsonPath("$.methods[1].doses").value(12))
                .andExpect(jsonPath("$.methods[2].method").value("GREEDY"))
                .andExpect(jsonPath("$.methods[2].doses").value(23));
    }

    @Test
    void wholeNetworkComparisonReportsTheExactRefusal() throws Exception {
        String id = loadSample("greedy-worst-case-18");
        mvc.perform(post("/api/networks/" + id + "/compare-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.methods[1].error", containsString("at most 40")))
                .andExpect(jsonPath("$.methods[2].doses").value(40));
        mvc.perform(post("/api/networks/net-missing/compare-all")).andExpect(status().isNotFound());
    }
}
