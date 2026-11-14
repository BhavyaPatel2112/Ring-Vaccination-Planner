package com.ringvax.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
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

/** Contact settings (E), vaccinate-by deadlines (B) and the monitoring list (C). */
@SpringBootTest
@AutoConfigureMockMvc
class FollowUpApiTest {

    private static final String CASES = "\"caseIds\": [\"P003\", \"P023\"]";

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
    class ContactSettings {

        @Test
        void withoutSettingsNothingIsFiltered() throws Exception {
            String id = loadSample("outbreak-small");
            postJson("/api/networks/" + id + "/ring", "{" + CASES + "}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.contactFilter").doesNotExist());
        }

        @Test
        void shortAndOldContactsAreLeftOutAndCounted() throws Exception {
            String id = loadSample("outbreak-small");
            postJson("/api/networks/" + id + "/ring", "{" + CASES + ", \"minDurationMinutes\": 15, \"windowDays\": 14}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.contactFilter.tooShort", greaterThan(0)))
                    .andExpect(jsonPath("$.contactFilter.beforeWindow").value(1))
                    .andExpect(jsonPath("$.contactFilter.referenceDate").value("2026-11-02"));
        }

        @Test
        void settingsChangeThePlanAndTheComparison() throws Exception {
            String id = loadSample("outbreak-small");
            String all = postJson("/api/networks/" + id + "/plan", "{" + CASES + "}")
                    .andReturn().getResponse().getContentAsString();
            String filtered = postJson("/api/networks/" + id + "/plan", "{" + CASES + ", \"minDurationMinutes\": 60}")
                    .andExpect(jsonPath("$.contactFilter.tooShort", greaterThan(0)))
                    .andReturn().getResponse().getContentAsString();
            int allLinks = JsonPath.read(all, "$.targetLinks");
            int filteredLinks = JsonPath.read(filtered, "$.targetLinks");
            org.junit.jupiter.api.Assertions.assertTrue(filteredLinks < allLinks);

            postJson("/api/networks/" + id + "/compare", "{" + CASES + ", \"windowDays\": 3}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.contactFilter.windowDays").value(3));
        }

        @Test
        void invalidSettingsAre400() throws Exception {
            String id = loadSample("outbreak-small");
            postJson("/api/networks/" + id + "/ring", "{" + CASES + ", \"minDurationMinutes\": -5}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("minDurationMinutes")));
            postJson("/api/networks/" + id + "/plan", "{" + CASES + ", \"windowDays\": 0}")
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class FollowUp {

        @Test
        void dosesHaveDeadlinesInUrgencyOrder() throws Exception {
            String id = loadSample("outbreak-small");
            String body = postJson("/api/networks/" + id + "/follow-up", "{" + CASES + "}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.asOf").value("2026-11-02T00:00:00"))
                    .andExpect(jsonPath("$.windowHours").value(72))
                    .andExpect(jsonPath("$.doses[0].order").value(1))
                    .andExpect(jsonPath("$.doses[0].deadline").exists())
                    .andReturn().getResponse().getContentAsString();
            java.util.List<String> deadlines = JsonPath.read(body, "$.doses[*].deadline");
            for (int i = 1; i < deadlines.size(); i++) {
                if (deadlines.get(i - 1) != null && deadlines.get(i) != null) {
                    org.junit.jupiter.api.Assertions.assertTrue(deadlines.get(i - 1).compareTo(deadlines.get(i)) <= 0,
                            "ordered by deadline");
                }
            }
        }

        @Test
        void monitoringListsPeopleWhoCannotBeVaccinated() throws Exception {
            String id = loadSample("outbreak-small");
            postJson("/api/networks/" + id + "/follow-up", "{" + CASES + "}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.monitoring[*].reason", hasItem("CANNOT_BE_VACCINATED")))
                    .andExpect(jsonPath("$.monitoring[*].personId", not(hasItem("P003"))))
                    .andExpect(jsonPath("$.monitoring[*].monitorUntil", everyItem(containsString("2026-11"))));
        }

        @Test
        void lateAsOfMovesEveryoneToMonitoring() throws Exception {
            String id = loadSample("outbreak-small");
            postJson("/api/networks/" + id + "/follow-up", "{" + CASES + ", \"asOf\": \"2026-11-20T09:00\"}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.doses[*].status", everyItem(org.hamcrest.Matchers.anyOf(
                            org.hamcrest.Matchers.is("PAST_WINDOW"), org.hamcrest.Matchers.is("DATE_UNKNOWN")))))
                    .andExpect(jsonPath("$.monitoring[*].reason", hasItem("PAST_WINDOW")));
        }

        @Test
        void invalidWindowIs400AndUnknownNetwork404() throws Exception {
            String id = loadSample("outbreak-small");
            postJson("/api/networks/" + id + "/follow-up", "{" + CASES + ", \"windowHours\": 0}")
                    .andExpect(status().isBadRequest());
            postJson("/api/networks/" + id + "/follow-up", "{\"caseIds\": [\"P404\"]}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("UNKNOWN_PERSON"));
            postJson("/api/networks/net-missing/follow-up", "{" + CASES + "}").andExpect(status().isNotFound());
        }
    }
}
