package uk.gov.hmcts.darts.audio.service.impl;

import com.azure.storage.blob.BlobContainerClient;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.exception.ExternalDataDeleterException;
import uk.gov.hmcts.darts.audio.service.InboundUnstructuredDataStoreDeleter;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.ObjectDirectoryStatusEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ObjectDirectoryStatusEnum;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectDirectoryStatusRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.datamanagement.config.DataManagementConfiguration;
import uk.gov.hmcts.darts.datamanagement.dao.DataManagementDao;

import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.INBOUND;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.UNSTRUCTURED;

@Service
public class InboundUnstructuredDataStoreStoreDeleterImpl implements InboundUnstructuredDataStoreDeleter {


    private final DataManagementDao dataManagementDao;

    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final ObjectDirectoryStatusRepository objectDirectoryStatusRepository;
    private final ExternalLocationTypeRepository externalLocationTypeRepository;
    private final DataManagementConfiguration dataManagementConfiguration;

    private final UserAccountRepository userAccountRepository;


    public InboundUnstructuredDataStoreStoreDeleterImpl(DataManagementDao dataManagementDao,
                                                        ExternalObjectDirectoryRepository externalObjectDirectoryRepository,
                                                        ObjectDirectoryStatusRepository objectDirectoryStatusRepository,
                                                        ExternalLocationTypeRepository externalLocationTypeRepository,
                                                        DataManagementConfiguration dataManagementConfiguration, UserAccountRepository userAccountRepository) {
        this.dataManagementDao = dataManagementDao;
        this.externalObjectDirectoryRepository = externalObjectDirectoryRepository;
        this.objectDirectoryStatusRepository = objectDirectoryStatusRepository;
        this.externalLocationTypeRepository = externalLocationTypeRepository;
        this.dataManagementConfiguration = dataManagementConfiguration;
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional
    public List<ExternalObjectDirectoryEntity> delete() {

        Optional<UserAccountEntity> systemUser = getSystemUser();

        if (systemUser.isEmpty()) {
            throw new DartsApiException(ExternalDataDeleterException.MISSING_SYSTEM_USER);
        }

        ObjectDirectoryStatusEntity markedForDeletionStatus = getMarkedForDeletionStatus();
        List<ExternalObjectDirectoryEntity> inboundData = deleteExternalData(
            externalLocationTypeRepository.getReferenceById(INBOUND.getId()), markedForDeletionStatus,
            dataManagementConfiguration.getInboundContainerName(),
            systemUser.get()
        );

        List<ExternalObjectDirectoryEntity> unstructuredData = deleteExternalData(
            externalLocationTypeRepository.getReferenceById(UNSTRUCTURED.getId()),
            markedForDeletionStatus,
            dataManagementConfiguration.getUnstructuredContainerName(),
            systemUser.get()
        );

        inboundData.addAll(unstructuredData);

        return inboundData;

    }

    private List<ExternalObjectDirectoryEntity> deleteExternalData(ExternalLocationTypeEntity externalLocationType,
                                                                   ObjectDirectoryStatusEntity markedForDeletionStatus,
                                                                   String containerName, UserAccountEntity systemUser) {
        List<ExternalObjectDirectoryEntity> externalData = externalObjectDirectoryRepository.findByExternalLocationTypeAndMarkedForDeletion(
            externalLocationType,
            markedForDeletionStatus
        );

        BlobContainerClient containerClient = dataManagementDao.getBlobContainerClient(containerName);

        ObjectDirectoryStatusEntity deletedStatus = getDeletedStatus();
        for (ExternalObjectDirectoryEntity data : externalData) {
            dataManagementDao.getBlobClient(containerClient, data.getExternalLocation()).delete();
            data.setStatus(deletedStatus);
            data.setLastModifiedBy(systemUser);
        }
        return externalData;
    }

    private Optional<UserAccountEntity> getSystemUser() {
        return userAccountRepository.findById(0);
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
