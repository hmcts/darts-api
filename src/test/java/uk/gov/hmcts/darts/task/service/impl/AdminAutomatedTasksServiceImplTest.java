package uk.gov.hmcts.darts.task.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audit.api.AuditActivity;
import uk.gov.hmcts.darts.audit.api.AuditApi;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.task.runner.impl.AbstractLockableAutomatedTask;
import uk.gov.hmcts.darts.task.service.AdminAutomatedTaskService;
import uk.gov.hmcts.darts.tasks.model.AutomatedTaskPatch;
import uk.gov.hmcts.darts.tasks.model.DetailedAutomatedTask;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAutomatedTasksServiceImplTest {

    @Mock
    private AutomatedTaskRepository automatedTaskRepository;
    @Mock
    private AutomatedTasksMapper mapper;
    @Mock
    private ManualTaskService manualTaskService;
    @Mock
    private AutomatedTaskRunner automatedTaskRunner;
    @Mock
    private CurrentTimeHelper currentTimeHelper;
    @Mock
    private AbstractLockableAutomatedTask someAutomatedTask;

    @Mock
    private AuditApi auditApi;

    private AdminAutomatedTaskService adminAutomatedTaskService;

    @BeforeEach
    void setUp() {
        adminAutomatedTaskService = new AdminAutomatedTasksServiceImpl(
            automatedTaskRepository,
            mapper,
            manualTaskService,
            automatedTaskRunner,
            currentTimeHelper,
            auditApi
        );
    }

    @Test
    void invokesTaskWhenTaskIsNotLocked() {
        when(someAutomatedTask.getTaskName()).thenReturn("some-task-name");
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(OffsetDateTime.now());

        when(automatedTaskRepository.findById(1)).thenReturn(anAutomatedTaskEntityWithName("some-task-name"));
        when(automatedTaskRepository.findLockedUntilForTask("some-task-name")).thenReturn(anSqlDateInThePast());
        when(manualTaskService.getAutomatedTasks()).thenReturn(List.of(someAutomatedTask));

        adminAutomatedTaskService.runAutomatedTask(1);

        verify(automatedTaskRunner, times(1)).run(someAutomatedTask);
        verify(auditApi, times(1)).record(AuditActivity.RUN_JOB_MANUALLY);
    }

    @Test
    void updateAutomatedTask() {
        Optional<AutomatedTaskEntity> automatedTaskEntity = anAutomatedTaskEntityWithName("some-task-name");
        when(automatedTaskRepository.findById(1)).thenReturn(automatedTaskEntity);

        AutomatedTaskPatch automatedTaskPatch = new AutomatedTaskPatch();
        automatedTaskPatch.setIsActive(false);

        when(automatedTaskRepository.save(automatedTaskEntity.get())).thenReturn(automatedTaskEntity.get());

        DetailedAutomatedTask expectedReturnTask = new DetailedAutomatedTask();
        when(mapper.mapEntityToDetailedAutomatedTask(automatedTaskEntity.get())).thenReturn(expectedReturnTask);

        DetailedAutomatedTask task = adminAutomatedTaskService.updateAutomatedTask(1, automatedTaskPatch);

        assertFalse(automatedTaskEntity.get().getTaskEnabled());
        assertEquals(expectedReturnTask, task);
        verify(auditApi, times(1)).record(AuditActivity.ENABLE_DISABLE_JOB);
    }

    private List<Timestamp> anSqlDateInThePast() {
        var epochSecond = LocalDateTime.now().minusWeeks(1).toEpochSecond(ZoneOffset.UTC);
        var epochMillis = epochSecond * 1000;
        return List.of(new Timestamp(epochMillis));
    }

    private Optional<AutomatedTaskEntity> anAutomatedTaskEntityWithName(String taskName) {
        var automatedTaskEntity = new AutomatedTaskEntity();
        automatedTaskEntity.setTaskName(taskName);
        automatedTaskEntity.setId(1234);
        automatedTaskEntity.setTaskEnabled(true);
        automatedTaskEntity.setCronEditable(true);
        automatedTaskEntity.setTaskDescription("task description");
        automatedTaskEntity.setTaskName(taskName != null ? taskName : "task name");
        automatedTaskEntity.setCronExpression("");
        UserAccountEntity accountEntity = new UserAccountEntity();
        accountEntity.setId(999);

        automatedTaskEntity.setLastModifiedBy(accountEntity);
        automatedTaskEntity.setCreatedDateTime(OffsetDateTime.now());
        automatedTaskEntity.setCreatedBy(accountEntity);
        automatedTaskEntity.setLastModifiedDateTime(OffsetDateTime.now());

        return Optional.of(automatedTaskEntity);
    }
}