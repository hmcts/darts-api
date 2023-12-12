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
import uk.gov.hmcts.darts.audio.enums.AudioRequestOutputFormat;
import uk.gov.hmcts.darts.audio.enums.MediaRequestStatus;
import uk.gov.hmcts.darts.audio.exception.AudioApiError;
import uk.gov.hmcts.darts.audio.exception.AudioRequestsApiError;
import uk.gov.hmcts.darts.audio.mapper.GetAudioRequestResponseMapper;
import uk.gov.hmcts.darts.audio.model.EnhancedMediaRequestInfo;
import uk.gov.hmcts.darts.audio.service.MediaRequestService;
import uk.gov.hmcts.darts.audiorequests.model.AudioNonAccessedResponse;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestDetails;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestType;
import uk.gov.hmcts.darts.audiorequests.model.GetAudioRequestResponse;
import uk.gov.hmcts.darts.audit.api.AuditActivity;
import uk.gov.hmcts.darts.audit.api.AuditApi;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity_;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity_;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity_;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.AzureDeleteBlobException;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.repository.MediaRequestRepository;
import uk.gov.hmcts.darts.common.repository.TransformedMediaRepository;
import uk.gov.hmcts.darts.common.repository.TransientObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.datamanagement.api.impl.DataManagementApiImpl;
import uk.gov.hmcts.darts.notification.api.NotificationApi;
import uk.gov.hmcts.darts.notification.dto.SaveNotificationToDbRequest;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.EXPIRED;
import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.OPEN;

@Slf4j
@RequiredArgsConstructor
@Service
public class MediaRequestServiceImpl implements MediaRequestService {

