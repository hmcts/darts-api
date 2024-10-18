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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;

@AutoConfigureMockMvc
class AdminGetAutomatedTasksByIdTest extends IntegrationBase {

    private static final URI ENDPOINT = URI.create("/admin/automated-tasks/");

    @Autowired
    private GivenBuilder given;

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @EnumSource(value = SecurityRoleEnum.class, names = {"SUPER_ADMIN"}, mode = Mode.INCLUDE)
    void allowsSuperAdminToRetrieveAllAutomatedTasks(SecurityRoleEnum role) throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(role);

        mockMvc.perform(
                get(ENDPOINT + "/8"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.is_cron_editable").value(true))
            .andExpect(jsonPath("$.batch_size").value(100_000))
            .andExpect(jsonPath("$.created_at").isNotEmpty())
            .andExpect(jsonPath("$.created_by").value(0))
            .andExpect(jsonPath("$.last_modified_at").isNotEmpty())
            .andExpect(jsonPath("$.last_modified_by").value(0))
            .andExpect(jsonPath("$.id").value(8))
            .andExpect(jsonPath("$.name").value("UnstructuredToArmDataStore"))
            .andExpect(jsonPath("$.description").value("Move files from Unstructured to ARM data store"))
            .andExpect(jsonPath("$.cron_expression").value("0 7 * * * *"))
            .andExpect(jsonPath("$.is_active").value(false))
            .andReturn();

    }

    @ParameterizedTest
    @EnumSource(value = SecurityRoleEnum.class, names = {"SUPER_ADMIN"}, mode = Mode.EXCLUDE)
    void disallowsAllUsersExceptSuperAdminToRetrieveAllAutomatedTasks(SecurityRoleEnum role) throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(role);

        mockMvc.perform(
                get(ENDPOINT + "/1"))
            .andExpect(status().isForbidden())
            .andReturn();
    }


    @Test
    void returns404WhenTheTaskDoesntExist() throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);

        mockMvc.perform(
                get(ENDPOINT + "/999"))
            .andExpect(status().isNotFound())
            .andReturn();
    }
}