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

import java.time.ZoneOffset;
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
    private NodeRegisterEntity nodeRegisterEntity2;

    @BeforeEach
    void setupData() {
        var node = PersistableFactory.getNodeRegisterTestData().someMinimal();
        var node2 = PersistableFactory.getNodeRegisterTestData().someMinimal();
        nodeRegisterEntity = dartsPersistence.save(node);
        nodeRegisterEntity2 = dartsPersistence.save(node2);
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
                    "created_at": "<created_at1>",
                    "created_by": <created_by1>
                },
                {
                    "id": <nodeid2>,
                    "courthouse": {
                        "id": 2,
                        "display_name": "Some Courthouse"
                    },
                    "courtroom": {
                        "id": 2,
                        "name": "<courtroom2>"
                    },
                    "ip_address": "192.168.1.3",
                    "hostname": "Host1",
                    "mac_address": "00:0a:95:9d:68:21",
                    "node_type": "DAR",
                    "created_at": "<created_at2>",
                    "created_by": <created_by2>
                }
            ]
            """
            .replace("<nodeid>", String.valueOf(nodeRegisterEntity.getNodeId()))
            .replace("<courtroom>", nodeRegisterEntity.getCourtroom().getName())
            .replace("<created_at1>", nodeRegisterEntity.getCreatedDateTime().format(DateTimeFormatter.ISO_ZONED_DATE_TIME))
            .replace("<created_by1>", String.valueOf(nodeRegisterEntity.getCreatedById()))
            .replace("<nodeid2>", String.valueOf(nodeRegisterEntity2.getNodeId()))
            .replace("<courtroom2>", nodeRegisterEntity2.getCourtroom().getName())
            .replace("<created_at2>", nodeRegisterEntity2.getCreatedDateTime().format(DateTimeFormatter.ISO_ZONED_DATE_TIME))
            .replace("<created_by2>", String.valueOf(nodeRegisterEntity2.getCreatedById()));
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