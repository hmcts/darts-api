package uk.gov.hmcts.darts.datamanagement.service.impl;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.models.BlobStorageException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.config.AudioConfigurationProperties;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.CaseDocumentEntity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;
import uk.gov.hmcts.darts.common.enums.SystemUsersEnum;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.datamanagement.config.DataManagementConfiguration;
import uk.gov.hmcts.darts.datamanagement.service.DataManagementService;
import uk.gov.hmcts.darts.datamanagement.service.InboundToUnstructuredProcessorSingleElement;
import uk.gov.hmcts.darts.transcriptions.config.TranscriptionConfigurationProperties;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import static java.lang.String.format;
import static org.apache.commons.codec.binary.Base64.encodeBase64;
import static org.apache.commons.codec.digest.DigestUtils.md5;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.UNSTRUCTURED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.AWAITING_VERIFICATION;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.FAILURE;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.FAILURE_CHECKSUM_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.FAILURE_EMPTY_FILE;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.FAILURE_FILE_NOT_FOUND;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.FAILURE_FILE_SIZE_CHECK_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.FAILURE_FILE_TYPE_CHECK_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;
import static uk.gov.hmcts.darts.datamanagement.service.impl.InboundToUnstructuredProcessorImpl.FAILURE_STATES_LIST;


@Service
@RequiredArgsConstructor
@Slf4j
public class InboundToUnstructuredProcessorSingleElementImpl implements InboundToUnstructuredProcessorSingleElement {

    private static final int INITIAL_VERIFICATION_ATTEMPTS = 1;
    private static final int INITIAL_TRANSFER_ATTEMPTS = 1;

    private final DataManagementService dataManagementService;
    private final DataManagementConfiguration dataManagementConfiguration;
    private final UserAccountRepository userAccountRepository;
    private final ObjectRecordStatusRepository objectRecordStatusRepository;
    private final ExternalLocationTypeRepository externalLocationTypeRepository;
    private final TranscriptionConfigurationProperties transcriptionConfigurationProperties;
    private final AudioConfigurationProperties audioConfigurationProperties;
    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final MediaRepository mediaRepository;

