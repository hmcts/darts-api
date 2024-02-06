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
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.audio.component.AudioRequestBeingProcessedFromArchiveQuery;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity_;
import uk.gov.hmcts.darts.audio.enums.AudioRequestOutputFormat;
import uk.gov.hmcts.darts.audio.enums.MediaRequestStatus;
import uk.gov.hmcts.darts.audio.exception.AudioApiError;
import uk.gov.hmcts.darts.audio.exception.AudioRequestsApiError;
import uk.gov.hmcts.darts.audio.mapper.GetAudioRequestResponseMapper;
import uk.gov.hmcts.darts.audio.mapper.MediaRequestDetailsMapper;
import uk.gov.hmcts.darts.audio.mapper.TransformedMediaDetailsMapper;
import uk.gov.hmcts.darts.audio.model.EnhancedMediaRequestInfo;
import uk.gov.hmcts.darts.audio.model.TransformedMediaDetailsDto;
import uk.gov.hmcts.darts.audio.service.MediaRequestService;
import uk.gov.hmcts.darts.audiorequests.model.AudioNonAccessedResponse;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestDetails;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestType;
import uk.gov.hmcts.darts.audiorequests.model.GetAudioRequestResponse;
import uk.gov.hmcts.darts.audiorequests.model.GetAudioRequestResponseV1;
import uk.gov.hmcts.darts.audiorequests.model.MediaRequestDetails;
import uk.gov.hmcts.darts.audiorequests.model.TransformedMediaDetails;
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
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;
import uk.gov.hmcts.darts.notification.api.NotificationApi;
import uk.gov.hmcts.darts.notification.dto.SaveNotificationToDbRequest;

