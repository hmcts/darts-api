package uk.gov.hmcts.darts.audio.helper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.CaseDocumentEntity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.enums.SystemUsersEnum;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.datamanagement.config.DataManagementConfiguration;
import uk.gov.hmcts.darts.datamanagement.service.DataManagementService;

import java.io.InputStream;
import java.util.UUID;

import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.UNSTRUCTURED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;

@Component
@RequiredArgsConstructor
@Slf4j
public class UnstructuredDataHelper {
    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final ObjectRecordStatusRepository objectRecordStatusRepository;
    private final ExternalLocationTypeRepository externalLocationTypeRepository;
    private final UserAccountRepository userAccountRepository;
    private final DataManagementService dataManagementService;
    private final DataManagementConfiguration dataManagementConfiguration;

    @Transactional
    public boolean createUnstructuredDataFromEod(
        ExternalObjectDirectoryEntity eodEntityToDelete,
        ExternalObjectDirectoryEntity eodEntity,
        InputStream inputStream) {
        boolean returnVal = true;
        UUID uuid = saveToUnstructuredDataStore(eodEntity, inputStream);
        if (uuid == null) {
            returnVal = false;
        } else {
            saveToDatabase(eodEntity, uuid);
            removeFromDatabase(eodEntityToDelete);
        }
        return returnVal;
    }

    private UUID saveToUnstructuredDataStore(ExternalObjectDirectoryEntity eodEntity, InputStream inputStream) {
        UUID uuid = null;
        try {
            uuid = dataManagementService.saveBlobData(dataManagementConfiguration.getUnstructuredContainerName(), inputStream)
                .getBlobName();
            log.debug(
                "Completed upload to unstructured data store for EOD {}. Successfully uploaded with blobId: {}",
                eodEntity.getId(),
                uuid
            );
        } catch (Exception e) {
            log.error(
                "Upload to unstructured data store failed for EOD {}.",
                eodEntity.getId(),
                e
            );
        }
        return uuid;
    }

    private void saveToDatabase(ExternalObjectDirectoryEntity eod, UUID uuid) {
        ExternalObjectDirectoryEntity eodUnstructured = new ExternalObjectDirectoryEntity();

        MediaEntity mediaEntity = eod.getMedia();
        if (mediaEntity != null) {
            eodUnstructured.setMedia(mediaEntity);
        }
        TranscriptionDocumentEntity transcriptionDocumentEntity = eod.getTranscriptionDocumentEntity();
        if (transcriptionDocumentEntity != null) {
            eodUnstructured.setTranscriptionDocumentEntity(transcriptionDocumentEntity);
        }
        AnnotationDocumentEntity annotationDocumentEntity = eod.getAnnotationDocumentEntity();
        if (annotationDocumentEntity != null) {
            eodUnstructured.setAnnotationDocumentEntity(annotationDocumentEntity);
        }
        CaseDocumentEntity caseDocumentEntity = eod.getCaseDocument();
        if (caseDocumentEntity != null) {
            eodUnstructured.setCaseDocument(caseDocumentEntity);
        }
        eodUnstructured.setExternalLocation(uuid);
        eodUnstructured.setChecksum(eod.getChecksum());
        ObjectRecordStatusEntity storedStatus = objectRecordStatusRepository.getReferenceById(STORED.getId());
        ExternalLocationTypeEntity unstructuredType = externalLocationTypeRepository.getReferenceById(UNSTRUCTURED.getId());
        eodUnstructured.setStatus(storedStatus);
        eodUnstructured.setExternalLocationType(unstructuredType);
        eodUnstructured.setVerificationAttempts(1);
        var systemUser = userAccountRepository.getReferenceById(SystemUsersEnum.DEFAULT.getId());
        eodUnstructured.setCreatedBy(systemUser);
        eodUnstructured.setLastModifiedBy(systemUser);
        externalObjectDirectoryRepository.save(eodUnstructured);
        log.debug(
            "Created new record in EOD {}. External location: {}",
            eodUnstructured.getId(),
            eodUnstructured.getExternalLocation()
        );
    }

    private void removeFromDatabase(ExternalObjectDirectoryEntity eodEntityToDelete) {
        if (eodEntityToDelete != null) {
            externalObjectDirectoryRepository.delete(eodEntityToDelete);
            log.debug(
                "Deleted old unstructured EOD {}. External location: {}",
                eodEntityToDelete.getId(),
                eodEntityToDelete.getExternalLocation()
            );
        }
    }

}
