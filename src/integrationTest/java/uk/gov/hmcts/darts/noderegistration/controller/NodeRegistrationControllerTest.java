package uk.gov.hmcts.darts.noderegistration.controller;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.NodeRegisterEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.noderegistration.model.PostNodeRegistrationResponse;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.DAR_PC;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.MID_TIER;

@Slf4j
@AutoConfigureMockMvc
@Transactional
class NodeRegistrationControllerTest extends IntegrationBase {

    @Autowired
    private transient MockMvc mockMvc;

    @MockBean
    private UserIdentity mockUserIdentity;


    @Test
    void testPostRegisterDevices() throws Exception {
        dartsDatabase.createCourthouseWithTwoCourtrooms();
        UserAccountEntity userCreated = setupExternalUserForCourthouse(null);

        CourtroomEntity courtroomEntity = dartsDatabase.findCourtroomBy("SWANSEA", "1");

        MockHttpServletRequestBuilder requestBuilder = buildRequest(courtroomEntity.getCourthouse().getCourthouseName(),
                                                                    courtroomEntity.getName(), "DAR");

        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(status().is2xxSuccessful()).andReturn();
        assertTrue(mvcResult.getResponse().getContentAsString().contains("node_id"));

        PostNodeRegistrationResponse response = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), PostNodeRegistrationResponse.class);
        Optional<NodeRegisterEntity> nodeRegisterEntity = dartsDatabase.findByNodeId(response.getNodeId());

        Assertions.assertEquals(userCreated.getId(), nodeRegisterEntity.get().getCreatedBy().getId());
    }

    @Test
    void testCourtroomNotExist() throws Exception {
        setupExternalUserForCourthouse(null);
        dartsDatabase.createCourthouseUnlessExists("SWANSEA");

        MockHttpServletRequestBuilder requestBuilder = buildRequest("SWANSEA", "999", "DAR");
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();
        CourtroomEntity courtroomEntity = dartsDatabase.findCourtroomBy("SWANSEA", "999");
        assertNotNull(courtroomEntity);
        String response = mvcResult.getResponse().getContentAsString();
        assertTrue(response.contains("node_id"));
    }

    @Test
    void testCourthouseNotExist() throws Exception {
        setupExternalUserForCourthouse(null);

        MockHttpServletRequestBuilder requestBuilder = buildRequest("SWANSEA", "999", "DAR");
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(status().isNotFound()).andReturn();
        String response = mvcResult.getResponse().getContentAsString();
        assertTrue(response.contains("Courthouse 'SWANSEA' not found."));
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

        setupExternalUserForCourthouse(null);

        CourtroomEntity courtroomEntity = dartsDatabase.findCourtroomBy("SWANSEA", "1");

        MockHttpServletRequestBuilder requestBuilder = buildRequest(courtroomEntity.getCourthouse().getCourthouseName(),
                                                                    courtroomEntity.getName(), "UPLOADER");

        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(status().is2xxSuccessful()).andReturn();
        assertTrue(mvcResult.getResponse().getContentAsString().contains("node_id"));
    }

    @Test
    void testAcceptsDuplicates() throws Exception {
        dartsDatabase.createCourthouseWithTwoCourtrooms();

        setupExternalUserForCourthouse(null);
        CourtroomEntity courtroomEntity = dartsDatabase.findCourtroomBy("SWANSEA", "1");

        MockHttpServletRequestBuilder requestBuilder = buildRequest(courtroomEntity.getCourthouse().getCourthouseName(),
                                                                    courtroomEntity.getName(), "DAR");

        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(status().is2xxSuccessful()).andReturn();

        PostNodeRegistrationResponse resp1 = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), PostNodeRegistrationResponse.class);

        mvcResult = mockMvc.perform(requestBuilder).andExpect(status().is2xxSuccessful()).andReturn();
        PostNodeRegistrationResponse resp2 = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), PostNodeRegistrationResponse.class);

        assertNotEquals(resp1, resp2);
    }

    @Test
    void testEmptyStrings() throws Exception {
        dartsDatabase.createCourthouseWithTwoCourtrooms();
        setupExternalUserForCourthouse(null);

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
            .param("courtroom", courtroomName)
            .param("host_name", "XXXXX.MMM.net")
            .param("ip_address", ipAddress)
            .param("mac_address", "6A-5F-90-A4-2C-12");
    }

    private UserAccountEntity setupExternalUserForCourthouse(CourthouseEntity courthouse) {
        String guid = UUID.randomUUID().toString();
        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().createDarPcExternalUser(guid, courthouse);
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);
        when(mockUserIdentity.userHasGlobalAccess(Set.of(MID_TIER, DAR_PC))).thenReturn(true);
        return testUser;
    }
}