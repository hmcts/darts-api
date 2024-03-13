package uk.gov.hmcts.darts.audio.deleter.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.darts.audio.deleter.DataStoreDeleter;
import uk.gov.hmcts.darts.audio.deleter.ExternalDataStoreDeleter;
import uk.gov.hmcts.darts.audio.deleter.ObjectDirectoryDeletedFinder;
import uk.gov.hmcts.darts.common.entity.ObjectDirectory;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.exception.AzureDeleteBlobException;
import uk.gov.hmcts.darts.common.repository.TransformedMediaRepository;

import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class ExternalDataStoreDeleterImpl<T extends ObjectDirectory> implements ExternalDataStoreDeleter<T> {

    private final JpaRepository<T, Integer> repository;
    private final ObjectDirectoryDeletedFinder<T> finder;
    private final DataStoreDeleter deleter;
    private final TransformedMediaRepository transformedMediaRepository;

    @Override
    public List<T> delete() {
        List<T> toBeDeleted = finder.findMarkedForDeletion();

        for (T entityToBeDeleted : toBeDeleted) {
            UUID externalLocation = entityToBeDeleted.getLocation();
            Integer entityId = entityToBeDeleted.getId();
            int statusId = entityToBeDeleted.getStatusId();
            log.info(
                "Deleting storage data with externalLocation={} for entityId={} and statusId={}",
                externalLocation, entityId, statusId
            );

            try {
                deleter.delete(externalLocation);
                repository.delete(entityToBeDeleted);
                if (entityToBeDeleted instanceof TransientObjectDirectoryEntity transientObjectDirectoryEntity) {
                    transformedMediaRepository.delete(transientObjectDirectoryEntity.getTransformedMedia());
                }
            } catch (AzureDeleteBlobException e) {
                log.error(
                    "Failed to delete storage data with externalLocation={} for entityId={} and statusId={}",
                    externalLocation, entityId, statusId, e
                );
            }
        }
        return toBeDeleted;
    }

}
