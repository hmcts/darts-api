package uk.gov.hmcts.darts.audio.deleter.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.darts.audio.deleter.DataStoreDeleter;
import uk.gov.hmcts.darts.audio.deleter.ExternalDataStoreDeleter;
import uk.gov.hmcts.darts.audio.deleter.ObjectDirectoryDeletedFinder;
import uk.gov.hmcts.darts.common.entity.ObjectDirectory;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;
import uk.gov.hmcts.darts.common.exception.AzureDeleteBlobException;
import uk.gov.hmcts.darts.common.helper.SystemUserHelper;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.repository.TransformedMediaRepository;

import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class ExternalDataStoreDeleterImpl<T extends ObjectDirectory> implements ExternalDataStoreDeleter<T> {

    private final ObjectRecordStatusRepository objectRecordStatusRepository;
    private final JpaRepository<T, Integer> repository;
    private final ObjectDirectoryDeletedFinder<T> finder;
    private final DataStoreDeleter deleter;
    private final SystemUserHelper systemUserHelper;
    private final TransformedMediaRepository transformedMediaRepository;

    @Override
    public List<T> delete() {
        List<T> toBeDeleted = finder.findMarkedForDeletion();

        UserAccountEntity systemUser = systemUserHelper.getHousekeepingUser();
        ObjectRecordStatusEntity deletedStatus = getDeletedStatus();

        for (T entityToBeDeleted : toBeDeleted) {
            UUID externalLocation = entityToBeDeleted.getLocation();
            log.info(
                "Deleting data with location: {} for entity with id: {} and status: {}",
                externalLocation,
                entityToBeDeleted.getId(),
                entityToBeDeleted.getStatusId()
            );

            try {
                deleter.delete(externalLocation);
                repository.delete(entityToBeDeleted);
                if (entityToBeDeleted instanceof TransientObjectDirectoryEntity transientObjectDirectoryEntity) {
                    transformedMediaRepository.delete(transientObjectDirectoryEntity.getTransformedMedia());
                }
            } catch (AzureDeleteBlobException e) {
                log.error("could not delete from container", e);
            }
        }
        return toBeDeleted;
    }

    private ObjectRecordStatusEntity getDeletedStatus() {
        return objectRecordStatusRepository.getReferenceById(ObjectRecordStatusEnum.DELETED.getId());
    }
}
