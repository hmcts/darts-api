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
import uk.gov.hmcts.darts.audio.mapper.CourthouseMapper;
import uk.gov.hmcts.darts.audio.mapper.CourtroomMapper;
import uk.gov.hmcts.darts.audio.mapper.GetAdminMediaResponseMapper;
import uk.gov.hmcts.darts.audio.mapper.ObjectActionMapper;
import uk.gov.hmcts.darts.audio.mapper.PostAdminMediaSearchResponseMapper;
import uk.gov.hmcts.darts.audio.model.AdminMediaResponse;
import uk.gov.hmcts.darts.audio.model.GetAdminMediaResponseItem;
import uk.gov.hmcts.darts.audio.model.GetAdminMediasMarkedForDeletionAdminAction;
import uk.gov.hmcts.darts.audio.model.GetAdminMediasMarkedForDeletionItem;
import uk.gov.hmcts.darts.audio.model.GetAdminMediasMarkedForDeletionMediaItem;
import uk.gov.hmcts.darts.audio.model.MediaApproveMarkedForDeletionResponse;
import uk.gov.hmcts.darts.audio.model.MediaSearchData;
import uk.gov.hmcts.darts.audio.model.PostAdminMediasSearchRequest;
import uk.gov.hmcts.darts.audio.model.PostAdminMediasSearchResponseItem;
import uk.gov.hmcts.darts.audio.service.AdminMediaService;
import uk.gov.hmcts.darts.audio.validation.MediaApproveMarkForDeletionValidator;
import uk.gov.hmcts.darts.audio.validation.SearchMediaValidator;
import uk.gov.hmcts.darts.audit.api.AuditActivity;
import uk.gov.hmcts.darts.audit.api.AuditApi;
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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminMediaServiceImpl implements AdminMediaService {

    private final MediaRepository mediaRepository;
    private final AdminMediaMapper adminMediaMapper;
    private final PostAdminMediasSearchHelper postAdminMediasSearchHelper;
    private final SearchMediaValidator searchMediaValidator;
    private final TransformedMediaRepository transformedMediaRepository;
    private final ObjectAdminActionRepository objectAdminActionRepository;
    private final MediaApproveMarkForDeletionValidator mediaApproveMarkForDeletionValidator;
    private final UserIdentity userIdentity;
    private final CurrentTimeHelper currentTimeHelper;
    private final AuditApi auditApi;
    private final AdminMarkedForDeletionMapper adminMarkedForDeletionMapper;

    private final CourthouseMapper courthouseMapper;
    private final CourtroomMapper courtroomMapper;
    private final ObjectActionMapper objectActionMapper;


    @Value("${darts.audio.admin-search.max-results}")
    private Integer adminSearchMaxResults;
    @Value("${darts.manual-deletion.enabled:false}")
    @Getter(AccessLevel.PACKAGE)
    private boolean manualDeletionEnabled;

    @Override
    public AdminMediaResponse getMediasById(Integer id) {
        var mediaEntity = mediaRepository.findById(id)
            .orElseThrow(() -> new DartsApiException(AudioApiError.MEDIA_NOT_FOUND));

        AdminMediaResponse adminMediaResponse = adminMediaMapper.toApiModel(mediaEntity);
        adminMediaResponse.getCases().sort((o1, o2) -> o2.getCaseNumber().compareTo(o1.getCaseNumber()));
        adminMediaResponse.getHearings().sort((o1, o2) -> o2.getCaseNumber().compareTo(o1.getCaseNumber()));
        return adminMediaResponse;
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
        }
        final List<GetAdminMediaResponseItem> responseMediaItemList = new ArrayList<>();
        List<MediaEntity> mediaList = mediaRepository.findMediaByDetails(hearingIds, startAt, endAt);

        mediaList.forEach(mediaEntity -> {
            List<HearingEntity> hearingEntityList = getApplicableMediaHearings(mediaEntity, hearingIds);

            hearingEntityList.forEach(hearing -> {
                responseMediaItemList.add(GetAdminMediaResponseMapper.createResponseItem(mediaEntity, hearing));
            });
        });

        Set<Integer> uniqueIds = new HashSet<>();
        return responseMediaItemList.stream()
            .filter(item -> uniqueIds.add(item.getId()))
            .sorted((o1, o2) -> o2.getCase().getCaseNumber().compareTo(o1.getCase().getCaseNumber()))
            .collect(Collectors.toList());
    }

    @Override
    public List<GetAdminMediasMarkedForDeletionItem> getMediasMarkedForDeletion() {
        if (!this.isManualDeletionEnabled()) {
            throw new DartsApiException(CommonApiError.FEATURE_FLAG_NOT_ENABLED, "Manual deletion is not enabled");
        }

        return objectAdminActionRepository.findAllMediaActionsWithAnyDeletionReason()
            .stream()
            .collect(Collectors.groupingBy(object -> {
                MediaEntity media = object.getMedia();
                return object.getTicketReference() + "-" + object.getHiddenBy().getId() + "-" + object.getObjectHiddenReason().getId()
                    + "-" + media.getCourtroom().getId() + "-" + media.getStart() + "-" + media.getEnd();
            }))
            .values()
            .stream()
            .filter(objectAdminActionEntities -> !objectAdminActionEntities.isEmpty())
            .map(actions -> toGetAdminMediasMarkedForDeletionItem(actions))
            .peek(getAdminMediasMarkedForDeletionItem -> {
                //We need to add the Media Entities to a List that supports sorting as the default one from Hibernate does not
                List<GetAdminMediasMarkedForDeletionMediaItem> mediaEntities = new ArrayList<>(getAdminMediasMarkedForDeletionItem.getMedia());
                mediaEntities.sort((o1, o2) -> o1.getChannel().compareTo(o2.getChannel()));
                getAdminMediasMarkedForDeletionItem.setMedia(mediaEntities);
            }).toList();
    }

    GetAdminMediasMarkedForDeletionItem toGetAdminMediasMarkedForDeletionItem(List<ObjectAdminActionEntity> actions) {
        ObjectAdminActionEntity base = actions.get(0);
        List<GetAdminMediasMarkedForDeletionMediaItem> media = actions.stream()
            .map(action -> action.getMedia())
            .map(mediaEntity -> {
                GetAdminMediasMarkedForDeletionMediaItem item = adminMarkedForDeletionMapper.toGetAdminMediasMarkedForDeletionMediaItem(mediaEntity);
                item.setVersionCount(mediaRepository.getVersionCount(mediaEntity.getChronicleId()));
                return item;
            })
            .toList();

        GetAdminMediasMarkedForDeletionItem item = new GetAdminMediasMarkedForDeletionItem();
        item.setMedia(media);
        item.setStartAt(base.getMedia().getStart());
        item.setEndAt(base.getMedia().getEnd());
        item.setCourtroom(courtroomMapper.toApiModel(base.getMedia().getCourtroom()));
        item.setCourthouse(courthouseMapper.toApiModel(base.getMedia().getCourtroom().getCourthouse()));
        GetAdminMediasMarkedForDeletionAdminAction adminAction = objectActionMapper.toGetAdminMediasMarkedForDeletionAdminAction(base);
        adminAction.setComments(actions.stream().map(ObjectAdminActionEntity::getComments).toList());
        item.setAdminAction(adminAction);
        return item;
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

        auditApi.record(AuditActivity.MANUAL_DELETION, currentUser, objectAdminActionEntity.getId().toString());

        return GetAdminMediaResponseMapper.mapMediaApproveMarkedForDeletionResponse(mediaEntity, objectAdminActionEntity);
    }
}