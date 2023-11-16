package uk.gov.hmcts.darts.audio.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.exception.AudioApiError;
import uk.gov.hmcts.darts.audio.service.InboundUnstructuredDataStoreDeleter;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.ObjectDirectoryStatusEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.enums.ObjectDirectoryStatusEnum;
import uk.gov.hmcts.darts.common.exception.AzureException;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectDirectoryStatusRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.INBOUND;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.UNSTRUCTURED;

@Service
@Slf4j
@RequiredArgsConstructor
public class InboundUnstructuredDataStoreStoreDeleterImpl implements InboundUnstructuredDataStoreDeleter {


    private final DataManagementApi dataManagementApi;

    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final ObjectDirectoryStatusRepository objectDirectoryStatusRepository;
    private final ExternalLocationTypeRepository externalLocationTypeRepository;

    private final UserAccountRepository userAccountRepository;


    @Transactional
    public List<ExternalObjectDirectoryEntity> delete() {

        List<ExternalObjectDirectoryEntity> deleteExternalData = deleteExternalData(
            externalLocationTypeRepository.getReferenceById(INBOUND.getId()), INBOUND
        );

        List<ExternalObjectDirectoryEntity> unstructuredData = deleteExternalData(
            externalLocationTypeRepository.getReferenceById(UNSTRUCTURED.getId()),
            UNSTRUCTURED
        );

        deleteExternalData.addAll(unstructuredData);

        return deleteExternalData;

    }

    private List<ExternalObjectDirectoryEntity> deleteExternalData(ExternalLocationTypeEntity externalLocationType,
                                                                   ExternalLocationTypeEnum locationType) {
        List<ExternalObjectDirectoryEntity> entitiesForDeletion = externalObjectDirectoryRepository.findByExternalLocationTypeAndMarkedForDeletion(
            externalLocationType,
            getMarkedForDeletionStatus()
        );


        UserAccountEntity systemUser = getSystemUser();
        ObjectDirectoryStatusEntity deletedStatus = getDeletedStatus();

        for (ExternalObjectDirectoryEntity entityToBeDeleted : entitiesForDeletion) {
            UUID externalLocation = entityToBeDeleted.getExternalLocation();
            log.debug(
                "Deleting data from container: {} with location: {} for entity with id: {} and status: {}",
                locationType, externalLocation, entityToBeDeleted.getId(), entityToBeDeleted.getStatus()
            );
            try {
                if (INBOUND == locationType) {
                    dataManagementApi.deleteBlobDataFromInboundContainer(externalLocation);
                } else if (UNSTRUCTURED == locationType) {
                    dataManagementApi.deleteBlobDataFromUnstructuredContainer(externalLocation);
                }

                entityToBeDeleted.setStatus(deletedStatus);
                entityToBeDeleted.setLastModifiedBy(systemUser);
            } catch (AzureException e) {
                log.error("could not delete from inbound/unstructured container", e);
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
