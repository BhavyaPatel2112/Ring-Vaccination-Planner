package com.ringvax.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

/** "How it works" data and the missing-contacts robustness test. */
@SpringBootTest
@AutoConfigureMockMvc
class WalkthroughApiTest {

    @Autowired
    private MockMvc mvc;

    private String loadSample(String id) throws Exception {
        String body = mvc.perform(post("/api/networks/samples/" + id)).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.networkId");
    }

    private ResultActions postJson(String path, String json) throws Exception {
        return mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Nested
    class Approximation {

        @Test
        void stepsAddUpToThePlan() throws Exception {
            String id = loadSample("outbreak-small");
            String body = postJson("/api/networks/" + id + "/walkthrough/approx", "{\"caseIds\": [\"P003\", \"P023\"]}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.doses").value(21))
                    .andExpect(jsonPath("$.steps[0].index").value(1))
                    .andReturn().getResponse().getContentAsString();
            int ring1 = ((List<?>) JsonPath.read(body, "$.ring1")).size();
            int forced = ((List<?>) JsonPath.read(body, "$.steps[?(@.kind == 'FORCED')]")).size();
            int matched = ((List<?>) JsonPath.read(body, "$.steps[?(@.kind == 'MATCHED')]")).size();
            org.junit.jupiter.api.Assertions.assertEquals(21, ring1 + forced + 2 * matched);
        }

        @Test
        void tooManyStepsIsRefusedWithAHint() throws Exception {
            String body = postJson("/api/networks/generate", "{\"population\": 3000, \"avgContacts\": 12, \"seed\": 1}")
                    .andReturn().getResponse().getContentAsString();
            String id = JsonPath.read(body, "$.networkId");
            List<String> cases = JsonPath.read(body, "$.caseIds");
            postJson("/api/networks/" + id + "/walkthrough/approx", "{\"caseIds\": [\"" + cases.get(0) + "\"], \"depth\": 3}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("too many to step through")));
        }

        @Test
        void unknownCaseAndNetworkAreRejected() throws Exception {
            String id = loadSample("star");
            postJson("/api/networks/" + id + "/walkthrough/approx", "{\"caseIds\": [\"P999\"]}")
                    .andExpect(status().isBadRequest());
            postJson("/api/networks/net-missing/walkthrough/approx", "{\"caseIds\": [\"P001\"]}")
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class Bloom {

        @Test
        void storedKeyIsPresentAndMissingKeyIsUsuallyDefinitelyNot() throws Exception {
            postJson("/api/bloom/demo", "{\"m\": 64, \"k\": 3, \"keys\": [\"V001|18\", \"V002|19\"], \"query\": \"V001|18\"}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.verdict").value("PRESENT"))
                    .andExpect(jsonPath("$.setBy", hasSize(64)))
                    .andExpect(jsonPath("$.query.positions", hasSize(3)));
            postJson("/api/bloom/demo", "{\"m\": 128, \"k\": 3, \"keys\": [\"V001|18\"], \"query\": \"V009|07\"}")
                    .andExpect(jsonPath("$.verdict").value("DEFINITELY_NOT"))
                    .andExpect(jsonPath("$.explanation", containsString("never added")));
        }

        @Test
        void crowdedTinyFilterProducesAFalsePositive() throws Exception {
            // 8 bits, 12 keys: nearly every bit is 1, so an unknown key passes.
            postJson("/api/bloom/demo", "{\"m\": 8, \"k\": 2, \"keys\": [\"a\",\"b\",\"c\",\"d\",\"e\",\"f\",\"g\",\"h\","
                    + "\"i\",\"j\",\"k\",\"l\"], \"query\": \"zebra\"}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.verdict").value("FALSE_POSITIVE"))
                    .andExpect(jsonPath("$.queryIsMember").value(false));
        }

        @Test
        void outOfRangeSettingsAreRejected() throws Exception {
            postJson("/api/bloom/demo", "{\"m\": 4, \"k\": 2, \"keys\": [], \"query\": \"x\"}").andExpect(status().isBadRequest());
            postJson("/api/bloom/demo", "{\"m\": 64, \"k\": 20, \"keys\": [], \"query\": \"x\"}").andExpect(status().isBadRequest());
            postJson("/api/bloom/demo", "{\"m\": 64, \"k\": 2, \"keys\": []}").andExpect(status().isBadRequest());
        }
    }

    @Nested
    class Robustness {

        @Test
        void hidingContactsWeakensThePlanButItStillHelps() throws Exception {
            String body = postJson("/api/networks/generate", "{\"population\": 1500, \"avgContacts\": 8, \"seed\": 3}")
                    .andReturn().getResponse().getContentAsString();
            String id = JsonPath.read(body, "$.networkId");
            List<String> cases = JsonPath.read(body, "$.caseIds");
            String result = postJson("/api/networks/" + id + "/simulate/robustness",
                    "{\"caseIds\": [\"" + cases.get(0) + "\", \"" + cases.get(1) + "\"], \"transmission\": 0.1,"
                            + " \"trials\": 60, \"missingShares\": [0, 0.5]}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rows", hasSize(2)))
                    .andExpect(jsonPath("$.rows[0].hiddenContacts").value(0))
                    .andExpect(jsonPath("$.rows[1].hiddenContacts", greaterThan(0)))
                    .andExpect(jsonPath("$.headline", containsString("misses 50%")))
                    .andReturn().getResponse().getContentAsString();
            double full = ((Number) JsonPath.read(result, "$.rows[0].mean")).doubleValue();
            double half = ((Number) JsonPath.read(result, "$.rows[1].mean")).doubleValue();
            double none = ((Number) JsonPath.read(result, "$.noVaccinationMean")).doubleValue();
            org.junit.jupiter.api.Assertions.assertTrue(full < half, full + " vs " + half);
            org.junit.jupiter.api.Assertions.assertTrue(half < none, half + " vs " + none);
        }

        @Test
        void invalidSharesAndUnknownCasesAreRejected() throws Exception {
            String id = loadSample("outbreak-small");
            postJson("/api/networks/" + id + "/simulate/robustness", "{\"caseIds\": [\"P003\"], \"missingShares\": [0.95]}")
                    .andExpect(status().isBadRequest());
            postJson("/api/networks/" + id + "/simulate/robustness", "{\"caseIds\": [\"P404\"]}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("UNKNOWN_PERSON"));
            postJson("/api/networks/net-missing/simulate/robustness", "{\"caseIds\": [\"P003\"]}")
                    .andExpect(status().isNotFound());
        }
    }
}