import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.COMPLETED;
import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.DELETED;
import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.EXPIRED;
import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.OPEN;
import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.PROCESSING;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;
import static uk.gov.hmcts.darts.notification.api.NotificationApi.NotificationTemplate.AUDIO_REQUEST_PROCESSING;
import static uk.gov.hmcts.darts.notification.api.NotificationApi.NotificationTemplate.AUDIO_REQUEST_PROCESSING_ARCHIVE;

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
    private final DataManagementApi dataManagementApi;
    private final NotificationApi notificationApi;
    private final AuditApi auditApi;
    private final TransformedMediaRepository transformedMediaRepository;
    private final TransformedMediaDetailsMapper transformedMediaDetailsMapper;
    private final MediaRequestDetailsMapper mediaRequestDetailsMapper;
    private final AudioRequestBeingProcessedFromArchiveQuery audioRequestBeingProcessedFromArchiveQuery;

    @Override
    public Optional<MediaRequestEntity> getOldestMediaRequestByStatus(MediaRequestStatus status) {
        return mediaRequestRepository.findTopByStatusOrderByCreatedDateTimeAsc(status);
    }

    @Override
    public AudioNonAccessedResponse countNonAccessedAudioForUser(Integer userId) {
        AudioNonAccessedResponse nonAccessedResponse = new AudioNonAccessedResponse();
        nonAccessedResponse.setCount(mediaRequestRepository.countTransformedEntitiesByRequestorIdAndStatusNotAccessed(userId, MediaRequestStatus.COMPLETED));
        return nonAccessedResponse;
    }

    @Override
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

    @Override
    public boolean isUserDuplicateAudioRequest(AudioRequestDetails audioRequestDetails) {

        var duplicateUserMediaRequests = mediaRequestRepository.findDuplicateUserMediaRequests(
              hearingRepository.getReferenceById(audioRequestDetails.getHearingId()),
              userAccountRepository.getReferenceById(audioRequestDetails.getRequestor()),
              audioRequestDetails.getStartTime(),
              audioRequestDetails.getEndTime(),
              audioRequestDetails.getRequestType(),
              List.of(OPEN, PROCESSING)
        );

        return duplicateUserMediaRequests.isPresent();
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
        NotificationApi.NotificationTemplate notificationTemplate;
        if (audioRequestBeingProcessedFromArchiveQuery.getResults(mediaRequest.getId())
              .isEmpty()) {
            notificationTemplate = AUDIO_REQUEST_PROCESSING;
        } else {
            notificationTemplate = AUDIO_REQUEST_PROCESSING_ARCHIVE;
        }

        try {
            var saveNotificationToDbRequest = SaveNotificationToDbRequest.builder()
                  .eventId(notificationTemplate.toString())
                  .caseId(mediaRequest.getHearing().getCourtCase().getId())
                  .emailAddresses(mediaRequest.getRequestor().getEmailAddress())
                  .build();
            notificationApi.scheduleNotification(saveNotificationToDbRequest);
        } catch (Exception e) {
            log.error("Unable to schedule media request [{}] pending notification [{}]", mediaRequest.getId(), notificationTemplate, e);
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
        TransformedMediaEntity transformedMedia = getTransformedMediaById(transformedMediaId);
        deleteTransientObjectDirectoryByTransformedMediaId(transformedMedia.getId());
        log.debug("deleting TransformedMediaEntity with id {}.", transformedMedia.getId());
        MediaRequestEntity mediaRequest = transformedMedia.getMediaRequest();
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
    public List<GetAudioRequestResponseV1> getAudioRequestsV1(Integer userId, Boolean expired) {
        List<GetAudioRequestResponseV1> response = new ArrayList<>();
        List<EnhancedMediaRequestInfo> enhancedMediaRequestInfoList = getEnhancedMediaRequestInfo(userId, expired);
        for (EnhancedMediaRequestInfo enhancedMediaRequestInfo : enhancedMediaRequestInfoList) {
            List<TransformedMediaEntity> transformedMediaList = transformedMediaRepository.findByMediaRequestId(enhancedMediaRequestInfo.getMediaRequestId());
            if (!transformedMediaList.isEmpty()) {
                TransformedMediaEntity transformedMedia = transformedMediaList.get(0);
                GetAudioRequestResponseV1 getAudioRequestResponseItem = GetAudioRequestResponseMapper.mapToAudioRequestSummary(
                      enhancedMediaRequestInfo,
                      transformedMedia
                );
                response.add(getAudioRequestResponseItem);
            }
        }
        return response;
    }

    @Override
    public GetAudioRequestResponse getAudioRequests(Integer userId, Boolean expired) {
        GetAudioRequestResponse response = new GetAudioRequestResponse();
        response.setTransformedMediaDetails(getTransformedMediaDetails(userId, expired));
        if (!expired) {
            //no need to get media requests for expired tab
            response.setMediaRequestDetails(getMediaRequestDetails(userId, expired));
        }
        return response;
    }


    private List<TransformedMediaDetails> getTransformedMediaDetails(Integer userId, Boolean expired) {
        List<TransformedMediaDetailsDto> transformedMediaDetailsDtoList = transformedMediaRepository.findTransformedMediaDetails(userId, expired);
        return transformedMediaDetailsMapper.mapToTransformedMediaDetails(transformedMediaDetailsDtoList);
    }

    private List<MediaRequestDetails> getMediaRequestDetails(Integer userId, Boolean expired) {
        List<EnhancedMediaRequestInfo> enhancedMediaRequestInfoList = getEnhancedMediaRequestInfo(userId, expired);
        return mediaRequestDetailsMapper.map(enhancedMediaRequestInfoList);
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
              expiredPredicate(expired, criteriaBuilder, mediaRequest),
              mediaRequest.get(MediaRequestEntity_.status).in(List.of(DELETED, COMPLETED)).not()
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
        TransformedMediaEntity foundEntity = getTransformedMediaById(transformedMediaId);
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
    public InputStream download(Integer transformedMediaId) {
        return downloadOrPlayback(transformedMediaId, AuditActivity.EXPORT_AUDIO, AudioRequestType.DOWNLOAD);
    }

    @Override
    public InputStream playback(Integer transformedMediaId) {
        return downloadOrPlayback(transformedMediaId, AuditActivity.AUDIO_PLAYBACK, AudioRequestType.PLAYBACK);
    }

    private InputStream downloadOrPlayback(Integer transformedMediaId, AuditActivity auditActivity, AudioRequestType expectedType) {
        final TransformedMediaEntity transformedMediaEntity = getTransformedMediaById(transformedMediaId);
        MediaRequestEntity mediaRequestEntity = transformedMediaEntity.getMediaRequest();
        validateMediaRequestType(mediaRequestEntity, expectedType);

        final UUID blobId = getBlobId(transformedMediaEntity);

        auditApi.recordAudit(
              auditActivity,
              this.getUserAccount(),
              mediaRequestEntity.getHearing().getCourtCase()
        );
        return dataManagementApi.getBlobDataFromOutboundContainer(blobId).toStream();
    }

    private UUID getBlobId(TransformedMediaEntity transformedMediaEntity) {
        final List<TransientObjectDirectoryEntity> transientObjectDirectoryEntities = transformedMediaEntity.getTransientObjectDirectoryEntities();
        if (transientObjectDirectoryEntities.isEmpty()) {
            throw new DartsApiException(AudioApiError.REQUESTED_DATA_CANNOT_BE_LOCATED);
        }

        var transientObjectEntity = transientObjectDirectoryEntities.stream()
              .filter(transientObjectDirectoryEntity -> STORED.getId().equals(transientObjectDirectoryEntity.getStatus().getId()))
              .findFirst()
              .orElseThrow(() -> new DartsApiException(AudioApiError.REQUESTED_DATA_CANNOT_BE_LOCATED));

        UUID blobId = transientObjectEntity.getExternalLocation();
        if (blobId == null) {
            throw new DartsApiException(AudioApiError.REQUESTED_DATA_CANNOT_BE_LOCATED);
        }
        return blobId;
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

    private TransformedMediaEntity getTransformedMediaById(Integer id) {
        return transformedMediaRepository.findById(id).orElseThrow(
              () -> new DartsApiException(AudioRequestsApiError.TRANSFORMED_MEDIA_NOT_FOUND));
    }

}
