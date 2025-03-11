package uk.gov.hmcts.darts.hearings.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.hearings.exception.HearingApiError;
import uk.gov.hmcts.darts.hearings.mapper.AdminHearingMapper;
import uk.gov.hmcts.darts.hearings.mapper.AdminHearingSearchResponseMapper;
import uk.gov.hmcts.darts.hearings.model.HearingsAudiosResponseInner;
import uk.gov.hmcts.darts.hearings.model.HearingsResponse;
import uk.gov.hmcts.darts.hearings.model.HearingsSearchRequest;
import uk.gov.hmcts.darts.hearings.model.HearingsSearchResponse;
import uk.gov.hmcts.darts.hearings.service.AdminHearingsService;
import uk.gov.hmcts.darts.hearings.service.HearingsService;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminHearingsServiceImpl implements AdminHearingsService {
    private final HearingRepository hearingRepository;
    private final HearingsService hearingsService;
    private final MediaRepository mediaRepository;

    @Value("${darts.hearings.admin-search.max-results}")
    private Integer adminSearchMaxResults;

    @Override
    public List<HearingsSearchResponse> adminHearingSearch(HearingsSearchRequest request) {
        if (CollectionUtils.isEmpty(request.getCourthouseIds())) {
            request.setCourthouseIds(null);
        }

        List<HearingEntity> hearingEntityList = hearingRepository
            .findHearingDetails(request.getCourthouseIds(), request.getCaseNumber(),
                                request.getCourtroomName(),
                                request.getHearingStartAt(), request.getHearingEndAt(),
                                adminSearchMaxResults + 1);

        if (hearingEntityList.size() > adminSearchMaxResults) {
            throw new DartsApiException(HearingApiError.TOO_MANY_RESULTS);
        }

        return AdminHearingSearchResponseMapper.mapResponse(hearingEntityList);
    }


    Integer getAdminSearchMaxResults() {
        return adminSearchMaxResults;
    }

    void setAdminSearchMaxResults(Integer adminSearchMaxResults) {
        this.adminSearchMaxResults = adminSearchMaxResults;
    }

    @Override
    public HearingsResponse getAdminHearings(Integer hearingId) {
        return AdminHearingMapper.mapToHearingsResponse(hearingsService.getHearingById(hearingId));
    }

    @Override
    public List<HearingsAudiosResponseInner> getHearingAudios(Integer hearingId) {
        hearingsService.validateHearingExistsElseError(hearingId);
        return mediaRepository.findAllCurrentMediaByHearingId(hearingId)
            .stream()
            .map(media -> {
                HearingsAudiosResponseInner hearingsAudiosResponseInner = new HearingsAudiosResponseInner();
                hearingsAudiosResponseInner.id(media.getId());
                hearingsAudiosResponseInner.startAt(media.getStart());
                hearingsAudiosResponseInner.endAt(media.getEnd());
                hearingsAudiosResponseInner.filename(media.getMediaFile());
                hearingsAudiosResponseInner.channel(media.getChannel());
                hearingsAudiosResponseInner.totalChannels(media.getTotalChannels());
                return hearingsAudiosResponseInner;
            }).toList();
    }
}