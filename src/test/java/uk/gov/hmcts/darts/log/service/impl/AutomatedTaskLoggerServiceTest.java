package uk.gov.hmcts.darts.log.service.impl;

import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.log.service.AutomatedTaskLoggerService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AutomatedTaskLoggerServiceTest {

    private AutomatedTaskLoggerService automatedTaskLoggerService;

    private static LogCaptor logCaptor;

    @BeforeAll
    public static void setupLogCaptor() {
        logCaptor = LogCaptor.forClass(AutomatedTaskLoggerServiceImpl.class);
        logCaptor.setLogLevelToInfo();
    }

    @AfterEach
    public void clearLogs() {
        logCaptor.clearLogs();
    }

    @AfterAll
    public static void tearDown() {
        logCaptor.close();
    }

    @BeforeEach
    void setUp() {
        automatedTaskLoggerService = new AutomatedTaskLoggerServiceImpl();
    }

    @Test
    void logsTaskStarted() {
        var taskExecutionId = UUID.randomUUID();

        automatedTaskLoggerService.taskStarted(taskExecutionId, "some-started-task", 10);

        assertThat(logCaptor.getInfoLogs()).containsExactly("Task started: run_id=" + taskExecutionId + ", task_name=some-started-task, batch_size=10");
    }

    @Test
    void logsTaskCompleted() {
        var taskExecutionId = UUID.randomUUID();

        automatedTaskLoggerService.taskCompleted(taskExecutionId, "some-completed-task");

        assertThat(logCaptor.getInfoLogs()).containsExactly("Task completed: run_id=" + taskExecutionId + ", task_name=some-completed-task");
    }

    @Test
    void logsTaskFailed() {
        var taskExecutionId = UUID.randomUUID();

        automatedTaskLoggerService.taskFailed(taskExecutionId, "some-failed-task");

        assertThat(logCaptor.getInfoLogs()).containsExactly("Task failed: run_id=" + taskExecutionId + ", task_name=some-failed-task");
    }

}
