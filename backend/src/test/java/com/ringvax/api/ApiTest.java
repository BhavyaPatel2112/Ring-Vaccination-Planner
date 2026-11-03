package com.ringvax.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

/** Success and error paths of every endpoint, through the real Spring stack. */
@SpringBootTest
@AutoConfigureMockMvc
class ApiTest {

    private static final Path SAMPLES = Path.of("..", "data", "sample");

    @Autowired
    private MockMvc mvc;

    private static MockMultipartFile file(String part, String text) {
        return new MockMultipartFile(part, part + ".csv", "text/csv", text.getBytes());
    }

    private static MockMultipartFile sampleFile(String folder, String part, String fileName) throws Exception {
        return new MockMultipartFile(part, fileName, "text/csv", Files.readAllBytes(SAMPLES.resolve(folder).resolve(fileName)));
    }

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
    void healthCheck() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }

    @Nested
    class Upload {

        @Test
        void validUploadReturnsCountsCasesAndWarnings() throws Exception {
            mvc.perform(multipart("/api/networks")
                            .file(sampleFile("outbreak-small", "people", "people.csv"))
                            .file(sampleFile("outbreak-small", "contacts", "contacts.csv"))
                            .file(sampleFile("outbreak-small", "checkins", "checkins.csv"))
                            .file(sampleFile("outbreak-small", "cases", "cases.csv"))
                            .param("name", "Ward 4"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.networkId", startsWith("net-")))
                    .andExpect(jsonPath("$.name").value("Ward 4"))
                    .andExpect(jsonPath("$.people").value(40))
                    .andExpect(jsonPath("$.contacts").value(73))
                    .andExpect(jsonPath("$.checkIns").value(13))
                    .andExpect(jsonPath("$.caseIds", hasSize(2)))
                    .andExpect(jsonPath("$.warnings", hasSize(0)));
        }

        @Test
        void rejectedRowsAreReportedNotHidden() throws Exception {
            mvc.perform(multipart("/api/networks")
                            .file(sampleFile("validation-demo", "people", "people.csv"))
                            .file(sampleFile("validation-demo", "contacts", "contacts.csv")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.people").value(5))
                    .andExpect(jsonPath("$.warnings[*].code", hasItem("SELF_CONTACT")))
                    .andExpect(jsonPath("$.warnings[*].code", hasItem("UNKNOWN_PERSON")));
        }

        @Test
        void emptyPeopleFileIs400() throws Exception {
            mvc.perform(multipart("/api/networks")
                            .file(file("people", ""))
                            .file(file("contacts", "person_a,person_b,date,duration_minutes\n")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("INVALID_FILE"))
                    .andExpect(jsonPath("$.details[0].code").value("EMPTY_FILE"));
        }

        @Test
        void missingContactsFileIs400() throws Exception {
            mvc.perform(multipart("/api/networks").file(file("people", "person_id,name,age,role,vaccinated\nP1,A,3,,false\n")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("MISSING_FILE"))
                    .andExpect(jsonPath("$.message", containsString("contacts")));
        }

        @Test
        void unknownCaseInCasesFileIs400NamingIt() throws Exception {
            mvc.perform(multipart("/api/networks")
                            .file(sampleFile("validation-demo", "people", "people.csv"))
                            .file(sampleFile("validation-demo", "contacts", "contacts.csv"))
                            .file(sampleFile("validation-demo", "cases", "cases-unknown-id.csv")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("P404")));
        }
    }

    @Nested
    class SamplesAndGenerator {

        @Test
        void samplesAreListedAndLoadable() throws Exception {
            mvc.perform(get("/api/samples"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value("outbreak-small"))
                    .andExpect(jsonPath("$[0].label").value("Neighbourhood outbreak"));
            String id = loadSample("outbreak-small");
            mvc.perform(get("/api/networks/" + id + "/summary"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.source").value("sample"));
            mvc.perform(get("/api/networks")).andExpect(jsonPath("$[*].networkId", hasItem(id)));
        }

        @Test
        void unknownSampleIs404() throws Exception {
            mvc.perform(post("/api/networks/samples/nope")).andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("NOT_FOUND"));
            mvc.perform(post("/api/networks/samples/..%2F..%2Fetc")).andExpect(status().is4xxClientError());
        }

        @Test
        void generateBuildsASyntheticNetwork() throws Exception {
            postJson("/api/networks/generate", "{\"population\": 500, \"avgContacts\": 8, \"seed\": 3}")
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.people").value(500))
                    .andExpect(jsonPath("$.source").value("generated"));
        }

        @Test
        void generateRejectsBadSettings() throws Exception {
            postJson("/api/networks/generate", "{\"population\": 1, \"avgContacts\": 8}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("INVALID_REQUEST"))
                    .andExpect(jsonPath("$.message", containsString("population")));
            postJson("/api/networks/generate", "{\"population\": 10, \"avgContacts\": 50}")
                    .andExpect(status().isBadRequest());
            postJson("/api/networks/generate", "not json").andExpect(status().isBadRequest());
        }
    }

    @Nested
    class NetworkView {

        @Test
        void smallNetworkIsSentWhole() throws Exception {
            String id = loadSample("outbreak-small");
            mvc.perform(get("/api/networks/" + id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.sampled").value(false))
                    .andExpect(jsonPath("$.nodes", hasSize(40)))
                    .andExpect(jsonPath("$.links", hasSize(73)))
                    .andExpect(jsonPath("$.nodes[2].confirmedCase").value(true));
        }

        @Test
        void largeNetworkIsSampled() throws Exception {
            String body = postJson("/api/networks/generate", "{\"population\": 5000, \"avgContacts\": 6}")
                    .andReturn().getResponse().getContentAsString();
            String id = JsonPath.read(body, "$.networkId");
            mvc.perform(get("/api/networks/" + id))
                    .andExpect(jsonPath("$.sampled").value(true))
                    .andExpect(jsonPath("$.totalPeople").value(5000))
                    .andExpect(jsonPath("$.nodes", hasSize(1500)));
        }

        @Test
        void unknownNetworkIs404() throws Exception {
            mvc.perform(get("/api/networks/net-missing")).andExpect(status().isNotFound());
        }

        @Test
        void unknownPathIs404NotServerError() throws Exception {
            mvc.perform(get("/api/does-not-exist")).andExpect(status().isNotFound());
        }
    }

    @Nested
    class Planning {

        @Test
        void ringListsRingsAndTargets() throws Exception {
            String id = loadSample("outbreak-small");
            postJson("/api/networks/" + id + "/ring", "{\"caseIds\": [\"P003\", \"P023\"]}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.depth").value(2))
                    .andExpect(jsonPath("$.rings[0]", hasSize(13)))
                    .andExpect(jsonPath("$.cannotVaccinate", hasItem("P004")))
                    .andExpect(jsonPath("$.targetLinks", hasSize(greaterThan(0))));
        }

        @Test
        void ringErrors() throws Exception {
            String id = loadSample("outbreak-small");
            postJson("/api/networks/" + id + "/ring", "{\"caseIds\": [\"P999\"]}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("UNKNOWN_PERSON"))
                    .andExpect(jsonPath("$.message", containsString("P999")));
            postJson("/api/networks/" + id + "/ring", "{\"caseIds\": [\"P003\"], \"depth\": 9}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("depth")));
            postJson("/api/networks/" + id + "/ring", "{\"caseIds\": []}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("confirmed case")));
        }

        @Test
        void approximatePlanStatesItsGuarantee() throws Exception {
            String id = loadSample("outbreak-small");
            postJson("/api/networks/" + id + "/plan", "{\"caseIds\": [\"P003\", \"P023\"]}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.method").value("APPROX"))
                    .andExpect(jsonPath("$.complete").value(true))
                    .andExpect(jsonPath("$.guarantee", startsWith("At most 2 times the minimum")))
                    .andExpect(jsonPath("$.people[0].reason").value("RING_1"))
                    .andExpect(jsonPath("$.people[*].personId", hasItem("P001")));
        }

        @Test
        void exactPlanAndBudgets() throws Exception {
            String id = loadSample("outbreak-small");
            postJson("/api/networks/" + id + "/plan", "{\"caseIds\": [\"P003\"], \"method\": \"exact\"}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.method").value("EXACT"))
                    .andExpect(jsonPath("$.provenRatioBound").value(1.0));
            postJson("/api/networks/" + id + "/plan", "{\"caseIds\": [\"P003\"], \"budget\": 0}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.doses").value(0))
                    .andExpect(jsonPath("$.budgetLimited").value(true))
                    .andExpect(jsonPath("$.ring1NotVaccinated", hasSize(greaterThan(0))));
            postJson("/api/networks/" + id + "/plan", "{\"caseIds\": [\"P003\"], \"budget\": -2}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("budget must be 0 or more")));
            postJson("/api/networks/" + id + "/plan", "{\"caseIds\": [\"P003\"], \"method\": \"MAGIC\"}")
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("Available")));
        }

        @Test
        void exactSolverRefusesLargeRingsWith422() throws Exception {
            String body = postJson("/api/networks/generate", "{\"population\": 3000, \"avgContacts\": 12, \"seed\": 1}")
                    .andReturn().getResponse().getContentAsString();
            String id = JsonPath.read(body, "$.networkId");
            postJson("/api/networks/" + id + "/plan", "{\"caseIds\": [\"P0001\"], \"method\": \"EXACT\"}")
                    .andExpect(status().isUnprocessableContent())
                    .andExpect(jsonPath("$.error").value("EXACT_SOLVER_TOO_LARGE"))
                    .andExpect(jsonPath("$.message", containsString("approximation")));
        }

        @Test
        void compareShowsEveryMethodAndExplainsRefusals() throws Exception {
            String id = loadSample("outbreak-small");
            postJson("/api/networks/" + id + "/compare", "{\"caseIds\": [\"P003\", \"P023\"]}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.methods[0].method").value("APPROX"))
                    .andExpect(jsonPath("$.methods[1].method").value("EXACT"))
                    .andExpect(jsonPath("$.methods[1].ratioToExact").value(1.0));

            String body = postJson("/api/networks/generate", "{\"population\": 3000, \"avgContacts\": 12, \"seed\": 1}")
                    .andReturn().getResponse().getContentAsString();
            String big = JsonPath.read(body, "$.networkId");
            postJson("/api/networks/" + big + "/compare", "{\"caseIds\": [\"P0001\"]}")
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.methods[1].error", containsString("at most 40")))
                    .andExpect(jsonPath("$.methods[0].doses", greaterThan(0)));
        }

        @Test
        void planOnUnknownNetworkIs404() throws Exception {
            postJson("/api/networks/net-missing/plan", "{\"caseIds\": [\"P1\"]}").andExpect(status().isNotFound());
        }
    }
}
