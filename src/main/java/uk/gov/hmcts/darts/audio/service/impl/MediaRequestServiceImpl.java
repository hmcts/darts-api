package uk.gov.hmcts.darts.audio.service.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity_;
import uk.gov.hmcts.darts.audio.enums.AudioRequestStatus;
import uk.gov.hmcts.darts.audio.exception.AudioRequestsApiError;
import uk.gov.hmcts.darts.audio.model.AudioRequestDetails;
import uk.gov.hmcts.darts.audio.model.AudioRequestType;
import uk.gov.hmcts.darts.audio.repository.MediaRequestRepository;
import uk.gov.hmcts.darts.audio.service.MediaRequestService;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity_;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity_;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity_;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.repository.TransientObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.datamanagement.api.impl.DataManagementApiImpl;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import static uk.gov.hmcts.darts.audio.enums.AudioRequestStatus.EXPIRED;
import static uk.gov.hmcts.darts.audio.enums.AudioRequestStatus.OPEN;

@RequiredArgsConstructor
@Service
public class MediaRequestServiceImpl implements MediaRequestService {

    private final HearingRepository hearingRepository;
    private final UserAccountRepository userAccountRepository;
    private final MediaRequestRepository mediaRequestRepository;
    private final EntityManager entityManager;
    private final TransientObjectDirectoryRepository transientObjectDirectoryRepository;
    private final DataManagementApiImpl dataManagementApi;

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public List<MediaRequestEntity> getMediaRequestsByStatus(AudioRequestStatus status) {
        return mediaRequestRepository.findByStatusOrderByCreatedDateTimeAsc(status);
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public MediaRequestEntity getMediaRequestById(Integer id) {
        return mediaRequestRepository.findById(id).orElseThrow();
    }

    @Transactional
    @Override
    public MediaRequestEntity updateAudioRequestStatus(Integer id, AudioRequestStatus status) {
        MediaRequestEntity mediaRequestEntity = getMediaRequestById(id);
        mediaRequestEntity.setStatus(status);

        return mediaRequestRepository.saveAndFlush(mediaRequestEntity);
    }

    @Transactional
    @Override
    public MediaRequestEntity saveAudioRequest(AudioRequestDetails request) {

        var audioRequest = saveAudioRequestToDb(
            hearingRepository.getReferenceById(request.getHearingId()),
            userAccountRepository.getReferenceById(request.getRequestor()),
            request.getStartTime(),
            request.getEndTime(),
            request.getRequestType()
        );

        return audioRequest;
    }

    @Transactional
    @Override
    public void deleteAudioRequest(Integer mediaRequestId) {

        var transientObject = transientObjectDirectoryRepository.getTransientObjectDirectoryEntityByMediaRequest_Id(mediaRequestId);

        if (transientObject.isPresent()) {
            TransientObjectDirectoryEntity mediaTransientObject  = transientObject.get();
            UUID blobId = mediaTransientObject.getExternalLocation();

            if (blobId != null) {
                dataManagementApi.deleteBlobDataFromOutboundContainer(blobId);
            }

            transientObjectDirectoryRepository.deleteById(mediaTransientObject.getId());
        }

        mediaRequestRepository.deleteById(mediaRequestId);
    }

    private MediaRequestEntity saveAudioRequestToDb(HearingEntity hearingEntity, UserAccountEntity requestor,
                                                    OffsetDateTime startTime, OffsetDateTime endTime,
                                                    AudioRequestType requestType) {

        MediaRequestEntity mediaRequestEntity = new MediaRequestEntity();
        mediaRequestEntity.setHearing(hearingEntity);
        mediaRequestEntity.setRequestor(requestor);
        mediaRequestEntity.setStartTime(startTime);
        mediaRequestEntity.setEndTime(endTime);
        mediaRequestEntity.setRequestType(requestType);
        mediaRequestEntity.setStatus(OPEN);
        mediaRequestEntity.setAttempts(0);
        mediaRequestEntity.setCreatedBy(requestor);
        mediaRequestEntity.setLastModifiedBy(requestor);

        return mediaRequestRepository.saveAndFlush(mediaRequestEntity);
    }

    @Override
    public List<AudioRequestSummaryResult> viewAudioRequests(Integer userId, Boolean expired) {

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<AudioRequestSummaryResult> criteriaQuery = criteriaBuilder.createQuery(AudioRequestSummaryResult.class);

        Root<MediaRequestEntity> mediaRequest = criteriaQuery.from(MediaRequestEntity.class);
        Join<MediaRequestEntity, HearingEntity> hearing = mediaRequest.join(MediaRequestEntity_.hearing);
        Join<HearingEntity, CourtCaseEntity> courtCase = hearing.join(HearingEntity_.courtCase);
        Join<CourtCaseEntity, CourthouseEntity> courthouse = courtCase.join(CourtCaseEntity_.courthouse);

        criteriaQuery.select(criteriaBuilder.construct(
            AudioRequestSummaryResult.class,
            mediaRequest.get(MediaRequestEntity_.id),
            courtCase.get(CourtCaseEntity_.caseNumber),
            courthouse.get(CourthouseEntity_.courthouseName),
            hearing.get(HearingEntity_.hearingDate),
            mediaRequest.get(MediaRequestEntity_.startTime),
            mediaRequest.get(MediaRequestEntity_.endTime),
            mediaRequest.get(MediaRequestEntity_.expiryTime),
            mediaRequest.get(MediaRequestEntity_.status)
        ));

        ParameterExpression<UserAccountEntity> paramRequestor = criteriaBuilder.parameter(UserAccountEntity.class);
        criteriaQuery.where(criteriaBuilder.and(
            criteriaBuilder.equal(mediaRequest.get(MediaRequestEntity_.requestor), paramRequestor),
            expiredPredicate(expired, criteriaBuilder, mediaRequest)
        ));

        criteriaQuery.orderBy(List.of(
            criteriaBuilder.asc(courtCase.get(CourtCaseEntity_.caseNumber)),
            criteriaBuilder.asc(mediaRequest.get(MediaRequestEntity_.startTime))
        ));

        TypedQuery<AudioRequestSummaryResult> query = entityManager.createQuery(criteriaQuery);

        query.setParameter(paramRequestor, userAccountRepository.getReferenceById(userId));

        return query.getResultList();
    }


    @Transactional
    @Override
    public void updateAudioRequestLastAccessedTimestamp(Integer mediaRequestId) {
        try {
            MediaRequestEntity mediaRequestEntity = getMediaRequestById(mediaRequestId);
            mediaRequestEntity.setLastAccessedDateTime(OffsetDateTime.now());
            mediaRequestRepository.saveAndFlush(mediaRequestEntity);
        } catch (NoSuchElementException e) {
            throw new DartsApiException(AudioRequestsApiError.MEDIA_REQUEST_NOT_FOUND);
        }
    }

    private Predicate expiredPredicate(Boolean expired, CriteriaBuilder criteriaBuilder,
                                       Root<MediaRequestEntity> mediaRequest) {

        final Predicate expiredPredicate;
        if (expired) {
            expiredPredicate = criteriaBuilder.equal(mediaRequest.get(MediaRequestEntity_.status), EXPIRED);
        } else {
            expiredPredicate = criteriaBuilder.notEqual(mediaRequest.get(MediaRequestEntity_.status), EXPIRED);
        }
        return expiredPredicate;
    }

}
