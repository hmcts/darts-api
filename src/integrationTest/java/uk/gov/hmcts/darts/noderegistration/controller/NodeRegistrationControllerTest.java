package uk.gov.hmcts.darts.noderegistration.controller;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

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

        @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
        String ipAddress = "192.0.0.1";
        MockHttpServletRequestBuilder requestBuilder = post("/register-devices")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .param("node_type", "DAR")
            .param("courthouse", courtroomEntity.getCourthouse().getCourthouseName())
            .param("court_room", courtroomEntity.getName())
            .param("host_name", "XXXXX.MMM.net")
            .param("ip_address", ipAddress)
            .param("mac_address", "6A-5F-90-A4-2C-12");
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(status().is2xxSuccessful()).andReturn();
        Assertions.assertTrue(mvcResult.getResponse().getContentAsString().contains("node_id"));


    }
}
