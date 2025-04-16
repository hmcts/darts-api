package uk.gov.hmcts.darts.audio.deleter;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.darts.common.entity.ObjectDirectory;

import java.util.Collection;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractExternalDataStoreDeleter<T extends ObjectDirectory,
    R extends JpaRepository<T, Integer>>
    implements ExternalDataStoreDeleter<T> {

    @Getter
    private final R repository;

    @Override
    public Collection<T> delete(Integer batchSize) {
        Collection<T> toBeDeleted = findItemsToDelete(batchSize);
        for (T entityToBeDeleted : toBeDeleted) {
            delete(entityToBeDeleted);
        }
        return toBeDeleted;
    }

    @Override
    public boolean delete(T entityToBeDeleted) {
        boolean deletedFromDataStore = deleteFromDataStore(entityToBeDeleted);
        deleteFromRepository(entityToBeDeleted);
        return deletedFromDataStore;
    }


    protected boolean deleteFromDataStore(T entityToBeDeleted) {
        String externalLocation = entityToBeDeleted.getLocation();
        Integer entityId = entityToBeDeleted.getId();
        int statusId = entityToBeDeleted.getStatusId();
        log.info(
            "Deleting storage data with externalLocation={} for entityId={} and statusId={}",
            externalLocation, entityId, statusId
        );
        try {
            deleteFromDataStore(externalLocation);
            return true;
        } catch (Exception e) {
            log.error(
                "Failed to delete storage data with externalLocation={} for entityId={} and statusId={}",
                externalLocation, entityId, statusId, e
            );
        }
        return false;
    }

    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    protected abstract void deleteFromDataStore(String externalLocation) throws Exception;

    protected abstract Collection<T> findItemsToDelete(int batchSize);

    protected void deleteFromRepository(T entityToBeDeleted) {
        repository.delete(entityToBeDeleted);
    }
}
