package uk.gov.hmcts.darts.hearings.mapper;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.hearings.model.HearingsSearchResponse;
import uk.gov.hmcts.darts.hearings.model.HearingsSearchResponseCase;
import uk.gov.hmcts.darts.hearings.model.HearingsSearchResponseCourthouse;
import uk.gov.hmcts.darts.hearings.model.HearingsSearchResponseCourtroom;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class AdminHearingSearchResponseMapper {
    public List<HearingsSearchResponse> mapResponse(List<HearingEntity> hearingEntityList) {
        List<HearingsSearchResponse> hearingsSearchResponses = new ArrayList<>();
        hearingEntityList.forEach(e -> {
            final HearingsSearchResponse response = new HearingsSearchResponse();
            HearingsSearchResponseCase responseCase = new HearingsSearchResponseCase();
            responseCase.setId(e.getCourtCase().getId());
            responseCase.setCaseNumber(e.getCourtCase().getCaseNumber());

            HearingsSearchResponseCourthouse responseCourthouse = new HearingsSearchResponseCourthouse();
            responseCourthouse.setId(e.getCourtroom().getCourthouse().getId());
            responseCourthouse.setDisplayName(e.getCourtroom().getCourthouse().getDisplayName());

            HearingsSearchResponseCourtroom responseCourtroom = new HearingsSearchResponseCourtroom();
            responseCourtroom.setId(e.getCourtroom().getId());
            responseCourtroom.setName(e.getCourtroom().getName());

            response.setCase(responseCase);
            response.setCourthouse(responseCourthouse);
            response.setCourtroom(responseCourtroom);

            response.setId(e.getId());
            response.setHearingDate(e.getHearingDate());

            hearingsSearchResponses.add(response);
        });

        return hearingsSearchResponses;
    }
}