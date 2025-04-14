package uk.gov.hmcts.darts.transcriptions.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audit.api.AuditApi;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.datamanagement.api.DataManagementFacade;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadResponseMetaData;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.TranscriptionDocumentRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionRepository;
import uk.gov.hmcts.darts.datamanagement.exception.FileNotDownloadedException;
import uk.gov.hmcts.darts.transcriptions.model.DownloadTranscriptResponse;

import java.io.IOException;
import java.util.Set;

import static java.util.Comparator.comparing;
import static uk.gov.hmcts.darts.audit.api.AuditActivity.DOWNLOAD_TRANSCRIPTION;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;
import static uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError.TRANSCRIPTION_NOT_FOUND;

@RequiredArgsConstructor
@Service
@Slf4j
public class TranscriptionDownloader {

    private final TranscriptionRepository transcriptionRepository;
    private final TranscriptionDocumentRepository transcriptionDocumentRepository;
    private final DataManagementFacade dataManagementFacade;
    private final AuditApi auditApi;
    private final UserIdentity userIdentity;

    public DownloadTranscriptResponse downloadTranscript(Integer transcriptionId) {
        var userAccountEntity = getUserAccount();
        var userIsSuperAdmin = this.userIdentity.userHasGlobalAccess(Set.of(SUPER_ADMIN));
        System.out.println("TMP: here 1");
        var transcriptionEntity = transcriptionRepository.findById(transcriptionId)
            // Only SUPER_ADMIN users are allowed to download hidden documents
            .filter(transcription -> userIsSuperAdmin
                || transcriptionDocumentRepository.findByTranscriptionIdAndHiddenTrueIncludeDeleted(transcription.getId()).isEmpty()
            )
            .orElseThrow(() -> new DartsApiException(TRANSCRIPTION_NOT_FOUND));
        System.out.println("TMP: here 2");
        // if the document is hidden and now deleted, this will successfully fail and not return the document
        var latestTranscriptionDocument = transcriptionEntity.getTranscriptionDocumentEntities()
            .stream()
            .max(comparing(TranscriptionDocumentEntity::getUploadedDateTime))
            .orElseThrow(() -> new DartsApiException(TRANSCRIPTION_NOT_FOUND));

        auditApi.record(DOWNLOAD_TRANSCRIPTION, userAccountEntity, transcriptionEntity.getCourtCase());

        return DownloadTranscriptResponse.builder()
            .resource(getResourceStreamFor(latestTranscriptionDocument))
            .contentType(latestTranscriptionDocument.getFileType())
            .fileName(latestTranscriptionDocument.getFileName())
            .transcriptionDocumentId(latestTranscriptionDocument.getId()).build();
    }

    private UserAccountEntity getUserAccount() {
        return userIdentity.getUserAccount();
    }

    @SuppressWarnings({"PMD.CloseResource"})
    private Resource getResourceStreamFor(TranscriptionDocumentEntity latestTranscriptionDocument) {
        try {
            DownloadResponseMetaData downloadResponseMetaData = dataManagementFacade.retrieveFileFromStorage(latestTranscriptionDocument);
            return downloadResponseMetaData.getResource();
        } catch (IOException | FileNotDownloadedException ex) {
            log.error("Failed to download transcript file using latestTranscriptionDocument ID {}",
                      latestTranscriptionDocument.getId(),
                      ex);
            throw new DartsApiException(TRANSCRIPTION_NOT_FOUND, ex);
        }
    }

}