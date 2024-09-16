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
import uk.gov.hmcts.darts.common.exception.DartsApiError;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.task.exception.AutomatedTaskApiError;
import uk.gov.hmcts.darts.task.runner.impl.AbstractLockableAutomatedTask;
import uk.gov.hmcts.darts.task.service.LockService;
import uk.gov.hmcts.darts.tasks.model.AutomatedTaskPatch;
import uk.gov.hmcts.darts.tasks.model.AutomatedTaskSummary;
import uk.gov.hmcts.darts.tasks.model.DetailedAutomatedTask;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
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

    private AdminAutomatedTasksServiceImpl adminAutomatedTaskService;

    @BeforeEach
    void setUp() {
        adminAutomatedTaskService = spy(new AdminAutomatedTasksServiceImpl(
            automatedTaskRepository,
            mapper,
            manualTaskService,
            automatedTaskRunner,
            auditApi,
            lockService,
            true
        ));
    }

    private void setCaseExpiryDeletionFalse() {
        doReturn(false)
            .when(adminAutomatedTaskService)
            .isCaseExpiryDeletionEnabled();
    }

    @Test
    void invokesTaskWhenTaskIsNotLocked() {
        var automatedTask = anAutomatedTaskEntityWithName("some-task-name");
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


    @Test
    void positiveGetAllAutomatedTasks() {
        AutomatedTaskEntity automatedTaskEntity1 =
            createAutomatedTaskEntity("task1");
        AutomatedTaskEntity automatedTaskEntity2 =
            createAutomatedTaskEntity("task2");
        AutomatedTaskEntity automatedTaskEntity3 =
            createAutomatedTaskEntity("CaseExpiryDeletion");

        when(automatedTaskRepository.findAll()).thenReturn(List.of(automatedTaskEntity1, automatedTaskEntity2, automatedTaskEntity3));

        adminAutomatedTaskService.getAllAutomatedTasks();

        verify(automatedTaskRepository, times(1))
            .findAll();
        verify(mapper, times(1))
            .mapEntitiesToModel(List.of(automatedTaskEntity1, automatedTaskEntity2, automatedTaskEntity3));
    }


    @Test
    void positiveGetAllAutomatedTasksWithExpiryDeletionExcludeFlag() {
        setCaseExpiryDeletionFalse();
        AutomatedTaskEntity automatedTaskEntity1 =
            createAutomatedTaskEntity("task1");
        AutomatedTaskEntity automatedTaskEntity2 =
            createAutomatedTaskEntity("task2");
        AutomatedTaskEntity automatedTaskEntity3 =
            createAutomatedTaskEntity("CaseExpiryDeletion");

        when(automatedTaskRepository.findAll()).thenReturn(List.of(automatedTaskEntity1, automatedTaskEntity2, automatedTaskEntity3));

        adminAutomatedTaskService.getAllAutomatedTasks();

        verify(automatedTaskRepository, times(1))
            .findAll();
        verify(mapper, times(1))
            .mapEntitiesToModel(List.of(automatedTaskEntity1, automatedTaskEntity2));
    }

    @Test
    void positiveGetAutomatedTaskById() {
        AutomatedTaskEntity automatedTaskEntity = createAutomatedTaskEntity("CaseExpiryDeletion");
        when(automatedTaskRepository.findById(1234)).thenReturn(Optional.of(automatedTaskEntity));
        DetailedAutomatedTask expectedDetailedAutomatedTask = mock(DetailedAutomatedTask.class);
        when(mapper.mapEntityToDetailedAutomatedTask(automatedTaskEntity)).thenReturn(expectedDetailedAutomatedTask);

        DetailedAutomatedTask actualDetailedAutomatedTask = adminAutomatedTaskService.getAutomatedTaskById(1234);

        assertEquals(expectedDetailedAutomatedTask, actualDetailedAutomatedTask);
    }

    @Test
    void positiveGetAutomatedTaskByIdWithExpiryDeletionExcludeFlag() {
        setCaseExpiryDeletionFalse();

        AutomatedTaskEntity automatedTaskEntity = createAutomatedTaskEntity("CaseExpiryDeletion");
        when(automatedTaskRepository.findById(1234)).thenReturn(Optional.of(automatedTaskEntity));

        DartsApiException exception = assertThrows(DartsApiException.class, () -> adminAutomatedTaskService.getAutomatedTaskById(1234));
        assertEquals(exception.getError(), AutomatedTaskApiError.AUTOMATED_TASK_NOT_FOUND);
    }

    @Test
    void positiveRunAutomatedTaskWithExpiryDeletionExcludeFlag() {
        setCaseExpiryDeletionFalse();

        AutomatedTaskEntity automatedTaskEntity = createAutomatedTaskEntity("CaseExpiryDeletion");
        when(automatedTaskRepository.findById(1234)).thenReturn(Optional.of(automatedTaskEntity));

        DartsApiException exception = assertThrows(DartsApiException.class, () -> adminAutomatedTaskService.runAutomatedTask(1234));
        assertEquals(exception.getError(), AutomatedTaskApiError.AUTOMATED_TASK_NOT_FOUND);
    }

    @Test
    void positiveUpdateAutomatedTaskWithExpiryDeletionExcludeFlag() {
        setCaseExpiryDeletionFalse();

        AutomatedTaskEntity automatedTaskEntity = createAutomatedTaskEntity("CaseExpiryDeletion");
        when(automatedTaskRepository.findById(1234)).thenReturn(Optional.of(automatedTaskEntity));

        DartsApiException exception = assertThrows(
            DartsApiException.class, () -> adminAutomatedTaskService.updateAutomatedTask(1234, new AutomatedTaskPatch()));
        assertEquals(exception.getError(), AutomatedTaskApiError.AUTOMATED_TASK_NOT_FOUND);
    }


    private AutomatedTaskEntity createAutomatedTaskEntity(String taskName) {
        return anAutomatedTaskEntityWithName(taskName).orElseThrow();
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