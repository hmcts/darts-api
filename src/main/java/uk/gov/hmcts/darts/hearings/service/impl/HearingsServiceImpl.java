package uk.gov.hmcts.darts.hearings.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.common.entity.AnnotationEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.AnnotationRepository;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.repository.MediaLinkedCaseRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionRepository;
import uk.gov.hmcts.darts.hearings.exception.HearingApiError;
import uk.gov.hmcts.darts.hearings.mapper.GetAnnotationsResponseMapper;
import uk.gov.hmcts.darts.hearings.mapper.GetEventsResponseMapper;
import uk.gov.hmcts.darts.hearings.mapper.GetHearingResponseMapper;
import uk.gov.hmcts.darts.hearings.mapper.HearingTranscriptionMapper;
import uk.gov.hmcts.darts.hearings.model.Annotation;
import uk.gov.hmcts.darts.hearings.model.EventResponse;
import uk.gov.hmcts.darts.hearings.model.GetHearingResponse;
import uk.gov.hmcts.darts.hearings.model.HearingTranscriptModel;
import uk.gov.hmcts.darts.hearings.model.Transcript;
import uk.gov.hmcts.darts.hearings.service.HearingsService;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("PMD.CouplingBetweenObjects")//TODO - refactor to reduce coupling when this class is next edited
public class HearingsServiceImpl implements HearingsService {

    private final GetHearingResponseMapper getHearingResponseMapper;
    private final HearingRepository hearingRepository;
    private final MediaLinkedCaseRepository mediaLinkedCaseRepository;
    private final TranscriptionRepository transcriptionRepository;
    private final EventRepository eventRepository;
    private final AnnotationRepository annotationRepository;
    private final AuthorisationApi authorisationApi;
    private final HearingTranscriptionMapper transcriptionMapper;

    public static final List<SecurityRoleEnum> SUPER_ADMIN_ROLE = List.of(SecurityRoleEnum.SUPER_ADMIN);
    private final MediaRepository mediaRepository;


    @Override
    public GetHearingResponse getHearings(Integer hearingId) {
        HearingEntity foundHearing = getHearingByIdWithValidation(hearingId);
        return getHearingResponseMapper.map(foundHearing);
    }

    @Override
    public HearingEntity getHearingByIdWithValidation(Integer hearingId) {
        HearingEntity hearingEntity = getHearingById(hearingId);
        if (!hearingEntity.getHearingIsActual()) {
            throw new DartsApiException(HearingApiError.HEARING_NOT_ACTUAL);
        }
        hearingEntity.getCourtCase().validateIsExpired();
        return hearingEntity;
    }

    @Override
    public HearingEntity getHearingById(Integer hearingId) {
        Optional<HearingEntity> foundHearingOpt = hearingRepository.findById(hearingId);
        if (foundHearingOpt.isEmpty()) {
            throw new DartsApiException(HearingApiError.HEARING_NOT_FOUND);
        }
        return foundHearingOpt.get();
    }

    @Override
    public void validateHearingExistsElseError(Integer hearingId) {
        if (!hearingRepository.existsById(hearingId)) {
            throw new DartsApiException(HearingApiError.HEARING_NOT_FOUND);
        }
    }

    @Override
    public List<EventResponse> getEvents(Integer hearingId) {
        validateCaseIsNotExpiredFromHearingId(hearingId);
        List<EventEntity> eventEntities = eventRepository.findAllByHearingId(hearingId);
        return GetEventsResponseMapper.mapToEvents(eventEntities);
    }


    @Override
    public List<Transcript> getTranscriptsByHearingId(Integer hearingId) {
        validateCaseIsNotExpiredFromHearingId(hearingId);
        List<TranscriptionEntity> transcriptionEntities =
            transcriptionRepository.findByHearingIdManualOrLegacyIncludeDeletedTranscriptionDocuments(hearingId);
        List<HearingTranscriptModel> hearingTranscriptModel = transcriptionMapper.mapResponse(transcriptionEntities);
        return transcriptionMapper.getTranscriptList(hearingTranscriptModel);
    }

    @Override
    public List<Annotation> getAnnotationsByHearingId(Integer hearingId) {
        validateCaseIsNotExpiredFromHearingId(hearingId);
        List<AnnotationEntity> annotations;
        if (authorisationApi.userHasOneOfRoles(SUPER_ADMIN_ROLE)) {
            //admin will see all annotations
            annotations = annotationRepository.findByHearingId(hearingId);
        } else {
            //Non-admin will only see their own annotations
            annotations = annotationRepository.findByHearingIdAndUser(hearingId, authorisationApi.getCurrentUser());
        }
        return GetAnnotationsResponseMapper.mapToAnnotations(annotations, hearingId);
    }

    @Override
    @Transactional
    public void removeMediaLinkToHearing(Integer courtCaseId) {
        mediaRepository.findByCaseIdWithMediaList(courtCaseId)
            .stream()
            .filter(media -> {
                // Check all cases linked to a hearing have been expired/anonymised
                if (mediaLinkedCaseRepository.areAllAssociatedCasesAnonymised(media)) {
                    return true;
                }
                log.info("Media {} link not removed for case id {} as not all associated cases are expired", media.getId(), courtCaseId);
                return false;
            })
            .forEach(media -> {
                Set<HearingEntity> hearingEntities = media.getHearings();
                hearingEntities.forEach(media::removeHearing);
                mediaRepository.save(media);
                hearingRepository.saveAll(hearingEntities);

                if (log.isInfoEnabled()) {
                    log.info(
                        "Media id {} link removed for hearing id's {} on the expiry of case id {}",
                        media.getId(),
                        hearingEntities.stream().map(HearingEntity::getId).toList(),
                        courtCaseId);
                }
            });
    }

    private void validateCaseIsNotExpiredFromHearingId(Integer hearingId) {
        Optional<HearingEntity> hearingEntity = hearingRepository.findById(hearingId);
        hearingEntity
            .map(HearingEntity::getCourtCase)
            .ifPresent(CourtCaseEntity::validateIsExpired);
    }
}