package uk.gov.hmcts.darts.log.service.impl;

import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith({MockitoExtension.class})
class ArmLoggerServiceImplTest {

    private static final Integer EOD_ID = 1;
    private static final Integer EXECUTION_ID = 2;
    private ArmLoggerServiceImpl armLoggerService;
    private static LogCaptor logCaptor;

    @BeforeAll
    public static void setupLogCaptor() {
        logCaptor = LogCaptor.forClass(ArmLoggerServiceImpl.class);
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
        armLoggerService = new ArmLoggerServiceImpl();
    }

    @Test
    void armPushSuccessful_shouldLogInfoWithEodId_whenProvidedWithEodId() {
        armLoggerService.armPushSuccessful(EOD_ID);

        var logEntry = String.format("Successfully pushed object to ARM dropzone: eod_id=%s", EOD_ID);

        List<String> infoLogs = logCaptor.getInfoLogs();
        assertEquals(1, infoLogs.size());
        assertEquals(logEntry, infoLogs.get(0));
    }

    @Test
    void armPushFailed_shouldLogErrorWithEodId_whenProvidedWithEodId() {
        armLoggerService.armPushFailed(EOD_ID);

        var logEntry = String.format("Failed to push object to ARM dropzone: eod_id=%s", EOD_ID);

        List<String> errorLogs = logCaptor.getErrorLogs();
        assertEquals(1, errorLogs.size());
        assertEquals(logEntry, errorLogs.get(0));
    }

    @Test
    void archiveToArmSuccessful_shouldLogInfoWithEodId_whenProvidedWithEodId() {
        armLoggerService.archiveToArmSuccessful(EOD_ID);

        var logEntry = String.format("Successfully archived object to ARM: eod_id=%s", EOD_ID);

        List<String> infoLogs = logCaptor.getInfoLogs();
        assertEquals(1, infoLogs.size());
        assertEquals(logEntry, infoLogs.get(0));
    }

    @Test
    void archiveToArmFailed_shouldLogErrorWithEodId_whenProvidedWithEodId() {
        armLoggerService.archiveToArmFailed(EOD_ID);

        var logEntry = String.format("Failed to archive object to ARM: eod_id=%s", EOD_ID);

        List<String> errorLogs = logCaptor.getErrorLogs();
        assertEquals(1, errorLogs.size());
        assertEquals(logEntry, errorLogs.get(0));
    }

    @Test
    void armRpoSearchSuccessful_shouldLogInfoWithEodId_whenProvidedWithExecutionId() {
        armLoggerService.armRpoSearchSuccessful(EXECUTION_ID);

        var logEntry = String.format("ARM RPO Search - Successfully completed for execution Id = %s", EXECUTION_ID);

        List<String> infoLogs = logCaptor.getInfoLogs();
        assertEquals(1, infoLogs.size());
        assertEquals(logEntry, infoLogs.get(0));
    }

    @Test
    void armRpoSearchFailed_shouldLogErrorWithEodId_whenProvidedWithExecutionId() {
        armLoggerService.armRpoSearchFailed(EXECUTION_ID);

        var logEntry = String.format("ARM RPO Search - Failed for execution Id = %s", EXECUTION_ID);

        List<String> errorLogs = logCaptor.getErrorLogs();
        assertEquals(1, errorLogs.size());
        assertEquals(logEntry, errorLogs.get(0));
    }

    @Test
    void armRpoPollingSuccessful_shouldLogInfoWithEodId_whenProvidedWithExecutionId() {
        armLoggerService.armRpoPollingSuccessful(EXECUTION_ID);

        var logEntry = String.format("ARM RPO Polling - Successfully completed for execution Id = %s", EXECUTION_ID);

        List<String> infoLogs = logCaptor.getInfoLogs();
        assertEquals(1, infoLogs.size());
        assertEquals(logEntry, infoLogs.get(0));
    }

    @Test
    void armRpoPollingFailed_shouldLogErrorWithEodId_whenProvidedWithExecutionId() {
        armLoggerService.armRpoPollingFailed(EXECUTION_ID);

        var logEntry = String.format("ARM RPO Polling - Failed for execution Id = %s", EXECUTION_ID);

        List<String> errorLogs = logCaptor.getErrorLogs();
        assertEquals(1, errorLogs.size());
        assertEquals(logEntry, errorLogs.get(0));
    }
}