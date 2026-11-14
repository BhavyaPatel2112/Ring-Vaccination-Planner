package com.ringvax.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
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

/** Outbreak simulation endpoint (feature 11). */
@SpringBootTest
@AutoConfigureMockMvc
class SimulationApiTest {

    @Autowired
    private MockMvc mvc;

    private ResultActions postJson(String path, String json) throws Exception {
        return mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private String generate() throws Exception {
        String body = postJson("/api/networks/generate", "{\"population\": 1500, \"avgContacts\": 8, \"seed\": 3}")
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.networkId");
    }

    @Test
    void planBeatsNoVaccinationAndRandomVaccination() throws Exception {
        String id = generate();
        String body = postJson("/api/networks/" + id + "/simulate",
                "{\"caseIds\": [\"P0001\", \"P0700\"], \"transmission\": 0.1, \"trials\": 100}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scenarios", hasSize(4)))
                .andExpect(jsonPath("$.scenarios[0].name").value("NONE"))
                .andExpect(jsonPath("$.scenarios[2].reductionVsNone", greaterThan(50.0)))
                .andExpect(jsonPath("$.headline", containsString("reduces average new infections")))
                .andReturn().getResponse().getContentAsString();
        double random = ((Number) JsonPath.read(body, "$.scenarios[1].mean")).doubleValue();
        double plan = ((Number) JsonPath.read(body, "$.scenarios[2].mean")).doubleValue();
        int randomDoses = JsonPath.read(body, "$.scenarios[1].doses");
        int planDoses = JsonPath.read(body, "$.scenarios[2].doses");
        org.junit.jupiter.api.Assertions.assertEquals(planDoses, randomDoses, "same number of doses");
        org.junit.jupiter.api.Assertions.assertTrue(plan < random, "plan " + plan + " vs random " + random);
    }

    @Test
    void sameSeedGivesTheSameNumbers() throws Exception {
        String id = generate();
        String request = "{\"caseIds\": [\"P0001\"], \"trials\": 30, \"seed\": 5}";
        String a = postJson("/api/networks/" + id + "/simulate", request).andReturn().getResponse().getContentAsString();
        String b = postJson("/api/networks/" + id + "/simulate", request).andReturn().getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertEquals((Object) JsonPath.read(a, "$.scenarios[0].mean"),
                JsonPath.read(b, "$.scenarios[0].mean"));
    }

    @Test
    void invalidSettingsAreRejected() throws Exception {
        String id = generate();
        postJson("/api/networks/" + id + "/simulate", "{\"caseIds\": [\"P0001\"], \"transmission\": 2}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("between 0 and 1")));
        postJson("/api/networks/" + id + "/simulate", "{\"caseIds\": [\"P0001\"], \"trials\": 0}")
                .andExpect(status().isBadRequest());
        postJson("/api/networks/" + id + "/simulate", "{\"caseIds\": []}").andExpect(status().isBadRequest());
        postJson("/api/networks/" + id + "/simulate", "{\"caseIds\": [\"NOPE\"]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("UNKNOWN_PERSON"));
    }

    @Test
    void tooMuchWorkIsRefusedWithAHint() throws Exception {
        String body = postJson("/api/networks/generate", "{\"population\": 20000, \"avgContacts\": 10, \"seed\": 1}")
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.networkId");
        postJson("/api/networks/" + id + "/simulate", "{\"caseIds\": [\"P00001\"], \"trials\": 2000}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("at most")));
    }

    @Test
    void unknownNetworkIs404() throws Exception {
        postJson("/api/networks/net-missing/simulate", "{\"caseIds\": [\"P1\"]}").andExpect(status().isNotFound());
    }
}
