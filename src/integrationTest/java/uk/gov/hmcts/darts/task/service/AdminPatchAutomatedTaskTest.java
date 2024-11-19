package uk.gov.hmcts.darts.task.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.darts.common.entity.ArmAutomatedTaskEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.repository.ArmAutomatedTaskRepository;
import uk.gov.hmcts.darts.testutils.GivenBuilder;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.EnumSource.Mode;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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

    @Autowired
    ArmAutomatedTaskRepository armAutomatedTaskRepository;

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

    @Test
    void returns422InvalidAutomatedTaskTypeWhenTheTaskIsNotArmButHasArmUpdateFields() throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);

        mockMvc.perform(
                patch(ENDPOINT + "/1")
                    .contentType(APPLICATION_JSON_VALUE)
                    .content("""
                                 { "rpo_csv_end_hour": 1 }
                                 """))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.type").value("AUTOMATED_TASK_103"))
            .andExpect(jsonPath("$.title").value("The automated task type is incorrect"))
            .andReturn();
    }

    @ParameterizedTest
    @EnumSource(value = SecurityRoleEnum.class, names = {"SUPER_ADMIN"}, mode = Mode.INCLUDE)
    void superAdminCanSuccessfullyPatchArmAutomatedTask(SecurityRoleEnum role) throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(role);

        ArmAutomatedTaskEntity armAutomatedTaskEntity = armAutomatedTaskRepository.findById(2).orElseThrow();
        armAutomatedTaskEntity.setRpoCsvEndHour(1);
        armAutomatedTaskRepository.save(armAutomatedTaskEntity);

        mockMvc.perform(
                patch(ENDPOINT + "/30")
                    .contentType(APPLICATION_JSON_VALUE)
                    .content("""
                                 { "rpo_csv_end_hour": 2 }
                                 """))
            .andExpect(status().isOk())
            .andReturn();

        armAutomatedTaskEntity = armAutomatedTaskRepository.findById(2).orElseThrow();
        assertEquals(2, armAutomatedTaskEntity.getRpoCsvEndHour());
    }
}