package uk.gov.hmcts.darts.audio.service.impl;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.exception.AudioApiError;
import uk.gov.hmcts.darts.audio.helper.PostAdminMediasSearchHelper;
import uk.gov.hmcts.darts.audio.mapper.AdminMarkedForDeletionMapper;
import uk.gov.hmcts.darts.audio.mapper.AdminMediaMapper;
import uk.gov.hmcts.darts.audio.mapper.GetAdminMediaResponseMapper;
import uk.gov.hmcts.darts.audio.mapper.PostAdminMediaSearchResponseMapper;
import uk.gov.hmcts.darts.audio.model.AdminMediaResponse;
import uk.gov.hmcts.darts.audio.model.GetAdminMediaResponseItem;
import uk.gov.hmcts.darts.audio.model.MediaApproveMarkedForDeletionResponse;
import uk.gov.hmcts.darts.audio.model.MediaSearchData;
import uk.gov.hmcts.darts.audio.model.PostAdminMediasMarkedForDeletionItem;
import uk.gov.hmcts.darts.audio.model.PostAdminMediasSearchRequest;
import uk.gov.hmcts.darts.audio.model.PostAdminMediasSearchResponseItem;
import uk.gov.hmcts.darts.audio.service.AdminMediaService;
import uk.gov.hmcts.darts.audio.validation.MediaApproveMarkForDeletionValidator;
import uk.gov.hmcts.darts.audio.validation.SearchMediaValidator;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectAdminActionEntity;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;
import uk.gov.hmcts.darts.common.exception.CommonApiError;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.ObjectAdminActionRepository;
import uk.gov.hmcts.darts.common.repository.TransformedMediaRepository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdminMediaServiceImpl implements AdminMediaService {

    private final MediaRepository mediaRepository;
    private final AdminMediaMapper adminMediaMapper;
    private final AdminMarkedForDeletionMapper adminMarkedForDeletionMapper;
    private final PostAdminMediasSearchHelper postAdminMediasSearchHelper;
    private final SearchMediaValidator searchMediaValidator;
    private final TransformedMediaRepository transformedMediaRepository;
    private final ObjectAdminActionRepository objectAdminActionRepository;
    private final MediaApproveMarkForDeletionValidator mediaApproveMarkForDeletionValidator;
    private final UserIdentity userIdentity;
    private final CurrentTimeHelper currentTimeHelper;

    @Value("${darts.audio.admin-search.max-results}")
    private Integer adminSearchMaxResults;
    @Value("${darts.manual-deletion.enabled:false}")
    @Getter(AccessLevel.PACKAGE)
    private boolean manualDeletionEnabled;

    public AdminMediaResponse getMediasById(Integer id) {
        var mediaEntity = mediaRepository.findByIdIncludeDeleted(id)
            .orElseThrow(() -> new DartsApiException(AudioApiError.MEDIA_NOT_FOUND));

        return adminMediaMapper.toApiModel(mediaEntity);
    }

    @Override
    public List<PostAdminMediasSearchResponseItem> performAdminMediasSearchPost(PostAdminMediasSearchRequest adminMediasSearchRequest) {
        List<MediaEntity> matchingMedia = postAdminMediasSearchHelper.getMatchingMedia(adminMediasSearchRequest);
        if (matchingMedia.size() > adminSearchMaxResults) {
            throw new DartsApiException(AudioApiError.TOO_MANY_RESULTS);
        }
        return PostAdminMediaSearchResponseMapper.createResponseItemList(matchingMedia);
    }

    @Override
    public List<GetAdminMediaResponseItem> filterMedias(Integer transformedMediaId, List<Integer> hearingIds, OffsetDateTime startAt,
                                                        OffsetDateTime endAt) {
        MediaSearchData searchData = new MediaSearchData(transformedMediaId, hearingIds, startAt, endAt);
        searchMediaValidator.validate(searchData);

        if (transformedMediaId != null) {
            Optional<TransformedMediaEntity> transformedMediaOpt = transformedMediaRepository.findById(transformedMediaId);

            if (transformedMediaOpt.isEmpty()) {
                return new ArrayList<>();
            }

            TransformedMediaEntity transformedMedia = transformedMediaOpt.get();
            MediaRequestEntity mediaRequest = transformedMedia.getMediaRequest();
            HearingEntity hearing = mediaRequest.getHearing();
            List<MediaEntity> mediaList = mediaRepository.findAllByHearingId(hearing.getId());
            List<MediaEntity> filteredMediaList = mediaList.stream().filter(mediaEntity -> mediaEntity.getStart().isBefore(mediaRequest.getEndTime())
                && mediaEntity.getEnd().isAfter(mediaRequest.getStartTime())).toList();
            return GetAdminMediaResponseMapper.createResponseItemList(filteredMediaList, hearing);
        } else {
            final List<GetAdminMediaResponseItem> responseMediaItemList = new ArrayList<>();
            List<MediaEntity> mediaList = mediaRepository.findMediaByDetails(hearingIds, startAt, endAt);

            mediaList.forEach(mediaEntity -> {
                List<HearingEntity> hearingEntityList = getApplicableMediaHearings(mediaEntity, hearingIds);

                hearingEntityList.forEach(hearing -> {
                    responseMediaItemList.add(GetAdminMediaResponseMapper.createResponseItem(mediaEntity, hearing));
                });
            });

            return responseMediaItemList;
        }
    }

    @Override
    public List<PostAdminMediasMarkedForDeletionItem> getMediasMarkedForDeletion() {
        if (!this.isManualDeletionEnabled()) {
            throw new DartsApiException(CommonApiError.FEATURE_FLAG_NOT_ENABLED, "Manual deletion is not enabled");
        }

        return objectAdminActionRepository.findAllMediaActionsWithAnyDeletionReason().stream()
            .map(ObjectAdminActionEntity::getMedia)
            .map(adminMarkedForDeletionMapper::toApiModel)
            .filter(media -> media != null)
            .sorted(Comparator.comparing(PostAdminMediasMarkedForDeletionItem::getMediaId))
            .toList();
    }

    private List<HearingEntity> getApplicableMediaHearings(MediaEntity mediaEntity, List<Integer> hearingsToMatchOn) {
        List<HearingEntity> hearingEntityList = CollectionUtils.isEmpty(hearingsToMatchOn) ? mediaEntity.getHearingList() : new ArrayList<>();

        if (!CollectionUtils.isEmpty(hearingsToMatchOn)) {
            for (HearingEntity hearingEntity : mediaEntity.getHearingList()) {
                if (hearingsToMatchOn.contains(hearingEntity.getId())) {
                    hearingEntityList.add(hearingEntity);
                }
            }
        }

        return hearingEntityList;
    }

    @Override
    @Transactional
    public MediaApproveMarkedForDeletionResponse adminApproveMediaMarkedForDeletion(Integer mediaId) {
        if (!this.isManualDeletionEnabled()) {
            throw new DartsApiException(CommonApiError.FEATURE_FLAG_NOT_ENABLED, "Manual deletion is not enabled");
        }

        mediaApproveMarkForDeletionValidator.validate(mediaId);
        List<ObjectAdminActionEntity> objectAdminActionEntityList = objectAdminActionRepository.findByMedia_Id(mediaId);

        Optional<MediaEntity> mediaEntityOptional = mediaRepository.findByIdIncludeDeleted(mediaId);
        if (mediaEntityOptional.isEmpty()) {
            throw new DartsApiException(AudioApiError.MEDIA_NOT_FOUND);
        }
        MediaEntity mediaEntity = mediaEntityOptional.get();
        var currentUser = userIdentity.getUserAccount();
        var objectAdminActionEntity = objectAdminActionEntityList.getFirst();
        objectAdminActionEntity.setMarkedForManualDeletion(true);
        objectAdminActionEntity.setMarkedForManualDelBy(currentUser);
        objectAdminActionEntity.setMarkedForManualDelDateTime(currentTimeHelper.currentOffsetDateTime());
        objectAdminActionRepository.save(objectAdminActionEntity);

        return GetAdminMediaResponseMapper.mapMediaApproveMarkedForDeletionResponse(mediaEntity, objectAdminActionEntity);
    }
}