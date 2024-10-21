package uk.gov.hmcts.darts.task.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.testutils.GivenBuilder;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.net.URI;

import static org.junit.jupiter.params.provider.EnumSource.Mode;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;

@AutoConfigureMockMvc
class AdminPatchAutomatedTaskTest extends IntegrationBase {

    private static final URI ENDPOINT = URI.create("/admin/automated-tasks/");

    @Autowired
    private GivenBuilder given;

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @EnumSource(value = SecurityRoleEnum.class, names = {"SUPER_ADMIN"}, mode = Mode.INCLUDE)
    void superAdminCanSuccessfullyPatchAutomatedTask(SecurityRoleEnum role) throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(role);

        mockMvc.perform(
                patch(ENDPOINT + "/1")
                    .contentType(APPLICATION_JSON_VALUE)
                    .content("""
                                 { "is_active": false, "batch_size": 10 }
                                 """))
            .andExpect(status().isOk())
            .andReturn();

        // reset data to predefined status
        mockMvc.perform(
                patch(ENDPOINT + "/1")
                    .contentType(APPLICATION_JSON_VALUE)
                    .content("""
                                 { "is_active": true }
                                 """))
            .andExpect(status().isOk())
            .andReturn();
    }

    @ParameterizedTest
    @EnumSource(value = SecurityRoleEnum.class, names = {"SUPER_ADMIN"}, mode = Mode.EXCLUDE)
    void disallowsAllUsersExceptSuperAdminToPatchAutomatedTasks(SecurityRoleEnum role) throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(role);

        mockMvc.perform(
                patch(ENDPOINT + "/1")
                    .contentType(APPLICATION_JSON_VALUE)
                    .content("""
                                 { "is_active": false }
                                 """))
            .andExpect(status().isForbidden())
            .andReturn();
    }

    @Test
    void returns404WhenTheTaskDoesntExist() throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);

        mockMvc.perform(
                patch(ENDPOINT + "/-1")
                    .contentType(APPLICATION_JSON_VALUE)
                    .content("""
                                 { "is_active": false }
                                 """))
            .andExpect(status().isNotFound())
            .andReturn();
    }


}