package uk.gov.hmcts.darts.arm.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.arm.service.CleanupDetsDataService;
import uk.gov.hmcts.darts.audio.deleter.impl.ExternalDetsDataStoreDeleter;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.exception.AzureDeleteBlobException;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectStateRecordRepository;
import uk.gov.hmcts.darts.common.util.EodHelper;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class CleanupDetsDataServiceImpl implements CleanupDetsDataService {

    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final CurrentTimeHelper currentTimeHelper;
    private final CleanupDetsEodTransactionalService cleanupDetsEodTransactionalService;

    @Override
    public void cleanupDetsData(int batchsize, Duration durationInArmStorage) {
        log.info("Started running DETS cleanup at: {}", OffsetDateTime.now());

        OffsetDateTime lastModifiedBefore = currentTimeHelper.currentOffsetDateTime().minus(durationInArmStorage);

        List<Long> detsEods = externalObjectDirectoryRepository.findEodsNotInOtherStorageLastModifiedBefore(EodHelper.storedStatus(),
                                                                                                            EodHelper.detsLocation(),
                                                                                                            EodHelper.armLocation(),
                                                                                                            lastModifiedBefore,
                                                                                                            batchsize);
        for (Long detsEodId : detsEods) {
            cleanupDetsEodTransactionalService.cleanupDetsEod(detsEodId);
        }

    }

    @Service
    @RequiredArgsConstructor
    public static class CleanupDetsEodTransactionalService {

        private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
        private final CurrentTimeHelper currentTimeHelper;
        private final ExternalDetsDataStoreDeleter detsDataStoreDeleter;
        private final ObjectStateRecordRepository objectStateRecordRepository;

        @Transactional
        public void cleanupDetsEod(Long detsEodId) {
            Optional<ExternalObjectDirectoryEntity> detsEodRecord = externalObjectDirectoryRepository.findById(detsEodId);
            if (detsEodRecord.isEmpty()) {
                log.warn("Unable to find ExternalObjectDirectory: {}", detsEodId);
                return;
            }
            var detsEod = detsEodRecord.get();
            try {
                detsDataStoreDeleter.deleteFromDataStore(detsEod.getExternalLocation());
                Long detsObectStateRecordId = detsEod.getOsrUuid();
                externalObjectDirectoryRepository.deleteById(detsEodId);
                var objectStateRecord = objectStateRecordRepository.findById(detsObectStateRecordId).get();
                objectStateRecord.setFlagFileDetsCleanupStatus(true);
                objectStateRecord.setDateFileDetsCleanup(currentTimeHelper.currentOffsetDateTime());
                objectStateRecordRepository.save(objectStateRecord);
            } catch (AzureDeleteBlobException e) {
                log.error("Unable to delete from DETS storage location {}", detsEod.getExternalLocation(), e);
            } catch (Exception e) {
                log.error("Unable to clean up DETS eod {}", detsEod.getId(), e);
            }
        }
    }

}
