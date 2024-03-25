package uk.gov.hmcts.darts.audio.helper;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.datamanagement.enums.DatastoreContainerType;
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
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.UNSTRUCTURED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;
import static uk.gov.hmcts.darts.datamanagement.DataManagementConstants.MetaDataNames.MEDIA_REQUEST_ID;

@Component
@RequiredArgsConstructor
@Slf4j
public class UnstructuredDataHelper {
    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final DataManagementApi dataManagementApi;
    private final ObjectRecordStatusRepository objectRecordStatusRepository;
    private final ExternalLocationTypeRepository externalLocationTypeRepository;
    private final UserAccountRepository userAccountRepository;

    private static final List<CompletableFuture> jobsList = new ArrayList<>();

    public static List<CompletableFuture> getJobsList() {
        return jobsList;
    }

    public boolean createUnstructured(ExternalObjectDirectoryEntity eodEntityToDelete, ExternalObjectDirectoryEntity eodEntity, BinaryData binaryData) {
        boolean returnVal = true;
        UUID uuid = saveToUnstructuredDataStore(eodEntity, binaryData);
        if (uuid == null) {
            returnVal = false;
        } else {
            saveToDatabase(eodEntity, uuid);
            removeFromDatabase(eodEntityToDelete);
        }
        return returnVal;
    }

    private UUID saveToUnstructuredDataStore(ExternalObjectDirectoryEntity eodEntity, BinaryData binaryData) {
        UUID uuid = null;
        try {
            Map<String, String> metadata = new HashMap<>();
            metadata.put(MEDIA_REQUEST_ID, String.valueOf(eodEntity.getMedia().getId()));
            BlobClient blobClient = dataManagementApi.saveBlobDataToContainer(binaryData, DatastoreContainerType.UNSTRUCTURED, metadata);
            uuid = UUID.fromString(blobClient.getBlobName());
            log.debug(
                "Completed upload to unstructured data store for mediaRequestId {}. Successfully uploaded with blobId: {}",
                eodEntity.getMedia().getId(),
                uuid
            );
        } catch (Exception e) {
            log.error(
                "Upload to unstructured data store failed for mediaRequestId {}.",
                eodEntity.getMedia().getId()
            );
        }
        return uuid;
    }

    @Transactional
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
        OffsetDateTime now = OffsetDateTime.now();
        eodUnstructured.setCreatedDateTime(now);
        eodUnstructured.setLastModifiedDateTime(now);
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
            "Created new record in EOD mediaRequestId {}. External location: {}",
            eodUnstructured.getMedia().getId(),
            eodUnstructured.getExternalLocation()
        );
    }

    @Transactional
    private void removeFromDatabase(ExternalObjectDirectoryEntity eodEntityToDelete) {
        if (eodEntityToDelete != null) {
            externalObjectDirectoryRepository.delete(eodEntityToDelete);
            log.debug(
                "Deleted old unstructured EOD mediaRequestId {}. External location: {}",
                eodEntityToDelete.getMedia().getId(),
                eodEntityToDelete.getExternalLocation()
            );
        }
    }

    public static void addToJobsList(CompletableFuture<Void> saveToUnstructuredFuture) {
        jobsList.add(saveToUnstructuredFuture);
    }

    public static void waitForAllJobsToFinish() {
        jobsList.forEach(CompletableFuture::join);
        jobsList.clear();
    }
}
