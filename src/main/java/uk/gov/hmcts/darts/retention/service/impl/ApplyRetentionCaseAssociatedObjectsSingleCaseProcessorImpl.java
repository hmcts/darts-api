package uk.gov.hmcts.darts.retention.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.cases.service.CaseService;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.CaseDocumentEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.MediaLinkedCaseEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.exception.DartsException;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.AnnotationDocumentRepository;
import uk.gov.hmcts.darts.common.repository.CaseDocumentRepository;
import uk.gov.hmcts.darts.common.repository.CaseRetentionRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.MediaLinkedCaseRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionDocumentRepository;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.retention.mapper.CaseRetentionConfidenceReasonMapper;
import uk.gov.hmcts.darts.retention.model.CaseRetentionConfidenceReason;
import uk.gov.hmcts.darts.retention.service.ApplyRetentionCaseAssociatedObjectsSingleCaseProcessor;
import uk.gov.hmcts.darts.transcriptions.service.TranscriptionService;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static uk.gov.hmcts.darts.retention.enums.RetentionConfidenceScoreEnum.CASE_NOT_PERFECTLY_CLOSED;
import static uk.gov.hmcts.darts.retention.enums.RetentionConfidenceScoreEnum.CASE_PERFECTLY_CLOSED;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings({
    "PMD.CouplingBetweenObjects",//TODO - refactor to reduce coupling when this class is next edited
    "PMD.TooManyMethods",//TODO - refactor to reduce methods when this class is next edited
    "PMD.GodClass"//TODO - refactor to reduce class size when this class is next edited
})
public class ApplyRetentionCaseAssociatedObjectsSingleCaseProcessorImpl implements ApplyRetentionCaseAssociatedObjectsSingleCaseProcessor {

    private final CaseRetentionRepository caseRetentionRepository;
    private final CaseService caseService;
    private final ExternalObjectDirectoryRepository eodRepository;
    private final MediaRepository mediaRepository;
    private final AnnotationDocumentRepository annotationDocumentRepository;
    private final TranscriptionService transcriptionService;
    private final CaseDocumentRepository caseDocumentRepository;
    private final TranscriptionDocumentRepository transcriptionDocumentRepository;

    private final CaseRetentionConfidenceReasonMapper caseRetentionConfidenceReasonMapper;

    private final CurrentTimeHelper currentTimeHelper;
    private final ObjectMapper objectMapper;

    private final MediaLinkedCaseRepository mediaLinkedCaseRepository;

    @Transactional
    @Override
    public void processApplyRetentionToCaseAssociatedObjects(Integer caseId) {

        log.info("applying retention to associated objects for case id '{}'", caseId);

        var courtCase = caseService.getCourtCaseById(caseId);

        applyRetentionToMedias(courtCase);
        applyRetentionToAnnotations(courtCase);
        applyRetentionToTranscriptions(courtCase);
        applyRetentionToCaseDocuments(courtCase);
    }

    private void applyRetentionToMedias(CourtCaseEntity courtCase) {
        Collection<MediaEntity> mediaEntities = courtCase.getAllAssociatedMedias();
        for (var media : mediaEntities) {
            List<CourtCaseEntity> cases = media.associatedCourtCases();
            mediaLinkedCaseRepository.findByMedia(media).stream()
                .map(MediaLinkedCaseEntity::getCourtCase)
                .filter(Objects::nonNull)
                .forEach(cases::add);

            var allCases = io.vavr.collection.List.ofAll(cases).distinctBy(CourtCaseEntity::getId).toJavaList();
            if (allClosed(allCases)) {
                setLongestRetentionDateForMedia(media, allCases);
                setRetentionConfidenceScoreAndReasonForMedia(media, allCases);
            }
        }
    }

