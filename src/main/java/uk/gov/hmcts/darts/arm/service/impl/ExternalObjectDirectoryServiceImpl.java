package uk.gov.hmcts.darts.arm.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.service.ExternalObjectDirectoryService;
import uk.gov.hmcts.darts.common.entity.CaseDocumentEntity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.AnnotationDocumentRepository;
import uk.gov.hmcts.darts.common.repository.CaseDocumentRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionDocumentRepository;
import uk.gov.hmcts.darts.common.util.EodHelper;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.nonNull;

@Service
@RequiredArgsConstructor
public class ExternalObjectDirectoryServiceImpl implements ExternalObjectDirectoryService {

    private static final int INITIAL_VERIFICATION_ATTEMPTS = 1;

    private final ExternalObjectDirectoryRepository eodRepository;
    private final ArmDataManagementConfiguration armConfig;
    private final MediaRepository mediaRepository;
    private final AnnotationDocumentRepository annotationDocumentRepository;
    private final CaseDocumentRepository caseDocumentRepository;
    private final TranscriptionDocumentRepository transcriptionDocumentRepository;

    @Override
    public boolean hasAllMediaBeenCopiedFromInboundStorage(List<MediaEntity> mediaEntities) {
        return mediaEntities.stream().allMatch(this::hasMediaBeenCopiedFromInboundStorage);
    }

    private boolean hasMediaBeenCopiedFromInboundStorage(MediaEntity mediaEntity) {
        return !eodRepository.hasMediaNotBeenCopiedFromInboundStorage(
            mediaEntity,
            EodHelper.storedStatus(),
            EodHelper.inboundLocation(),
            EodHelper.awaitingVerificationStatus(),
            List.of(EodHelper.unstructuredLocation(), EodHelper.armLocation()));
    }

    @Override
    @Transactional
    public Optional<ExternalObjectDirectoryEntity> eagerLoadExternalObjectDirectory(Integer externalObjectDirectoryId) {
        return eodRepository.findById(externalObjectDirectoryId);
    }

    @Override
    @Transactional
    public void updateStatus(ObjectRecordStatusEntity newStatus, UserAccountEntity userAccount, List<Integer> idsToUpdate, OffsetDateTime timestamp) {
        eodRepository.updateStatus(newStatus, userAccount, idsToUpdate, timestamp);
    }

    @Override
    @Transactional
    public ExternalObjectDirectoryEntity createAndSaveCaseDocumentEod(UUID externalLocation,
                                                                      UserAccountEntity userAccountEntity,
                                                                      CaseDocumentEntity caseDocumentEntity,
                                                                      ExternalLocationTypeEntity externalLocationType) {
        var externalObjectDirectoryEntity = new ExternalObjectDirectoryEntity();
        externalObjectDirectoryEntity.setCaseDocument(caseDocumentEntity);
        externalObjectDirectoryEntity.setStatus(EodHelper.storedStatus());
        externalObjectDirectoryEntity.setExternalLocationType(externalLocationType);
        externalObjectDirectoryEntity.setExternalLocation(externalLocation);
        externalObjectDirectoryEntity.setChecksum(caseDocumentEntity.getChecksum());
        externalObjectDirectoryEntity.setVerificationAttempts(INITIAL_VERIFICATION_ATTEMPTS);
        externalObjectDirectoryEntity.setCreatedBy(userAccountEntity);
        externalObjectDirectoryEntity.setLastModifiedBy(userAccountEntity);
        return eodRepository.save(externalObjectDirectoryEntity);
    }

    @Override
    public Long getFileSize(ExternalObjectDirectoryEntity detsExternalObjectDirectory) {
        Long fileSize = null;
        if (nonNull(detsExternalObjectDirectory.getMedia())) {
            fileSize = mediaRepository.findById(detsExternalObjectDirectory.getMedia().getId()).map(
                media -> media.getFileSize()).orElse(null);
        } else if (nonNull(detsExternalObjectDirectory.getAnnotationDocumentEntity())) {
            fileSize = annotationDocumentRepository.findById(detsExternalObjectDirectory.getAnnotationDocumentEntity().getId()).map(
                annotationDocument -> Long.valueOf(annotationDocument.getFileSize())).orElse(null);
        } else if (nonNull(detsExternalObjectDirectory.getCaseDocument())) {
            fileSize = caseDocumentRepository.findById(detsExternalObjectDirectory.getCaseDocument().getId()).map(
                caseDocument -> Long.valueOf(caseDocument.getFileSize())).orElse(null);
        } else if (nonNull(detsExternalObjectDirectory.getTranscriptionDocumentEntity())) {
            fileSize = transcriptionDocumentRepository.findById(detsExternalObjectDirectory.getTranscriptionDocumentEntity().getId()).map(
                transcriptionDocument -> Long.valueOf(transcriptionDocument.getFileSize())).orElse(null);
        }
        return fileSize;
    }
}
