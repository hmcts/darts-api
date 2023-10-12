package uk.gov.hmcts.darts.noderegistration.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.noderegistration.model.RegisterNodeResponse;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@AutoConfigureMockMvc
class NodeRegistrationControllerTest  extends IntegrationBase {

    @Autowired
    private transient MockMvc mockMvc;

    @Test
    void testPostRegisterDevices() throws Exception {
        dartsDatabase.createCourthouseWithTwoCourtrooms();

        CourtroomEntity courtroomEntity = dartsDatabase.findCourtroomBy("SWANSEA", "1");

        MockHttpServletRequestBuilder requestBuilder = buildRequest(courtroomEntity.getCourthouse().getCourthouseName(),
                                                                    courtroomEntity.getName(), "DAR");

        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(status().is2xxSuccessful()).andReturn();
        assertTrue(mvcResult.getResponse().getContentAsString().contains("node_id"));
    }

    @Test
    void testInvalidCourtroom() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = buildRequest("SWANSEA", "999", "DAR");
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(status().is4xxClientError()).andReturn();
        assertTrue(mvcResult.getResponse().getContentAsString().contains("Could not find the courtroom"));
    }

    @Test
    void testEmptyParams() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = post("/register-devices")
            .contentType(MediaType.APPLICATION_JSON_VALUE);

        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(status().is4xxClientError()).andReturn();
        String response = mvcResult.getResponse().getContentAsString();
        assertTrue(response.contains("node_type"));
    }

    @Test
    void testAcceptNonDarDevices() throws Exception {
        dartsDatabase.createCourthouseWithTwoCourtrooms();

        CourtroomEntity courtroomEntity = dartsDatabase.findCourtroomBy("SWANSEA", "1");

        MockHttpServletRequestBuilder requestBuilder = buildRequest(courtroomEntity.getCourthouse().getCourthouseName(),
                                                                    courtroomEntity.getName(), "UPLOADER");

        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(status().is2xxSuccessful()).andReturn();
        assertTrue(mvcResult.getResponse().getContentAsString().contains("node_id"));
    }

    @Test
    void testAcceptsDuplicates() throws Exception {
        dartsDatabase.createCourthouseWithTwoCourtrooms();

        CourtroomEntity courtroomEntity = dartsDatabase.findCourtroomBy("SWANSEA", "1");

        MockHttpServletRequestBuilder requestBuilder = buildRequest(courtroomEntity.getCourthouse().getCourthouseName(),
                                                                    courtroomEntity.getName(), "DAR");

        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(status().is2xxSuccessful()).andReturn();
        ObjectMapper objectMapper = new ObjectMapper();

        RegisterNodeResponse resp1 = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), RegisterNodeResponse.class);

        mvcResult = mockMvc.perform(requestBuilder).andExpect(status().is2xxSuccessful()).andReturn();
        RegisterNodeResponse resp2 = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), RegisterNodeResponse.class);

        assertNotEquals(resp1, resp2);
    }

    @Test
    void testEmptyStrings() throws Exception {
        dartsDatabase.createCourthouseWithTwoCourtrooms();

        CourtroomEntity courtroomEntity = dartsDatabase.findCourtroomBy("SWANSEA", "1");

        MockHttpServletRequestBuilder requestBuilder = buildRequest(courtroomEntity.getCourthouse().getCourthouseName(),
                                                                    courtroomEntity.getName(), "");

        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(status().is4xxClientError()).andReturn();
        assertTrue(mvcResult.getResponse().getContentAsString().contains("size must be between 1 and 2147483647"));
    }

    private MockHttpServletRequestBuilder buildRequest(String courthouseName, String courtroomName,
                                                       String nodeType) {
        @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
        String ipAddress = "192.0.0.1";
        return post("/register-devices")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .param("node_type", nodeType)
            .param("courthouse", courthouseName)
            .param("court_room", courtroomName)
            .param("host_name", "XXXXX.MMM.net")
            .param("ip_address", ipAddress)
            .param("mac_address", "6A-5F-90-A4-2C-12");
    }

}
