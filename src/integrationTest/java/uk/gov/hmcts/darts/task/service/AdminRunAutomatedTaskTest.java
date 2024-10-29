package uk.gov.hmcts.darts.task.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.darts.audit.api.AuditActivity;
import uk.gov.hmcts.darts.common.entity.AuditEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.repository.AuditRepository;
import uk.gov.hmcts.darts.testutils.GivenBuilder;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.net.URI;
import java.time.OffsetDateTime;

import static org.junit.jupiter.params.provider.EnumSource.Mode;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;

@AutoConfigureMockMvc
class AdminRunAutomatedTaskTest extends IntegrationBase {

    private static final URI ENDPOINT = URI.create("/admin/automated-tasks/1/run");
    private static final URI ENDPOINT_WITH_NON_EXISTING_TASK_ID = URI.create("/admin/automated-tasks/-1/run");

    @Autowired
    private GivenBuilder given;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditRepository auditRepository;

    @ParameterizedTest
    @EnumSource(value = SecurityRoleEnum.class, names = {"SUPER_ADMIN"}, mode = Mode.INCLUDE)
    void allowsSuperAdminToRetrieveAllAutomatedTasks(SecurityRoleEnum role) throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(role);

        mockMvc.perform(
                post(ENDPOINT))
            .andExpect(status().isAccepted())
            .andReturn();

        Assertions.assertEquals(1, auditRepository.findAll().size());
        AuditEntity auditEntity = auditRepository.findAll().get(0);
        Assertions.assertEquals(AuditActivity.RUN_JOB_MANUALLY.getId(), auditEntity.getAuditActivity().getId());
        Assertions.assertEquals("ProcessDailyList", auditEntity.getAdditionalData());
    }

    @ParameterizedTest
    @EnumSource(value = SecurityRoleEnum.class, names = {"SUPER_ADMIN"}, mode = Mode.EXCLUDE)
    void disallowsAllUsersExceptSuperAdminToRetrieveAllAutomatedTasks(SecurityRoleEnum role) throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(role);

        mockMvc.perform(
                post(ENDPOINT))
            .andExpect(status().isForbidden())
            .andReturn();
    }

    @Test
    void returns409WhenTaskIsAlreadyRunning() throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);

        dartsDatabase.lockTaskUntil("ProcessDailyList", OffsetDateTime.now().plusWeeks(1));

        mockMvc.perform(
                post(ENDPOINT))
            .andExpect(status().isConflict())
            .andReturn();
    }

    @Test
    void returns202WhenTaskIsNotAlreadyRunning() throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);

        dartsDatabase.lockTaskUntil("ProcessDailyList", OffsetDateTime.now().minusWeeks(1));

        mockMvc.perform(
                post(ENDPOINT))
            .andExpect(status().isAccepted())
            .andReturn();
    }

    @Test
    void returns404WhenTaskDoesNotExist() throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);

        mockMvc.perform(
                post(ENDPOINT_WITH_NON_EXISTING_TASK_ID))
            .andExpect(status().isNotFound())
            .andReturn();
    }
}