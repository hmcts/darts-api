package uk.gov.hmcts.darts.datamanagement.service.impl;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.models.BlobStorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.audio.config.AudioConfigurationProperties;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.enums.ObjectDirectoryStatusEnum;
import uk.gov.hmcts.darts.common.enums.SystemUsersEnum;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectDirectoryStatusRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.datamanagement.config.DataManagementConfiguration;
import uk.gov.hmcts.darts.datamanagement.service.DataManagementService;
import uk.gov.hmcts.darts.datamanagement.service.InboundToUnstructuredProcessor;
import uk.gov.hmcts.darts.transcriptions.config.TranscriptionConfigurationProperties;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.apache.commons.codec.binary.Base64.encodeBase64;
import static org.apache.commons.codec.digest.DigestUtils.md5;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.INBOUND;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.UNSTRUCTURED;
import static uk.gov.hmcts.darts.common.enums.ObjectDirectoryStatusEnum.AWAITING_VERIFICATION;
import static uk.gov.hmcts.darts.common.enums.ObjectDirectoryStatusEnum.FAILURE;
import static uk.gov.hmcts.darts.common.enums.ObjectDirectoryStatusEnum.FAILURE_ARM_INGESTION_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectDirectoryStatusEnum.FAILURE_CHECKSUM_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectDirectoryStatusEnum.FAILURE_FILE_NOT_FOUND;
import static uk.gov.hmcts.darts.common.enums.ObjectDirectoryStatusEnum.FAILURE_FILE_SIZE_CHECK_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectDirectoryStatusEnum.FAILURE_FILE_TYPE_CHECK_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectDirectoryStatusEnum.STORED;


@Service
@RequiredArgsConstructor
@Slf4j
public class InboundToUnstructuredProcessorImpl implements InboundToUnstructuredProcessor {

    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final ObjectDirectoryStatusRepository objectDirectoryStatusRepository;
    private final ExternalLocationTypeRepository externalLocationTypeRepository;
    private final DataManagementService dataManagementService;
    private final DataManagementConfiguration dataManagementConfiguration;
    private final UserAccountRepository userAccountRepository;
    private final TranscriptionConfigurationProperties transcriptionConfigurationProperties;
    private final AudioConfigurationProperties audioConfigurationProperties;
    private final List<Integer> failureStatesList =
        new ArrayList<>(Arrays.asList(
            FAILURE.getId(),
            FAILURE_FILE_NOT_FOUND.getId(),
            FAILURE_FILE_SIZE_CHECK_FAILED.getId(),
            FAILURE_FILE_TYPE_CHECK_FAILED.getId(),
            FAILURE_CHECKSUM_FAILED.getId(),
            FAILURE_ARM_INGESTION_FAILED.getId()
        ));
    private List<ExternalObjectDirectoryEntity> unstructuredStoredList;
    private List<ExternalObjectDirectoryEntity> unstructuredFailedList;


    @Override
    @Transactional
    public void processInboundToUnstructured() {
        log.debug("Processing Inbound data store");
        processAllStoredInboundExternalObjects();
    }