    private void applyRetentionToAnnotations(CourtCaseEntity courtCase) {

        var annotationDocuments = courtCase.getAllAssociatedAnnotationDocuments();

        for (var annotationDoc : annotationDocuments) {
            var cases = annotationDoc.associatedCourtCases();
            if (allClosed(cases)) {
                setLongestRetentionDateForAnnotationDocument(annotationDoc, cases);
                setRetentionConfidenceScoreAndReasonForAnnotationDocument(annotationDoc, cases);
            }
        }
    }

    private void applyRetentionToTranscriptions(CourtCaseEntity courtCase) {

        var transcriptionDocuments = transcriptionService.getAllCaseTranscriptionDocuments(courtCase.getId());

        for (var transcriptionDoc : transcriptionDocuments) {
            var cases = transcriptionDoc.getTranscription().getAssociatedCourtCases();
            if (allClosed(cases)) {
                setLongestRetentionDateForTranscriptionDocument(transcriptionDoc, cases);
                setRetentionConfidenceScoreAndReasonForTranscriptionDocument(transcriptionDoc, cases);
            }
        }
    }

    private void applyRetentionToCaseDocuments(CourtCaseEntity courtCase) {
        var caseDocuments = courtCase.getCaseDocumentEntities();
        for (var caseDocument : caseDocuments) {
            if (allClosed(List.of(courtCase))) {
                setLongestRetentionDateForCaseDocument(courtCase, caseDocument);
                setRetentionConfidenceScoreAndReasonForCaseDocument(courtCase, caseDocument);
            }
        }
    }

    private void setLongestRetentionDateForMedia(MediaEntity media, List<CourtCaseEntity> cases) {
        var longestRetentionDate = findLongestRetentionDate(cases);
        if (longestRetentionDate != null) {
            media.setRetainUntilTs(longestRetentionDate);
            var armOrDetsEodList = eodRepository.findByMediaIdAndExternalLocationTypes(
                media.getId(), List.of(EodHelper.armLocation(), EodHelper.detsLocation()));

            String errorMessage = format("Unexpected number of EODs {} for media '%s'", armOrDetsEodList.size(), media.getId());
            updateRetentionForArm(armOrDetsEodList, errorMessage);
        } else {
            throw new DartsException(format("No retentions found on cases for media '%s'", media.getId()));
        }
    }

    private void setLongestRetentionDateForAnnotationDocument(AnnotationDocumentEntity annotationDoc, List<CourtCaseEntity> cases) {
        var longestRetentionDate = findLongestRetentionDate(cases);
        if (longestRetentionDate != null) {
            annotationDoc.setRetainUntilTs(longestRetentionDate);
            var armOrDetsEodList = eodRepository.findByAnnotationDocumentIdAndExternalLocationTypes(
                annotationDoc.getId(), List.of(EodHelper.armLocation(), EodHelper.detsLocation()));
            String errorMessage = format("Unexpected number of EODs {} for annotation document '%s'", armOrDetsEodList.size(), annotationDoc.getId());
            updateRetentionForArm(armOrDetsEodList, errorMessage);
        } else {
            throw new DartsException(format("No retentions found on cases for annotationDocument '%s'", annotationDoc.getId()));
        }
    }

    private void setLongestRetentionDateForTranscriptionDocument(TranscriptionDocumentEntity transcriptionDoc, List<CourtCaseEntity> cases) {
        var longestRetentionDate = findLongestRetentionDate(cases);
        if (longestRetentionDate != null) {
            transcriptionDoc.setRetainUntilTs(longestRetentionDate);
            var armOrDetsEodList = eodRepository.findByTranscriptionDocumentIdAndExternalLocationTypes(
                transcriptionDoc.getId(), List.of(EodHelper.armLocation(), EodHelper.detsLocation()));
            String errorMessage = format("Unexpected number of EODs {} for transcription document '%s'", armOrDetsEodList.size(), transcriptionDoc.getId());
            updateRetentionForArm(armOrDetsEodList, errorMessage);
        } else {
            throw new DartsException(format("No retentions found on cases for transcriptionDocument '%s'", transcriptionDoc.getId()));
        }
    }

