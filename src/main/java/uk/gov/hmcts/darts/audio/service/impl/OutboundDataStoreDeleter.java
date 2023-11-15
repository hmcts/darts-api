package uk.gov.hmcts.darts.audio.service.impl;

import com.azure.storage.blob.BlobContainerClient;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.exception.ExternalDataDeleterException;
import uk.gov.hmcts.darts.common.entity.ObjectDirectoryStatusEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ObjectDirectoryStatusEnum;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.ObjectDirectoryStatusRepository;
import uk.gov.hmcts.darts.common.repository.TransientObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.datamanagement.config.DataManagementConfiguration;
import uk.gov.hmcts.darts.datamanagement.dao.DataManagementDao;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class OutboundDataStoreDeleter implements uk.gov.hmcts.darts.audio.service.OutboundDataStoreDeleter {

    private final DataManagementDao dataManagementDao;
    private final ObjectDirectoryStatusRepository objectDirectoryStatusRepository;
    private final DataManagementConfiguration dataManagementConfiguration;
    private final UserAccountRepository userAccountRepository;
    private final TransientObjectDirectoryRepository transientObjectDirectoryRepository;


    @Override
    @Transactional
    public List<TransientObjectDirectoryEntity> delete() {

        Optional<UserAccountEntity> systemUser = getSystemUser();

        if (systemUser.isEmpty()) {
            throw new DartsApiException(ExternalDataDeleterException.MISSING_SYSTEM_USER);
        }

        List<TransientObjectDirectoryEntity> transientData = transientObjectDirectoryRepository.findByStatus(
            getMarkedForDeletionStatus());


        BlobContainerClient containerClient = dataManagementDao.getBlobContainerClient(
            dataManagementConfiguration.getOutboundContainerName());

        ObjectDirectoryStatusEntity deletedStatus = getDeletedStatus();
        for (TransientObjectDirectoryEntity data : transientData) {
            dataManagementDao.getBlobClient(containerClient, data.getExternalLocation()).delete();
            data.setStatus(deletedStatus);
            data.setLastModifiedBy(systemUser.get());
        }

        return transientData;
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
