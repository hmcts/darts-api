package uk.gov.hmcts.darts.log.service.impl;

import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.COMPLETED;
import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.FAILED;
import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.OPEN;
import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.PROCESSING;


@ExtendWith({MockitoExtension.class})
class AtsLoggerServiceImplTest {

    private static final Integer MEDIA_REQUEST_ID = 1;
    private static final Integer HEARING_ID = 2;
    AtsLoggerServiceImpl atsLoggerService;
    private static LogCaptor logCaptor;

    @BeforeAll
    public static void setupLogCaptor() {
        logCaptor = LogCaptor.forClass(AtsLoggerServiceImpl.class);
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
        atsLoggerService = new AtsLoggerServiceImpl();
    }

    @Test
    void testLogsAtsStarted() {

        var mediaRequestEntity = createMediaRequest();
        mediaRequestEntity.setStatus(PROCESSING);

        atsLoggerService.atsProcessingUpdate(mediaRequestEntity);

        var logEntry = String.format("ATS request process started: med_req_id=%s, hearing_id=%s",
                                     mediaRequestEntity.getId(), mediaRequestEntity.getHearing().getId());

        List<String> infoLogs = logCaptor.getInfoLogs();
        assertEquals(1, infoLogs.size());
        assertEquals(logEntry, infoLogs.getFirst());
    }

    @Test
    void testLogsAtsCompleted() {

        var mediaRequestEntity = createMediaRequest();
        mediaRequestEntity.setStatus(COMPLETED);

        atsLoggerService.atsProcessingUpdate(mediaRequestEntity);

        var logEntry = String.format("ATS request processed successfully: med_req_id=%s, hearing_id=%s",
                                     mediaRequestEntity.getId(), mediaRequestEntity.getHearing().getId());

        List<String> infoLogs = logCaptor.getInfoLogs();
        assertEquals(1, infoLogs.size());
        assertEquals(logEntry, infoLogs.getFirst());
    }

    @Test
    void testLogsAtsFailed() {

        var mediaRequestEntity = createMediaRequest();
        mediaRequestEntity.setStatus(FAILED);

        atsLoggerService.atsProcessingUpdate(mediaRequestEntity);

        var logEntry = String.format("ATS request process failed: med_req_id=%s, hearing_id=%s",
                                     mediaRequestEntity.getId(), mediaRequestEntity.getHearing().getId());

        List<String> errorLogs = logCaptor.getErrorLogs();
        assertEquals(1, errorLogs.size());
        assertEquals(logEntry, errorLogs.getFirst());
    }

    @Test
    void testLogsAudioRequest() {

        var mediaRequestEntity = createMediaRequest();
        mediaRequestEntity.setStatus(OPEN);

        atsLoggerService.atsProcessingUpdate(mediaRequestEntity);

        var logEntry = String.format("ATS request received: med_req_id=%s, hearing_id=%s",
                                     mediaRequestEntity.getId(), mediaRequestEntity.getHearing().getId());

        List<String> infoLogs = logCaptor.getInfoLogs();
        assertEquals(1, infoLogs.size());
        assertEquals(logEntry, infoLogs.getFirst());
    }

    private MediaRequestEntity createMediaRequest() {
        var hearing = new HearingEntity();
        hearing.setId(HEARING_ID);

        var mediaRequestEntity = new MediaRequestEntity();
        mediaRequestEntity.setId(MEDIA_REQUEST_ID);
        mediaRequestEntity.setHearing(hearing);

        return mediaRequestEntity;
    }
}
