package uk.gov.hmcts.darts.hearings.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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

@Service
@RequiredArgsConstructor
@Slf4j
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
    public void validateHearingExsistsElseError(Integer hearingId) {
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
    public void removeMediaLinkToHearing(Integer courtCaseId) {
        hearingRepository.findByCaseIdWithMediaList(courtCaseId)
            .ifPresent(hearing -> hearing.getMediaList()
                .forEach(media -> {
                    // Check all cases linked to a hearing have been expired/anonymised
                    if (!mediaLinkedCaseRepository.areAllAssociatedCasesAnonymised(media)) {
                        log.info(
                            "Media {} link not removed for case id {} as not all associated cases are expired",
                            media.getId(), courtCaseId);
                    } else {
                        hearingRepository.findHearingIdsByMediaId(media.getId()).forEach(
                            hearingForMedia -> {
                                hearingForMedia.setMediaList(null);
                                hearingRepository.save(hearingForMedia);
                                log.info(
                                    "Media id {} link removed for hearing id {} on the expiry of case id {}",
                                    media.getId(), hearingForMedia.getId(), courtCaseId);
                            });
                    }
                })
            );
    }

    private void validateCaseIsNotExpiredFromHearingId(Integer hearingId) {
        Optional<HearingEntity> hearingEntity = hearingRepository.findById(hearingId);
        hearingEntity
            .map(HearingEntity::getCourtCase)
            .ifPresent(CourtCaseEntity::validateIsExpired);
    }
}