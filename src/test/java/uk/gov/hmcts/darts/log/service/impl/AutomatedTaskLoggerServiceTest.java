package uk.gov.hmcts.darts.log.service.impl;

import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.log.service.AutomatedTaskLoggerService;

import static java.util.UUID.randomUUID;
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
        var taskExecutionId = randomUUID();

        automatedTaskLoggerService.taskStarted(taskExecutionId, "some-started-task");

        assertThat(logCaptor.getInfoLogs()).containsExactly("Task started: run_id=" + taskExecutionId + ", task_name=some-started-task");
    }

    @Test
    void logsTaskCompleted() {
        var taskExecutionId = randomUUID();

        automatedTaskLoggerService.taskCompleted(taskExecutionId, "some-completed-task");

        assertThat(logCaptor.getInfoLogs()).containsExactly("Task completed: run_id=" + taskExecutionId + ", task_name=some-completed-task");
    }

    @Test
    void logsTaskFailed() {
        var taskExecutionId = randomUUID();

        automatedTaskLoggerService.taskFailed(taskExecutionId, "some-failed-task");

        assertThat(logCaptor.getInfoLogs()).containsExactly("Task failed: run_id=" + taskExecutionId + ", task_name=some-failed-task");
    }

}
