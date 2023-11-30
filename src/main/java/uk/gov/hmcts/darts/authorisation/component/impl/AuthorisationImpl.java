package uk.gov.hmcts.darts.authorisation.component.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.authorisation.component.Authorisation;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.MediaRequestRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionRepository;

import java.util.List;
import java.util.Set;

import static uk.gov.hmcts.darts.audio.exception.AudioApiError.MEDIA_NOT_FOUND;
import static uk.gov.hmcts.darts.audio.exception.AudioRequestsApiError.MEDIA_REQUEST_NOT_FOUND;
import static uk.gov.hmcts.darts.audio.exception.AudioRequestsApiError.MEDIA_REQUEST_NOT_VALID_FOR_USER;
import static uk.gov.hmcts.darts.cases.exception.CaseApiError.CASE_NOT_FOUND;
import static uk.gov.hmcts.darts.hearings.exception.HearingApiError.HEARING_NOT_FOUND;
import static uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError.TRANSCRIPTION_NOT_FOUND;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthorisationImpl implements Authorisation {

    private final CaseRepository caseRepository;
    private final HearingRepository hearingRepository;
    private final MediaRequestRepository mediaRequestRepository;
    private final MediaRepository mediaRepository;
    private final TranscriptionRepository transcriptionRepository;
    private final AuthorisationApi authorisationApi;
    private final UserIdentity userIdentity;

    @Override
    public void authoriseByCaseId(Integer caseId, Set<SecurityRoleEnum> securityRoles) {
        try {
            final List<CourthouseEntity> courthouses = List.of(caseRepository.getReferenceById(caseId)
                                                                   .getCourthouse());
            authorisationApi.checkCourthouseAuthorisation(courthouses, securityRoles);
        } catch (EntityNotFoundException e) {
            log.error("Unable to find Case-Courthouse for checkAuthorisation", e);
            throw new DartsApiException(CASE_NOT_FOUND);
        }
    }

    @Override
    public void authoriseByHearingId(Integer hearingId, Set<SecurityRoleEnum> securityRoles) {
        try {
            final List<CourthouseEntity> courthouses = List.of(hearingRepository.getReferenceById(hearingId)
                                                                   .getCourtroom().getCourthouse());
            authorisationApi.checkCourthouseAuthorisation(courthouses, securityRoles);
        } catch (EntityNotFoundException e) {
            log.error("Unable to find Hearing-Courtroom-Courthouse for checkAuthorisation", e);
            throw new DartsApiException(HEARING_NOT_FOUND);
        }
    }

    @Override
    public void authoriseByMediaRequestId(Integer mediaRequestId, Set<SecurityRoleEnum> securityRoles) {
        try {
            final List<CourthouseEntity> courthouses = List.of(mediaRequestRepository.getReferenceById(mediaRequestId)
                                                                   .getHearing().getCourtroom().getCourthouse());
            authorisationApi.checkCourthouseAuthorisation(courthouses, securityRoles);
        } catch (EntityNotFoundException e) {
            log.error("Unable to find MediaRequest-Hearing-Courtroom-Courthouse for checkAuthorisation", e);
            throw new DartsApiException(MEDIA_REQUEST_NOT_FOUND);
        }
    }

    @Override
    public void authoriseByMediaId(Integer mediaId, Set<SecurityRoleEnum> securityRoles) {
        try {
            final List<CourthouseEntity> courthouses = List.of(mediaRepository.getReferenceById(mediaId)
                                                                   .getCourtroom().getCourthouse());
            authorisationApi.checkCourthouseAuthorisation(courthouses, securityRoles);
        } catch (EntityNotFoundException e) {
            log.error("Unable to find Media-Courtroom-Courthouse for checkAuthorisation", e);
            throw new DartsApiException(MEDIA_NOT_FOUND);
        }
    }

    @Override
    public void authoriseByTranscriptionId(Integer transcriptionId, Set<SecurityRoleEnum> securityRoles) {
        try {
            final List<CourthouseEntity> courthouses = List.of(transcriptionRepository.getReferenceById(transcriptionId)
                                                                   .getCourtCase().getCourthouse());
            authorisationApi.checkCourthouseAuthorisation(courthouses, securityRoles);
        } catch (EntityNotFoundException e) {
            log.error("Unable to find Transcription-Courtroom-Courthouse for checkAuthorisation", e);
            throw new DartsApiException(TRANSCRIPTION_NOT_FOUND);
        }
    }

    public void authoriseMediaRequestAgainstUser(Integer mediaRequestId) {
        try {
            MediaRequestEntity mediaRequest = mediaRequestRepository.getReferenceById(mediaRequestId);
            UserAccountEntity userAccount = userIdentity.getUserAccount();

            if (!mediaRequest.getRequestor().getId().equals(userAccount.getId())) {
                throw new DartsApiException(MEDIA_REQUEST_NOT_VALID_FOR_USER);
            }
        } catch (EntityNotFoundException | IllegalStateException e) {
            log.error("Unable to validate media requests for user", e);
            throw new DartsApiException(MEDIA_REQUEST_NOT_VALID_FOR_USER);
        }
    }

}
