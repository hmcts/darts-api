package uk.gov.hmcts.darts.task.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import uk.gov.hmcts.darts.audit.api.AuditActivity;
import uk.gov.hmcts.darts.audit.api.AuditApi;
import uk.gov.hmcts.darts.common.entity.ArmAutomatedTaskEntity;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.ArmAutomatedTaskRepository;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.task.exception.AutomatedTaskApiError;
import uk.gov.hmcts.darts.task.runner.AutomatedTask;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAutomatedTasksServiceImplTest {

    @Mock
    private AutomatedTaskRepository automatedTaskRepository;
    @Mock
    private ArmAutomatedTaskRepository armAutomatedTaskRepository;
    @Mock
    private AutomatedTasksMapper mapper;
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
        adminAutomatedTaskService = spy(adminAutomatedTaskService);
        var automatedTask = anAutomatedTaskEntityWithName("some-task-name", null);
        when(someAutomatedTask.getTaskName()).thenReturn("some-task-name");
        when(lockService.isLocked(automatedTask)).thenReturn(false);

        when(automatedTaskRepository.findById(1)).thenReturn(Optional.of(automatedTask));
        doReturn(List.of((AutomatedTask) someAutomatedTask)).when(adminAutomatedTaskService).getAllAutomatedTasks();

        adminAutomatedTaskService.runAutomatedTask(1);

        verify(automatedTaskRunner, times(1)).run(someAutomatedTask, true);
        verify(auditApi, times(1)).record(AuditActivity.RUN_JOB_MANUALLY, "some-task-name");
        verify(adminAutomatedTaskService, times(1)).getAllAutomatedTasks();
    }

    @Test
    void updateAutomatedTask() {
        AutomatedTaskEntity automatedTaskEntity = anAutomatedTaskEntityWithName("some-task-name", null);
        automatedTaskEntity.setBatchSize(1);
        when(automatedTaskRepository.findById(1)).thenReturn(Optional.of(automatedTaskEntity));

        AutomatedTaskPatch automatedTaskPatch = new AutomatedTaskPatch();
        automatedTaskPatch.setIsActive(false);
        automatedTaskPatch.setBatchSize(100);

        when(automatedTaskRepository.save(automatedTaskEntity)).thenReturn(automatedTaskEntity);

        DetailedAutomatedTask expectedReturnTask = new DetailedAutomatedTask();
        when(mapper.mapEntityToDetailedAutomatedTask(automatedTaskEntity)).thenReturn(expectedReturnTask);

        DetailedAutomatedTask task = adminAutomatedTaskService.updateAutomatedTask(1, automatedTaskPatch);

        assertFalse(automatedTaskEntity.getTaskEnabled());
        assertEquals(100, automatedTaskEntity.getBatchSize());
        assertEquals(expectedReturnTask, task);
        verify(auditApi).record(AuditActivity.ENABLE_DISABLE_JOB, "some-task-name disabled");
        verifyNoMoreInteractions(auditApi);
    }

    @Test
    void updateAutomatedTaskDoesNotUpdateFieldsWithNullValueInPatchRequest() {
        AutomatedTaskEntity automatedTaskEntity = anAutomatedTaskEntityWithName("some-task-name", true);
        automatedTaskEntity.setBatchSize(1);
        when(automatedTaskRepository.findById(1)).thenReturn(Optional.of(automatedTaskEntity));

        AutomatedTaskPatch automatedTaskPatch = new AutomatedTaskPatch();

        adminAutomatedTaskService.updateAutomatedTask(1, automatedTaskPatch);

        assertTrue(automatedTaskEntity.getTaskEnabled());
        assertEquals(1, automatedTaskEntity.getBatchSize());
        verifyNoInteractions(auditApi, armAutomatedTaskRepository);
    }

    @Test
    void updateAutomatedTaskWithAllArmValues() {
        ArmAutomatedTaskEntity armAutomatedTaskEntity = mock(ArmAutomatedTaskEntity.class);
        AutomatedTaskEntity automatedTaskEntity = anAutomatedTaskEntityWithName("some-task-name", null);
        automatedTaskEntity.setBatchSize(1);
        automatedTaskEntity.setArmAutomatedTaskEntity(armAutomatedTaskEntity);
        when(automatedTaskRepository.findById(1)).thenReturn(Optional.of(automatedTaskEntity));

        AutomatedTaskPatch automatedTaskPatch = new AutomatedTaskPatch();
        automatedTaskPatch.setIsActive(false);
        automatedTaskPatch.setBatchSize(100);

        OffsetDateTime armReplyStartTs = OffsetDateTime.now();
        OffsetDateTime armReplyEndTs = OffsetDateTime.now().plusMinutes(4);
        automatedTaskPatch.setArmReplayStartTs(armReplyStartTs);
        automatedTaskPatch.setArmReplayEndTs(armReplyEndTs);
        automatedTaskPatch.setRpoCsvStartHour(1);
        automatedTaskPatch.setRpoCsvEndHour(3);

        when(automatedTaskRepository.save(automatedTaskEntity)).thenReturn(automatedTaskEntity);

        DetailedAutomatedTask expectedReturnTask = new DetailedAutomatedTask();
        when(mapper.mapEntityToDetailedAutomatedTask(automatedTaskEntity)).thenReturn(expectedReturnTask);

        DetailedAutomatedTask task = adminAutomatedTaskService.updateAutomatedTask(1, automatedTaskPatch);
        assertEquals(expectedReturnTask, task);

        assertFalse(automatedTaskEntity.getTaskEnabled());
        assertEquals(100, automatedTaskEntity.getBatchSize());
        verify(armAutomatedTaskEntity).setArmReplayStartTs(armReplyStartTs);
        verify(armAutomatedTaskEntity).setArmReplayEndTs(armReplyEndTs);
        verify(armAutomatedTaskEntity).setRpoCsvStartHour(1);
        verify(armAutomatedTaskEntity).setRpoCsvEndHour(3);

        verify(armAutomatedTaskEntity).getArmReplayStartTs();
        verify(armAutomatedTaskEntity).getArmReplayEndTs();
        verify(armAutomatedTaskEntity).getRpoCsvStartHour();
        verify(armAutomatedTaskEntity).getRpoCsvEndHour();
        verifyNoMoreInteractions(armAutomatedTaskEntity);


        verify(auditApi).record(AuditActivity.ENABLE_DISABLE_JOB, "some-task-name disabled");
        verify(armAutomatedTaskRepository).save(armAutomatedTaskEntity);
        verify(automatedTaskRepository).save(automatedTaskEntity);
        verifyNoMoreInteractions(auditApi);
    }

    @Test
    void updateAutomatedTaskWithSomeArmValues() {
        ArmAutomatedTaskEntity armAutomatedTaskEntity = mock(ArmAutomatedTaskEntity.class);
        AutomatedTaskEntity automatedTaskEntity = anAutomatedTaskEntityWithName("some-task-name", null);
        automatedTaskEntity.setBatchSize(1);
        automatedTaskEntity.setArmAutomatedTaskEntity(armAutomatedTaskEntity);
        when(automatedTaskRepository.findById(1)).thenReturn(Optional.of(automatedTaskEntity));

        AutomatedTaskPatch automatedTaskPatch = new AutomatedTaskPatch();
        OffsetDateTime armReplyStartTs = OffsetDateTime.now();
        automatedTaskPatch.setArmReplayStartTs(armReplyStartTs);
        automatedTaskPatch.setRpoCsvStartHour(1);

        when(automatedTaskRepository.save(automatedTaskEntity)).thenReturn(automatedTaskEntity);

        DetailedAutomatedTask expectedReturnTask = new DetailedAutomatedTask();
        when(mapper.mapEntityToDetailedAutomatedTask(automatedTaskEntity)).thenReturn(expectedReturnTask);

        DetailedAutomatedTask task = adminAutomatedTaskService.updateAutomatedTask(1, automatedTaskPatch);
        assertEquals(expectedReturnTask, task);

        assertTrue(automatedTaskEntity.getTaskEnabled());
        assertEquals(1, automatedTaskEntity.getBatchSize());
        verify(armAutomatedTaskEntity).setArmReplayStartTs(armReplyStartTs);
        verify(armAutomatedTaskEntity).setRpoCsvStartHour(1);
        verify(armAutomatedTaskEntity).getArmReplayStartTs();
        verify(armAutomatedTaskEntity).getRpoCsvStartHour();
        verifyNoMoreInteractions(armAutomatedTaskEntity);


        verify(armAutomatedTaskRepository).save(armAutomatedTaskEntity);
        verify(automatedTaskRepository).save(automatedTaskEntity);
        verifyNoInteractions(auditApi);
    }

    @Test
    void updateArmAutomatedTaskButTaskIsNotArm() {
        AutomatedTaskEntity automatedTaskEntity = anAutomatedTaskEntityWithName("some-task-name", null);
        automatedTaskEntity.setBatchSize(1);
        when(automatedTaskRepository.findById(1)).thenReturn(Optional.of(automatedTaskEntity));

        AutomatedTaskPatch automatedTaskPatch = new AutomatedTaskPatch();
        OffsetDateTime armReplyStartTs = OffsetDateTime.now();
        automatedTaskPatch.setArmReplayStartTs(armReplyStartTs);
        automatedTaskPatch.setRpoCsvStartHour(1);

        DartsApiException exception = assertThrows(DartsApiException.class, () -> adminAutomatedTaskService.updateAutomatedTask(1, automatedTaskPatch));
        assertEquals(AutomatedTaskApiError.INCORRECT_AUTOMATED_TASK_TYPE, exception.getError());
        assertEquals("The automated task type is incorrect. Task some-task-name is not an arm automated task as such can not update arm related fields",
                     exception.getMessage());

        verifyNoInteractions(armAutomatedTaskRepository, auditApi);
        verify(automatedTaskRepository).findById(1);
        verifyNoMoreInteractions(automatedTaskRepository);
    }


    @Test
    void positiveGetAllAutomatedTasksSummaries() {
        AutomatedTaskEntity automatedTaskEntity1 =
            createAutomatedTaskEntity("task1", true);
        AutomatedTaskEntity automatedTaskEntity2 =
            createAutomatedTaskEntity("task2", true);
        AutomatedTaskEntity automatedTaskEntity3 =
            createAutomatedTaskEntity("CaseExpiryDeletion", true);

        when(automatedTaskRepository.findAll()).thenReturn(List.of(automatedTaskEntity1, automatedTaskEntity2, automatedTaskEntity3));

        adminAutomatedTaskService.getAllAutomatedTasksSummaries();

        verify(automatedTaskRepository, times(1))
            .findAll();
        verify(mapper, times(1))
            .mapEntitiesToModel(List.of(automatedTaskEntity1, automatedTaskEntity2, automatedTaskEntity3));
    }


    @Test
    void positiveGetAllAutomatedTasksSummariesDisabledExcludeFlag() {
        AutomatedTaskEntity automatedTaskEntity1 =
            createAutomatedTaskEntity("task1", true);
        AutomatedTaskEntity automatedTaskEntity2 =
            createAutomatedTaskEntity("task2", true);
        AutomatedTaskEntity automatedTaskEntity3 =
            createAutomatedTaskEntity("CaseExpiryDeletion", false);

        when(automatedTaskRepository.findAll()).thenReturn(List.of(automatedTaskEntity1, automatedTaskEntity2, automatedTaskEntity3));

        adminAutomatedTaskService.getAllAutomatedTasksSummaries();

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
        return anAutomatedTaskEntityWithName(taskName, enabled);
    }

    private AutomatedTaskEntity anAutomatedTaskEntityWithName(String taskName, Boolean enabled) {
        var automatedTaskEntity = new AutomatedTaskEntity();
        automatedTaskEntity.setTaskName(taskName);
        automatedTaskEntity.setId(1234);
        automatedTaskEntity.setTaskEnabled(true);
        automatedTaskEntity.setCronEditable(true);
        automatedTaskEntity.setTaskDescription("task description");
        automatedTaskEntity.setTaskName(taskName != null ? taskName : "task name");
        automatedTaskEntity.setCronExpression("");
        if (enabled != null) {
            lenient().when(configurableBeanFactory.resolveEmbeddedValue(any())).thenReturn(enabled.toString());
        }
        UserAccountEntity accountEntity = new UserAccountEntity();
        accountEntity.setId(999);

        automatedTaskEntity.setLastModifiedBy(accountEntity);
        automatedTaskEntity.setCreatedDateTime(OffsetDateTime.now());
        automatedTaskEntity.setCreatedBy(accountEntity);
        automatedTaskEntity.setLastModifiedDateTime(OffsetDateTime.now());

        return automatedTaskEntity;
    }
}