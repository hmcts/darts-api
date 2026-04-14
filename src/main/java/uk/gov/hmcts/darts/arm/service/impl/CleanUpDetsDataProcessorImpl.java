package uk.gov.hmcts.darts.arm.service.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.arm.service.CleanUpDetsDataProcessor;
import uk.gov.hmcts.darts.common.exception.AzureDeleteBlobException;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectStateRecordRepository;
import uk.gov.hmcts.darts.common.util.EodHelper;
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

    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final CleanUpDetsDataBatchProcessor cleanUpDetsDataBatchProcessor;
    private final Clock clock;

    @Override
    public void processCleanUpDetsData(int batchSize, CleanUpDetsDataAutomatedTaskConfig config) {
        log.info("Processing clean up of DETS data with batch size: {}", batchSize);

        List<Long> eodIdsToCleanUp = externalObjectDirectoryRepository.findEodsWithMatchingRecordInArm(
            EodHelper.storedStatus(),
            EodHelper.detsLocation(),
            EodHelper.armLocation(),
            OffsetDateTime.now(clock),
            Limit.of(batchSize)
        );

        log.info("Found {} EOD records to clean up", eodIdsToCleanUp.size());
        if (CollectionUtils.isNotEmpty(eodIdsToCleanUp)) {
            //Chunk the EOD IDs into batches and process each batch in parallel
            List<List<Long>> batchesForArm = ListUtils.partition(eodIdsToCleanUp, config.getChunkSize());


            List<Callable<Void>> tasks = batchesForArm
                .stream()
                .map(eodsForBatch -> (Callable<Void>) () -> {
                    //Call another serive to ensure the batch is processed within a transaction
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
        } else {
            log.info("No DETS EODs require clean up");
        }
    }

    @Component
    @RequiredArgsConstructor
    public class CleanUpDetsDataBatchProcessor {

        private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
        private final ObjectStateRecordRepository objectStateRecordRepository;
        private final DetsApiService detsApiService;

        @Transactional
        public void process(List<Long> eodsForBatch) {
            log.info("Cleaning up DETS data for batch of EOD IDs: {}", eodsForBatch);
            List<CleanUpDetsProcedureResponse> responses = externalObjectDirectoryRepository.cleanUpDetsDataProcedure(eodsForBatch);
            log.info("Clean up DETS data procedure completed for batch. Responses: {}", responses);

            List<Long> objectStateRecordsForDetsRecordsCleanedUpSuccessfully = new ArrayList<>();
            List<Long> objectStateRecordsForDetsRecordsFailedToCleanUp = new ArrayList<>();
            for (CleanUpDetsProcedureResponse response : responses) {
                try {
                    log.debug("Processing clean up response for EOD ID: {}, Location: {}", response.getId(), response.getLocation());
                    if (detsApiService.deleteBlobDataFromContainer(response.location)) {
                        log.debug("Successfully deleted DETS blob for EOD ID: {}, Location: {}", response.getId(), response.getLocation());
                        objectStateRecordsForDetsRecordsCleanedUpSuccessfully.add(response.getId());
                        continue;
                    } else {
                        log.error("Failed to delete DETS blob for EOD ID: {}, Location: {}. Blob may not exist or deletion failed.",
                                  response.getId(), response.getLocation());
                    }
                } catch (AzureDeleteBlobException azureDeleteBlobException) {
                    log.error("AzureDeleteBlobException while deleting DETS blob for EOD Location: {}, object state record id: {}.",
                              response.getLocation(), response.getId(), azureDeleteBlobException);
                }
                objectStateRecordsForDetsRecordsFailedToCleanUp.add(response.getId());
            }
            objectStateRecordRepository.markDetsCleanupStatusAsComplete(objectStateRecordsForDetsRecordsCleanedUpSuccessfully);
            log.info("Marked object state records as clean up complete for EOD IDs: {}", objectStateRecordsForDetsRecordsCleanedUpSuccessfully);

            if (CollectionUtils.isNotEmpty(objectStateRecordsForDetsRecordsFailedToCleanUp)) {
                log.info("Dets clean up failed for Object state record Ids: {}", objectStateRecordsForDetsRecordsFailedToCleanUp);
            }
        }
    }

    @AllArgsConstructor
    @Getter
    @Setter
    public static class CleanUpDetsProcedureResponse {
        private Long id;
        private String location;
    }
}