    private void updateRetentionForArm(List<ExternalObjectDirectoryEntity> armOrDetsEodList, String errorMessage) {
        // There should be either 1 or 2 EODs (ARM and/or DETS)
        if (armOrDetsEodList.size() < 1 || armOrDetsEodList.size() > 2) {
            throw new DartsException(errorMessage);
        }
        // Get only ARM EOD. If EOD is ARM, update EOD retention, if EOD is DETS do nothing DMP-5264
        List<ExternalObjectDirectoryEntity> armEodList = armOrDetsEodList.stream().filter(
            eod ->
                EodHelper.armLocation().getId().equals(
                    eod.getExternalLocationType().getId())
        ).toList();

        if (CollectionUtils.isNotEmpty(armEodList)) {
            updateArmEodRetention(armEodList, errorMessage);
        }
    }

    private void setLongestRetentionDateForCaseDocument(CourtCaseEntity courtCase, CaseDocumentEntity caseDocument) {
        var longestRetentionDate = findLongestRetentionDate(List.of(courtCase));
        if (longestRetentionDate != null) {
            caseDocument.setRetainUntilTs(longestRetentionDate);
            var armOrDetsEodList = eodRepository.findByCaseDocumentIdAndExternalLocationTypes(
                caseDocument.getId(), List.of(EodHelper.armLocation(), EodHelper.detsLocation()));
            String errorMessage = format("Unexpected number of EODs {} for case document '%s'", armOrDetsEodList.size(), caseDocument.getId());
            updateRetentionForArm(armOrDetsEodList, errorMessage);
        } else {
            throw new DartsException(format("No retentions found on courtCase for caseDocument '%s'", caseDocument.getId()));
        }
    }

    private void setRetentionConfidenceScoreAndReasonForMedia(MediaEntity media, List<CourtCaseEntity> cases) {
        if (areAllCasesPerfectlyClosed(cases)) {
            media.setRetConfScore(CASE_PERFECTLY_CLOSED);
        } else {
            var notPerfectlyClosedCases = filterCasesNotPerfectlyClosed(cases);
            media.setRetConfScore(CASE_NOT_PERFECTLY_CLOSED);
            try {
                String retentionConfidenceReasonJson = generateRetentionConfidenceReasonJson(
                    notPerfectlyClosedCases,
                    format("Unable to generate retention confidence reason for media %s", media.getId()));
                media.setRetConfReason(StringEscapeUtils.escapeJson(retentionConfidenceReasonJson));
            } catch (Exception e) {
                log.error("Unable to generate case retention confidence reason for media {}", media.getId());
            }
        }
        mediaRepository.saveAndFlush(media);
    }

    private void setRetentionConfidenceScoreAndReasonForAnnotationDocument(AnnotationDocumentEntity annotationDoc, List<CourtCaseEntity> cases) {
        if (areAllCasesPerfectlyClosed(cases)) {
            annotationDoc.setRetConfScore(CASE_PERFECTLY_CLOSED);
        } else {
            var notPerfectlyClosedCases = filterCasesNotPerfectlyClosed(cases);
            annotationDoc.setRetConfScore(CASE_NOT_PERFECTLY_CLOSED);
            String retentionConfidenceReasonJson = generateRetentionConfidenceReasonJson(
                notPerfectlyClosedCases,
                format("Unable to generate retention confidence reason for annotation document %s", annotationDoc.getId()));
            annotationDoc.setRetConfReason(StringEscapeUtils.escapeJson(retentionConfidenceReasonJson));
        }
        annotationDocumentRepository.saveAndFlush(annotationDoc);
    }

