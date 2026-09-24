package uk.gov.hmcts.darts.arm.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.task.config.CleanUpDetsDataAutomatedTaskConfig;
import uk.gov.hmcts.darts.testutils.AsyncTestUtil;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CleanUpDetsDataProcessorImpl Tests")
class CleanUpDetsDataProcessorImplTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2024-01-01T00:00:00Z");
    private static final Duration DEFAULT_MINIMUM_STORED_AGE = Duration.ofDays(30);

    @Mock
    private CleanUpDetsDataProcessorImpl.CleanUpDetsDataTransactionalProcessor cleanUpDetsDataTransactionalProcessor;
    
    @Mock
    private CleanUpDetsDataAutomatedTaskConfig config;

    @InjectMocks
    private CleanUpDetsDataProcessorImpl processor;

    @BeforeEach
    void setUp() {
        processor = new CleanUpDetsDataProcessorImpl(cleanUpDetsDataTransactionalProcessor, Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC));
    }


    @DisplayName("Mehod: processCleanUpDetsData Tests")
    @Nested
    class ProcessCleanUpDetsDataTests {

        @Test
        @DisplayName("Skips processing when no EOD rows are returned")
        void shouldSkipProcessingWhenRepositoryReturnsEmptyList() {
            configureTaskConfig(4, 2);
            when(cleanUpDetsDataTransactionalProcessor.callDetsCleanUpStoredProcedure(eq(4), any()))
                .thenReturn(Collections.emptyList());

            assertDoesNotThrow(() -> processor.processCleanUpDetsData(10, config));

            verify(cleanUpDetsDataTransactionalProcessor, times(1))
                .callDetsCleanUpStoredProcedure(4, expectedMinimumStoredAge(DEFAULT_MINIMUM_STORED_AGE));
        }
        
        @Test
        @DisplayName("Continues looping until repository reports there is no more data")
        void shouldContinueProcessingUntilNoMoreData() {
            configureTaskConfig(4, 2);
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

            AsyncTestUtil.processTasksSynchronously(() -> processor.processCleanUpDetsData(12, config));

            //Called 3 times - first batch, second batch, then empty list to end processing
            verify(cleanUpDetsDataTransactionalProcessor, times(3))
                .callDetsCleanUpStoredProcedure(4, expectedMinimumStoredAge(DEFAULT_MINIMUM_STORED_AGE));
        }

        @Test
        @DisplayName("Stops processing once batch size limit is reached, even if repository returns more data")
        void shouldStopProcessingWhenBatchSizeLimitReached() {
            configureTaskConfig(4, 2);

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

            AsyncTestUtil.processTasksSynchronously(() -> processor.processCleanUpDetsData(4, config));

            verify(cleanUpDetsDataTransactionalProcessor, times(1))
                .callDetsCleanUpStoredProcedure(4, expectedMinimumStoredAge(DEFAULT_MINIMUM_STORED_AGE));
        }


        private OffsetDateTime expectedMinimumStoredAge(Duration minimumStoredAge) {
            return OffsetDateTime.ofInstant(FIXED_INSTANT, ZoneOffset.UTC).minus(minimumStoredAge);
        }
    }
    

    private void configureTaskConfig(int chunkSize, int threads) {
        when(config.getMinimumStoredAge()).thenReturn(DEFAULT_MINIMUM_STORED_AGE);
        when(config.getChunkSize()).thenReturn(chunkSize);
        lenient().when(config.getThreads()).thenReturn(threads);
    }

    private CleanUpDetsDataProcessorImpl.CleanUpDetsProcedureResponse response(long osrUuid) {
        return new CleanUpDetsDataProcessorImpl.CleanUpDetsProcedureResponse(osrUuid, "location-" + osrUuid);
    }
    
}