    @SuppressWarnings("java:S4790")
    @Override
    @Transactional
    public void processSingleElement(Integer inboundObjectId) {
        ExternalObjectDirectoryEntity inboundExternalObjectDirectory = externalObjectDirectoryRepository.findById(inboundObjectId)
            .orElseThrow(() -> new NoSuchElementException(format("external object directory not found with id: %d", inboundObjectId)));

        ExternalObjectDirectoryEntity unstructuredExternalObjectDirectoryEntity = getNewOrExistingInUnstructuredFailed(inboundExternalObjectDirectory);

        unstructuredExternalObjectDirectoryEntity.setStatus(getStatus(AWAITING_VERIFICATION));
        externalObjectDirectoryRepository.saveAndFlush(unstructuredExternalObjectDirectoryEntity);

        try {
            BinaryData inboundFile = dataManagementService.getBlobData(getInboundContainerName(), inboundExternalObjectDirectory.getExternalLocation());
            byte[] bytes = inboundFile.toBytes();
            final String calculatedChecksum = new String(encodeBase64(md5(bytes)));

            validate(calculatedChecksum, inboundExternalObjectDirectory, unstructuredExternalObjectDirectoryEntity, Long.valueOf(bytes.length));

            if (unstructuredExternalObjectDirectoryEntity.getStatus().equals(getStatus(AWAITING_VERIFICATION))) {
                Instant start = Instant.now();
                log.info("INBOUND TO UNSTRUCTURED COPY PERFORMANCE for EOD {} started at {}",
                         unstructuredExternalObjectDirectoryEntity.getId(), start);

                // copy file
                UUID blobId = inboundExternalObjectDirectory.getExternalLocation();
                dataManagementService.copyBlobData(getInboundContainerName(), getUnstructuredContainerName(), blobId);
                Instant copyCompleted = Instant.now();
                log.info("INBOUND TO UNSTRUCTURED COPY PERFORMANCE for EOD {} copy completed at {}",
                         unstructuredExternalObjectDirectoryEntity.getId(), copyCompleted);

                unstructuredExternalObjectDirectoryEntity.setChecksum(inboundExternalObjectDirectory.getChecksum());
                unstructuredExternalObjectDirectoryEntity.setExternalLocation(blobId);
                unstructuredExternalObjectDirectoryEntity.setStatus(getStatus(STORED));
                log.debug("Saving unstructured stored EOD for media ID: {}", unstructuredExternalObjectDirectoryEntity.getId());
                externalObjectDirectoryRepository.saveAndFlush(unstructuredExternalObjectDirectoryEntity);
                log.debug("Transfer complete for EOD ID: {}", inboundExternalObjectDirectory.getId());

                Instant finish = Instant.now();
                long timeElapsed = Duration.between(start, finish).toMillis();
                log.info("INBOUND TO UNSTRUCTURED COPY PERFORMANCE for EOD {} ended at {}",
                         unstructuredExternalObjectDirectoryEntity.getId(), finish);
                log.info("INBOUND TO UNSTRUCTURED COPY PERFORMANCE for EOD {} took {} ms",
                         unstructuredExternalObjectDirectoryEntity.getId(), timeElapsed);
            }
        } catch (BlobStorageException e) {
            log.error("Failed to get BLOB from datastore {} for file {} for EOD ID: {}",
                      getInboundContainerName(), inboundExternalObjectDirectory.getExternalLocation(), inboundExternalObjectDirectory.getId()
            );
            unstructuredExternalObjectDirectoryEntity.setStatus(getStatus(FAILURE_FILE_NOT_FOUND));
            setNumTransferAttempts(unstructuredExternalObjectDirectoryEntity);
        } catch (Exception e) {
            log.error("Failed to move from inboundExternalObjectDirectory to unstructuredExternalObjectDirectoryEntity for EOD ID: {}, with error: {}",
                      inboundExternalObjectDirectory.getId(), e.getMessage(), e
            );
            unstructuredExternalObjectDirectoryEntity.setStatus(getStatus(FAILURE));
            setNumTransferAttempts(unstructuredExternalObjectDirectoryEntity);
        } finally {
            externalObjectDirectoryRepository.saveAndFlush(unstructuredExternalObjectDirectoryEntity);
        }
    }


    private ExternalObjectDirectoryEntity getNewOrExistingInUnstructuredFailed(ExternalObjectDirectoryEntity inboundExternalObjectDirectory) {
        Integer mediaId = null;
        Integer caseDocumentId = null;
        Integer annotationDocumentId = null;
        Integer transcriptionDocumentId = null;
        if (inboundExternalObjectDirectory.getMedia() != null) {
            mediaId = inboundExternalObjectDirectory.getMedia().getId();
        }
        if (inboundExternalObjectDirectory.getCaseDocument() != null) {
            caseDocumentId = inboundExternalObjectDirectory.getCaseDocument().getId();
        }
        if (inboundExternalObjectDirectory.getAnnotationDocumentEntity() != null) {
            annotationDocumentId = inboundExternalObjectDirectory.getAnnotationDocumentEntity().getId();
        }
        if (inboundExternalObjectDirectory.getTranscriptionDocumentEntity() != null) {
            transcriptionDocumentId = inboundExternalObjectDirectory.getTranscriptionDocumentEntity().getId();
        }
        ExternalObjectDirectoryEntity unstructuredExternalObjectDirectoryEntity =
            externalObjectDirectoryRepository.findByIdsAndFailure(mediaId, caseDocumentId, annotationDocumentId, transcriptionDocumentId, FAILURE_STATES_LIST);
        if (unstructuredExternalObjectDirectoryEntity == null) {
            unstructuredExternalObjectDirectoryEntity = createUnstructuredAwaitingVerificationExternalObjectDirectoryEntity(
                inboundExternalObjectDirectory);
        }

        return unstructuredExternalObjectDirectoryEntity;
    }

    private ExternalObjectDirectoryEntity getMatchingExternalObjectDirectoryEntity(
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
        if (inbound.getCaseDocument() != null
            && unstructured.getCaseDocument() != null
            && (inbound.getCaseDocument().getId()).equals(unstructured.getCaseDocument().getId())) {
            externalObjectDirectoryEntity = unstructured;
        }
        return externalObjectDirectoryEntity;
    }

