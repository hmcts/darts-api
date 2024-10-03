package uk.gov.hmcts.darts.task.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import uk.gov.hmcts.darts.audit.api.AuditActivity;
import uk.gov.hmcts.darts.audit.api.AuditApi;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.task.exception.AutomatedTaskApiError;
import uk.gov.hmcts.darts.task.runner.impl.AbstractLockableAutomatedTask;
import uk.gov.hmcts.darts.task.service.LockService;
import uk.gov.hmcts.darts.tasks.model.AutomatedTaskPatch;
import uk.gov.hmcts.darts.tasks.model.DetailedAutomatedTask;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
    private LockService lockService;
    @Mock
    private AbstractLockableAutomatedTask someAutomatedTask;

    @Mock
    private AuditApi auditApi;
    @Mock
    private ConfigurableBeanFactory configurableBeanFactory;

    @InjectMocks
    private AdminAutomatedTasksServiceImpl adminAutomatedTaskService;


    @Test
    void invokesTaskWhenTaskIsNotLocked() {
        var automatedTask = anAutomatedTaskEntityWithName("some-task-name", null);
        when(someAutomatedTask.getTaskName()).thenReturn("some-task-name");
        when(lockService.isLocked(automatedTask.get())).thenReturn(false);

        when(automatedTaskRepository.findById(1)).thenReturn(automatedTask);
        when(manualTaskService.getAutomatedTasks()).thenReturn(List.of(someAutomatedTask));

        adminAutomatedTaskService.runAutomatedTask(1);

        verify(automatedTaskRunner, times(1)).run(someAutomatedTask);
        verify(auditApi, times(1)).record(AuditActivity.RUN_JOB_MANUALLY);
    }

    @Test
    void updateAutomatedTask() {
        Optional<AutomatedTaskEntity> automatedTaskEntity = anAutomatedTaskEntityWithName("some-task-name", null);
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


    @Test
    void positiveGetAllAutomatedTasks() {
        AutomatedTaskEntity automatedTaskEntity1 =
            createAutomatedTaskEntity("task1", true);
        AutomatedTaskEntity automatedTaskEntity2 =
            createAutomatedTaskEntity("task2", true);
        AutomatedTaskEntity automatedTaskEntity3 =
            createAutomatedTaskEntity("CaseExpiryDeletion", true);

        when(automatedTaskRepository.findAll()).thenReturn(List.of(automatedTaskEntity1, automatedTaskEntity2, automatedTaskEntity3));

        adminAutomatedTaskService.getAllAutomatedTasks();

        verify(automatedTaskRepository, times(1))
            .findAll();
        verify(mapper, times(1))
            .mapEntitiesToModel(List.of(automatedTaskEntity1, automatedTaskEntity2, automatedTaskEntity3));
    }


    @Test
    void positiveGetAllAutomatedTasksDisabledExcludeFlag() {
        AutomatedTaskEntity automatedTaskEntity1 =
            createAutomatedTaskEntity("task1", true);
        AutomatedTaskEntity automatedTaskEntity2 =
            createAutomatedTaskEntity("task2", true);
        AutomatedTaskEntity automatedTaskEntity3 =
            createAutomatedTaskEntity("CaseExpiryDeletion", false);

        when(automatedTaskRepository.findAll()).thenReturn(List.of(automatedTaskEntity1, automatedTaskEntity2, automatedTaskEntity3));

        adminAutomatedTaskService.getAllAutomatedTasks();

        verify(automatedTaskRepository, times(1))
            .findAll();
        verify(mapper, times(1))
            .mapEntitiesToModel(List.of(automatedTaskEntity1, automatedTaskEntity2));
    }

    @Test
    void positiveGetAutomatedTaskById() {
        AutomatedTaskEntity automatedTaskEntity = createAutomatedTaskEntity("CaseExpiryDeletion", true);
        when(automatedTaskRepository.findById(1234)).thenReturn(Optional.of(automatedTaskEntity));
        DetailedAutomatedTask expectedDetailedAutomatedTask = mock(DetailedAutomatedTask.class);
        when(mapper.mapEntityToDetailedAutomatedTask(automatedTaskEntity)).thenReturn(expectedDetailedAutomatedTask);

        DetailedAutomatedTask actualDetailedAutomatedTask = adminAutomatedTaskService.getAutomatedTaskById(1234);

        assertEquals(expectedDetailedAutomatedTask, actualDetailedAutomatedTask);
    }

    @Test
    void positiveGetAutomatedTaskByIdWithDisabledFlag() {
        AutomatedTaskEntity automatedTaskEntity = createAutomatedTaskEntity("CaseExpiryDeletion", false);
        when(automatedTaskRepository.findById(1234)).thenReturn(Optional.of(automatedTaskEntity));

        DartsApiException exception = assertThrows(DartsApiException.class, () -> adminAutomatedTaskService.getAutomatedTaskById(1234));
        assertEquals(exception.getError(), AutomatedTaskApiError.AUTOMATED_TASK_NOT_FOUND);
    }

    @Test
    void positiveRunAutomatedTaskWithDisabledFlag() {
        AutomatedTaskEntity automatedTaskEntity = createAutomatedTaskEntity("CaseExpiryDeletion", false);
        when(automatedTaskRepository.findById(1234)).thenReturn(Optional.of(automatedTaskEntity));

        DartsApiException exception = assertThrows(DartsApiException.class, () -> adminAutomatedTaskService.runAutomatedTask(1234));
        assertEquals(exception.getError(), AutomatedTaskApiError.AUTOMATED_TASK_NOT_FOUND);
    }

    @Test
    void positiveUpdateAutomatedTaskWithDisabledTaskFlag() {
        AutomatedTaskEntity automatedTaskEntity = createAutomatedTaskEntity("CaseExpiryDeletion", false);
        when(automatedTaskRepository.findById(1234)).thenReturn(Optional.of(automatedTaskEntity));

        DartsApiException exception = assertThrows(
            DartsApiException.class, () -> adminAutomatedTaskService.updateAutomatedTask(1234, new AutomatedTaskPatch()));
        assertEquals(exception.getError(), AutomatedTaskApiError.AUTOMATED_TASK_NOT_FOUND);
    }


    private AutomatedTaskEntity createAutomatedTaskEntity(String taskName, boolean enabled) {
        return anAutomatedTaskEntityWithName(taskName, enabled).orElseThrow();
    }

    private Optional<AutomatedTaskEntity> anAutomatedTaskEntityWithName(String taskName, Boolean enabled) {
        var automatedTaskEntity = new AutomatedTaskEntity();
        automatedTaskEntity.setTaskName(taskName);
        automatedTaskEntity.setId(1234);
        automatedTaskEntity.setTaskEnabled(true);
        automatedTaskEntity.setCronEditable(true);
        automatedTaskEntity.setTaskDescription("task description");
        automatedTaskEntity.setTaskName(taskName != null ? taskName : "task name");
        automatedTaskEntity.setCronExpression("");
        if (enabled != null) {
            when(configurableBeanFactory.resolveEmbeddedValue(any())).thenReturn(enabled.toString());
        }
        UserAccountEntity accountEntity = new UserAccountEntity();
        accountEntity.setId(999);

        automatedTaskEntity.setLastModifiedBy(accountEntity);
        automatedTaskEntity.setCreatedDateTime(OffsetDateTime.now());
        automatedTaskEntity.setCreatedBy(accountEntity);
        automatedTaskEntity.setLastModifiedDateTime(OffsetDateTime.now());

        return Optional.of(automatedTaskEntity);
    }
}