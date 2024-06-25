package uk.gov.hmcts.darts.audio.service.impl;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.exception.AudioApiError;
import uk.gov.hmcts.darts.audio.mapper.AdminMediaMapper;
import uk.gov.hmcts.darts.audio.mapper.AdminMediaSearchResponseMapper;
import uk.gov.hmcts.darts.audio.model.AdminMediaResponse;
import uk.gov.hmcts.darts.audio.model.AdminMediaSearchResponseItem;
import uk.gov.hmcts.darts.audio.model.MediaSearchData;
import uk.gov.hmcts.darts.audio.service.AdminMediaService;
import uk.gov.hmcts.darts.audio.validation.SearchMediaValidator;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.TransformedMediaRepository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdminMediaServiceImpl implements AdminMediaService {

    private final MediaRepository mediaRepository;
    private final AdminMediaMapper adminMediaMapper;
    private final SearchMediaValidator searchMediaValidator;
    private final TransformedMediaRepository transformedMediaRepository;

    public AdminMediaResponse getMediasById(Integer id) {
        var mediaEntity = mediaRepository.findById(id)
            .orElseThrow(() -> new DartsApiException(AudioApiError.MEDIA_NOT_FOUND));

        return adminMediaMapper.toApiModel(mediaEntity);
    }

    @Override
    public List<AdminMediaSearchResponseItem> filterMedias(Integer transformedMediaId, List<Integer> hearingIds, OffsetDateTime startAt,
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
            return AdminMediaSearchResponseMapper.createResponseItemList(filteredMediaList, hearing);
        } else {
            final List<AdminMediaSearchResponseItem> responseMediaItemList = new ArrayList<>();
            List<MediaEntity> mediaList = mediaRepository.findMediaByDetails(hearingIds, startAt, endAt);

            mediaList.forEach(mediaEntity -> {
               List<HearingEntity> hearingEntityList = getApplicableMediaHearings(mediaEntity, hearingIds);

               hearingEntityList.forEach(hearing -> {
                   responseMediaItemList.add(AdminMediaSearchResponseMapper.createResponseItem(mediaEntity, hearing));
               });
            });

            return responseMediaItemList;
        }
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
}