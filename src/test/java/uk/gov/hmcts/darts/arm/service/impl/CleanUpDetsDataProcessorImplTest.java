package uk.gov.hmcts.darts.arm.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.exception.AzureDeleteBlobException;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectStateRecordRepository;
import uk.gov.hmcts.darts.dets.service.DetsApiService;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CleanUpDetsDataProcessorImpl Tests")
class CleanUpDetsDataProcessorImplTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2024-01-01T00:00:00Z");
    private static final Duration DEFAULT_MINIMUM_STORED_AGE = Duration.ofDays(30);

    @Mock
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;

    @Mock
    private CleanUpDetsDataProcessorImpl.CleanUpDetsDataBatchProcessor cleanUpDetsDataBatchProcessor;


    @Mock
    private CleanUpDetsDataAutomatedTaskConfig config;

    @Mock
    private ObjectStateRecordRepository objectStateRecordRepository;

    @Mock
    private DetsApiService detsApiService;

    @InjectMocks
    private CleanUpDetsDataProcessorImpl processor;

    @BeforeEach
    void setUp() {
        processor = new CleanUpDetsDataProcessorImpl(cleanUpDetsDataBatchProcessor, Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC));
    }


    @DisplayName("Mehod: processCleanUpDetsData Tests")
    @Nested
    class ProcessCleanUpDetsDataTests {

        @Test
        @DisplayName("Skips processing when no EOD rows are returned")
        void shouldSkipProcessingWhenRepositoryReturnsEmptyList() {
            configureTaskConfig(4, 2);
            when(cleanUpDetsDataBatchProcessor.callDetsCleanUpStoredProcedure(eq(4), any()))
                .thenReturn(Collections.emptyList());

            assertDoesNotThrow(() -> processor.processCleanUpDetsData(10, config));

            verify(cleanUpDetsDataBatchProcessor, never()).process(anyList());
            verify(cleanUpDetsDataBatchProcessor, times(1))
                .callDetsCleanUpStoredProcedure(4, expectedMinimumStoredAge(DEFAULT_MINIMUM_STORED_AGE));
        }


        @Test
        @DisplayName("Partitions responses and delegates to batch processor per chunk")
        void shouldPartitionResponsesIntoBatches() {
            configureTaskConfig(4, 2);
            List<CleanUpDetsDataProcessorImpl.CleanUpDetsProcedureResponse> responses =
                List.of(response(1L), response(2L), response(3L), response(4L));

            when(cleanUpDetsDataBatchProcessor.callDetsCleanUpStoredProcedure(eq(4), any()))
                .thenReturn(responses)//First call returns 4 records to process
                .thenReturn(Collections.emptyList()); //Second call returns empty list to end processing

            AsyncTestUtil.processTasksSynchronously(() -> processor.processCleanUpDetsData(8, config));

            ArgumentCaptor<List<CleanUpDetsDataProcessorImpl.CleanUpDetsProcedureResponse>> captor = ArgumentCaptor.captor();
            //Call twice because with chunk size of 2 and 4 records, we expect 2 batches to be processed (Chunk size 4 / threads = 2 = batch size for processor)
            verify(cleanUpDetsDataBatchProcessor, times(2)).process(captor.capture());
            List<List<CleanUpDetsDataProcessorImpl.CleanUpDetsProcedureResponse>> batches = captor.getAllValues();

            assertEquals(2, batches.size());
            assertEquals(List.of(responses.get(0), responses.get(1)), batches.get(0));
            assertEquals(List.of(responses.get(2), responses.get(3)), batches.get(1));
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

            when(cleanUpDetsDataBatchProcessor.callDetsCleanUpStoredProcedure(eq(4), any()))
                .thenReturn(firstBatch)
                .thenReturn(secondBatch)
                .thenReturn(Collections.emptyList());

            AsyncTestUtil.processTasksSynchronously(() -> processor.processCleanUpDetsData(12, config));

            //Called 3 times - first batch, second batch, then empty list to end processing
            verify(cleanUpDetsDataBatchProcessor, times(3))
                .callDetsCleanUpStoredProcedure(4, expectedMinimumStoredAge(DEFAULT_MINIMUM_STORED_AGE));
            verify(cleanUpDetsDataBatchProcessor).process(List.of(firstBatch.get(0), firstBatch.get(1)));
            verify(cleanUpDetsDataBatchProcessor).process(List.of(firstBatch.get(2), firstBatch.get(3)));
            verify(cleanUpDetsDataBatchProcessor).process(List.of(secondBatch.get(0), secondBatch.get(1)));
            verify(cleanUpDetsDataBatchProcessor).process(List.of(secondBatch.get(2), secondBatch.get(3)));
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

            when(cleanUpDetsDataBatchProcessor.callDetsCleanUpStoredProcedure(eq(4), any()))
                .thenReturn(firstBatch)
                .thenReturn(secondBatch)
                .thenReturn(Collections.emptyList());

            AsyncTestUtil.processTasksSynchronously(() -> processor.processCleanUpDetsData(4, config));

            verify(cleanUpDetsDataBatchProcessor, times(1))
                .callDetsCleanUpStoredProcedure(4, expectedMinimumStoredAge(DEFAULT_MINIMUM_STORED_AGE));
            verify(cleanUpDetsDataBatchProcessor).process(List.of(firstBatch.get(0), firstBatch.get(1)));
            verify(cleanUpDetsDataBatchProcessor).process(List.of(firstBatch.get(2), firstBatch.get(3)));
        }


        private OffsetDateTime expectedMinimumStoredAge(Duration minimumStoredAge) {
            return OffsetDateTime.ofInstant(FIXED_INSTANT, ZoneOffset.UTC).minus(minimumStoredAge);
        }
    }


    @DisplayName("CleanUpDetsDataBatchProcessor tests")
    @Nested
    class CleanUpDetsDataBatchProcessorTests {

        @Test
        @DisplayName("Deletes each blob and marks the OSRs as complete when everything succeeds")
        void shouldDeleteAllRecordsSuccessfully() throws AzureDeleteBlobException {
            CleanUpDetsDataProcessorImpl.CleanUpDetsDataBatchProcessor batchProcessor = createBatchProcessor();
            CleanUpDetsDataProcessorImpl.CleanUpDetsProcedureResponse response1 = response(1L);
            CleanUpDetsDataProcessorImpl.CleanUpDetsProcedureResponse response2 = response(2L);

            when(detsApiService.deleteBlobDataFromContainer(response1.getDetsLocation())).thenReturn(true);
            when(detsApiService.deleteBlobDataFromContainer(response2.getDetsLocation())).thenReturn(true);

            batchProcessor.process(List.of(response1, response2));

            verify(detsApiService).deleteBlobDataFromContainer(response1.getDetsLocation());
            verify(detsApiService).deleteBlobDataFromContainer(response2.getDetsLocation());
            verify(objectStateRecordRepository).markDetsCleanupStatusAsComplete(List.of(1L, 2L));
        }

        @Test
        @DisplayName("Logs failures but continues when deleteBlobDataFromContainer returns false")
        void shouldContinueProcessingWhenDeleteReturnsFalse() throws AzureDeleteBlobException {
            CleanUpDetsDataProcessorImpl.CleanUpDetsDataBatchProcessor batchProcessor = createBatchProcessor();
            CleanUpDetsDataProcessorImpl.CleanUpDetsProcedureResponse response1 = response(1L);
            CleanUpDetsDataProcessorImpl.CleanUpDetsProcedureResponse response2 = response(2L);
            CleanUpDetsDataProcessorImpl.CleanUpDetsProcedureResponse response3 = response(3L);

            when(detsApiService.deleteBlobDataFromContainer(response1.getDetsLocation())).thenReturn(true);
            when(detsApiService.deleteBlobDataFromContainer(response2.getDetsLocation())).thenReturn(false);
            when(detsApiService.deleteBlobDataFromContainer(response3.getDetsLocation())).thenReturn(true);

            assertDoesNotThrow(() -> batchProcessor.process(List.of(response1, response2, response3)));

            verify(detsApiService).deleteBlobDataFromContainer(response1.getDetsLocation());
            verify(detsApiService).deleteBlobDataFromContainer(response2.getDetsLocation());
            verify(detsApiService).deleteBlobDataFromContainer(response3.getDetsLocation());
            verify(objectStateRecordRepository).markDetsCleanupStatusAsComplete(List.of(1L, 3L));
        }

        @Test
        @DisplayName("Handles exceptions thrown while deleting blobs and continues with remaining records")
        void shouldHandleExceptionsAndContinueProcessing() throws AzureDeleteBlobException {
            CleanUpDetsDataProcessorImpl.CleanUpDetsDataBatchProcessor batchProcessor = createBatchProcessor();
            CleanUpDetsDataProcessorImpl.CleanUpDetsProcedureResponse response1 = response(1L);
            CleanUpDetsDataProcessorImpl.CleanUpDetsProcedureResponse response2 = response(2L);
            CleanUpDetsDataProcessorImpl.CleanUpDetsProcedureResponse response3 = response(3L);

            when(detsApiService.deleteBlobDataFromContainer(response1.getDetsLocation())).thenReturn(true);
            when(detsApiService.deleteBlobDataFromContainer(response2.getDetsLocation()))
                .thenThrow(new AzureDeleteBlobException("failure"));
            when(detsApiService.deleteBlobDataFromContainer(response3.getDetsLocation())).thenReturn(true);

            assertDoesNotThrow(() -> batchProcessor.process(List.of(response1, response2, response3)));

            InOrder order = inOrder(detsApiService);
            order.verify(detsApiService).deleteBlobDataFromContainer(response1.getDetsLocation());
            order.verify(detsApiService).deleteBlobDataFromContainer(response2.getDetsLocation());
            order.verify(detsApiService).deleteBlobDataFromContainer(response3.getDetsLocation());

            verify(objectStateRecordRepository).markDetsCleanupStatusAsComplete(List.of(1L, 3L));
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

    private CleanUpDetsDataProcessorImpl.CleanUpDetsDataBatchProcessor createBatchProcessor() {
        return new CleanUpDetsDataProcessorImpl.CleanUpDetsDataBatchProcessor(externalObjectDirectoryRepository, objectStateRecordRepository, detsApiService);
    }
}