    private void processAllStoredInboundExternalObjects() {
        List<ExternalObjectDirectoryEntity> inboundList = externalObjectDirectoryRepository.findByStatusAndType(getStatus(
            STORED), getType(INBOUND));
        unstructuredStoredList = externalObjectDirectoryRepository.findByStatusAndType(getStatus(STORED), getType(UNSTRUCTURED));
        unstructuredFailedList = externalObjectDirectoryRepository.findByFailedAndType(
            getStatus(FAILURE),
            getStatus(FAILURE_FILE_NOT_FOUND),
            getStatus(FAILURE_ARM_INGESTION_FAILED),
            getStatus(FAILURE_FILE_TYPE_CHECK_FAILED),
            getStatus(FAILURE_FILE_SIZE_CHECK_FAILED),
            getStatus(FAILURE_CHECKSUM_FAILED),
            getType(UNSTRUCTURED)
        );

        log.info("processAllStoredInboundExternalObjects::inboundList {}", inboundList.stream().map(ExternalObjectDirectoryEntity::getId).toList());
        log.info("processAllStoredInboundExternalObjects::unstructuredStoredList {}",
                 unstructuredStoredList.stream().map(ExternalObjectDirectoryEntity::getId).toList());
        log.info("processAllStoredInboundExternalObjects::unstructuredFailedList {}",
                 unstructuredFailedList.stream().map(ExternalObjectDirectoryEntity::getId).toList());

        for (ExternalObjectDirectoryEntity inboundExternalObjectDirectory : inboundList) {
            log.info("processStoredInboundExternalObject::EOD ID: {}", inboundExternalObjectDirectory.getId());
            ExternalObjectDirectoryEntity unstructuredExternalObjectDirectoryEntity = getNewOrExistingExternalObjectDirectory(inboundExternalObjectDirectory);
            ObjectRecordStatusEntity unstructuredStatus = unstructuredExternalObjectDirectoryEntity.getStatus();
            if (unstructuredStatus == null
                || unstructuredStatus.getId().equals(STORED.getId())
                || attemptsExceeded(unstructuredStatus, unstructuredExternalObjectDirectoryEntity)) {
                log.info("processStoredInboundExternalObject::EOD ID: {} being skipped", inboundExternalObjectDirectory.getId());
                continue;
            }

            // save it as AWAITING_VERIFICATION
            unstructuredExternalObjectDirectoryEntity.setStatus(getStatus(AWAITING_VERIFICATION));
            externalObjectDirectoryRepository.saveAndFlush(unstructuredExternalObjectDirectoryEntity);

            log.info("processStoredInboundExternalObject::EOD ID: {} status set to AWAITING_VERIFICATION", inboundExternalObjectDirectory.getId());

            try {
                log.info("processStoredInboundExternalObject::EOD ID: {} trying to move to unstructured", inboundExternalObjectDirectory.getId());
                BinaryData inboundFile = dataManagementService.getBlobData(getInboundContainerName(), inboundExternalObjectDirectory.getExternalLocation());
                final String calculatedChecksum = new String(encodeBase64(md5(inboundFile.toBytes())));
                validate(calculatedChecksum, inboundExternalObjectDirectory, unstructuredExternalObjectDirectoryEntity);

                if (unstructuredExternalObjectDirectoryEntity.getStatus().equals(getStatus(AWAITING_VERIFICATION))) {
                    log.info("processStoredInboundExternalObject::EOD ID: {} passed validation", inboundExternalObjectDirectory.getId());
                    // upload file
                    UUID uuid = dataManagementService.saveBlobData(getUnstructuredContainerName(), inboundFile);
                    unstructuredExternalObjectDirectoryEntity.setChecksum(inboundExternalObjectDirectory.getChecksum());
                    unstructuredExternalObjectDirectoryEntity.setExternalLocation(uuid);
                    unstructuredExternalObjectDirectoryEntity.setStatus(getStatus(STORED));
                    externalObjectDirectoryRepository.saveAndFlush(unstructuredExternalObjectDirectoryEntity);
                    log.info("processStoredInboundExternalObject::EOD ID: {} transfer complete", inboundExternalObjectDirectory.getId());
                }
            } catch (BlobStorageException e) {
                log.error("Failed to get BLOB from datastore {} for file {}", getInboundContainerName(), inboundExternalObjectDirectory.getExternalLocation());
                unstructuredExternalObjectDirectoryEntity.setStatus(getStatus(FAILURE_FILE_NOT_FOUND));
                setNumTransferAttempts(unstructuredExternalObjectDirectoryEntity);
            } catch (Exception e) {
                log.error("Failed to move from inbound to unstructured for EOD ID: {}, with error: {}",
                          inboundExternalObjectDirectory.getId(), e.getMessage(), e);
                unstructuredExternalObjectDirectoryEntity.setStatus(getStatus(FAILURE));
                setNumTransferAttempts(unstructuredExternalObjectDirectoryEntity);
            } finally {
                externalObjectDirectoryRepository.saveAndFlush(unstructuredExternalObjectDirectoryEntity);
            }
        }
    }

    private boolean attemptsExceeded(ObjectRecordStatusEntity unstructuredStatus, ExternalObjectDirectoryEntity unstructuredExternalObjectDirectoryEntity) {
        if (failureStatesList.contains(unstructuredStatus.getId()) && (unstructuredExternalObjectDirectoryEntity.getTransferAttempts() != null)) {
            int numAttempts = unstructuredExternalObjectDirectoryEntity.getTransferAttempts();
            return numAttempts >= 3;
        }
        return false;
    }

