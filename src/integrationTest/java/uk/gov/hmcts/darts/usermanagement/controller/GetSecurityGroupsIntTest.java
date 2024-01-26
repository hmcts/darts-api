package uk.gov.hmcts.darts.usermanagement.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.repository.SecurityGroupRepository;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.data.SecurityGroupTestData;
import uk.gov.hmcts.darts.testutils.stubs.AdminUserStub;
import uk.gov.hmcts.darts.testutils.stubs.CourthouseStub;
import uk.gov.hmcts.darts.usermanagement.model.SecurityGroupWithIdAndRole;

import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class GetSecurityGroupsIntTest extends IntegrationBase {

    private static final String ENDPOINT_URL = "/admin/security-groups";
    @Autowired
    private AdminUserStub adminUserStub;

    @Autowired
    CourthouseStub courthouseStub;

    @MockBean
    private UserIdentity userIdentity;

    @Autowired
    private SecurityGroupRepository securityGroupRepository;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnAllSecurityGroupsWithCourthouseIds() throws Exception {
        adminUserStub.givenUserIsAuthorised(userIdentity);

        MvcResult mvcResult = mockMvc.perform(get(ENDPOINT_URL))
            .andExpect(status().isOk())
            .andReturn();

        List<SecurityGroupWithIdAndRole> groups = objectMapper.readValue(mvcResult.getResponse().getContentAsString(),
                               new TypeReference<List<SecurityGroupWithIdAndRole>>(){});

        assertFalse(groups.isEmpty());

        groups.sort(Comparator.comparingInt(SecurityGroupWithIdAndRole::getId).reversed());

        checkGroup(groups.get(0), "ADMIN", true, 11, true);
        checkGroup(groups.get(1), "Test Approver", false, 1, true);
        checkGroup(groups.get(2), "Test Requestor", false, 2, true);
        checkGroup(groups.get(3), "Test Judge", false, 3, true);
        checkGroup(groups.get(4), "Test Transcriber", false, 4, true);
        checkGroup(groups.get(5), "Test Language Shop", false, 5, true);
        checkGroup(groups.get(6), "Test RCJ Appeals", false, 6, true);
        checkGroup(groups.get(7), "Xhibit Group", true, 7, true);
        checkGroup(groups.get(8), "Cpp Group", true, 8, true);
        checkGroup(groups.get(9), "Dar Pc Group", true, 9, true);
        checkGroup(groups.get(10), "Mid Tier Group", true, 10, true);
    }

    private void checkGroup(SecurityGroupWithIdAndRole group, String name, boolean globalAccess, Integer roleId, boolean displayState) {
        assertEquals(name, group.getName());
        assertEquals(globalAccess, group.getGlobalAccess());
        assertEquals(roleId, group.getSecurityRoleId());
        assertEquals(displayState, group.getDisplayState());
        //assertTrue(group.getCourthouseIds().contains(1));
    }

    @Test
    void givenAUserNotAuthorisedThenReturnA403() throws Exception {
        adminUserStub.givenUserIsNotAuthorised(userIdentity);

        CourthouseEntity courthouseEntity = courthouseStub.createCourthouseUnlessExists("int-test-courthouse");
        SecurityGroupEntity securityGroupEntity = SecurityGroupTestData.buildGroupForRoleAndCourthouse(SecurityRoleEnum.APPROVER, courthouseEntity);
        securityGroupRepository.saveAndFlush(securityGroupEntity);

        MvcResult mvcResult = mockMvc.perform(get(ENDPOINT_URL))
            .andExpect(status().isForbidden())
            .andReturn();

        String expectedResponse = """
            {"type":"AUTHORISATION_109","title":"User is not authorised for this endpoint","status":403}""";

        assertEquals(expectedResponse, mvcResult.getResponse().getContentAsString());
    }


    }
