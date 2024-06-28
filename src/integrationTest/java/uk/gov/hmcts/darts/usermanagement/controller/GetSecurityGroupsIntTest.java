package uk.gov.hmcts.darts.usermanagement.controller;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.repository.SecurityGroupRepository;
import uk.gov.hmcts.darts.test.common.data.SecurityGroupTestData;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.CourthouseStub;
import uk.gov.hmcts.darts.testutils.stubs.SuperAdminUserStub;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class GetSecurityGroupsIntTest extends IntegrationBase {

    private static final String ENDPOINT_URL = "/admin/security-groups";
    @Autowired
    private SuperAdminUserStub superAdminUserStub;

    @Autowired
    CourthouseStub courthouseStub;

    @MockBean
    private UserIdentity userIdentity;

    @Autowired
    private SecurityGroupRepository securityGroupRepository;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void givenAUserNotAuthorisedThenReturnA403() throws Exception {
        superAdminUserStub.givenUserIsNotAuthorised(userIdentity);

        CourthouseEntity courthouseEntity = courthouseStub.createCourthouseUnlessExists("int-test-courthouse");
        SecurityGroupEntity securityGroupEntity = SecurityGroupTestData.buildGroupForRoleAndCourthouse(SecurityRoleEnum.APPROVER, courthouseEntity);
        securityGroupRepository.saveAndFlush(securityGroupEntity);

        mockMvc.perform(get(ENDPOINT_URL))
            .andExpect(status().isForbidden())
            .andExpect(MockMvcResultMatchers.jsonPath("$.type", Matchers.is("AUTHORISATION_109")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.status", Matchers.is(403)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.title", Matchers.is("User is not authorised for this endpoint")));

    }


}