package uk.gov.hmcts.darts.audio.deleter.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.TransformedMediaRepository;
import uk.gov.hmcts.darts.common.repository.TransientObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class ExternalOutboundDataStoreDeleterWithBuffer extends ExternalOutboundDataStoreDeleter {

    private final Duration transientObjectDirectoryDeleteBuffer;
    private final CurrentTimeHelper currentTimeHelper;

    public ExternalOutboundDataStoreDeleterWithBuffer(TransientObjectDirectoryRepository repository,
                                                      TransformedMediaRepository transformedMediaRepository,
                                                      DataManagementApi dataManagementApi,
                                                      CurrentTimeHelper currentTimeHelper,
                                                      @Value("${darts.automated.task.external-datastore-deleter.transient-object-directory-delete-buffer}")
                                                      Duration transientObjectDirectoryDeleteBuffer) {
        super(repository, transformedMediaRepository, dataManagementApi);
        this.currentTimeHelper = currentTimeHelper;
        this.transientObjectDirectoryDeleteBuffer = transientObjectDirectoryDeleteBuffer;
    }


    @Override
    public Collection<TransientObjectDirectoryEntity> delete(Integer batchSize) {
        Collection<TransientObjectDirectoryEntity> result = super.delete(batchSize);
        deleteExpiredTransientObjectEntities(batchSize);
        return result;
    }

    void deleteExpiredTransientObjectEntities(Integer batchSize) {
        OffsetDateTime maxTimeToDelete = currentTimeHelper.currentOffsetDateTime().minus(transientObjectDirectoryDeleteBuffer);

        Collection<TransientObjectDirectoryEntity> expiredTransientObjectDirectoryEntities =
            this.getRepository().findByTransformedMediaIsNullOrExpirtyBeforeMaxExpiryTime(maxTimeToDelete,
                                                                                          ObjectRecordStatusEnum.DATASTORE_DELETED.getId(),
                                                                                          Limit.of(batchSize));

        if (log.isInfoEnabled()) {
            log.info("Deleting {} expired transient object directories older than {}. Ids: {}",
                     expiredTransientObjectDirectoryEntities.size(), maxTimeToDelete,
                     expiredTransientObjectDirectoryEntities.stream()
                         .map(TransientObjectDirectoryEntity::getId)
                         .toList());
        }
        // Delete transformed media entities associated with the expired transient object directories
        List<TransformedMediaEntity> transformedMediaEntityList = expiredTransientObjectDirectoryEntities.stream()
            .map(TransientObjectDirectoryEntity::getTransformedMedia)
            .filter(Objects::nonNull)
            .toList();
        transformedMediaRepository.deleteAll(transformedMediaEntityList);
        this.getRepository().deleteAll(expiredTransientObjectDirectoryEntities);
    }

    @Override
    public void datastoreDeletionCallback(TransientObjectDirectoryEntity transientObjectDirectoryEntity) {
        transientObjectDirectoryEntity.setStatus(EodHelper.datastoreDeletionStatus());
        this.getRepository().save(transientObjectDirectoryEntity);
    }
}
