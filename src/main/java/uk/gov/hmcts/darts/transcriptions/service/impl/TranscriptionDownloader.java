package uk.gov.hmcts.darts.transcriptions.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audit.api.AuditApi;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.datamanagement.api.DataManagementFacade;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadResponseMetaData;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadableExternalObjectDirectories;
import uk.gov.hmcts.darts.common.datamanagement.enums.DatastoreContainerType;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.TranscriptionRepository;
import uk.gov.hmcts.darts.transcriptions.model.DownloadTranscriptResponse;

import java.io.IOException;

import static java.util.Comparator.comparing;
import static uk.gov.hmcts.darts.audit.api.AuditActivity.DOWNLOAD_TRANSCRIPTION;
import static uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError.FAILED_TO_DOWNLOAD_TRANSCRIPT;

@RequiredArgsConstructor
@Service
@Slf4j
@SuppressWarnings({"PMD.ExcessiveImports"})
public class TranscriptionDownloader {

    private final TranscriptionRepository transcriptionRepository;
    private final DataManagementFacade dataManagementFacade;
    private final AuditApi auditApi;
    private final UserIdentity userIdentity;


    public DownloadTranscriptResponse downloadTranscript(Integer transcriptionId) {
        var userAccountEntity = getUserAccount();
        var transcriptionEntity = transcriptionRepository.findById(transcriptionId).orElseThrow(() -> new DartsApiException(FAILED_TO_DOWNLOAD_TRANSCRIPT));

        var latestTranscriptionDocument = transcriptionEntity.getTranscriptionDocumentEntities()
            .stream()
            .max(comparing(TranscriptionDocumentEntity::getUploadedDateTime))
            .orElseThrow(() -> new DartsApiException(FAILED_TO_DOWNLOAD_TRANSCRIPT));

        auditApi.recordAudit(DOWNLOAD_TRANSCRIPTION, userAccountEntity, transcriptionEntity.getCourtCase());

        return DownloadTranscriptResponse.builder()
            .resource(getResourceStreamFor(latestTranscriptionDocument))
            .contentType(latestTranscriptionDocument.getFileType())
            .fileName(latestTranscriptionDocument.getFileName())
            .transcriptionDocumentId(latestTranscriptionDocument.getId()).build();
    }

    private UserAccountEntity getUserAccount() {
        return userIdentity.getUserAccount();
    }

    private InputStreamResource getResourceStreamFor(TranscriptionDocumentEntity latestTranscriptionDocument) {
        DownloadableExternalObjectDirectories downloadableExternalObjectDirectories;
        try {
            downloadableExternalObjectDirectories = DownloadableExternalObjectDirectories.getFileBasedDownload(
                latestTranscriptionDocument.getExternalObjectDirectoryEntities());
        } catch (IOException e) {
            log.error("Failed to get DownloadableExternalObjectDirectories using latestTranscriptionDocument ID {}",
                      latestTranscriptionDocument.getId(),
                      e);
            throw new DartsApiException(FAILED_TO_DOWNLOAD_TRANSCRIPT);
        }
        dataManagementFacade.getDataFromUnstructuredArmAndDetsBlobs(downloadableExternalObjectDirectories);

        DownloadResponseMetaData downloadResponseMetaData = downloadableExternalObjectDirectories.getResponse();
        DatastoreContainerType containerTypeUsedToDownload = downloadResponseMetaData.getContainerTypeUsedToDownload();
        if (!downloadResponseMetaData.isSuccessfulDownload()) {
            throw new DartsApiException(FAILED_TO_DOWNLOAD_TRANSCRIPT);
        }

        try {
            return new InputStreamResource(downloadResponseMetaData.getInputStream());
        } catch (IOException e) {
            log.error("Failed to download transcript file using latestTranscriptionDocument ID {}, containerTypeUsedToDownload = {}",
                      latestTranscriptionDocument.getId(),
                      containerTypeUsedToDownload,
                      e);
            throw new DartsApiException(FAILED_TO_DOWNLOAD_TRANSCRIPT);
        }
    }

}
