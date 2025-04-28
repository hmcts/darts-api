package uk.gov.hmcts.darts.log.service.impl;

import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequest;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.log.service.AudioLoggerService;

import java.time.OffsetDateTime;
import java.util.Locale;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AudioLoggerServiceImplTest {

    private static final OffsetDateTime STARTED_AT = OffsetDateTime.parse("2021-01-01T01:00:00Z");
    private static final OffsetDateTime ENDED_AT = OffsetDateTime.parse("2021-01-01T02:00:00Z");
    private static final String SOME_COURTHOUSE = "SOME-CoUrTHoUsE";
    private static final String SOME_COURTROOM = "some-courtroom";

    private static LogCaptor logCaptor;

    private AudioLoggerService audioLoggerService;

    @BeforeAll
    public static void setupLogCaptor() {
        logCaptor = LogCaptor.forClass(AudioLoggerServiceImpl.class);
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
        CurrentTimeHelper currentTimeHelper = mock(CurrentTimeHelper.class);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(STARTED_AT);
        audioLoggerService = new AudioLoggerServiceImpl(currentTimeHelper);
    }

    @Test
    void logsAudioCorrectly() {
        var request = someAddAudioMetadataRequest();

        audioLoggerService.audioUploaded(request);

        var expectedLogEntry = format("Audio uploaded: courthouse=%s, courtroom=%s, started_at=%s, ended_at=%s",
                                      SOME_COURTHOUSE.toUpperCase(Locale.ROOT),
                                      SOME_COURTROOM.toUpperCase(Locale.ROOT),
                                      "2021-01-01T01:00:00Z",
                                      "2021-01-01T02:00:00Z");
        assertThat(logCaptor.getInfoLogs()).containsExactly(expectedLogEntry);
    }

    @Test
    void missingCourthouse_shouldLog() {
        audioLoggerService.missingCourthouse(SOME_COURTHOUSE, SOME_COURTROOM);
        var expectedLogEntry = format("Courthouse not found: courthouse=%s, courtroom=%s, timestamp=%s",
                                      SOME_COURTHOUSE.toUpperCase(Locale.ROOT),
                                      SOME_COURTROOM.toUpperCase(Locale.ROOT),
                                      "2021-01-01T01:00:00Z"
        );
        assertThat(logCaptor.getWarnLogs()).containsExactly(expectedLogEntry);
    }

    @Test
    void addAudioSmallFileWithLongDuration_logsCorrectData() {
        audioLoggerService.addAudioSmallFileWithLongDuration(SOME_COURTHOUSE, SOME_COURTROOM, STARTED_AT, ENDED_AT, 123L, 1024L);

        var expectedLogEntry = format("Audio file size problem: courthouse=%s, courtroom=%s, started_at=%s, ended_at=%s, med_id=%s, file_size=%s",
                                      SOME_COURTHOUSE.toUpperCase(Locale.ROOT),
                                      SOME_COURTROOM.toUpperCase(Locale.ROOT),
                                      "2021-01-01T01:00:00Z",
                                      "2021-01-01T02:00:00Z",
                                      "123",
                                      "1024");
        assertThat(logCaptor.getWarnLogs()).containsExactly(expectedLogEntry);
    }

    private AddAudioMetadataRequest someAddAudioMetadataRequest() {
        var addAudioMetadataRequest = new AddAudioMetadataRequest();
        addAudioMetadataRequest.setCourthouse(SOME_COURTHOUSE);
        addAudioMetadataRequest.setCourtroom(SOME_COURTROOM);
        addAudioMetadataRequest.setStartedAt(STARTED_AT);
        addAudioMetadataRequest.setEndedAt(ENDED_AT);
        return addAudioMetadataRequest;
    }

}