package com.ringvax.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

/** Exposure self-check (6), double-dose guard (9) and Bloom filter tuning (10). */
@SpringBootTest
@AutoConfigureMockMvc
class BloomApiTest {

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
    class Exposure {

        @Test
        void overlappingVisitIsPossibleThenConfirmed() throws Exception {
            String id = loadSample("outbreak-small");
            // P031 was at the market (V002) 18:30 to 19:15 while case P023 was there 18:00 to 19:00.
            postJson("/api/exposure/check", "{\"networkId\": \"" + id + "\", \"personId\": \"P031\"}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.quickCheck").value("POSSIBLY_EXPOSED"))
                    .andExpect(jsonPath("$.result").value("EXPOSED"))
                    .andExpect(jsonPath("$.visits[0].confirmed").value(true))
                    .andExpect(jsonPath("$.visits[0].overlapMinutes").value(30))
                    .andExpect(jsonPath("$.visits[0].exposureDate").value("2026-11-01"))
                    .andExpect(jsonPath("$.message", containsString("Exposed")));
        }

        @Test
        void visitsWithoutACaseAreNotExposed() throws Exception {
            String id = loadSample("outbreak-small");
            // P020 went to the food court the day after; P036 arrived after the case left; P033 went elsewhere.
            for (String person : new String[] {"P020", "P036", "P033"}) {
                postJson("/api/exposure/check", "{\"networkId\": \"" + id + "\", \"personId\": \"" + person + "\"}")
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.result").value("NOT_EXPOSED"));
            }
        }

        @Test
        void sameHourWithoutOverlapIsAFalseAlarmThatTheExactCheckClears() throws Exception {
            String id = loadSample("outbreak-small");
            // With P031 as the case, the market's 18:00 and 19:00 hours are in the filter.
            // A visit from 19:30 hits the 19:00 hour, but P031 had left at 19:15.
            postJson("/api/exposure/check", "{\"networkId\": \"" + id + "\", \"caseIds\": [\"P031\"], \"visits\": ["
                    + "{\"venueId\": \"V002\", \"start\": \"2026-11-01T19:30\", \"end\": \"2026-11-01T20:00\"}]}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.visits[0].quickCheck").value("POSSIBLY_EXPOSED"))
                    .andExpect(jsonPath("$.visits[0].confirmed").value(false))
                    .andExpect(jsonPath("$.result").value("NOT_EXPOSED"))
                    .andExpect(jsonPath("$.falseAlarms").value(1));
        }

        @Test
        void responseNeverNamesTheCase() throws Exception {
            String id = loadSample("outbreak-small");
            String body = postJson("/api/exposure/check", "{\"networkId\": \"" + id + "\", \"personId\": \"P032\"}")
                    .andExpect(jsonPath("$.result").value("EXPOSED"))
                    .andReturn().getResponse().getContentAsString();
            org.junit.jupiter.api.Assertions.assertFalse(body.contains("P003"), "the case's id must not appear");
        }

        @Test
        void invalidVisitsAreRejected() throws Exception {
            String id = loadSample("outbreak-small");
            postJson("/api/exposure/check", "{\"networkId\": \"" + id + "\", \"visits\": ["
                    + "{\"venueId\": \"V002\", \"start\": \"2026-11-01T19:30\", \"end\": \"2026-11-01T19:00\"}]}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("end after it starts")));
            postJson("/api/exposure/check", "{\"networkId\": \"" + id + "\"}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("at least one visit")));
            postJson("/api/exposure/check", "{\"networkId\": \"" + id + "\", \"personId\": \"P999\"}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("UNKNOWN_PERSON"));
            postJson("/api/exposure/check", "{\"networkId\": \"" + id + "\", \"infectiousDays\": 0, \"personId\": \"P031\"}")
                    .andExpect(status().isBadRequest());
        }

        @Test
        void unknownNetworkIs404() throws Exception {
            postJson("/api/exposure/check", "{\"networkId\": \"net-missing\", \"personId\": \"P1\"}")
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class Registry {

        @Test
        void vaccinatedPeopleAreSkippedOthersGetADose() throws Exception {
            String id = loadSample("outbreak-small");
            postJson("/api/registry/check", "{\"networkId\": \"" + id + "\", \"personIds\": [\"P005\", \"P001\", \"P999\"]}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.results[0].quickCheck").value("POSSIBLY_VACCINATED"))
                    .andExpect(jsonPath("$.results[0].vaccinated").value(true))
                    .andExpect(jsonPath("$.results[0].action").value("SKIP"))
                    .andExpect(jsonPath("$.results[1].action").value("GIVE_DOSE"))
                    .andExpect(jsonPath("$.results[2].action").value("UNKNOWN_PERSON"))
                    .andExpect(jsonPath("$.alreadyVaccinated").value(1))
                    .andExpect(jsonPath("$.filter.keys").value(3));
        }

        @Test
        void emptyListIsRejected() throws Exception {
            String id = loadSample("outbreak-small");
            postJson("/api/registry/check", "{\"networkId\": \"" + id + "\", \"personIds\": []}")
                    .andExpect(status().isBadRequest());
        }

        @Test
        void unknownNetworkIs404() throws Exception {
            postJson("/api/registry/check", "{\"networkId\": \"net-missing\", \"personIds\": [\"P1\"]}")
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class Tuning {

        @Test
        void measuredRateIsCloseToTheory() throws Exception {
            postJson("/api/bloom/config", "{\"m\": 100000, \"k\": 7, \"n\": 10000}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result.falseNegatives").value(0))
                    .andExpect(jsonPath("$.result.measuredRate", lessThan(0.0099)))
                    .andExpect(jsonPath("$.result.measuredRate", greaterThan(0.0066)))
                    .andExpect(jsonPath("$.result.optimalK").value(7))
                    .andExpect(jsonPath("$.result.memoryRatio", greaterThan(5.0)))
                    .andExpect(jsonPath("$.theoryByK", hasSize(20)));
        }

        @Test
        void absurdParametersAreRejected() throws Exception {
            postJson("/api/bloom/config", "{\"m\": 0, \"k\": 7}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("m must be at least 64")));
            postJson("/api/bloom/config", "{\"m\": 1000, \"k\": 0}").andExpect(status().isBadRequest());
            postJson("/api/bloom/config", "{\"m\": 1000, \"k\": 99}").andExpect(status().isBadRequest());
            postJson("/api/bloom/config", "{\"m\": 1000000000000, \"k\": 3}").andExpect(status().isBadRequest());
            postJson("/api/bloom/config", "{\"k\": 3}").andExpect(status().isBadRequest());
        }

        @Test
        void statsDescribeTheLiveFilters() throws Exception {
            String id = loadSample("outbreak-small");
            mvc.perform(get("/api/bloom/stats").param("networkId", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[1].keys").value(3));
            mvc.perform(get("/api/bloom/stats").param("networkId", "net-missing")).andExpect(status().isNotFound());
        }
    }
}
