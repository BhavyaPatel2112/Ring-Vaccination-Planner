package com.ringvax.api;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

/** Every sample and every generated town opens with confirmed cases, so there are rings to show. */
@SpringBootTest
@AutoConfigureMockMvc
class StartingCasesApiTest {

    @Autowired
    private MockMvc mvc;

    private ResultActions postJson(String path, String json) throws Exception {
        return mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void everySampleHasAConfirmedCase() throws Exception {
        for (String sample : List.of("outbreak-small", "validation-demo", "star", "complete-bipartite-3-3",
                "greedy-worst-case-12", "greedy-worst-case-18")) {
            mvc.perform(post("/api/networks/samples/" + sample))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.caseIds", hasSize(greaterThan(0))));
        }
    }

    @Test
    void greedyTrapCaseMakesTheThreeMethodsDiffer() throws Exception {
        String body = mvc.perform(post("/api/networks/samples/greedy-worst-case-12")).andReturn().getResponse()
                .getContentAsString();
        String id = JsonPath.read(body, "$.networkId");
        postJson("/api/networks/" + id + "/compare", "{\"caseIds\": [\"P012\"]}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.methods[0].doses").value(27))
                .andExpect(jsonPath("$.methods[1].doses").value(16))
                .andExpect(jsonPath("$.methods[2].doses").value(23));
    }

    @Test
    void generatedTownStartsWithTwoCasesWhoHaveContacts() throws Exception {
        String body = postJson("/api/networks/generate", "{\"population\": 800, \"avgContacts\": 6, \"seed\": 4}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.caseIds", hasSize(2)))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.networkId");
        List<String> cases = JsonPath.read(body, "$.caseIds");
        postJson("/api/networks/" + id + "/ring", "{\"caseIds\": [\"" + cases.get(0) + "\"]}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rings[0]", hasSize(greaterThan(1))));
    }

    @Test
    void sameSeedPicksTheSameCasesAndTheCountCanBeChanged() throws Exception {
        String a = postJson("/api/networks/generate", "{\"population\": 500, \"avgContacts\": 6, \"seed\": 9}")
                .andReturn().getResponse().getContentAsString();
        String b = postJson("/api/networks/generate", "{\"population\": 500, \"avgContacts\": 6, \"seed\": 9}")
                .andReturn().getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertEquals((Object) JsonPath.read(a, "$.caseIds"), JsonPath.read(b, "$.caseIds"));
        postJson("/api/networks/generate", "{\"population\": 500, \"avgContacts\": 6, \"cases\": 0}")
                .andExpect(jsonPath("$.caseIds", hasSize(0)));
        postJson("/api/networks/generate", "{\"population\": 500, \"avgContacts\": 6, \"cases\": 50}")
                .andExpect(status().isBadRequest());
    }
}
