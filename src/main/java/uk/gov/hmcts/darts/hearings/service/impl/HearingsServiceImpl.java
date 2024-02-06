package uk.gov.hmcts.darts.hearings.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.authorisation.api.AuthorisationApi;
import uk.gov.hmcts.darts.common.entity.AnnotationEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.AnnotationRepository;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionRepository;
import uk.gov.hmcts.darts.hearings.exception.HearingApiError;
import uk.gov.hmcts.darts.hearings.mapper.GetAnnotationsResponseMapper;
import uk.gov.hmcts.darts.hearings.mapper.GetEventsResponseMapper;
import uk.gov.hmcts.darts.hearings.mapper.GetHearingResponseMapper;
import uk.gov.hmcts.darts.hearings.mapper.TranscriptionMapper;
import uk.gov.hmcts.darts.hearings.model.Annotation;
import uk.gov.hmcts.darts.hearings.model.EventResponse;
import uk.gov.hmcts.darts.hearings.model.GetHearingResponse;
import uk.gov.hmcts.darts.hearings.model.Transcript;
import uk.gov.hmcts.darts.hearings.service.HearingsService;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class HearingsServiceImpl implements HearingsService {

    public static final List<SecurityRoleEnum> ADMIN_ROLE = List.of(SecurityRoleEnum.ADMIN);
    private final GetHearingResponseMapper getHearingResponseMapper;
    private final HearingRepository hearingRepository;
    private final TranscriptionRepository transcriptionRepository;
    private final EventRepository eventRepository;
    private final AnnotationRepository annotationRepository;
    private final AuthorisationApi authorisationApi;

    @Override
    public GetHearingResponse getHearings(Integer hearingId) {
        HearingEntity foundHearing = getHearingById(hearingId);
        return getHearingResponseMapper.map(foundHearing);
    }

    public HearingEntity getHearingById(Integer hearingId) {
        Optional<HearingEntity> foundHearingOpt = hearingRepository.findById(hearingId);
        if (foundHearingOpt.isEmpty()) {
            throw new DartsApiException(HearingApiError.HEARING_NOT_FOUND);
        }
        return foundHearingOpt.get();
    }

    @Override
    public List<EventResponse> getEvents(Integer hearingId) {
        List<EventEntity> eventEntities = eventRepository.findAllByHearingId(hearingId);
        return GetEventsResponseMapper.mapToEvents(eventEntities);
    }

    @Override
    public List<Transcript> getTranscriptsByHearingId(Integer hearingId) {
        List<TranscriptionEntity> transcriptionEntities = transcriptionRepository.findByHearingId(hearingId);
        List<TranscriptionEntity> filteredTranscriptionEntities = findNonAutomaticTranscripts(transcriptionEntities);
        return TranscriptionMapper.mapResponse(filteredTranscriptionEntities);
    }

    private List<TranscriptionEntity> findNonAutomaticTranscripts(List<TranscriptionEntity> transcriptionEntities) {
        //only show manual transcriptions or ones that came from legacy. Do not show Modernised automatic transcriptions.
        return transcriptionEntities.stream()
              .filter(transcriptionEntity -> BooleanUtils.isTrue(transcriptionEntity.getIsManualTranscription())
                    || StringUtils.isNotBlank(transcriptionEntity.getLegacyObjectId()))
              .toList();
    }

    @Override
    public List<Annotation> getAnnotationsByHearingId(Integer hearingId) {
        List<AnnotationEntity> annotations;
        if (authorisationApi.userHasOneOfRoles(ADMIN_ROLE)) {
            //admin will see all annotations
            annotations = annotationRepository.findByHearingId(hearingId);
        } else {
            //Non-admin will only see their own annotations
            annotations = annotationRepository.findByHearingIdAndUser(hearingId, authorisationApi.getCurrentUser());
        }

        return GetAnnotationsResponseMapper.mapToAnnotations(annotations, hearingId);
    }
}