    private ExternalObjectDirectoryEntity getNewOrExistingExternalObjectDirectory(ExternalObjectDirectoryEntity inboundExternalObjectDirectory) {

        ExternalObjectDirectoryEntity unstructuredExternalObjectDirectoryEntity = getUnstructuredStored(inboundExternalObjectDirectory);
        if (unstructuredExternalObjectDirectoryEntity == null) {
            unstructuredExternalObjectDirectoryEntity = getUnstructuredFailed(inboundExternalObjectDirectory);
            if (unstructuredExternalObjectDirectoryEntity == null) {
                unstructuredExternalObjectDirectoryEntity = createUnstructuredExternalObjectDirectoryEntity(
                    inboundExternalObjectDirectory);
            }
        }
        return unstructuredExternalObjectDirectoryEntity;
    }

    private ExternalObjectDirectoryEntity getUnstructuredFailed(ExternalObjectDirectoryEntity inboundExternalObjectDirectory) {
        ExternalObjectDirectoryEntity externalObjectDirectoryEntity = null;
        for (ExternalObjectDirectoryEntity eod : unstructuredFailedList) {
            if (failureStatesList.contains(eod.getStatus().getId())) {
                externalObjectDirectoryEntity = getMatchingExternalObjectDirectoryEntity(inboundExternalObjectDirectory, eod);
                if (externalObjectDirectoryEntity != null) {
                    break;
                }
            }
        }

        return externalObjectDirectoryEntity;
    }

    private ExternalObjectDirectoryEntity getUnstructuredStored(ExternalObjectDirectoryEntity inbound) {
        // check in unstructuredStoredList
        ExternalObjectDirectoryEntity externalObjectDirectoryEntity = null;
        for (ExternalObjectDirectoryEntity unstructured : unstructuredStoredList) {
            externalObjectDirectoryEntity = getMatchingExternalObjectDirectoryEntity(
                inbound,
                unstructured
            );
            if (externalObjectDirectoryEntity != null) {
                break;
            }

        }

        return externalObjectDirectoryEntity;
    }

    private static ExternalObjectDirectoryEntity getMatchingExternalObjectDirectoryEntity(
        ExternalObjectDirectoryEntity inbound, ExternalObjectDirectoryEntity unstructured) {
        ExternalObjectDirectoryEntity externalObjectDirectoryEntity = null;
        if (inbound.getMedia() != null
            && unstructured.getMedia() != null
            && (inbound.getMedia().getId().equals(unstructured.getMedia().getId()))) {
            externalObjectDirectoryEntity = unstructured;

        }
        if (inbound.getTranscriptionDocumentEntity() != null
            && unstructured.getTranscriptionDocumentEntity() != null
            && (inbound.getTranscriptionDocumentEntity().getId().equals(unstructured.getTranscriptionDocumentEntity().getId()))) {
            externalObjectDirectoryEntity = unstructured;

        }
        if (inbound.getAnnotationDocumentEntity() != null
            && unstructured.getAnnotationDocumentEntity() != null
            && (inbound.getAnnotationDocumentEntity().getId().equals(unstructured.getAnnotationDocumentEntity().getId()))) {
            externalObjectDirectoryEntity = unstructured;

        }
        return externalObjectDirectoryEntity;
    }

    private void validate(String checksum, ExternalObjectDirectoryEntity inbound, ExternalObjectDirectoryEntity unstructured) {
        MediaEntity mediaEntity = inbound.getMedia();
        if (mediaEntity != null) {
            performValidation(
                unstructured,
                mediaEntity.getChecksum(),
                checksum,
                audioConfigurationProperties.getAllowedExtensions(),
                mediaEntity.getMediaFormat().toLowerCase(),
                audioConfigurationProperties.getMaxFileSize(),
                mediaEntity.getFileSize()
            );
        }

        TranscriptionDocumentEntity transcriptionDocumentEntity = inbound.getTranscriptionDocumentEntity();
        if (transcriptionDocumentEntity != null) {
            performValidation(
                unstructured,
                transcriptionDocumentEntity.getChecksum(),
                checksum,
                transcriptionConfigurationProperties.getAllowedExtensions(),
                FilenameUtils.getExtension(transcriptionDocumentEntity.getFileName()).toLowerCase(),
                transcriptionConfigurationProperties.getMaxFileSize(),
                Long.valueOf(transcriptionDocumentEntity.getFileSize())
            );
        }

        AnnotationDocumentEntity annotationDocumentEntity = inbound.getAnnotationDocumentEntity();
        if (annotationDocumentEntity != null) {
            performValidation(
                unstructured,
                annotationDocumentEntity.getChecksum(),
                checksum,
                transcriptionConfigurationProperties.getAllowedExtensions(),
                FilenameUtils.getExtension(annotationDocumentEntity.getFileName()).toLowerCase(),
                transcriptionConfigurationProperties.getMaxFileSize(),
                Long.valueOf(annotationDocumentEntity.getFileSize())
            );
        }

    }

