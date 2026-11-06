package uk.gov.hmcts.darts.audio.service.impl;

import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.JobLastSuccessfulRunTimeEntity;
import uk.gov.hmcts.darts.common.repository.JobLastSuccessfulRunTimeRepository;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Execution(ExecutionMode.SAME_THREAD)
@ExtendWith(MockitoExtension.class)
class FindFilesServiceImplTest {

    private static final Instant START_TIME = Instant.parse("2026-11-06T10:00:00Z");
    private static final Instant PREVIOUS_TIME = START_TIME.minusSeconds(600);

    @Mock
    private JobLastSuccessfulRunTimeRepository repository;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void given_previousRunPresence_when_findFiles_then_logTimesAndSaveStartTime(boolean previousRunExists) {
        JobLastSuccessfulRunTimeEntity previousRun = new JobLastSuccessfulRunTimeEntity();
        previousRun.setJobName("find_files");
        previousRun.setLastSuccessfulRunTimestampUtc(PREVIOUS_TIME);
        when(repository.findById("find_files")).thenReturn(previousRunExists ? Optional.of(previousRun) : Optional.empty());
        FindFilesServiceImpl service = new FindFilesServiceImpl(repository, Clock.fixed(START_TIME, ZoneOffset.UTC));

        try (LogCaptor logCaptor = LogCaptor.forClass(FindFilesServiceImpl.class)) {
            service.findFiles(1000);

            assertThat(logCaptor.getInfoLogs()).containsExactly(
                "Find files job: current time UTC " + START_TIME + ", last successful run time UTC "
                    + (previousRunExists ? PREVIOUS_TIME : "none") + ", batch size 1000");
        }
        ArgumentCaptor<JobLastSuccessfulRunTimeEntity> savedRun = ArgumentCaptor.forClass(JobLastSuccessfulRunTimeEntity.class);
        verify(repository).save(savedRun.capture());
        assertThat(savedRun.getValue().getJobName()).isEqualTo("find_files");
        assertThat(savedRun.getValue().getLastSuccessfulRunTimestampUtc()).isEqualTo(START_TIME);
    }

    @Test
    void given_readFailure_when_findFiles_then_doNotSaveTimestamp() {
        IllegalStateException failure = new IllegalStateException("Read failed");
        when(repository.findById("find_files")).thenThrow(failure);
        FindFilesServiceImpl service = new FindFilesServiceImpl(repository, Clock.fixed(START_TIME, ZoneOffset.UTC));

        assertThatThrownBy(() -> service.findFiles(1000)).isSameAs(failure);

        verify(repository, never()).save(any());
    }

    @Test
    void given_saveFailure_when_findFiles_then_propagateFailure() {
        when(repository.findById("find_files")).thenReturn(Optional.empty());
        IllegalStateException failure = new IllegalStateException("Save failed");
        when(repository.save(any())).thenThrow(failure);
        FindFilesServiceImpl service = new FindFilesServiceImpl(repository, Clock.fixed(START_TIME, ZoneOffset.UTC));

        assertThatThrownBy(() -> service.findFiles(1000)).isSameAs(failure);
    }
}
