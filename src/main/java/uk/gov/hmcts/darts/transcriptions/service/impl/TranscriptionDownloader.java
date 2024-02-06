package uk.gov.hmcts.darts.transcriptions.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audit.api.AuditApi;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.TranscriptionRepository;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;
import uk.gov.hmcts.darts.transcriptions.model.DownloadTranscriptResponse;

import java.util.UUID;

import static java.util.Comparator.comparing;
import static java.util.Objects.nonNull;
import static uk.gov.hmcts.darts.audit.api.AuditActivity.DOWNLOAD_TRANSCRIPTION;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.INBOUND;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.UNSTRUCTURED;
import static uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError.FAILED_TO_DOWNLOAD_TRANSCRIPT;

@RequiredArgsConstructor
@Service
@Slf4j
@SuppressWarnings({"PMD.ExcessiveImports"})
public class TranscriptionDownloader {

    private final TranscriptionRepository transcriptionRepository;
    private final DataManagementApi dataManagementApi;
    private final AuditApi auditApi;
    private final UserIdentity userIdentity;


    public DownloadTranscriptResponse downloadTranscript(Integer transcriptionId) {
        var userAccountEntity = getUserAccount();
        var transcriptionEntity = transcriptionRepository.findById(transcriptionId).orElseThrow(() -> new DartsApiException(FAILED_TO_DOWNLOAD_TRANSCRIPT));

        var latestTranscriptionDocument = transcriptionEntity.getTranscriptionDocumentEntities()
              .stream()
              .max(comparing(TranscriptionDocumentEntity::getUploadedDateTime))
              .orElseThrow(() -> new DartsApiException(FAILED_TO_DOWNLOAD_TRANSCRIPT));

        var latestExternalObjectDirectory = latestTranscriptionDocument.getExternalObjectDirectoryEntities()
              .stream()
              .filter(dir -> nonNull(dir.getExternalLocation()))
              .max(comparing(ExternalObjectDirectoryEntity::getCreatedDateTime))
              .orElseThrow(() -> new DartsApiException(FAILED_TO_DOWNLOAD_TRANSCRIPT));

        auditApi.recordAudit(DOWNLOAD_TRANSCRIPTION, userAccountEntity, transcriptionEntity.getCourtCase());

        return DownloadTranscriptResponse.builder()
              .resource(getResourceStreamFor(latestExternalObjectDirectory))
              .contentType(latestTranscriptionDocument.getFileType())
              .fileName(latestTranscriptionDocument.getFileName())
              .externalLocation(latestExternalObjectDirectory.getExternalLocation())
              .transcriptionDocumentId(latestTranscriptionDocument.getId()).build();
    }

    private UserAccountEntity getUserAccount() {
        return userIdentity.getUserAccount();
    }

    private InputStreamResource getResourceStreamFor(ExternalObjectDirectoryEntity externalObjectDirectoryEntity) {
        final UUID externalLocation = externalObjectDirectoryEntity.getExternalLocation();
        InputStreamResource stream;
        if (externalObjectDirectoryEntity.isForLocationType(UNSTRUCTURED)) {
            stream = new InputStreamResource(dataManagementApi.getBlobDataFromUnstructuredContainer(externalLocation).toStream());
        } else if (externalObjectDirectoryEntity.isForLocationType(INBOUND)) {
            stream = new InputStreamResource(dataManagementApi.getBlobDataFromInboundContainer(externalLocation).toStream());
        } else {
            throw new DartsApiException(FAILED_TO_DOWNLOAD_TRANSCRIPT);
        }
        return stream;
    }

}
