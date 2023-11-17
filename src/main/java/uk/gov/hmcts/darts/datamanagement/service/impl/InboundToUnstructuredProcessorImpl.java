package uk.gov.hmcts.darts.datamanagement.service.impl;

import com.azure.core.util.BinaryData;
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
import uk.gov.hmcts.darts.common.entity.ObjectDirectoryStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.enums.SystemUsersEnum;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectDirectoryStatusRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.datamanagement.service.InboundToUnstructuredProcessor;
import uk.gov.hmcts.darts.datamanagement.config.DataManagementConfiguration;
import uk.gov.hmcts.darts.datamanagement.service.DataManagementService;
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

    private ObjectDirectoryStatusEntity statusAwaiting;
    private ObjectDirectoryStatusEntity statusFailed;
    private ObjectDirectoryStatusEntity statusStored ;
    private ExternalLocationTypeEntity externalLocationTypeUnstructured;
    private ObjectDirectoryStatusEntity statusFailedFileSize;
    private ObjectDirectoryStatusEntity statusFailedFileType;
    private ObjectDirectoryStatusEntity statusFailedChecksum;
    private List<Integer> failureStatesList = new ArrayList<>(Arrays.asList(FAILURE.getId(), FAILURE_FILE_NOT_FOUND.getId(), FAILURE_FILE_SIZE_CHECK_FAILED.getId(),FAILURE_FILE_TYPE_CHECK_FAILED.getId(),FAILURE_CHECKSUM_FAILED.getId(),FAILURE_ARM_INGESTION_FAILED.getId()));

    @Override
    @Transactional
    public void processInboundToUnstructured() {
        statusAwaiting = objectDirectoryStatusRepository.getReferenceById(AWAITING_VERIFICATION.getId());
        statusFailed = objectDirectoryStatusRepository.getReferenceById(FAILURE.getId());
        statusStored = objectDirectoryStatusRepository.getReferenceById(STORED.getId());
        statusFailedFileSize = objectDirectoryStatusRepository.getReferenceById(FAILURE_FILE_SIZE_CHECK_FAILED.getId());
        statusFailedFileType = objectDirectoryStatusRepository.getReferenceById(FAILURE_FILE_TYPE_CHECK_FAILED.getId());
        statusFailedChecksum = objectDirectoryStatusRepository.getReferenceById(FAILURE_CHECKSUM_FAILED.getId());

        ExternalLocationTypeEntity externalLocationTypeInbound = externalLocationTypeRepository.getReferenceById(INBOUND.getId());
        externalLocationTypeUnstructured = externalLocationTypeRepository.getReferenceById(UNSTRUCTURED.getId());

        List<ExternalObjectDirectoryEntity> externalObjectDirectoryStoredInbound = externalObjectDirectoryRepository.findByStatusAndType(
            statusStored, externalLocationTypeInbound);

        processAllStoredInboundExternalObjects(externalObjectDirectoryStoredInbound);
    }

    @Transactional
    private void processAllStoredInboundExternalObjects(List<ExternalObjectDirectoryEntity> externalObjectDirectoryStoredInbound) {
        for (ExternalObjectDirectoryEntity inboundExternalObjectDirectory : externalObjectDirectoryStoredInbound) {

            ExternalObjectDirectoryEntity unstructuredExternalObjectDirectoryEntity = getNewOrExistingExternalObjectDirectory(inboundExternalObjectDirectory);

            if (failureStatesList.contains(unstructuredExternalObjectDirectoryEntity.getStatus().getId())) {
                int numAttempts = 0;
                if( unstructuredExternalObjectDirectoryEntity.getTransferAttempts()!=null) {
                    numAttempts = unstructuredExternalObjectDirectoryEntity.getTransferAttempts();
                    if (numAttempts >= 3) {
                        break;
                    }
                }
                unstructuredExternalObjectDirectoryEntity.setTransferAttempts(numAttempts + 1);
            }

            // save it as AWAITING_VERIFICATION
            unstructuredExternalObjectDirectoryEntity.setStatus(statusAwaiting);
            externalObjectDirectoryRepository.saveAndFlush(unstructuredExternalObjectDirectoryEntity);

            BinaryData inboundFile = dataManagementService.getBlobData(getInboundContainerName(), inboundExternalObjectDirectory.getExternalLocation());

            final String calculatedChecksum = new String(encodeBase64(md5(inboundFile.toBytes())));
            validate(calculatedChecksum, inboundExternalObjectDirectory, unstructuredExternalObjectDirectoryEntity);

            if(unstructuredExternalObjectDirectoryEntity.getStatus().equals(statusAwaiting)) {
                // upload file
                UUID uuid = dataManagementService.saveBlobData(getUnstructuredContainerName(), inboundFile);
                unstructuredExternalObjectDirectoryEntity.setChecksum(inboundExternalObjectDirectory.getChecksum());
                unstructuredExternalObjectDirectoryEntity.setExternalLocation(uuid);
                unstructuredExternalObjectDirectoryEntity.setStatus(statusStored);
            }

            externalObjectDirectoryRepository.saveAndFlush(unstructuredExternalObjectDirectoryEntity);

        }
    }

    private ExternalObjectDirectoryEntity getNewOrExistingExternalObjectDirectory(ExternalObjectDirectoryEntity inboundExternalObjectDirectory) {
        ExternalObjectDirectoryEntity unstructuredExternalObjectDirectoryEntity = externalObjectDirectoryRepository.findByMediaTranscriptionAndAnnotation(externalLocationTypeUnstructured, inboundExternalObjectDirectory.getMedia(), inboundExternalObjectDirectory.getTranscriptionDocumentEntity(), inboundExternalObjectDirectory.getAnnotationDocumentEntity());
        if ( unstructuredExternalObjectDirectoryEntity == null) {
            unstructuredExternalObjectDirectoryEntity = createUnstructuredExternalObjectDirectoryEntity(inboundExternalObjectDirectory);
        }
        return unstructuredExternalObjectDirectoryEntity;
    }

    private void validate(String calculatedChecksum, ExternalObjectDirectoryEntity externalObjectDirectory, ExternalObjectDirectoryEntity unstructuredExternalObjectDirectoryEntity) {

        MediaEntity mediaEntity = externalObjectDirectory.getMedia();
        if(mediaEntity != null) {
            performValidation(
                unstructuredExternalObjectDirectoryEntity,
                mediaEntity.getChecksum(),
                calculatedChecksum,
                audioConfigurationProperties.getAllowedExtensions(),
                FilenameUtils.getExtension(mediaEntity.getMediaFile()).toLowerCase(),
                audioConfigurationProperties.getMaxFileSize(),
                mediaEntity.getFileSize() );
        }

        TranscriptionDocumentEntity transcriptionDocumentEntity = externalObjectDirectory.getTranscriptionDocumentEntity();
        if(transcriptionDocumentEntity!=null) {
            performValidation(
                unstructuredExternalObjectDirectoryEntity,
                transcriptionDocumentEntity.getChecksum(),
                calculatedChecksum,
                transcriptionConfigurationProperties.getAllowedExtensions(),
                FilenameUtils.getExtension(transcriptionDocumentEntity.getFileName()).toLowerCase(),
                transcriptionConfigurationProperties.getMaxFileSize(),
                transcriptionDocumentEntity.getFileSize() );
        }

        AnnotationDocumentEntity annotationDocumentEntity = externalObjectDirectory.getAnnotationDocumentEntity();
        if(annotationDocumentEntity!=null) {
            performValidation(
                unstructuredExternalObjectDirectoryEntity,
                annotationDocumentEntity.getChecksum(),
                calculatedChecksum,
                transcriptionConfigurationProperties.getAllowedExtensions(),
                FilenameUtils.getExtension(annotationDocumentEntity.getFileName()).toLowerCase(),
                transcriptionConfigurationProperties.getMaxFileSize(),
                annotationDocumentEntity.getFileSize() );
        }

    }

    private void performValidation(ExternalObjectDirectoryEntity unstructuredExternalObjectDirectoryEntity, String incomingChecksum, String calculatedChecksum, List<String> allowedExtensions, String extension, Integer maxFileSize, Integer fileSize) {
        if (calculatedChecksum.compareTo(incomingChecksum) != 0) {
            unstructuredExternalObjectDirectoryEntity.setStatus(statusFailedChecksum);
        }
        if (!allowedExtensions.contains(extension)) {
            unstructuredExternalObjectDirectoryEntity.setStatus(statusFailedFileType);
        }
        if (fileSize > maxFileSize) {
            unstructuredExternalObjectDirectoryEntity.setStatus(statusFailedFileSize);
        }
    }

    @Transactional
    private ExternalObjectDirectoryEntity createUnstructuredExternalObjectDirectoryEntity(ExternalObjectDirectoryEntity externalObjectDirectory) {
        TranscriptionDocumentEntity transcriptionDocumentEntity = externalObjectDirectory.getTranscriptionDocumentEntity();
        MediaEntity mediaEntity = externalObjectDirectory.getMedia();
        AnnotationDocumentEntity annotationDocumentEntity = externalObjectDirectory.getAnnotationDocumentEntity();
        OffsetDateTime now = OffsetDateTime.now();
        var systemUser = userAccountRepository.getReferenceById(SystemUsersEnum.DEFAULT.getId());

        ExternalObjectDirectoryEntity  unstructuredExternalObjectDirectoryEntity = new ExternalObjectDirectoryEntity();
        unstructuredExternalObjectDirectoryEntity.setExternalLocationType(externalLocationTypeRepository.getReferenceById(UNSTRUCTURED.getId()));
        unstructuredExternalObjectDirectoryEntity.setStatus(statusAwaiting);
        unstructuredExternalObjectDirectoryEntity.setExternalLocation(externalObjectDirectory.getExternalLocation());
        if(mediaEntity!=null) {
            unstructuredExternalObjectDirectoryEntity.setMedia(mediaEntity);
        }
        if(transcriptionDocumentEntity != null) {
            unstructuredExternalObjectDirectoryEntity.setTranscriptionDocumentEntity(transcriptionDocumentEntity);
        }
        if(annotationDocumentEntity!=null) {
            unstructuredExternalObjectDirectoryEntity.setAnnotationDocumentEntity(annotationDocumentEntity);
        }
        unstructuredExternalObjectDirectoryEntity.setCreatedDateTime(now);
        unstructuredExternalObjectDirectoryEntity.setLastModifiedDateTime(now);
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

}
