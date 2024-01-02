package uk.gov.hmcts.darts.audio.deleter.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.darts.audio.deleter.DataStoreDeleter;
import uk.gov.hmcts.darts.audio.deleter.ExternalDataStoreDeleter;
import uk.gov.hmcts.darts.audio.deleter.ObjectDirectoryDeletedFinder;
import uk.gov.hmcts.darts.audio.exception.AudioApiError;
import uk.gov.hmcts.darts.common.entity.ObjectDirectory;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;
import uk.gov.hmcts.darts.common.exception.AzureDeleteBlobException;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.helper.SystemUserHelper;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;

import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class ExternalDataStoreDeleterImpl<T extends ObjectDirectory> implements ExternalDataStoreDeleter<T> {

    private final ObjectRecordStatusRepository objectRecordStatusRepository;
    private final UserAccountRepository userAccountRepository;
    private final JpaRepository<T, Integer> repository;
    private final ObjectDirectoryDeletedFinder<T> finder;
    private final DataStoreDeleter deleter;
    private final SystemUserHelper systemUserHelper;


    @Override
    public List<T> delete() {
        List<T> toBeDeleted = finder.findMarkedForDeletion();


        UserAccountEntity systemUser = getSystemUser();
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
                entityToBeDeleted.setStatus(deletedStatus);
                entityToBeDeleted.setLastModifiedBy(systemUser);
                repository.saveAndFlush(entityToBeDeleted);
            } catch (AzureDeleteBlobException e) {
                log.error("could not delete from container", e);
            }
        }
        return toBeDeleted;
    }

    private UserAccountEntity getSystemUser() {
        UserAccountEntity user = userAccountRepository.findSystemUser(systemUserHelper.findSystemUserGuid("housekeeping"));
        if (user == null) {
            throw new DartsApiException(AudioApiError.MISSING_SYSTEM_USER);
        }
        return user;
    }


    private ObjectRecordStatusEntity getDeletedStatus() {
        return objectRecordStatusRepository.getReferenceById(ObjectRecordStatusEnum.DELETED.getId());
    }
}
