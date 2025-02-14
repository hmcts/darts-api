package uk.gov.hmcts.darts.datamanagement.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.CaseDocumentEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.enums.SystemUsersEnum;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.datamanagement.config.DataManagementConfiguration;
import uk.gov.hmcts.darts.datamanagement.service.DataManagementService;
import uk.gov.hmcts.darts.datamanagement.service.InboundToUnstructuredProcessorSingleElement;

import java.util.UUID;

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
    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;

    @SuppressWarnings({"java:S4790", "PMD.AvoidFileStream"})
    @Override
    @Transactional
    public void processSingleElement(Integer inboundEodEntityId) {
        ExternalObjectDirectoryEntity inboundEodEntity = externalObjectDirectoryRepository.findById(inboundEodEntityId).orElseThrow();
        ExternalObjectDirectoryEntity unstructuredExternalObjectDirectoryEntity = getNewOrExistingInUnstructuredFailed(inboundEodEntity);

        unstructuredExternalObjectDirectoryEntity.setStatus(EodHelper.awaitingVerificationStatus());
        externalObjectDirectoryRepository.saveAndFlush(unstructuredExternalObjectDirectoryEntity);
        try {
            UUID inboundExternalLocation = inboundEodEntity.getExternalLocation();
            UUID unstructuredExternalLocation = UUID.randomUUID();
            dataManagementService.copyBlobData(
                getInboundContainerName(), getUnstructuredContainerName(), inboundExternalLocation.toString(), unstructuredExternalLocation.toString());
            unstructuredExternalObjectDirectoryEntity.setChecksum(inboundEodEntity.getChecksum());
            unstructuredExternalObjectDirectoryEntity.setExternalLocation(unstructuredExternalLocation);
            unstructuredExternalObjectDirectoryEntity.setStatus(EodHelper.storedStatus());
            log.debug("Saved unstructured stored EOD with Id: {}", unstructuredExternalObjectDirectoryEntity.getId());
            externalObjectDirectoryRepository.saveAndFlush(unstructuredExternalObjectDirectoryEntity);
            log.debug("Transfer complete for EOD ID: {}", inboundEodEntity.getId());
        } catch (Exception e) {
            log.error("Failed to move file from inbound store to unstructured store. EOD id: {}", inboundEodEntity.getId(), e);
            unstructuredExternalObjectDirectoryEntity.setStatus(EodHelper.failureStatus());
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
        unstructuredExternalObjectDirectoryEntity.setExternalLocationType(EodHelper.unstructuredLocation());
        unstructuredExternalObjectDirectoryEntity.setStatus(EodHelper.awaitingVerificationStatus());
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
}