    private void setRetentionConfidenceScoreAndReasonForTranscriptionDocument(TranscriptionDocumentEntity transcriptionDoc, List<CourtCaseEntity> cases) {

        if (areAllCasesPerfectlyClosed(cases)) {
            transcriptionDoc.setRetConfScore(CASE_PERFECTLY_CLOSED);
        } else {
            var notPerfectlyClosedCases = filterCasesNotPerfectlyClosed(cases);
            transcriptionDoc.setRetConfScore(CASE_NOT_PERFECTLY_CLOSED);
            String retentionConfidenceReasonJson = generateRetentionConfidenceReasonJson(
                notPerfectlyClosedCases,
                format("Unable to generate retention confidence reason for transcription document %s", transcriptionDoc.getId()));
            transcriptionDoc.setRetConfReason(StringEscapeUtils.escapeJson(retentionConfidenceReasonJson));
        }
        transcriptionDocumentRepository.saveAndFlush(transcriptionDoc);
    }

    private void setRetentionConfidenceScoreAndReasonForCaseDocument(CourtCaseEntity courtCase, CaseDocumentEntity caseDocument) {
        var courtCases = List.of(courtCase);
        if (areAllCasesPerfectlyClosed(courtCases)) {
            caseDocument.setRetConfScore(CASE_PERFECTLY_CLOSED);
        } else {
            caseDocument.setRetConfScore(CASE_NOT_PERFECTLY_CLOSED);
            String retentionConfidenceReasonJson = generateRetentionConfidenceReasonJson(
                courtCases,
                format("Unable to generate retention confidence reason for annotation document %s", caseDocument.getId()));
            caseDocument.setRetConfReason(StringEscapeUtils.escapeJson(retentionConfidenceReasonJson));
        }
        caseDocumentRepository.save(caseDocument);
    }

    private String generateRetentionConfidenceReasonJson(List<CourtCaseEntity> notPerfectlyClosedCases, String errorMessage) {
        String retentionConfidenceReasonJson = null;
        CaseRetentionConfidenceReason caseRetentionConfidenceReason = null;

        try {
            caseRetentionConfidenceReason = caseRetentionConfidenceReasonMapper.mapToCaseRetentionConfidenceReason(
                currentTimeHelper.currentOffsetDateTime(), notPerfectlyClosedCases);

            if (nonNull(caseRetentionConfidenceReason)) {
                retentionConfidenceReasonJson = objectMapper.writeValueAsString(caseRetentionConfidenceReason);
            }
        } catch (JsonProcessingException e) {
            throw new DartsException(errorMessage + " with reason " + caseRetentionConfidenceReason, e);
        }
        return retentionConfidenceReasonJson;
    }

    private boolean allClosed(List<CourtCaseEntity> cases) {
        return cases.stream().allMatch(CourtCaseEntity::getClosed);
    }

    private boolean areAllCasesPerfectlyClosed(List<CourtCaseEntity> cases) {
        return cases.stream().allMatch(courtCase -> CASE_PERFECTLY_CLOSED.equals(courtCase.getRetConfScore()));
    }

    private List<CourtCaseEntity> filterCasesNotPerfectlyClosed(List<CourtCaseEntity> cases) {
        return cases.stream().filter(courtCase -> !CASE_PERFECTLY_CLOSED.equals(courtCase.getRetConfScore())).toList();
    }

    private OffsetDateTime findLongestRetentionDate(List<CourtCaseEntity> cases) {
        OffsetDateTime longestRetentionDate = null;
        for (var courtCase : cases) {
            var mostRecentRetentionRecordOpt = caseRetentionRepository.findTopByCourtCaseOrderByRetainUntilAppliedOnDesc(courtCase);
            if (mostRecentRetentionRecordOpt.isPresent()) {
                var retention = mostRecentRetentionRecordOpt.get().getRetainUntil();
                if (longestRetentionDate == null || retention.isAfter(longestRetentionDate)) {
                    longestRetentionDate = retention;
                }
            }
        }
        return longestRetentionDate;
    }

    private void updateArmEodRetention(List<ExternalObjectDirectoryEntity> armEods, String errorMessage) {
        if (armEods.size() == 1) {
            ExternalObjectDirectoryEntity eod = armEods.getFirst();
            eod.setUpdateRetention(true);
            eodRepository.save(eod);
        } else {
            throw new DartsException(errorMessage);
        }
    }
}

