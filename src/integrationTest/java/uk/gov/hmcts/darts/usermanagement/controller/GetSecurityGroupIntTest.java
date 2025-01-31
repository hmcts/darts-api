package uk.gov.hmcts.darts.usermanagement.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.model.AuthorisationErrorCode;
import uk.gov.hmcts.darts.common.model.AuthorisationTitleErrors;
import uk.gov.hmcts.darts.common.repository.SecurityGroupRepository;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.SuperAdminUserStub;
import uk.gov.hmcts.darts.usermanagement.model.UserManagementErrorCode;
import uk.gov.hmcts.darts.usermanagement.model.UserManagementTitleErrors;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class GetSecurityGroupIntTest extends IntegrationBase {

    private static final String ENDPOINT_URL = "/admin/security-groups/{security_group_id}";

    @Autowired
    private SecurityGroupRepository securityGroupRepository;

    @Autowired
    private SuperAdminUserStub superAdminUserStub;

    @MockitoBean
    private UserIdentity userIdentity;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnSecurityGroupWithUserIds() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        Integer securityGroupId = securityGroupRepository.findByGroupNameIgnoreCase("SUPER_USER").get().getId();

        mockMvc.perform(get(ENDPOINT_URL, securityGroupId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user_ids").isEmpty())
            .andExpect(jsonPath("$.id").value(securityGroupId))
            .andExpect(jsonPath("$.security_role_id").value(SecurityRoleEnum.SUPER_USER.getId()))
            .andExpect(jsonPath("$.global_access").value(true))
            .andExpect(jsonPath("$.display_state").value(true))
            .andExpect(jsonPath("$.courthouse_ids").isArray())
            .andExpect(jsonPath("$.name").value("SUPER_USER"))
            .andExpect(jsonPath("$.display_name").value("Super User"));
    }

    @Test
    void shouldReturnNotFoundResponseWhenSecurityGroupIdDoesNotExist() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        Integer securityGroupId = -999;

        mockMvc.perform(get(ENDPOINT_URL, securityGroupId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.type").value(UserManagementErrorCode.SECURITY_GROUP_NOT_FOUND.getValue()))
            .andExpect(jsonPath("$.title").value(UserManagementTitleErrors.SECURITY_GROUP_NOT_FOUND.getValue()))
            .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void shouldReturnForbiddenResponseWhenUserIsNotAuthorised() throws Exception {
        superAdminUserStub.givenUserIsNotAuthorised(userIdentity);

        Integer securityGroupId = securityGroupRepository.findByGroupNameIgnoreCase("SUPER_USER").get().getId();

        mockMvc.perform(get(ENDPOINT_URL, securityGroupId))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.type").value(AuthorisationErrorCode.USER_NOT_AUTHORISED_FOR_ENDPOINT.getValue()))
            .andExpect(jsonPath("$.title").value(AuthorisationTitleErrors.USER_NOT_AUTHORISED_FOR_ENDPOINT.getValue()))
            .andExpect(jsonPath("$.status").value(403));
    }

}
