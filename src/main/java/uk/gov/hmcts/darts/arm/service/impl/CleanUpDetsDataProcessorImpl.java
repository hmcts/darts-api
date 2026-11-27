package uk.gov.hmcts.darts.arm.service.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.arm.service.CleanUpDetsDataProcessor;
import uk.gov.hmcts.darts.common.repository.ObjectStateRecordRepository;
import uk.gov.hmcts.darts.task.config.CleanUpDetsDataAutomatedTaskConfig;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CleanUpDetsDataProcessorImpl implements CleanUpDetsDataProcessor {

    private static final Duration VACUUM_SHUTDOWN_BUFFER = Duration.ofMinutes(1);

    private final CleanUpDetsDataTransactionalProcessor cleanUpDetsDataTransactionalProcessor;
    private final DetsCleanupMaintenanceService detsCleanupMaintenanceService;
    private final Clock clock;

    @Override
    public void processCleanUpDetsData(int batchSize, CleanUpDetsDataAutomatedTaskConfig config) {
        log.info("Processing clean up of DETS data with batch size: {}", batchSize);

        Instant started = clock.instant();
        OffsetDateTime minimumStoredAge = OffsetDateTime.now(clock).minus(config.getMinimumStoredAge());
        int totalProcessed = cleanUpChunks(batchSize, config, started, minimumStoredAge);
        if (totalProcessed > 0) {
            detsCleanupMaintenanceService.vacuumTables(getVacuumTimeoutSeconds(started, config));
        }
        log.info("Completed processing clean up of DETS data. Total processed: {}", totalProcessed);
    }

    private int cleanUpChunks(int batchSize, CleanUpDetsDataAutomatedTaskConfig config, Instant started, OffsetDateTime minimumStoredAge) {
        int chunkSize = config.getChunkSize();
        int totalProcessed = 0;

        while (totalProcessed < batchSize && chunkSize > 0) {
            int timeoutSeconds = getCleanupTimeoutSeconds(started, config);
            if (timeoutSeconds < 1) {
                log.info("DETS cleanup time budget exhausted. Ending cleanup to allow time for vacuum.");
                break;
            }
            log.info("Processing clean up of DETS data with chunk size: {}", chunkSize);

            List<CleanUpDetsProcedureResponse> cleanedUpDetsRecords;
            try {
                cleanedUpDetsRecords = cleanUpDetsDataTransactionalProcessor.callDetsCleanUpStoredProcedure(chunkSize, minimumStoredAge, timeoutSeconds);
            } catch (QueryTimeoutException exception) {
                log.warn("DETS cleanup statement timed out; the current chunk was rolled back. Total previously processed: {}", totalProcessed, exception);
                break;
            }

            if (cleanedUpDetsRecords.isEmpty()) {
                log.info("No more DETS data to clean up. Ending process.");
                break;
            }
            
            //Update total processed count and adjust chunk size for next iteration if needed
            totalProcessed += cleanedUpDetsRecords.size();
            //Ensure we do not exceed the batch size in the next iteration
            //Takes into account the possibility that the procedure may return more records than requested
            if (totalProcessed + chunkSize > batchSize) {
                chunkSize = batchSize - totalProcessed;
            }
            log.info("Processed batch of DETS data clean up. Total processed so far: {}. Batch size: {}", totalProcessed, cleanedUpDetsRecords.size());
        }
        return totalProcessed;
    }

    private int getCleanupTimeoutSeconds(Instant started, CleanUpDetsDataAutomatedTaskConfig config) {
        Duration elapsed = Duration.between(started, clock.instant());
        Duration remaining = getRemainingTaskTime(elapsed, config);
        return Math.toIntExact(Math.min(config.getCleanupTimeout().minus(elapsed).toSeconds(), remaining.toSeconds()));
    }

    private int getVacuumTimeoutSeconds(Instant started, CleanUpDetsDataAutomatedTaskConfig config) {
        Duration elapsed = Duration.between(started, clock.instant());
        Duration remaining = getRemainingTaskTime(elapsed, config);
        long timeoutSeconds = Math.min(config.getVacuumTimeout().toSeconds(), remaining.toSeconds());
        if (timeoutSeconds < 1) {
            throw new IllegalStateException("Insufficient task time remaining to vacuum DETS cleanup tables");
        }
        return Math.toIntExact(timeoutSeconds);
    }

    private Duration getRemainingTaskTime(Duration elapsed, CleanUpDetsDataAutomatedTaskConfig config) {
        return config.getLock().getAtMostFor().minus(elapsed).minus(VACUUM_SHUTDOWN_BUFFER);
    }

    @Component
    @RequiredArgsConstructor
    public static class CleanUpDetsDataTransactionalProcessor {

        private final ObjectStateRecordRepository objectStateRecordRepository;

        @Transactional(propagation = Propagation.REQUIRES_NEW)
        public List<CleanUpDetsProcedureResponse> callDetsCleanUpStoredProcedure(int chunkSize, OffsetDateTime minimumStoredAge, int timeoutSeconds) {
            objectStateRecordRepository.setCleanupStatementTimeout(timeoutSeconds + "s");
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