    private void performValidation(
        ExternalObjectDirectoryEntity unstructured,
        String incomingChecksum, String calculatedChecksum,
        List<String> allowedExtensions, String extension,
        Integer maxFileSize, Long fileSize) {
        if (incomingChecksum == null || calculatedChecksum.compareTo(incomingChecksum) != 0) {
            log.error("Checksum comparison failed, incoming \"{}\" not equal to calculated \"{}\", for unstructured EOD: {}",
                      incomingChecksum, calculatedChecksum, unstructured.getId());
            unstructured.setStatus(getStatus(FAILURE_CHECKSUM_FAILED));
        }
        if (!allowedExtensions.contains(extension)) {
            unstructured.setStatus(getStatus(FAILURE_FILE_TYPE_CHECK_FAILED));
        }
        if (fileSize > maxFileSize) {
            unstructured.setStatus(getStatus(FAILURE_FILE_SIZE_CHECK_FAILED));
        }

        setNumTransferAttempts(unstructured);
    }

    private void setNumTransferAttempts(ExternalObjectDirectoryEntity unstructuredExternalObjectDirectoryEntity) {
        if (failureStatesList.contains(unstructuredExternalObjectDirectoryEntity.getStatus().getId())) {
            int numAttempts = 1;
            if (unstructuredExternalObjectDirectoryEntity.getTransferAttempts() != null) {
                numAttempts = unstructuredExternalObjectDirectoryEntity.getTransferAttempts() + 1;
            }
            unstructuredExternalObjectDirectoryEntity.setTransferAttempts(numAttempts);
        }
    }

    private ExternalObjectDirectoryEntity createUnstructuredExternalObjectDirectoryEntity(ExternalObjectDirectoryEntity externalObjectDirectory) {

        ExternalObjectDirectoryEntity unstructuredExternalObjectDirectoryEntity = new ExternalObjectDirectoryEntity();
        unstructuredExternalObjectDirectoryEntity.setExternalLocationType(externalLocationTypeRepository.getReferenceById(UNSTRUCTURED.getId()));
        unstructuredExternalObjectDirectoryEntity.setStatus(getStatus(AWAITING_VERIFICATION));
        unstructuredExternalObjectDirectoryEntity.setExternalLocation(externalObjectDirectory.getExternalLocation());
        MediaEntity mediaEntity = externalObjectDirectory.getMedia();
        if (mediaEntity != null) {
            unstructuredExternalObjectDirectoryEntity.setMedia(mediaEntity);
        }
        TranscriptionDocumentEntity transcriptionDocumentEntity = externalObjectDirectory.getTranscriptionDocumentEntity();
        if (transcriptionDocumentEntity != null) {
            unstructuredExternalObjectDirectoryEntity.setTranscriptionDocumentEntity(transcriptionDocumentEntity);
        }
        AnnotationDocumentEntity annotationDocumentEntity = externalObjectDirectory.getAnnotationDocumentEntity();
        if (annotationDocumentEntity != null) {
            unstructuredExternalObjectDirectoryEntity.setAnnotationDocumentEntity(annotationDocumentEntity);
        }
        OffsetDateTime now = OffsetDateTime.now();
        unstructuredExternalObjectDirectoryEntity.setCreatedDateTime(now);
        unstructuredExternalObjectDirectoryEntity.setLastModifiedDateTime(now);
        var systemUser = userAccountRepository.getReferenceById(SystemUsersEnum.DEFAULT.getId());
        unstructuredExternalObjectDirectoryEntity.setCreatedBy(systemUser);
        unstructuredExternalObjectDirectoryEntity.setLastModifiedBy(systemUser);

        return unstructuredExternalObjectDirectoryEntity;
    }

    private String getInboundContainerName() {
        return dataManagementConfiguration.getInboundContainerName();
    }

    private String getUnstructuredContainerName() {
        return dataManagementConfiguration.getUnstructuredContainerName();
    }

    private ObjectRecordStatusEntity getStatus(ObjectDirectoryStatusEnum status) {
        if (objectDirectoryStatusRepository != null) {
            return objectDirectoryStatusRepository.getReferenceById(status.getId());
        }
        return null;
    }

    private ExternalLocationTypeEntity getType(ExternalLocationTypeEnum type) {
        return externalLocationTypeRepository.getReferenceById(type.getId());
    }
}
