package uk.gov.hmcts.darts.task.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.testutils.GivenBuilder;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;

@AutoConfigureMockMvc
class AdminEditAutomatedTaskCronExpressionTest extends IntegrationBase {

    private static final URI ENDPOINT = URI.create("/admin/automated-tasks/");
    private static final Instant FIXED_INSTANT = Instant.parse("2026-07-16T08:30:00Z");

    @Autowired
    private GivenBuilder given;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AutomatedTaskRepository automatedTaskRepository;

    @MockitoBean
    private Clock clock;

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(FIXED_INSTANT);
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
    }

    @Test
    void superAdminCanSuccessfullyPatchAutomatedTaskCronExpression() throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);

        var automatedTaskEntity = automatedTaskRepository.findById(1).orElseThrow();
        String originalCronExpression = automatedTaskEntity.getCronExpression();
        automatedTaskEntity.setCronEditable(true);
        automatedTaskRepository.saveAndFlush(automatedTaskEntity);

        try {
            mockMvc.perform(
                    patch(ENDPOINT + "/1/edit-cron-expression")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content("""
                                     { "cron_expression": "0 0 10 * * *" }
                                     """))
                .andExpect(status().isOk())
                .andReturn();

            automatedTaskEntity = automatedTaskRepository.findById(1).orElseThrow();
            assertEquals("0 0 10 * * *", automatedTaskEntity.getCronExpression());
        } finally {
            automatedTaskEntity = automatedTaskRepository.findById(1).orElseThrow();
            automatedTaskEntity.setCronExpression(originalCronExpression);
            automatedTaskRepository.saveAndFlush(automatedTaskEntity);
        }
    }

    @Test
    void superAdminCanSuccessfullyPostAutomatedTaskCronExpressionSchedulePreview() throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);

        var automatedTaskEntity = automatedTaskRepository.findById(1).orElseThrow();
        Boolean originalCronEditable = automatedTaskEntity.getCronEditable();
        automatedTaskEntity.setCronEditable(true);
        automatedTaskRepository.saveAndFlush(automatedTaskEntity);

        try {
            mockMvc.perform(
                    post(ENDPOINT + "/1/edit-cron-expression")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content("""
                                     { "cron_expression": "0 0 10 * * *" }
                                     """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(10)))
                .andExpect(jsonPath("$[0].execution_number").value("1"))
                .andExpect(jsonPath("$[0].scheduled_at").value("2026-07-16T10:00:00+01:00"))
                .andExpect(jsonPath("$[1].execution_number").value("2"))
                .andExpect(jsonPath("$[1].scheduled_at").value("2026-07-17T10:00:00+01:00"))
                .andExpect(jsonPath("$[9].execution_number").value("10"))
                .andExpect(jsonPath("$[9].scheduled_at").value("2026-07-25T10:00:00+01:00"))
                .andReturn();
        } finally {
            automatedTaskEntity = automatedTaskRepository.findById(1).orElseThrow();
            automatedTaskEntity.setCronEditable(originalCronEditable);
            automatedTaskRepository.saveAndFlush(automatedTaskEntity);
        }
    }

    @Test
    void returns400WhenPatchingCronExpressionForNonEditableTask() throws Exception {
        given.anAuthenticatedUserWithGlobalAccessAndRole(SUPER_ADMIN);

        var automatedTaskEntity = automatedTaskRepository.findById(1).orElseThrow();
        String originalCronExpression = automatedTaskEntity.getCronExpression();
        Boolean originalCronEditable = automatedTaskEntity.getCronEditable();
        automatedTaskEntity.setCronEditable(false);
        automatedTaskRepository.saveAndFlush(automatedTaskEntity);

        try {
            mockMvc.perform(
                    patch(ENDPOINT + "/1/edit-cron-expression")
                        .contentType(APPLICATION_JSON_VALUE)
                        .content("""
                                     { "cron_expression": "0 0 10 * * *" }
                                     """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("AUTOMATED_TASK_104"))
                .andExpect(jsonPath("$.title").value("The automated task request is incorrect"))
                .andReturn();

            automatedTaskEntity = automatedTaskRepository.findById(1).orElseThrow();
            assertEquals(originalCronExpression, automatedTaskEntity.getCronExpression());
        } finally {
            automatedTaskEntity = automatedTaskRepository.findById(1).orElseThrow();
            automatedTaskEntity.setCronEditable(originalCronEditable);
            automatedTaskEntity.setCronExpression(originalCronExpression);
            automatedTaskRepository.saveAndFlush(automatedTaskEntity);
        }
    }
}
