package uk.gov.hmcts.darts.arm.service.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.arm.service.CleanUpDetsDataProcessor;
import uk.gov.hmcts.darts.common.repository.ObjectStateRecordRepository;
import uk.gov.hmcts.darts.task.config.CleanUpDetsDataAutomatedTaskConfig;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CleanUpDetsDataProcessorImpl implements CleanUpDetsDataProcessor {

    private final CleanUpDetsDataTransactionalProcessor cleanUpDetsDataTransactionalProcessor;
    private final Clock clock;

    @Override
    public void processCleanUpDetsData(int batchSize, CleanUpDetsDataAutomatedTaskConfig config) {
        log.info("Processing clean up of DETS data with batch size: {}", batchSize);

        OffsetDateTime minimumStoredAge = OffsetDateTime.now(clock).minus(config.getMinimumStoredAge());
        int chunkSize = config.getChunkSize();

        int totalProcessed = 0;

        while (totalProcessed < batchSize && chunkSize > 0) {
            log.info("Processing clean up of DETS data with chunk size: {}", chunkSize);

            List<CleanUpDetsDataProcessorImpl.CleanUpDetsProcedureResponse> processedRows =
                cleanUpDetsDataTransactionalProcessor.callDetsCleanUpStoredProcedure(chunkSize, minimumStoredAge);

            if (processedRows.isEmpty()) {
                log.info("No more DETS data to clean up. Ending process.");
                break;
            }
            
            //Update total processed count and adjust chunk size for next iteration if needed
            totalProcessed += processedRows.size();
            //Ensure we do not exceed the batch size in the next iteration
            //Takes into account the possibility that the procedure may return more records than requested
            if (totalProcessed + chunkSize > batchSize) {
                chunkSize = batchSize - totalProcessed;
            }
            log.info("Processed batch of DETS data clean up. Total processed so far: {}. Batch size: {}", totalProcessed, processedRows.size());
        }
        log.info("Completed processing clean up of DETS data. Total processed: {}", totalProcessed);
    }

    @Component
    @RequiredArgsConstructor
    public static class CleanUpDetsDataTransactionalProcessor {

        private final ObjectStateRecordRepository objectStateRecordRepository;

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
