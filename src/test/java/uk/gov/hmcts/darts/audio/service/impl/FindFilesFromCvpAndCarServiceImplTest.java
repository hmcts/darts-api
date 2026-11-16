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
class FindFilesFromCvpAndCarServiceImplTest {

    private static final Instant START_TIME = Instant.parse("2026-11-06T10:00:00Z");
    private static final Instant PREVIOUS_TIME = START_TIME.minusSeconds(600);

    @Mock
    private JobLastSuccessfulRunTimeRepository repository;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void findFilesFromCvpAndCar_logsTimesAndSavesStartTime_whenPreviousRunPresent(boolean previousRunExists) {
        JobLastSuccessfulRunTimeEntity previousRun = new JobLastSuccessfulRunTimeEntity();
        previousRun.setJobName("find-files-from-cvp-and-car");
        previousRun.setLastSuccessfulRunTimestampUtc(PREVIOUS_TIME);
        when(repository.findById("find-files-from-cvp-and-car")).thenReturn(previousRunExists ? Optional.of(previousRun) : Optional.empty());
        FindFilesFromCvpAndCarServiceImpl service = new FindFilesFromCvpAndCarServiceImpl(repository, Clock.fixed(START_TIME, ZoneOffset.UTC));

        try (LogCaptor logCaptor = LogCaptor.forClass(FindFilesFromCvpAndCarServiceImpl.class)) {
            service.findFilesFromCvpAndCar(1000);

            assertThat(logCaptor.getInfoLogs()).hasSize(1);
            String message = logCaptor.getInfoLogs().getFirst();

            assertThat(message).contains(START_TIME.toString());
            assertThat(message).contains(previousRunExists ? PREVIOUS_TIME.toString() : "none");
            assertThat(message).contains("1000");
        }
        ArgumentCaptor<JobLastSuccessfulRunTimeEntity> savedRun = ArgumentCaptor.forClass(JobLastSuccessfulRunTimeEntity.class);
        verify(repository).save(savedRun.capture());
        assertThat(savedRun.getValue().getJobName()).isEqualTo("find-files-from-cvp-and-car");
        assertThat(savedRun.getValue().getLastSuccessfulRunTimestampUtc()).isEqualTo(START_TIME);
    }

    @Test
    void findFilesFromCvpAndCar_doesNotSaveTimestamp_whenReadFails() {
        IllegalStateException failure = new IllegalStateException("Read failed");
        when(repository.findById("find-files-from-cvp-and-car")).thenThrow(failure);
        FindFilesFromCvpAndCarServiceImpl service = new FindFilesFromCvpAndCarServiceImpl(repository, Clock.fixed(START_TIME, ZoneOffset.UTC));

        assertThatThrownBy(() -> service.findFilesFromCvpAndCar(1000)).isSameAs(failure);

        verify(repository, never()).save(any());
    }

    @Test
    void findFilesFromCvpAndCar_propagatesFailure_whenSaveFails() {
        when(repository.findById("find-files-from-cvp-and-car")).thenReturn(Optional.empty());
        IllegalStateException failure = new IllegalStateException("Save failed");
        when(repository.save(any())).thenThrow(failure);
        FindFilesFromCvpAndCarServiceImpl service = new FindFilesFromCvpAndCarServiceImpl(repository, Clock.fixed(START_TIME, ZoneOffset.UTC));

        assertThatThrownBy(() -> service.findFilesFromCvpAndCar(1000)).isSameAs(failure);
    }
}
