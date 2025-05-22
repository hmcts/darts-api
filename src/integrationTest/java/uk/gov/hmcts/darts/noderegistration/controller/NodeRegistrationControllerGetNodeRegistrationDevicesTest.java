package uk.gov.hmcts.darts.noderegistration.controller;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.NodeRegisterEntity;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.SuperAdminUserStub;

import java.time.format.DateTimeFormatter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@Slf4j
@AutoConfigureMockMvc
class NodeRegistrationControllerGetNodeRegistrationDevicesTest extends IntegrationBase {

    private static final String ENDPOINT_URL = "/admin/node-register-management";

    @Autowired
    private transient MockMvc mockMvc;

    @MockitoBean
    private UserIdentity userIdentity;

    @Autowired
    private SuperAdminUserStub superAdminUserStub;

    private NodeRegisterEntity nodeRegisterEntity;

    @BeforeEach
    void setupData() {
        var node = PersistableFactory.getNodeRegisterTestData().someMinimal();
        nodeRegisterEntity = dartsPersistence.save(node);
    }

    @Test
    void adminNodeRegisterManagementGet_shouldReturnNodeRegisters_whenSuperAdminRequest() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);
        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL);
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();
        String expectedJson = """
            [
                {
                    "id": <nodeid>,
                    "courthouse": {
                        "id": 1,
                        "display_name": "Some Courthouse"
                    },
                    "courtroom": {
                        "id": 1,
                        "name": "<courtroom>"
                    },
                    "ip_address": "192.168.1.3",
                    "hostname": "Host1",
                    "mac_address": "00:0a:95:9d:68:21",
                    "node_type": "DAR",
                    "created_at": "<created_at>",
                    "created_by": <created_by>
                    }
                ]
            """
            .replace("<nodeid>", String.valueOf(nodeRegisterEntity.getNodeId()))
            .replace("<courtroom>", nodeRegisterEntity.getCourtroom().getName())
            .replace("<created_at>", nodeRegisterEntity.getCreatedDateTime().format(DateTimeFormatter.ISO_DATE_TIME))
            .replace("<created_by>", String.valueOf(nodeRegisterEntity.getCreatedById()));
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void adminNodeRegisterManagementGet_whenNotAuthenticated_shouldReturnForbiddenError() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL);

        MvcResult response = mockMvc.perform(requestBuilder)
            .andExpect(MockMvcResultMatchers.status().isForbidden())
            .andReturn();

        String actualResponse = response.getResponse().getContentAsString();

        String expectedResponse = """
            {"type":"AUTHORISATION_109","title":"User is not authorised for this endpoint","status":403}
            """;
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }
}