package com.ringvax.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
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

/** Saved benchmark tables and quick live runs. */
@SpringBootTest
@AutoConfigureMockMvc
class BenchmarkApiTest {

    @Autowired
    private MockMvc mvc;

    private ResultActions postJson(String json) throws Exception {
        return mvc.perform(post("/api/benchmarks/run").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void savedTablesAreListed() throws Exception {
        mvc.perform(get("/api/benchmarks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem("selection")))
                .andExpect(jsonPath("$[*].name", hasItem("approx-vs-greedy")));
    }

    @Test
    void liveRunGivesThreeSizes() throws Exception {
        postJson("{\"experiment\": \"dsu\", \"size\": 5000}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("dsu (live)"))
                .andExpect(jsonPath("$.rows", hasSize(3)))
                .andExpect(jsonPath("$.header[0]").value("people"));
    }

    @Test
    void invalidRunsAreRejected() throws Exception {
        postJson("{\"experiment\": \"magic\", \"size\": 5000}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Available")));
        postJson("{\"experiment\": \"bloom\", \"size\": 5000000}").andExpect(status().isBadRequest());
        postJson("{\"size\": 5000}").andExpect(status().isBadRequest());
    }
}
