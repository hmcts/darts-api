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
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.MediaRequestRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionRepository;
import uk.gov.hmcts.darts.common.repository.TransformedMediaRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
public class AuthorisationImpl implements Authorisation {

    private final CaseRepository caseRepository;
    private final HearingRepository hearingRepository;
    private final MediaRequestRepository mediaRequestRepository;
    private final MediaRepository mediaRepository;
    private final TranscriptionRepository transcriptionRepository;
    private final TransformedMediaRepository transformedMediaRepository;
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
            final Set<CourthouseEntity> courthouses = mediaRepository.getReferenceById(mediaId)
                  .getHearingList()
                  .stream()
                  .map(hearingEntity -> hearingEntity.getCourtroom().getCourthouse())
                  .collect(Collectors.toUnmodifiableSet());

            authorisationApi.checkCourthouseAuthorisation(
                  courthouses
                        .stream()
                        .collect(Collectors.toUnmodifiableList()),
                  securityRoles
            );
        } catch (EntityNotFoundException e) {
            log.error("Unable to find Media-Hearing-Courtroom-Courthouse for checkAuthorisation", e);
            throw new DartsApiException(MEDIA_NOT_FOUND);
        }
    }

    @Override
    public void authoriseByTranscriptionId(Integer transcriptionId, Set<SecurityRoleEnum> securityRoles) {
        try {
            final List<CourthouseEntity> courthouses = getCourthousesFromTranscription(transcriptionId);
            if (CollectionUtils.isEmpty(courthouses)) {
                throw new EntityNotFoundException();
            }
            authorisationApi.checkCourthouseAuthorisation(courthouses, securityRoles);
        } catch (EntityNotFoundException e) {
            log.error("Unable to find Transcription-Courtroom-Courthouse for checkAuthorisation. TranscriptionId={}", transcriptionId, e);
            throw new DartsApiException(TRANSCRIPTION_NOT_FOUND);
        }
    }

    private List<CourthouseEntity> getCourthousesFromTranscription(Integer transcriptionId) {
        Optional<TranscriptionEntity> transcriptionEntityOpt = transcriptionRepository.findById(transcriptionId);
        List<CourthouseEntity> returnList = new ArrayList<>();
        if (transcriptionEntityOpt.isEmpty()) {
            return returnList;
        }
        TranscriptionEntity transcriptionEntity = transcriptionEntityOpt.get();
        List<CourtCaseEntity> courtCases = transcriptionEntity.getCourtCases();
        if (CollectionUtils.isNotEmpty(courtCases)) {
            CollectionUtils.addAll(returnList, courtCases.stream().map(CourtCaseEntity::getCourthouse).toList());
        }
        List<HearingEntity> hearings = transcriptionEntity.getHearings();
        if (CollectionUtils.isNotEmpty(hearings)) {
            CollectionUtils.addAll(returnList, hearings.stream()
                  .map(HearingEntity::getCourtCase)
                  .map(CourtCaseEntity::getCourthouse)
                  .toList());
        }
        return returnList.stream().distinct().toList();

    }

    @Override
    public void authoriseByTransformedMediaId(Integer transformedMediaId, Set<SecurityRoleEnum> securityRoles) {
        try {
            final List<CourthouseEntity> courthouses = List.of(transformedMediaRepository.getReferenceById(transformedMediaId)
                  .getMediaRequest().getHearing().getCourtroom().getCourthouse());
            authorisationApi.checkCourthouseAuthorisation(courthouses, securityRoles);
        } catch (EntityNotFoundException e) {
            log.error("Unable to find TransformedMedia-MediaRequest-Hearing-Courtroom-Courthouse for checkAuthorisation", e);
            throw new DartsApiException(TRANSFORMED_MEDIA_NOT_FOUND);
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

    private void checkMediaRequestIsRequestedByUser(MediaRequestEntity mediaRequest) {
        UserAccountEntity userAccount = userIdentity.getUserAccount();
        if (!mediaRequest.getRequestor().getId().equals(userAccount.getId())) {
            throw new DartsApiException(MEDIA_REQUEST_NOT_VALID_FOR_USER);
        }
    }

}
