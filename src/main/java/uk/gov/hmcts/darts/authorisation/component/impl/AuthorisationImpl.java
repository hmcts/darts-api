package uk.gov.hmcts.darts.authorisation.component.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.authorisation.component.Authorisation;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.AnnotationRepository;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.MediaRequestRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionRepository;
import uk.gov.hmcts.darts.common.repository.TransformedMediaRepository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static uk.gov.hmcts.darts.annotation.errors.AnnotationApiError.ANNOTATION_NOT_FOUND;
import static uk.gov.hmcts.darts.audio.exception.AudioApiError.MEDIA_NOT_FOUND;
import static uk.gov.hmcts.darts.audio.exception.AudioRequestsApiError.MEDIA_REQUEST_NOT_FOUND;
import static uk.gov.hmcts.darts.audio.exception.AudioRequestsApiError.MEDIA_REQUEST_NOT_VALID_FOR_USER;
import static uk.gov.hmcts.darts.audio.exception.AudioRequestsApiError.TRANSFORMED_MEDIA_NOT_FOUND;
import static uk.gov.hmcts.darts.cases.exception.CaseApiError.CASE_NOT_FOUND;
import static uk.gov.hmcts.darts.hearings.exception.HearingApiError.HEARING_NOT_FOUND;
import static uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError.TRANSCRIPTION_NOT_FOUND;

@Component
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("PMD.TooManyMethods")//TODO - refactor to reduce methods when this class is next edited
public class AuthorisationImpl implements Authorisation {

    private final CaseRepository caseRepository;
    private final HearingRepository hearingRepository;
    private final MediaRequestRepository mediaRequestRepository;
    private final MediaRepository mediaRepository;
    private final TranscriptionRepository transcriptionRepository;
    private final TransformedMediaRepository transformedMediaRepository;
    private final AnnotationRepository annotationRepository;
    private final AuthorisationApi authorisationApi;
    private final UserIdentity userIdentity;

    @Override
    public void authoriseByCaseId(Integer caseId, Set<SecurityRoleEnum> securityRoles) {
        try {
            final List<CourthouseEntity> courthouses = List.of(caseRepository.getReferenceById(caseId)
                                                                   .getCourthouse());
            authorisationApi.checkCourthouseAuthorisation(courthouses, securityRoles);
        } catch (EntityNotFoundException ex) {
            log.error("Unable to find Case-Courthouse for checkAuthorisation", ex);
            throw new DartsApiException(CASE_NOT_FOUND, ex);
        }
    }

    @Override
    public void authoriseByHearingId(Integer hearingId, Set<SecurityRoleEnum> securityRoles) {
        try {
            final List<CourthouseEntity> courthouses = List.of(hearingRepository.getReferenceById(hearingId)
                                                                   .getCourtroom().getCourthouse());
            authorisationApi.checkCourthouseAuthorisation(courthouses, securityRoles);
        } catch (EntityNotFoundException ex) {
            log.error("Unable to find Hearing-Courtroom-Courthouse for checkAuthorisation", ex);
            throw new DartsApiException(HEARING_NOT_FOUND, ex);
        }
    }

    @Override
    public void authoriseByMediaRequestId(Integer mediaRequestId, Set<SecurityRoleEnum> securityRoles) {
        try {
            final List<CourthouseEntity> courthouses = List.of(mediaRequestRepository.getReferenceById(mediaRequestId)
                                                                   .getHearing().getCourtroom().getCourthouse());
            authorisationApi.checkCourthouseAuthorisation(courthouses, securityRoles);
        } catch (EntityNotFoundException ex) {
            log.error("Unable to find MediaRequest-Hearing-Courtroom-Courthouse for checkAuthorisation", ex);
            throw new DartsApiException(MEDIA_REQUEST_NOT_FOUND, ex);
        }
    }