    private void validate(String checksum, ExternalObjectDirectoryEntity inbound, ExternalObjectDirectoryEntity unstructured, Long actualFileSize) {
        MediaEntity mediaEntityLazy = inbound.getMedia();
        if (mediaEntityLazy != null) {
            MediaEntity mediaEntity = mediaRepository.findById(mediaEntityLazy.getId()).orElseThrow(
                () -> new RuntimeException("Media not found: " + mediaEntityLazy.getId()));
            performValidation(
                unstructured,
                mediaEntity.getChecksum(),
                checksum,
                audioConfigurationProperties.getAllowedMediaFormats(),
                mediaEntity.getMediaFormat().toLowerCase(),
                audioConfigurationProperties.getMaxFileSize(),
                actualFileSize
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
        List<String> allowedMediaFormats, String mediaFormat,
        Integer maxFileSize, Long fileSize) {
        if (incomingChecksum == null || calculatedChecksum.compareTo(incomingChecksum) != 0) {
            log.error("Checksum comparison failed, incoming \"{}\" not equal to calculated \"{}\", for unstructured EOD: {}",
                      incomingChecksum, calculatedChecksum, unstructured.getId()
            );
            unstructured.setStatus(getStatus(FAILURE_CHECKSUM_FAILED));
        } else if (!allowedMediaFormats.contains(mediaFormat)) {
            log.error("Media format failed, format {} not in allowed list for unstructured EOD {}", mediaFormat, unstructured.getId());
            unstructured.setStatus(getStatus(FAILURE_FILE_TYPE_CHECK_FAILED));
        } else if (fileSize > maxFileSize) {
            log.error("File size failed, file size {} exceeds max file size {} for unstructured EOD {} ", fileSize, maxFileSize, unstructured.getId());
            unstructured.setStatus(getStatus(FAILURE_FILE_SIZE_CHECK_FAILED));
        } else if (0 == fileSize) {
            log.error("Empty file failed, the file is empty for unstructured EOD {}", unstructured.getId());
            unstructured.setStatus(getStatus(FAILURE_EMPTY_FILE));
        }

        setNumTransferAttempts(unstructured);
    }

    private void setNumTransferAttempts(ExternalObjectDirectoryEntity unstructuredExternalObjectDirectoryEntity) {
        if (FAILURE_STATES_LIST.contains(unstructuredExternalObjectDirectoryEntity.getStatus().getId())) {
            int numAttempts = INITIAL_TRANSFER_ATTEMPTS;
            if (unstructuredExternalObjectDirectoryEntity.getTransferAttempts() != null) {
                numAttempts = unstructuredExternalObjectDirectoryEntity.getTransferAttempts() + INITIAL_TRANSFER_ATTEMPTS;
            }
            unstructuredExternalObjectDirectoryEntity.setTransferAttempts(numAttempts);
        }
    }

    private ExternalObjectDirectoryEntity createUnstructuredAwaitingVerificationExternalObjectDirectoryEntity(
        ExternalObjectDirectoryEntity externalObjectDirectory) {

        ExternalObjectDirectoryEntity unstructuredExternalObjectDirectoryEntity = new ExternalObjectDirectoryEntity();
        unstructuredExternalObjectDirectoryEntity.setExternalLocationType(getType(UNSTRUCTURED));
        unstructuredExternalObjectDirectoryEntity.setStatus(getStatus(AWAITING_VERIFICATION));
        unstructuredExternalObjectDirectoryEntity.setExternalLocation(externalObjectDirectory.getExternalLocation());
        unstructuredExternalObjectDirectoryEntity.setVerificationAttempts(INITIAL_VERIFICATION_ATTEMPTS);
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
        CaseDocumentEntity caseDocumentEntity = externalObjectDirectory.getCaseDocument();
        if (caseDocumentEntity != null) {
            unstructuredExternalObjectDirectoryEntity.setCaseDocument(caseDocumentEntity);
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

    private ObjectRecordStatusEntity getStatus(ObjectRecordStatusEnum status) {
        return objectRecordStatusRepository.getReferenceById(status.getId());
    }

    private ExternalLocationTypeEntity getType(ExternalLocationTypeEnum type) {
        return externalLocationTypeRepository.getReferenceById(type.getId());
    }
}
