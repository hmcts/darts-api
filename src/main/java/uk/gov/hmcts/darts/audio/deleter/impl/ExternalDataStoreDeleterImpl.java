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

@Slf4j
@RequiredArgsConstructor
public class ExternalDataStoreDeleterImpl<T extends ObjectDirectory> implements ExternalDataStoreDeleter<T> {

    private final JpaRepository<T, Integer> repository;
    private final ObjectDirectoryDeletedFinder<T> finder;
    private final DataStoreDeleter deleter;
    private final TransformedMediaRepository transformedMediaRepository;

    @Override
    public List<T> delete(Integer batchSize) {
        List<T> toBeDeleted = finder.findMarkedForDeletion(batchSize);

        for (T entityToBeDeleted : toBeDeleted) {
            delete(entityToBeDeleted);
        }
        return toBeDeleted;
    }

    @Override
    public List<T> deleteTod(Integer batchSize) {
        List<T> toBeDeleted = finder.findMarkedForDeletion(batchSize);

        for (T entityToBeDeleted : toBeDeleted) {
            deleteTod(entityToBeDeleted, false);
        }
        return toBeDeleted;
    }

    @Override
    public boolean delete(T entityToBeDeleted) {
        String externalLocation = entityToBeDeleted.getLocation();
        Integer entityId = entityToBeDeleted.getId();
        boolean deleted = false;
        int statusId = entityToBeDeleted.getStatusId();
        log.info(
            "Deleting storage data with externalLocation={} for entityId={} and statusId={}",
            externalLocation, entityId, statusId
        );

        try {
            deleter.delete(externalLocation);
            deleted = true;
        } catch (AzureDeleteBlobException e) {
            log.error(
                "Failed to delete storage data with externalLocation={} for entityId={} and statusId={}",
                externalLocation, entityId, statusId, e
            );
        }

        repository.delete(entityToBeDeleted);

        return deleted;
    }

    @Override
    public boolean deleteTod(T entityToBeDeleted, boolean forceDelete) {
        String externalLocation = entityToBeDeleted.getLocation();
        Integer entityId = entityToBeDeleted.getId();
        boolean deleted = false;
        int statusId = entityToBeDeleted.getStatusId();
        log.info(
            "Deleting storage data with externalLocation={} for entityId={} and statusId={}",
            externalLocation, entityId, statusId
        );

        try {
            if (entityToBeDeleted.getLocation() != null) {
                deleter.delete(externalLocation);
                deleted = true;

                entityToBeDeleted.setLocation(null);

                repository.save(entityToBeDeleted);
            }
        } catch (AzureDeleteBlobException e) {
            log.error(
                "Failed to delete storage data with externalLocation={} for entityId={} and statusId={}",
                externalLocation, entityId, statusId, e
            );
        }

        if (entityToBeDeleted instanceof TransientObjectDirectoryEntity transientObjectDirectoryEntity
            && transientObjectDirectoryEntity.getTransformedMedia() != null
            && (forceDelete || transientObjectDirectoryEntity.getTransformedMedia().getExpiryTime() < DateTime.now().getMillis())) {

             repository.delete(entityToBeDeleted);

            log.debug("Deleting transformed media {} with transient object directory id={}",
                      transientObjectDirectoryEntity.getTransformedMedia().getId(), transientObjectDirectoryEntity.getId());

            transformedMediaRepository.delete(transientObjectDirectoryEntity.getTransformedMedia());
        }
        return deleted;
    }
}
