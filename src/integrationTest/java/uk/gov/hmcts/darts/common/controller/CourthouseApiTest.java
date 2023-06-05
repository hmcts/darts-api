package uk.gov.hmcts.darts.common.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.courthouse.CourthouseRepository;
import uk.gov.hmcts.darts.courthouse.service.CourthouseService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.common.util.TestUtils.getContentsFromFile;

@SpringBootTest
@ActiveProfiles({"intTest", "h2db"})
@AutoConfigureMockMvc(addFilters = false)
class CourthouseApiTest {

    @Autowired
    private CourthouseService courthouseService;

    @Autowired
    private CourthouseRepository courthouseRepository;

    @Autowired
    private transient MockMvc mockMvc;

    @Test
    void courthousesGet() throws Exception {
        makeRequestToAddCourthouseToDatabase();
        MockHttpServletRequestBuilder requestBuilder = get("/courthouses")
            .contentType(MediaType.APPLICATION_JSON_VALUE);
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().is2xxSuccessful()).andReturn();
        assertThat(response.getResponse().getContentAsString()).isEqualTo("");
    }

    @Test
    void courthousesPost() throws Exception {
        MvcResult response = makeRequestToAddCourthouseToDatabase();
        assertThat(response.getResponse().getContentAsString()).isEqualTo("");
    }

    private MvcResult makeRequestToAddCourthouseToDatabase() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = post("/courthouses")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content("Tests/CourthousesTest/courthousesPostEndpoint/requestBody.json");
        return mockMvc.perform(requestBuilder).andExpect(status().is2xxSuccessful()).andReturn();
    }

    @Test
    void courthousesPut() throws Exception {
        String requestBody = getContentsFromFile("Tests/CourthousesTest/courthousesPutEndpoint/requestBody.json");
        MockHttpServletRequestBuilder requestBuilder = put("/courthouses/{courthouse_id}", 1)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(requestBody);
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isNotImplemented()).andReturn();
        assertThat(response.getResponse().getContentAsString()).isEqualTo("");
    }

    @Test
    void courthousesDelete() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = delete("/courthouses/{courthouse_id}", 1)
            .contentType(MediaType.APPLICATION_JSON_VALUE);
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isNotImplemented()).andReturn();
        assertThat(response.getResponse().getContentAsString()).isEqualTo("");
    }
}