    @Override
    public void authoriseByMediaId(Integer mediaId, Set<SecurityRoleEnum> securityRoles) {
        try {
            final Set<CourthouseEntity> courthouses = mediaRepository.getReferenceById(mediaId)
                .getHearingList()
                .stream()
                .map(hearingEntity -> hearingEntity.getCourtroom().getCourthouse())
                .collect(Collectors.toUnmodifiableSet());

            authorisationApi.checkCourthouseAuthorisation(
                courthouses
                    .stream()
                    .toList(),
                securityRoles
            );
        } catch (EntityNotFoundException ex) {
            log.error("Unable to find Media-Hearing-Courtroom-Courthouse for checkAuthorisation", ex);
            throw new DartsApiException(MEDIA_NOT_FOUND, ex);
        }
    }

    @Override
    @SuppressWarnings({"PMD.ExceptionAsFlowControl"})
    public void authoriseByTranscriptionId(Integer transcriptionId, Set<SecurityRoleEnum> securityRoles) {
        try {
            final List<CourthouseEntity> courthouses = getCourthousesFromTranscription(transcriptionId);
            if (CollectionUtils.isEmpty(courthouses)) {
                throw new EntityNotFoundException();
            }
            authorisationApi.checkCourthouseAuthorisation(courthouses, securityRoles);
        } catch (EntityNotFoundException ex) {
            log.error("Unable to find Transcription-Courtroom-Courthouse for checkAuthorisation. TranscriptionId={}", transcriptionId, ex);
            throw new DartsApiException(TRANSCRIPTION_NOT_FOUND, ex);
        }
    }

    private List<CourthouseEntity> getCourthousesFromTranscription(Integer transcriptionId) {
        Optional<TranscriptionEntity> transcriptionEntityOpt = transcriptionRepository.findById(transcriptionId);
        if (transcriptionEntityOpt.isEmpty()) {
            return Collections.emptyList();
        }

        TranscriptionEntity transcriptionEntity = transcriptionEntityOpt.get();

        var courtCases = transcriptionEntity.getAssociatedCourtCases();
        return courtCases.stream().map(CourtCaseEntity::getCourthouse).distinct().toList();
    }

    @Override
    public void authoriseByTransformedMediaId(Integer transformedMediaId, Set<SecurityRoleEnum> securityRoles) {
        try {
            final List<CourthouseEntity> courthouses = List.of(transformedMediaRepository.getReferenceById(transformedMediaId)
                                                                   .getMediaRequest().getHearing().getCourtroom().getCourthouse());
            authorisationApi.checkCourthouseAuthorisation(courthouses, securityRoles);
        } catch (EntityNotFoundException ex) {
            log.error("Unable to find TransformedMedia-MediaRequest-Hearing-Courtroom-Courthouse for checkAuthorisation", ex);
            throw new DartsApiException(TRANSFORMED_MEDIA_NOT_FOUND, ex);
        }
    }

    @Override
    public void authoriseMediaRequestAgainstUser(Integer mediaRequestId) {
        checkMediaRequestIsRequestedByUser(mediaRequestRepository.getReferenceById(mediaRequestId));
    }

    @Override
    public void authoriseTransformedMediaAgainstUser(Integer transformedMediaId) {
        checkMediaRequestIsRequestedByUser(transformedMediaRepository.getReferenceById(transformedMediaId).getMediaRequest());
    }

    @Override
    public void authoriseByAnnotationId(Integer annotationId, Set<SecurityRoleEnum> securityRoles) {
        var annotation = annotationRepository.findById(annotationId).orElseThrow(this::logAndThrowAnnotationNotFound);
        var courthouses = annotation.getHearings().stream()
            .map(hea -> hea.getCourtroom().getCourthouse())
            .toList();

        authorisationApi.checkCourthouseAuthorisation(courthouses, securityRoles);
    }

    private EntityNotFoundException logAndThrowAnnotationNotFound() {
        log.error("Unable to find Annotation-Hearing-Courtroom-Courthouse for checkAuthorisation");
        throw new DartsApiException(ANNOTATION_NOT_FOUND);
    }

    private void checkMediaRequestIsRequestedByUser(MediaRequestEntity mediaRequest) {
        UserAccountEntity userAccount = userIdentity.getUserAccount();
        if (!mediaRequest.getRequestor().getId().equals(userAccount.getId())) {
            throw new DartsApiException(MEDIA_REQUEST_NOT_VALID_FOR_USER);
        }
    }

}
