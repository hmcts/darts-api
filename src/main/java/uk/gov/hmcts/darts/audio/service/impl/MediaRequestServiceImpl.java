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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity_;
import uk.gov.hmcts.darts.audio.enums.AudioRequestStatus;
import uk.gov.hmcts.darts.audio.exception.AudioApiError;
import uk.gov.hmcts.darts.audio.exception.AudioRequestsApiError;
import uk.gov.hmcts.darts.audio.service.MediaRequestService;
import uk.gov.hmcts.darts.audiorequests.model.AudioNonAccessedResponse;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestDetails;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestType;
import uk.gov.hmcts.darts.audit.enums.AuditActivityEnum;
import uk.gov.hmcts.darts.audit.service.AuditService;
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
import uk.gov.hmcts.darts.common.repository.MediaRequestRepository;
import uk.gov.hmcts.darts.common.repository.TransientObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.datamanagement.api.impl.DataManagementApiImpl;
import uk.gov.hmcts.darts.notification.api.NotificationApi;
import uk.gov.hmcts.darts.notification.dto.SaveNotificationToDbRequest;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static uk.gov.hmcts.darts.audio.enums.AudioRequestStatus.EXPIRED;
import static uk.gov.hmcts.darts.audio.enums.AudioRequestStatus.OPEN;

@Slf4j
@RequiredArgsConstructor
@Service
public class MediaRequestServiceImpl implements MediaRequestService {

    private final HearingRepository hearingRepository;
    private final UserAccountRepository userAccountRepository;
    private final MediaRequestRepository mediaRequestRepository;
    private final EntityManager entityManager;
    private final TransientObjectDirectoryRepository transientObjectDirectoryRepository;
    private final DataManagementApiImpl dataManagementApi;
    private final NotificationApi notificationApi;

    private final AuditService auditService;

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public Optional<MediaRequestEntity> getOldestMediaRequestByStatus(AudioRequestStatus status) {
        return mediaRequestRepository.findTopByStatusOrderByCreatedDateTimeAsc(status);
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public AudioNonAccessedResponse countNonAccessedAudioForUser(Integer userId) {
        AudioNonAccessedResponse nonAccessedResponse = new AudioNonAccessedResponse();
        nonAccessedResponse.setCount(mediaRequestRepository.countByRequestor_IdAndStatusAndLastAccessedDateTime(userId, AudioRequestStatus.COMPLETED, null));
        return nonAccessedResponse;
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public MediaRequestEntity getMediaRequestById(Integer id) {
        return mediaRequestRepository.findById(id).orElseThrow(
            () -> new DartsApiException(AudioRequestsApiError.MEDIA_REQUEST_NOT_FOUND));
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
        return saveAudioRequestToDb(
            hearingRepository.getReferenceById(request.getHearingId()),
            userAccountRepository.getReferenceById(request.getRequestor()),
            request.getStartTime(),
            request.getEndTime(),
            request.getRequestType()
        );
    }

    @Override
    public void scheduleMediaRequestPendingNotification(MediaRequestEntity mediaRequest) {
        try {
            var hearingEntity = mediaRequest.getHearing();
            var courtCase = hearingEntity.getCourtCase();
            var saveNotificationToDbRequest = SaveNotificationToDbRequest.builder()
                .eventId(NotificationApi.NotificationTemplate.AUDIO_REQUEST_PROCESSING.toString())
                .caseId(courtCase.getId())
                .emailAddresses(mediaRequest.getRequestor().getEmailAddress())
                .build();
            notificationApi.scheduleNotification(saveNotificationToDbRequest);
        } catch (Exception e) {
            log.error("Unable to schedule media request pending notification: {}", e.getMessage());
        }
    }

    @Transactional
    @Override
    public void deleteAudioRequest(Integer mediaRequestId) {

        var transientObject = transientObjectDirectoryRepository.getTransientObjectDirectoryEntityByMediaRequest_Id(
            mediaRequestId);

        if (transientObject.isPresent()) {
            TransientObjectDirectoryEntity mediaTransientObject = transientObject.get();
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
            courtCase.get(CourtCaseEntity_.id),
            courtCase.get(CourtCaseEntity_.caseNumber),
            courthouse.get(CourthouseEntity_.courthouseName),
            hearing.get(HearingEntity_.hearingDate),
            hearing.get(HearingEntity_.id),
            mediaRequest.get(MediaRequestEntity_.requestType),
            mediaRequest.get(MediaRequestEntity_.startTime),
            mediaRequest.get(MediaRequestEntity_.endTime),
            mediaRequest.get(MediaRequestEntity_.expiryTime),
            mediaRequest.get(MediaRequestEntity_.status),
            mediaRequest.get(MediaRequestEntity_.lastAccessedDateTime),
            mediaRequest.get(MediaRequestEntity_.outputFilename),
            mediaRequest.get(MediaRequestEntity_.outputFormat)
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

    @Override
    public InputStream download(Integer mediaRequestId) {
        return downloadOrPlayback(mediaRequestId, AuditActivityEnum.EXPORT_AUDIO, AudioRequestType.DOWNLOAD);
    }

    @Override
    public InputStream playback(Integer mediaRequestId) {
        return downloadOrPlayback(mediaRequestId, AuditActivityEnum.AUDIO_PLAYBACK, AudioRequestType.PLAYBACK);
    }

    private InputStream downloadOrPlayback(Integer mediaRequestId, AuditActivityEnum auditActivityEnum, AudioRequestType expectedType) {
        MediaRequestEntity mediaRequestEntity = getMediaRequestById(mediaRequestId);
        validateMediaRequestType(mediaRequestEntity, expectedType);
        var transientObjectEntity = transientObjectDirectoryRepository.getTransientObjectDirectoryEntityByMediaRequest_Id(
                mediaRequestId)
            .orElseThrow(() -> new DartsApiException(AudioApiError.REQUESTED_DATA_CANNOT_BE_LOCATED));

        UUID blobId = transientObjectEntity.getExternalLocation();
        if (blobId == null) {
            throw new DartsApiException(AudioApiError.REQUESTED_DATA_CANNOT_BE_LOCATED);
        }

        auditService.recordAudit(
            auditActivityEnum,
            mediaRequestEntity.getRequestor(),
            mediaRequestEntity.getHearing().getCourtCase()
        );
        return dataManagementApi.getBlobDataFromOutboundContainer(blobId).toStream();
    }

    private void validateMediaRequestType(MediaRequestEntity mediaRequestEntity, AudioRequestType expectedType) {
        if (expectedType != mediaRequestEntity.getRequestType()) {
            throw new DartsApiException(AudioRequestsApiError.MEDIA_REQUEST_TYPE_IS_INVALID_FOR_ENDPOINT);
        }
    }
}
