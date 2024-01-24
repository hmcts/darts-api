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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class GetSecurityGroupsTest extends IntegrationBase {

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

        CourthouseEntity courthouseEntity = courthouseStub.createCourthouseUnlessExists("func-test-courthouse");
        SecurityGroupEntity securityGroupEntity = SecurityGroupTestData.buildGroupForRoleAndCourthouse(SecurityRoleEnum.APPROVER, courthouseEntity);
        securityGroupRepository.saveAndFlush(securityGroupEntity);

        MvcResult mvcResult = mockMvc.perform(get(ENDPOINT_URL))
            .andExpect(status().isOk())
            .andReturn();

        List<SecurityGroupWithIdAndRole> groups = objectMapper.readValue(mvcResult.getResponse().getContentAsString(),
                               new TypeReference<List<SecurityGroupWithIdAndRole>>(){});
        SecurityGroupWithIdAndRole securityGroupWithIdAndRole = groups.stream().filter(g -> "some-group-name".equals(g.getName())).findFirst().orElse(null);
        assertEquals(securityGroupWithIdAndRole.getSecurityRoleId(), 1);
        assertTrue(securityGroupWithIdAndRole.getCourthouseIds().contains(courthouseEntity.getId()));
    }

    @Test
    void givenAUserNotAuthorisedThenReturnA403() throws Exception {
        adminUserStub.givenUserIsNotAuthorised(userIdentity);

        CourthouseEntity courthouseEntity = courthouseStub.createCourthouseUnlessExists("func-test-courthouse");
        SecurityGroupEntity securityGroupEntity = SecurityGroupTestData.buildGroupForRoleAndCourthouse(SecurityRoleEnum.APPROVER, courthouseEntity);
        securityGroupRepository.saveAndFlush(securityGroupEntity);

        mockMvc.perform(get(ENDPOINT_URL))
            .andExpect(status().isForbidden())
            .andReturn();
    }


    }
