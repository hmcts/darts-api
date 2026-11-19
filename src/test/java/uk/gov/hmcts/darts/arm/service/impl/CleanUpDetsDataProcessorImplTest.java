package uk.gov.hmcts.darts.arm.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.task.config.CleanUpDetsDataAutomatedTaskConfig;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CleanUpDetsDataProcessorImpl Tests")
class CleanUpDetsDataProcessorImplTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2024-01-01T00:00:00Z");
    private static final Duration DEFAULT_MINIMUM_STORED_AGE = Duration.ofDays(30);

    @Mock
    private CleanUpDetsDataProcessorImpl.CleanUpDetsDataTransactionalProcessor cleanUpDetsDataTransactionalProcessor;

    @Mock
    private DetsCleanupMaintenanceService detsCleanupMaintenanceService;
    
    @Mock
    private CleanUpDetsDataAutomatedTaskConfig config;

    private CleanUpDetsDataProcessorImpl processor;

    @BeforeEach
    void setUp() {
        processor = new CleanUpDetsDataProcessorImpl(cleanUpDetsDataTransactionalProcessor, detsCleanupMaintenanceService,
                                                   Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC));
    }


    @DisplayName("Method: processCleanUpDetsData Tests")
    @Nested
    class ProcessCleanUpDetsDataTests {

        @Test
        @DisplayName("Skips processing when no EOD rows are returned")
        void processCleanUpDetsData_shouldSkipVacuum_whenNoRecordsAreReturned() {
            configureTaskConfig(4);
            when(cleanUpDetsDataTransactionalProcessor.callDetsCleanUpStoredProcedure(eq(4), any()))
                .thenReturn(Collections.emptyList());

            assertDoesNotThrow(() -> processor.processCleanUpDetsData(10, config));

            verify(cleanUpDetsDataTransactionalProcessor, times(1))
                .callDetsCleanUpStoredProcedure(4, expectedMinimumStoredAge(DEFAULT_MINIMUM_STORED_AGE));
            verifyNoInteractions(detsCleanupMaintenanceService);
        }
        
        @Test
        @DisplayName("Continues looping until repository reports there is no more data")
        void processCleanUpDetsData_shouldVacuumOnceAfterAllChunks_whenNoMoreDataRemains() {
            configureTaskConfig(4);
            List<CleanUpDetsDataProcessorImpl.CleanUpDetsProcedureResponse> firstBatch = List.of(
                response(1L), response(2L), response(3L), response(4L)
            );
            List<CleanUpDetsDataProcessorImpl.CleanUpDetsProcedureResponse> secondBatch = List.of(
                response(5L), response(6L), response(7L), response(8L)
            );

            when(cleanUpDetsDataTransactionalProcessor.callDetsCleanUpStoredProcedure(eq(4), any()))
                .thenReturn(firstBatch)
                .thenReturn(secondBatch)
                .thenReturn(Collections.emptyList());

            processor.processCleanUpDetsData(12, config);

            //Called 3 times - first batch, second batch, then empty list to end processing
            InOrder ordered = inOrder(cleanUpDetsDataTransactionalProcessor, detsCleanupMaintenanceService);
            ordered.verify(cleanUpDetsDataTransactionalProcessor, times(3))
                .callDetsCleanUpStoredProcedure(4, expectedMinimumStoredAge(DEFAULT_MINIMUM_STORED_AGE));
            ordered.verify(detsCleanupMaintenanceService).vacuumTables();
            ordered.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("Stops processing once batch size limit is reached, even if repository returns more data")
        void processCleanUpDetsData_shouldVacuumOnce_whenBatchSizeLimitIsReached() {
            configureTaskConfig(4);

            List<CleanUpDetsDataProcessorImpl.CleanUpDetsProcedureResponse> firstBatch = List.of(
                response(1L), response(2L), response(3L), response(4L)
            );
            List<CleanUpDetsDataProcessorImpl.CleanUpDetsProcedureResponse> secondBatch = List.of(
                response(5L), response(6L), response(7L), response(8L)
            );

            when(cleanUpDetsDataTransactionalProcessor.callDetsCleanUpStoredProcedure(eq(4), any()))
                .thenReturn(firstBatch)
                .thenReturn(secondBatch)
                .thenReturn(Collections.emptyList());

            processor.processCleanUpDetsData(4, config);

            InOrder ordered = inOrder(cleanUpDetsDataTransactionalProcessor, detsCleanupMaintenanceService);
            ordered.verify(cleanUpDetsDataTransactionalProcessor)
                .callDetsCleanUpStoredProcedure(4, expectedMinimumStoredAge(DEFAULT_MINIMUM_STORED_AGE));
            ordered.verify(detsCleanupMaintenanceService).vacuumTables();
            ordered.verifyNoMoreInteractions();
        }

        @ParameterizedTest
        @CsvSource({"0, 4", "-1, 4", "10, 0", "10, -1"})
        void processCleanUpDetsData_shouldSkipVacuum_whenBatchOrChunkSizePreventsProcessing(int batchSize, int chunkSize) {
            configureTaskConfig(chunkSize);

            processor.processCleanUpDetsData(batchSize, config);

            verifyNoInteractions(cleanUpDetsDataTransactionalProcessor, detsCleanupMaintenanceService);
        }

        @Test
        void processCleanUpDetsData_shouldPropagateFailureWithoutVacuuming_whenChunkFails() {
            configureTaskConfig(4);
            IllegalStateException failure = new IllegalStateException("Cleanup failed");
            when(cleanUpDetsDataTransactionalProcessor.callDetsCleanUpStoredProcedure(eq(4), any()))
                .thenReturn(List.of(response(1L)))
                .thenThrow(failure);

            assertThatThrownBy(() -> processor.processCleanUpDetsData(10, config)).isSameAs(failure);

            verifyNoInteractions(detsCleanupMaintenanceService);
        }

        @Test
        void processCleanUpDetsData_shouldPropagateMaintenanceFailure_whenVacuumFails() {
            configureTaskConfig(4);
            when(cleanUpDetsDataTransactionalProcessor.callDetsCleanUpStoredProcedure(eq(4), any()))
                .thenReturn(List.of(response(1L)))
                .thenReturn(Collections.emptyList());
            IllegalStateException failure = new IllegalStateException("Vacuum failed");
            doThrow(failure).when(detsCleanupMaintenanceService).vacuumTables();

            assertThatThrownBy(() -> processor.processCleanUpDetsData(10, config)).isSameAs(failure);
        }

        private OffsetDateTime expectedMinimumStoredAge(Duration minimumStoredAge) {
            return OffsetDateTime.ofInstant(FIXED_INSTANT, ZoneOffset.UTC).minus(minimumStoredAge);
        }
    }
    

    private void configureTaskConfig(int chunkSize) {
        when(config.getMinimumStoredAge()).thenReturn(DEFAULT_MINIMUM_STORED_AGE);
        when(config.getChunkSize()).thenReturn(chunkSize);
    }

    private CleanUpDetsDataProcessorImpl.CleanUpDetsProcedureResponse response(long osrUuid) {
        return new CleanUpDetsDataProcessorImpl.CleanUpDetsProcedureResponse(osrUuid, "location-" + osrUuid);
    }
    
}
