package uk.gov.hmcts.darts.task.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.darts.audit.api.AuditActivity;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.AuditEntity;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;
import uk.gov.hmcts.darts.common.repository.AuditRepository;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.tasks.model.AutomatedTaskPatch;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.SuperAdminUserStub;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("PMD.ExcessiveImports")
class AdminUpdateAutomatedTaskServiceTest extends IntegrationBase {

    @Autowired
    private AdminAutomatedTaskService adminAutomatedTaskService;
    private boolean initialValue;
    private AutomatedTaskEntity persistedAutomatedTaskEntity;

    @Autowired
    private AuditRepository auditRepository;

    @Autowired
    private SuperAdminUserStub superAdminUserStub;

    @MockitoBean
    private UserIdentity userIdentity;

    @Autowired
    private AutomatedTaskRepository automatedTaskRepository;

    @AfterEach
    void tearDown() {
        persistedAutomatedTaskEntity.setTaskEnabled(initialValue);
        dartsDatabase.save(persistedAutomatedTaskEntity);
    }

    @Test
    void canUpdateIsEnabledFieldToFalse() {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        persistedAutomatedTaskEntity = dartsDatabase.getAutomatedTask(1);
        initialValue = persistedAutomatedTaskEntity.getTaskEnabled(); // So we can revert it later as part of test clean up
        persistedAutomatedTaskEntity.setTaskEnabled(true);
        dartsDatabase.save(persistedAutomatedTaskEntity);

        adminAutomatedTaskService.updateAutomatedTask(1, new AutomatedTaskPatch().isActive(false));

        Assertions.assertEquals(1, auditRepository.findAll().size());
        AuditEntity auditEntity = auditRepository.findAll().get(0);
        Assertions.assertEquals(AuditActivity.ENABLE_DISABLE_JOB.getId(), auditEntity.getAuditActivity().getId());
        Assertions.assertEquals("ProcessDailyList disabled", auditEntity.getAdditionalData());

        Assertions.assertFalse(automatedTaskRepository.findRevisions(1).isEmpty());

        var updatedAutomatedTaskEntity = dartsDatabase.getAutomatedTask(1);
        assertThat(updatedAutomatedTaskEntity.getTaskEnabled()).isFalse();
    }

    @Test
    void canUpdateIsEnabledFieldToTrue() {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        persistedAutomatedTaskEntity = dartsDatabase.getAutomatedTask(1);
        initialValue = persistedAutomatedTaskEntity.getTaskEnabled(); // So we can revert it later as part of test clean up
        persistedAutomatedTaskEntity.setTaskEnabled(false);
        dartsDatabase.save(persistedAutomatedTaskEntity);

        adminAutomatedTaskService.updateAutomatedTask(1, new AutomatedTaskPatch().isActive(true));

        Assertions.assertEquals(1, auditRepository.findAll().size());
        AuditEntity auditEntity = auditRepository.findAll().get(0);
        Assertions.assertEquals(AuditActivity.ENABLE_DISABLE_JOB.getId(), auditEntity.getAuditActivity().getId());
        Assertions.assertEquals("ProcessDailyList enabled", auditEntity.getAdditionalData());

        Assertions.assertFalse(automatedTaskRepository.findRevisions(1).isEmpty());

        var updatedAutomatedTaskEntity = dartsDatabase.getAutomatedTask(1);
        assertThat(updatedAutomatedTaskEntity.getTaskEnabled()).isTrue();
    }

    @Test
    void returnsDetailedAutomatedTaskAfterUpdate() {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);

        persistedAutomatedTaskEntity = dartsDatabase.getAutomatedTask(1);
        initialValue = persistedAutomatedTaskEntity.getTaskEnabled(); // So we can revert it later as part of test clean up

        var response = adminAutomatedTaskService.updateAutomatedTask(1, new AutomatedTaskPatch().isActive(true));

        var updatedAutomatedTaskEntity = dartsDatabase.getAutomatedTask(1);

        assertThat(response.getId()).isEqualTo(1);
        assertThat(response.getName()).isEqualTo(updatedAutomatedTaskEntity.getTaskName());
        assertThat(response.getDescription()).isEqualTo(updatedAutomatedTaskEntity.getTaskDescription());
        assertThat(response.getCronExpression()).isEqualTo(updatedAutomatedTaskEntity.getCronExpression());
        assertThat(response.getIsActive()).isEqualTo(updatedAutomatedTaskEntity.getTaskEnabled());
        assertThat(response.getIsCronEditable()).isEqualTo(updatedAutomatedTaskEntity.getCronEditable());
        assertThat(response.getCreatedAt()).isEqualTo(updatedAutomatedTaskEntity.getCreatedDateTime());
        assertThat(response.getCreatedBy()).isEqualTo(updatedAutomatedTaskEntity.getCreatedBy().getId());
        assertThat(response.getLastModifiedAt()).isEqualTo(updatedAutomatedTaskEntity.getLastModifiedDateTime());
        assertThat(response.getLastModifiedBy()).isEqualTo(updatedAutomatedTaskEntity.getLastModifiedBy().getId());
    }
}