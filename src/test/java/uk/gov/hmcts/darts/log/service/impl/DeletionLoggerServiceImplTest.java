package uk.gov.hmcts.darts.log.service.impl;

import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.log.service.DeletionLoggerService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeletionLoggerServiceImplTest {

    private static LogCaptor logCaptor;

    private DeletionLoggerService deletionLoggerService;

    @BeforeAll
    public static void setupLogCaptor() {
        logCaptor = LogCaptor.forClass(DeletionLoggerServiceImpl.class);
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
        deletionLoggerService = new DeletionLoggerServiceImpl();
    }

    @Test
    void testMediaDeleted() {
        var mediaId = 123L;
        deletionLoggerService.mediaDeleted(mediaId);

        List<String> logs = logCaptor.getInfoLogs();
        assertEquals(1, logs.size());
        assertEquals(String.format("Media deleted: med_id=%s", mediaId), logs.getFirst());
    }

    @Test
    void testTranscriptionDeleted() {
        var transcriptionId = 123L;
        deletionLoggerService.transcriptionDeleted(transcriptionId);

        List<String> logs = logCaptor.getInfoLogs();
        assertEquals(1, logs.size());
        assertEquals(String.format("Transcript deleted: trd_id=%s", transcriptionId), logs.getFirst());
    }

}