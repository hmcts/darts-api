package uk.gov.hmcts.darts.usermanagement.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.model.AuthorisationErrorCode;
import uk.gov.hmcts.darts.common.model.AuthorisationTitleErrors;
import uk.gov.hmcts.darts.common.repository.SecurityGroupRepository;
import uk.gov.hmcts.darts.testutils.GivenBuilder;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
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
    private MockMvc mockMvc;

    @Autowired
    private GivenBuilder given;

    @ParameterizedTest
    @EnumSource(value = SecurityRoleEnum.class, names = {"SUPER_ADMIN", "SUPER_USER"}, mode = EnumSource.Mode.INCLUDE)
    void adminGetSecurityGroup_shouldReturnSecurityGroup_whenSuperUserOrSuperAdmin(SecurityRoleEnum role) throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(role);

        Integer securityGroupId = securityGroupRepository.findByGroupNameIgnoreCase("MEDIA_IN_PERPETUITY").get().getId();

        mockMvc.perform(get(ENDPOINT_URL, securityGroupId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user_ids").isEmpty())
            .andExpect(jsonPath("$.id").value(securityGroupId))
            .andExpect(jsonPath("$.security_role_id").value(SecurityRoleEnum.MEDIA_IN_PERPETUITY.getId()))
            .andExpect(jsonPath("$.global_access").value(true))
            .andExpect(jsonPath("$.display_state").value(true))
            .andExpect(jsonPath("$.courthouse_ids").isArray())
            .andExpect(jsonPath("$.name").value("MEDIA_IN_PERPETUITY"))
            .andExpect(jsonPath("$.display_name").value("MEDIA_IN_PERPETUITY"))
            .andReturn();
    }

    @Test
    void adminGetSecurityGroup_shouldReturnNotFoundResponse_whenSecurityGroupIdDoesNotExist() throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(SecurityRoleEnum.SUPER_ADMIN);

        Integer securityGroupId = -999;

        mockMvc.perform(get(ENDPOINT_URL, securityGroupId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.type").value(UserManagementErrorCode.SECURITY_GROUP_NOT_FOUND.getValue()))
            .andExpect(jsonPath("$.title").value(UserManagementTitleErrors.SECURITY_GROUP_NOT_FOUND.getValue()))
            .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void adminGetSecurityGroup_shouldReturnForbiddenResponse_whenUserIsNotAuthorised() throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(SecurityRoleEnum.JUDICIARY);

        Integer securityGroupId = securityGroupRepository.findByGroupNameIgnoreCase("SUPER_USER").get().getId();

        mockMvc.perform(get(ENDPOINT_URL, securityGroupId))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.type").value(AuthorisationErrorCode.USER_NOT_AUTHORISED_FOR_ENDPOINT.getValue()))
            .andExpect(jsonPath("$.title").value(AuthorisationTitleErrors.USER_NOT_AUTHORISED_FOR_ENDPOINT.getValue()))
            .andExpect(jsonPath("$.status").value(403));
    }

}