    private final HearingRepository hearingRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserIdentity userIdentity;
    private final MediaRequestRepository mediaRequestRepository;
    private final EntityManager entityManager;
    private final TransientObjectDirectoryRepository transientObjectDirectoryRepository;
    private final DataManagementApiImpl dataManagementApi;
    private final NotificationApi notificationApi;
    private final AuditApi auditApi;
    private final TransformedMediaRepository transformedMediaRepository;

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public Optional<MediaRequestEntity> getOldestMediaRequestByStatus(MediaRequestStatus status) {
        return mediaRequestRepository.findTopByStatusOrderByCreatedDateTimeAsc(status);
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public AudioNonAccessedResponse countNonAccessedAudioForUser(Integer userId) {
        AudioNonAccessedResponse nonAccessedResponse = new AudioNonAccessedResponse();
        nonAccessedResponse.setCount(mediaRequestRepository.countTransformedEntitiesByRequestorIdAndStatusNotAccessed(userId, MediaRequestStatus.COMPLETED));
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
    public MediaRequestEntity updateAudioRequestStatus(Integer id, MediaRequestStatus status) {
        MediaRequestEntity mediaRequestEntity = getMediaRequestById(id);
        mediaRequestEntity.setStatus(status);

        return mediaRequestRepository.saveAndFlush(mediaRequestEntity);
    }

    @Transactional
    @Override
    public MediaRequestEntity saveAudioRequest(AudioRequestDetails request) {
        MediaRequestEntity mediaRequest = saveAudioRequestToDb(
            hearingRepository.getReferenceById(request.getHearingId()),
            userAccountRepository.getReferenceById(request.getRequestor()),
            request.getStartTime(),
            request.getEndTime(),
            request.getRequestType()
        );
        auditApi.recordAudit(AuditActivity.REQUEST_AUDIO, mediaRequest.getRequestor(), mediaRequest.getHearing().getCourtCase());
        return mediaRequest;
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
        deleteTransformedMediaForMediaRequestId(mediaRequestId);
        log.debug("deleting MediaRequestEntity with id {}.", mediaRequestId);
        mediaRequestRepository.deleteById(mediaRequestId);
    }

    private void deleteTransformedMediaForMediaRequestId(Integer mediaRequestId) {
        List<TransformedMediaEntity> transformedMediaList = transformedMediaRepository.findByMediaRequestId(mediaRequestId);
        for (TransformedMediaEntity transformedMedia : transformedMediaList) {
            deleteTransientObjectDirectoryByTransformedMediaId(transformedMedia.getId());
            log.debug("deleting TransformedMediaEntity with id {}.", transformedMedia.getId());
            transformedMediaRepository.delete(transformedMedia);
        }
    }

    @Override
    public void deleteTransformedMedia(Integer transformedMediaId) {
        Optional<TransformedMediaEntity> transformedMediaOpt = transformedMediaRepository.findById(transformedMediaId);
        if (transformedMediaOpt.isEmpty()) {
            throw new DartsApiException(AudioRequestsApiError.TRANSFORMED_MEDIA_NOT_FOUND);
        }
        TransformedMediaEntity transformedMedia = transformedMediaOpt.get();
        MediaRequestEntity mediaRequest = transformedMedia.getMediaRequest();
        deleteTransientObjectDirectoryByTransformedMediaId(transformedMedia.getId());
        log.debug("deleting TransformedMediaEntity with id {}.", transformedMedia.getId());
        transformedMediaRepository.delete(transformedMedia);

        if (transformedMediaRepository.findByMediaRequestId(mediaRequest.getId()).isEmpty()) {
            log.debug("There are no more TransformedMediaEntities associated with media_request_id {}, so deleting.", mediaRequest.getId());
            mediaRequest.setStatus(MediaRequestStatus.DELETED);
            mediaRequestRepository.saveAndFlush(mediaRequest);
        }
    }

    private void deleteTransientObjectDirectoryByTransformedMediaId(Integer transformedMediaId) {
        List<TransientObjectDirectoryEntity> transientObjectDirectoryEntities = transientObjectDirectoryRepository.findByTransformedMediaId(transformedMediaId);
        for (TransientObjectDirectoryEntity mediaTransientObject : transientObjectDirectoryEntities) {
            log.debug("deleting TransientObjectDirectoryEntity with id {}.", mediaTransientObject.getId());
            UUID blobId = mediaTransientObject.getExternalLocation();

            if (blobId != null) {
                try {
                    dataManagementApi.deleteBlobDataFromOutboundContainer(blobId);
                } catch (AzureDeleteBlobException e) {
                    log.error("Error while deleting audio request", e);
                }

            }

            transientObjectDirectoryRepository.deleteById(mediaTransientObject.getId());
        }
    }

    private MediaRequestEntity saveAudioRequestToDb(HearingEntity hearingEntity, UserAccountEntity requestor,
                                                    OffsetDateTime startTime, OffsetDateTime endTime,
                                                    AudioRequestType requestType) {

        MediaRequestEntity mediaRequestEntity = new MediaRequestEntity();
        mediaRequestEntity.setHearing(hearingEntity);
        mediaRequestEntity.setRequestor(requestor);
        mediaRequestEntity.setCurrentOwner(requestor);
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
    public List<GetAudioRequestResponse> getAudioRequests(Integer userId, Boolean expired) {
        List<GetAudioRequestResponse> response = new ArrayList<>();
        List<EnhancedMediaRequestInfo> enhancedMediaRequestInfoList = getEnhancedMediaRequestInfo(userId, expired);
        for (EnhancedMediaRequestInfo enhancedMediaRequestInfo : enhancedMediaRequestInfoList) {
            List<TransformedMediaEntity> transformedMediaList = transformedMediaRepository.findByMediaRequestId(enhancedMediaRequestInfo.getMediaRequestId());
            if (transformedMediaList.size() > 0) {
                TransformedMediaEntity transformedMedia = transformedMediaList.get(0);
                GetAudioRequestResponse getAudioRequestResponseItem = GetAudioRequestResponseMapper.mapToAudioRequestSummary(
                    enhancedMediaRequestInfo,
                    transformedMedia
                );
                response.add(getAudioRequestResponseItem);
            }
        }
        return response;
    }

    private List<EnhancedMediaRequestInfo> getEnhancedMediaRequestInfo(Integer userId, Boolean expired) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<EnhancedMediaRequestInfo> criteriaQuery = criteriaBuilder.createQuery(EnhancedMediaRequestInfo.class);

        Root<MediaRequestEntity> mediaRequest = criteriaQuery.from(MediaRequestEntity.class);
        Join<MediaRequestEntity, HearingEntity> hearing = mediaRequest.join(MediaRequestEntity_.hearing);
        Join<HearingEntity, CourtCaseEntity> courtCase = hearing.join(HearingEntity_.courtCase);
        Join<CourtCaseEntity, CourthouseEntity> courthouse = courtCase.join(CourtCaseEntity_.courthouse);

        criteriaQuery.select(criteriaBuilder.construct(
            EnhancedMediaRequestInfo.class,
            mediaRequest.get(MediaRequestEntity_.id),
            courtCase.get(CourtCaseEntity_.id),
            courtCase.get(CourtCaseEntity_.caseNumber),
            courthouse.get(CourthouseEntity_.courthouseName),
            hearing.get(HearingEntity_.hearingDate),
            hearing.get(HearingEntity_.id),
            mediaRequest.get(MediaRequestEntity_.requestType),
            mediaRequest.get(MediaRequestEntity_.startTime),
            mediaRequest.get(MediaRequestEntity_.endTime),
            mediaRequest.get(MediaRequestEntity_.status)
        ));

        ParameterExpression<UserAccountEntity> paramRequestor = criteriaBuilder.parameter(UserAccountEntity.class);
        criteriaQuery.where(criteriaBuilder.and(
            criteriaBuilder.equal(mediaRequest.get(MediaRequestEntity_.CURRENT_OWNER), paramRequestor),
            expiredPredicate(expired, criteriaBuilder, mediaRequest)
        ));

        criteriaQuery.orderBy(List.of(
            criteriaBuilder.asc(courtCase.get(CourtCaseEntity_.caseNumber)),
            criteriaBuilder.asc(mediaRequest.get(MediaRequestEntity_.startTime))
        ));

        TypedQuery<EnhancedMediaRequestInfo> query = entityManager.createQuery(criteriaQuery);

        query.setParameter(paramRequestor, userAccountRepository.getReferenceById(userId));

        return query.getResultList();
    }


    @Transactional
    @Override
    public void updateTransformedMediaLastAccessedTimestamp(Integer transformedMediaId) {
        Optional<TransformedMediaEntity> foundEntityOpt = transformedMediaRepository.findById(transformedMediaId);
        if (foundEntityOpt.isEmpty()) {
            throw new DartsApiException(AudioRequestsApiError.TRANSFORMED_MEDIA_NOT_FOUND);
        }
        TransformedMediaEntity foundEntity = foundEntityOpt.get();
        foundEntity.setLastAccessed(OffsetDateTime.now());
        transformedMediaRepository.saveAndFlush(foundEntity);
    }

    @Transactional
    @Override
    public void updateTransformedMediaLastAccessedTimestampForMediaRequestId(Integer mediaRequestId) {
        List<TransformedMediaEntity> foundEntityList = transformedMediaRepository.findByMediaRequestId(mediaRequestId);
        if (foundEntityList.isEmpty()) {
            throw new DartsApiException(AudioRequestsApiError.TRANSFORMED_MEDIA_NOT_FOUND);
        }
        for (TransformedMediaEntity transformedMedia : foundEntityList) {
            transformedMedia.setLastAccessed(OffsetDateTime.now());
            transformedMediaRepository.saveAndFlush(transformedMedia);
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
        return downloadOrPlayback(mediaRequestId, AuditActivity.EXPORT_AUDIO, AudioRequestType.DOWNLOAD);
    }

    @Override
    public InputStream playback(Integer mediaRequestId) {
        return downloadOrPlayback(mediaRequestId, AuditActivity.AUDIO_PLAYBACK, AudioRequestType.PLAYBACK);
    }

    private InputStream downloadOrPlayback(Integer mediaRequestId, AuditActivity auditActivity, AudioRequestType expectedType) {
        MediaRequestEntity mediaRequestEntity = getMediaRequestById(mediaRequestId);
        validateMediaRequestType(mediaRequestEntity, expectedType);
        var transientObjectEntity = transientObjectDirectoryRepository.findByMediaRequestId(
                mediaRequestId)
            .orElseThrow(() -> new DartsApiException(AudioApiError.REQUESTED_DATA_CANNOT_BE_LOCATED));

        UUID blobId = transientObjectEntity.getExternalLocation();
        if (blobId == null) {
            throw new DartsApiException(AudioApiError.REQUESTED_DATA_CANNOT_BE_LOCATED);
        }

        auditApi.recordAudit(
            auditActivity,
            this.getUserAccount(),
            mediaRequestEntity.getHearing().getCourtCase()
        );
        return dataManagementApi.getBlobDataFromOutboundContainer(blobId).toStream();
    }

    private void validateMediaRequestType(MediaRequestEntity mediaRequestEntity, AudioRequestType expectedType) {
        if (expectedType != mediaRequestEntity.getRequestType()) {
            throw new DartsApiException(AudioRequestsApiError.MEDIA_REQUEST_TYPE_IS_INVALID_FOR_ENDPOINT);
        }
    }

    @Override
    public MediaRequestEntity updateAudioRequestCompleted(MediaRequestEntity mediaRequestEntity, String fileName,
                                                          AudioRequestOutputFormat audioRequestOutputFormat) {

        mediaRequestEntity.setStatus(MediaRequestStatus.COMPLETED);
        //todo update transformed media info
        return mediaRequestRepository.saveAndFlush(mediaRequestEntity);
    }

    private UserAccountEntity getUserAccount() {
        return userIdentity.getUserAccount();
    }
}
