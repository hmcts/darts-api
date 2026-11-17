package uk.gov.hmcts.darts.task.runner.impl;

import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockingTaskExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audio.service.FindFilesFromCvpAndCarService;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.config.AbstractAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.config.FindFilesFromCvpAndCarAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.service.LockService;

import java.time.Duration;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindFilesFromCvpAndCarAutomatedTaskTest {

    @Mock
    private AutomatedTaskRepository repository;
    @Mock
    private LogApi logApi;
    @Mock
    private LockService lockService;
    @Mock
    private LockingTaskExecutor lockingTaskExecutor;
    @Mock
    private FindFilesFromCvpAndCarService service;

    private FindFilesFromCvpAndCarAutomatedTask task;
    private AutomatedTaskEntity taskDetails;

    @BeforeEach
    void setUp() {
        FindFilesFromCvpAndCarAutomatedTaskConfig config = new FindFilesFromCvpAndCarAutomatedTaskConfig();
        AbstractAutomatedTaskConfig.Lock lock = new AbstractAutomatedTaskConfig.Lock();
        lock.setAtLeastFor(Duration.ofMinutes(1));
        lock.setAtMostFor(Duration.ofMinutes(9));
        config.setLock(lock);
        task = spy(new FindFilesFromCvpAndCarAutomatedTask(repository, config, logApi, lockService, service));
        task.setLastCronExpression("0 */10 * * * *");
        taskDetails = new AutomatedTaskEntity();
        taskDetails.setTaskName("FindFilesFromCVPAndCAR");
        taskDetails.setCronExpression("0 */10 * * * *");
        taskDetails.setBatchSize(1000);
        when(repository.findByTaskName("FindFilesFromCVPAndCAR")).thenReturn(Optional.of(taskDetails));
    }

    @Test
    void runTask_requestsLockedExecution_whenEnabled() {
        doNothing().when(task).preRunTask();
        taskDetails.setTaskEnabled(true);
        when(lockService.getLockingTaskExecutor()).thenReturn(lockingTaskExecutor);
        when(lockService.getLockAtMostFor()).thenReturn(Duration.ofMinutes(9));

        task.run(false);

        verify(lockingTaskExecutor).executeWithLock(any(Runnable.class), any(LockConfiguration.class));
    }

    @Test
    void runTask_doesNotRequestExecution_whenDisabled() {
        doNothing().when(task).preRunTask();
        taskDetails.setTaskEnabled(false);
        when(lockService.getLockAtMostFor()).thenReturn(Duration.ofMinutes(9));

        task.run(false);

        verify(lockService, never()).getLockingTaskExecutor();
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(ints = {100, 1000})
    void runTask_delegatesWithBatchSize_whenBatchSizeConfigured(int batchSize) {
        taskDetails.setBatchSize(batchSize);

        task.runTask();

        verify(service).findFilesFromCvpAndCar(batchSize);
    }
}
