package uk.gov.hmcts.darts.noderegistration.controller;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@SpringBootTest
@ActiveProfiles("intTest")
@AutoConfigureMockMvc
public class NodeRegistrationControllerTest {

    @Autowired
    private transient MockMvc mockMvc;

    @Test
    void checkPostEndpoint() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = post("/register-devices")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .param("node_type", "DAR")
            .param("courthouse", "SWANSEA")
            .param("court_room", "1")
            .param("host_name", "XXXXX.MMM.net")
            .param("ip_address", "192.0.0.1")
            .param("mac_address", "6A-5F-90-A4-2C-12");
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(status().is2xxSuccessful()).andReturn();
        Assertions.assertTrue(mvcResult.getResponse().getContentAsString().contains("node_id"));
    }
}
