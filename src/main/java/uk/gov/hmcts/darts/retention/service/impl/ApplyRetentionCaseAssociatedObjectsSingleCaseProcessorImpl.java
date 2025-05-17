package uk.gov.hmcts.darts.retention.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
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
import java.util.ArrayList;
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
        List<MediaEntity> mediaEntities = new ArrayList<>();
        // get all current media linked to the case
        mediaEntities.addAll(mediaRepository.findAllByCaseId(courtCase.getId()));
        // get all media linked to the case via media linked case
        mediaEntities.addAll(mediaRepository.findAllLinkedByMediaLinkedCaseByCaseId(courtCase.getId()));
        // remove duplicates
        var allMedia = io.vavr.collection.List.ofAll(mediaEntities).distinctBy(MediaEntity::getId).toJavaList();
        for (var media : allMedia) {
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

        var annotationDocuments = annotationDocumentRepository.findAllByCaseId(courtCase.getId());

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
        var caseDocuments = caseDocumentRepository.findByCourtCase(courtCase);
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
            var armEods = eodRepository.findByMediaAndExternalLocationType(media, EodHelper.armLocation());
            updateArmEodRetention(
                armEods,
                format("Expecting one arm EOD for media '%s' but found zero or more than one", media.getId())
            );
        } else {
            throw new DartsException(format("No retentions found on cases for media '%s'", media.getId()));
        }
    }

    private void setLongestRetentionDateForAnnotationDocument(AnnotationDocumentEntity annotationDoc, List<CourtCaseEntity> cases) {
        var longestRetentionDate = findLongestRetentionDate(cases);
        if (longestRetentionDate != null) {
            annotationDoc.setRetainUntilTs(longestRetentionDate);
            var armEods = eodRepository.findByAnnotationDocumentEntityAndExternalLocationType(annotationDoc, EodHelper.armLocation());
            updateArmEodRetention(
                armEods,
                format("Expecting one arm EOD for annotationDocument '%s' but found zero or more than one", annotationDoc.getId())
            );
        } else {
            throw new DartsException(format("No retentions found on cases for annotationDocument '%s'", annotationDoc.getId()));
        }
    }

    private void setLongestRetentionDateForTranscriptionDocument(TranscriptionDocumentEntity transcriptionDoc, List<CourtCaseEntity> cases) {
        var longestRetentionDate = findLongestRetentionDate(cases);
        if (longestRetentionDate != null) {
            transcriptionDoc.setRetainUntilTs(longestRetentionDate);
            var armEods = eodRepository.findByTranscriptionDocumentEntityAndExternalLocationType(transcriptionDoc, EodHelper.armLocation());
            updateArmEodRetention(
                armEods,
                format("Expecting one arm EOD for transcriptionDocument '%s' but found zero or more than one", transcriptionDoc.getId())
            );
        } else {
            throw new DartsException(format("No retentions found on cases for transcriptionDocument '%s'", transcriptionDoc.getId()));
        }
    }

    private void setLongestRetentionDateForCaseDocument(CourtCaseEntity courtCase, CaseDocumentEntity caseDocument) {
        var longestRetentionDate = findLongestRetentionDate(List.of(courtCase));
        if (longestRetentionDate != null) {
            caseDocument.setRetainUntilTs(longestRetentionDate);
            var armEods = eodRepository.findByCaseDocumentAndExternalLocationType(caseDocument, EodHelper.armLocation());
            updateArmEodRetention(
                armEods,
                format("Expecting one arm EOD for caseDocument '%s' but found zero or more than one", caseDocument.getId())
            );
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
                if (longestRetentionDate == null) {
                    longestRetentionDate = retention;
                } else if (retention.isAfter(longestRetentionDate)) {
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

