package uk.gov.hmcts.darts.audio.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.exception.AudioApiError;
import uk.gov.hmcts.darts.audio.service.OutboundDataStoreDeleter;
import uk.gov.hmcts.darts.common.entity.ObjectDirectoryStatusEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ObjectDirectoryStatusEnum;
import uk.gov.hmcts.darts.common.exception.AzureDeleteBlobException;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.ObjectDirectoryStatusRepository;
import uk.gov.hmcts.darts.common.repository.TransientObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
@Slf4j
public class OutboundDataStoreDeleterImpl implements OutboundDataStoreDeleter {

    private final DataManagementApi dataManagementApi;
    private final ObjectDirectoryStatusRepository objectDirectoryStatusRepository;
    private final UserAccountRepository userAccountRepository;
    private final TransientObjectDirectoryRepository transientObjectDirectoryRepository;


    @Override
    @Transactional
    public List<TransientObjectDirectoryEntity> delete() {

        List<TransientObjectDirectoryEntity> entitiesForDeletion = transientObjectDirectoryRepository.findByStatus(
            getMarkedForDeletionStatus());

        UserAccountEntity systemUser = getSystemUser();
        ObjectDirectoryStatusEntity deletedStatus = getDeletedStatus();

        for (TransientObjectDirectoryEntity entityToBeDeleted : entitiesForDeletion) {
            UUID externalLocation = entityToBeDeleted.getExternalLocation();
            log.debug(
                "Deleting data from outbound container with location: {} for entity with id: {} and status: {}",
                externalLocation,
                entityToBeDeleted.getId(),
                entityToBeDeleted.getStatus()
            );

            try {
                dataManagementApi.deleteBlobDataFromOutboundContainer(externalLocation);
                entityToBeDeleted.setStatus(deletedStatus);
                entityToBeDeleted.setLastModifiedBy(systemUser);
            } catch (AzureDeleteBlobException e) {
                log.error("could not delete from outbound container", e);
            }

        }

        return entitiesForDeletion;
    }


    private UserAccountEntity getSystemUser() {

        Optional<UserAccountEntity> systemUser = userAccountRepository.findById(0);
        if (systemUser.isEmpty()) {
            throw new DartsApiException(AudioApiError.MISSING_SYSTEM_USER);
        }
        return systemUser.get();
    }


    private ObjectDirectoryStatusEntity getMarkedForDeletionStatus() {
        return objectDirectoryStatusRepository.getReferenceById(
            ObjectDirectoryStatusEnum.MARKED_FOR_DELETION.getId());
    }

    private ObjectDirectoryStatusEntity getDeletedStatus() {
        return objectDirectoryStatusRepository.getReferenceById(
            ObjectDirectoryStatusEnum.DELETED.getId());
    }


}
