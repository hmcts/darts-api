package uk.gov.hmcts.darts.arm.service.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.arm.service.CleanUpDetsDataProcessor;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectStateRecordRepository;
import uk.gov.hmcts.darts.dets.service.DetsApiService;
import uk.gov.hmcts.darts.task.config.CleanUpDetsDataAutomatedTaskConfig;
import uk.gov.hmcts.darts.util.AsyncUtil;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@Slf4j
@Service
@RequiredArgsConstructor
public class CleanUpDetsDataProcessorImpl implements CleanUpDetsDataProcessor {

    private final CleanUpDetsDataBatchProcessor cleanUpDetsDataBatchProcessor;
    private final Clock clock;

    @Override
    public void processCleanUpDetsData(int batchSize, CleanUpDetsDataAutomatedTaskConfig config) {
        log.info("Processing clean up of DETS data with batch size: {}", batchSize);

        OffsetDateTime minimumStoredAge = OffsetDateTime.now(clock).minus(config.getMinimumStoredAge());
        int chunkSize = config.getChunkSize();

        int totalProcessed = 0;

        while (totalProcessed < batchSize && chunkSize > 0) {
            log.info("Processing clean up of DETS data with chunk size: {}", chunkSize);

            List<CleanUpDetsDataProcessorImpl.CleanUpDetsProcedureResponse> eodIdsToCleanUp =
                cleanUpDetsDataBatchProcessor.callDetsCleanUpStoredProcedure(chunkSize, minimumStoredAge);

            if (eodIdsToCleanUp.isEmpty()) {
                log.info("No more DETS data to clean up. Ending process.");
                break;
            }

            List<List<CleanUpDetsDataProcessorImpl.CleanUpDetsProcedureResponse>> batchesToDeleteBlobStoreRecordFor =
                ListUtils.partition(eodIdsToCleanUp, config.getChunkSize() / config.getThreads());

            List<Callable<Void>> tasks = batchesToDeleteBlobStoreRecordFor
                .stream()
                .map(eodsForBatch -> (Callable<Void>) () -> {
                    cleanUpDetsDataBatchProcessor.process(eodsForBatch);
                    return null;
                })
                .toList();

            try {
                AsyncUtil.invokeAllAwaitTermination(tasks, config);
            } catch (InterruptedException e) {
                log.error("Clean up dets data batch processing interrupted", e);
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.error("Clean up dets data unexpected exception", e);
            }
            //Update total processed count and adjust chunk size for next iteration if needed
            totalProcessed += eodIdsToCleanUp.size();
            //Ensure we do not exceed the batch size in the next iteration
            //Takes into account the possibility that the procedure may return more records than requested
            if (totalProcessed + chunkSize > batchSize) {
                chunkSize = batchSize - totalProcessed;
            }
            log.info("Processed batch of DETS data clean up. Total processed so far: {}. Batch size: {}", totalProcessed, eodIdsToCleanUp.size());
        }
        log.info("Completed processing clean up of DETS data. Total processed: {}", totalProcessed);
    }

    @Component
    @RequiredArgsConstructor
    public static class CleanUpDetsDataBatchProcessor {

        private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
        private final ObjectStateRecordRepository objectStateRecordRepository;
        private final DetsApiService detsApiService;

        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public void process(List<CleanUpDetsDataProcessorImpl.CleanUpDetsProcedureResponse> eodsCleanedUp) {
            List<Long> objectStateRecordsForDetsRecordsCleanedUpSuccessfully = new ArrayList<>();
            List<Long> objectStateRecordsForDetsRecordsFailedToCleanUp = new ArrayList<>();

            if (eodsCleanedUp.isEmpty()) {
                return;
            }

            for (CleanUpDetsProcedureResponse response : eodsCleanedUp) {
                try {
                    log.debug("Processing clean up response for EOD ID: {}, Location: {}", response.getOsrUuid(), response.getDetsLocation());
                    if (detsApiService.deleteBlobDataFromContainer(response.getDetsLocation())) {
                        log.debug("Successfully deleted DETS blob for EOD ID: {}, Location: {}", response.getOsrUuid(), response.getDetsLocation());
                        objectStateRecordsForDetsRecordsCleanedUpSuccessfully.add(response.getOsrUuid());
                        continue;
                    } else {
                        log.error("Failed to delete DETS blob for EOD ID: {}, Location: {}. Blob may not exist or deletion failed.",
                                  response.getOsrUuid(), response.getDetsLocation());
                    }
                } catch (Exception exception) {
                    log.error("Failed to delete DETS blob for EOD Location: {}, object state record id: {}.",
                              response.getDetsLocation(), response.getOsrUuid(), exception);
                }
                objectStateRecordsForDetsRecordsFailedToCleanUp.add(response.getOsrUuid());
            }
            objectStateRecordRepository.markDetsCleanupStatusAsComplete(objectStateRecordsForDetsRecordsCleanedUpSuccessfully);
            log.info("Marked object state records as clean up complete for EOD IDs: {}", objectStateRecordsForDetsRecordsCleanedUpSuccessfully);

            if (CollectionUtils.isNotEmpty(objectStateRecordsForDetsRecordsFailedToCleanUp)) {
                log.error("Dets clean up failed for Object state record Ids: {}", objectStateRecordsForDetsRecordsFailedToCleanUp);
            }
        }

        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public List<CleanUpDetsDataProcessorImpl.CleanUpDetsProcedureResponse> callDetsCleanUpStoredProcedure(int chunkSize, OffsetDateTime minimumStoredAge) {
            return objectStateRecordRepository.cleanUpDetsDataProcedure(chunkSize, minimumStoredAge);
        }
    }

    @AllArgsConstructor
    @Getter
    @Setter
    public static class CleanUpDetsProcedureResponse {
        private Long osrUuid;
        private String detsLocation;
    }
}
