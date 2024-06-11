package uk.gov.hmcts.darts.task.service.impl;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audit.api.AuditApi;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.task.runner.impl.AbstractLockableAutomatedTask;
import uk.gov.hmcts.darts.task.service.AdminAutomatedTaskService;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

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

    @Mock
    private AuthorisationApi authorisationApi;

    private AdminAutomatedTaskService adminAutomatedTaskService;

    @BeforeEach
    void setUp() {
        adminAutomatedTaskService = new AdminAutomatedTasksServiceImpl(
            automatedTaskRepository,
            mapper,
            manualTaskService,
            automatedTaskRunner,
            currentTimeHelper,
            auditApi,
            authorisationApi
        );

        when(someAutomatedTask.getTaskName()).thenReturn("some-task-name");
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(OffsetDateTime.now());
    }

    @Test
    void invokesTaskWhenTaskIsNotLocked() {
        when(automatedTaskRepository.findById(1)).thenReturn(anAutomatedTaskEntityWithName("some-task-name"));
        when(automatedTaskRepository.findLockedUntilForTask("some-task-name")).thenReturn(anSqlDateInThePast());
        when(manualTaskService.getAutomatedTasks()).thenReturn(List.of(someAutomatedTask));

        adminAutomatedTaskService.runAutomatedTask(1);

        verify(automatedTaskRunner, times(1)).run(someAutomatedTask);
    }

    private List<Timestamp> anSqlDateInThePast() {
        var epochSecond = LocalDateTime.now().minusWeeks(1).toEpochSecond(ZoneOffset.UTC);
        var epochMillis = epochSecond * 1000;
        return List.of(new Timestamp(epochMillis));
    }

    private Optional<AutomatedTaskEntity> anAutomatedTaskEntityWithName(String taskName) {
        var automatedTaskEntity = new AutomatedTaskEntity();
        automatedTaskEntity.setTaskName(taskName);
        return Optional.of(automatedTaskEntity);
    }
}