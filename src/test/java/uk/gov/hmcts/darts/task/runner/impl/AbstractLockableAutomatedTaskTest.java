package uk.gov.hmcts.darts.task.runner.impl;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.AbstractAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.service.LockService;
import uk.gov.hmcts.darts.task.status.AutomatedTaskStatus;
import uk.gov.hmcts.darts.util.LogUtil;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AbstractLockableAutomatedTaskTest {


    @Mock
    private AutomatedTaskRepository automatedTaskRepository;
    @Mock
    private AbstractAutomatedTaskConfig abstractAutomatedTaskConfig;
    @Mock
    private LogApi logApi;
    @Mock
    private LockService lockService;


    @Nested
    @ExtendWith(OutputCaptureExtension.class)
    public class LockedTaskTest {


        @Test
        void lockedTaskRun_shouldBeSuccessfull_whenTaskCompletesWithinLockAtMostFor(CapturedOutput output) {
            Runnable task = () -> {
                System.out.println("LockedTaskTest: Task is running");
            };

            AbstractAutomatedTaskConfig.Lock lock = new AbstractAutomatedTaskConfig.Lock();
            lock.setAtLeastFor(Duration.ofMillis(1));
            lock.setAtMostFor(Duration.ofSeconds(1));
            when(abstractAutomatedTaskConfig.getLock()).thenReturn(lock);
            AbstractLockableAutomatedTask<AbstractAutomatedTaskConfig> abstractLockableAutomatedTask = spy(createAbstractLockableAutomatedTask(task));

            AbstractLockableAutomatedTask.LockedTask lockedTask = spy(abstractLockableAutomatedTask.createLockableTask());

            doNothing().when(lockedTask).assertLocked();

            lockedTask.run();

            //Ensure task is run
            LogUtil.waitUntilMessag(output, "LockedTaskTest: Task is running", 10);
            //Ensure no task errors are logged
            assertThat(output.getErr()).doesNotContain("Task:");

            verify(abstractLockableAutomatedTask, never()).setAutomatedTaskStatus(any());
            verify(abstractLockableAutomatedTask, never()).handleException(any());
            verify(lockedTask).assertLocked();
        }

        @Test
        //Timeout after 10 seconds to ensure the task is killed before completion
        @Timeout(value = 10, unit = TimeUnit.SECONDS)
        void lockedTaskRun_shouldFail_whenTaskDoesNotCompletesWithinLockAtMostFor(CapturedOutput output) {
            Runnable task = () -> {
                try {
                    System.out.println("LockedTaskTest: Task is running");
                    Thread.sleep(30000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            };

            AbstractAutomatedTaskConfig.Lock lock = new AbstractAutomatedTaskConfig.Lock();
            lock.setAtLeastFor(Duration.ofMillis(1));
            lock.setAtMostFor(Duration.ofSeconds(1));
            when(abstractAutomatedTaskConfig.getLock()).thenReturn(lock);
            AbstractLockableAutomatedTask<AbstractAutomatedTaskConfig> abstractLockableAutomatedTask = spy(createAbstractLockableAutomatedTask(task));

            AbstractLockableAutomatedTask.LockedTask lockedTask = spy(abstractLockableAutomatedTask.createLockableTask());

            doNothing().when(lockedTask).assertLocked();


            lockedTask.run();

            //Ensure task is run
            LogUtil.waitUntilMessag(output, "LockedTaskTest: Task is running", 10);
            //Ensure task errors are logged correctly
            LogUtil.waitUntilMessag(output, "Task: TEST_TASK timed out after 1000ms", 10);

            verify(abstractLockableAutomatedTask).setAutomatedTaskStatus(AutomatedTaskStatus.FAILED);
            verify(lockedTask).assertLocked();
        }

        @Test
        void lockedTaskRun_shouldFail_whenTaskHasException(CapturedOutput output) {
            RuntimeException exception = new RuntimeException("Task failed");
            Runnable task = () -> {
                System.out.println("LockedTaskTest: Task is running");
                throw exception;
            };

            AbstractAutomatedTaskConfig.Lock lock = new AbstractAutomatedTaskConfig.Lock();
            lock.setAtLeastFor(Duration.ofMillis(1));
            lock.setAtMostFor(Duration.ofSeconds(1));
            when(abstractAutomatedTaskConfig.getLock()).thenReturn(lock);
            AbstractLockableAutomatedTask<AbstractAutomatedTaskConfig> abstractLockableAutomatedTask = spy(createAbstractLockableAutomatedTask(task));

            AbstractLockableAutomatedTask.LockedTask lockedTask = spy(abstractLockableAutomatedTask.createLockableTask());

            doNothing().when(lockedTask).assertLocked();


            lockedTask.run();

            //Ensure task is run
            LogUtil.waitUntilMessag(output, "LockedTaskTest: Task is running", 5);
            //Ensure no task errors are logged

            LogUtil.waitUntilMessag(output, "Task: TEST_TASK exception during execution of the task business logic", 5);
            LogUtil.waitUntilMessag(output, "Task: TEST_TASK execution exception", 5);

            verify(abstractLockableAutomatedTask).setAutomatedTaskStatus(AutomatedTaskStatus.FAILED);
            verify(abstractLockableAutomatedTask).handleException(exception);
            verify(lockedTask).assertLocked();
        }


        private AbstractLockableAutomatedTask<AbstractAutomatedTaskConfig> createAbstractLockableAutomatedTask(Runnable runTask) {
            return new AbstractLockableAutomatedTask<AbstractAutomatedTaskConfig>(
                automatedTaskRepository,
                abstractAutomatedTaskConfig,
                logApi,
                lockService
            ) {
                @Override
                public void runTask() {
                    runTask.run();
                }

                @Override
                public AutomatedTaskName getAutomatedTaskName() {
                    AutomatedTaskName taskName = mock(AutomatedTaskName.class);
                    when(taskName.getTaskName()).thenReturn("TEST_TASK");
                    return taskName;
                }
            };
        }
    }
}