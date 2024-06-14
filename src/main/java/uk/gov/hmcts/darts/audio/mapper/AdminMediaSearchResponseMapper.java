package uk.gov.hmcts.darts.audio.mapper;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.darts.audio.model.AdminMediaSearchResponseCase;
import uk.gov.hmcts.darts.audio.model.AdminMediaSearchResponseCourthouse;
import uk.gov.hmcts.darts.audio.model.AdminMediaSearchResponseCourtroom;
import uk.gov.hmcts.darts.audio.model.AdminMediaSearchResponseHearing;
import uk.gov.hmcts.darts.audio.model.AdminMediaSearchResponseItem;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@UtilityClass
public class AdminMediaSearchResponseMapper {

    public List<AdminMediaSearchResponseItem> createResponseItemList(List<MediaEntity> mediaEntities, HearingEntity hearing) {
        List<AdminMediaSearchResponseItem> responseList = new ArrayList<>();
        for (MediaEntity mediaEntity : mediaEntities) {
            responseList.add(createResponseItem(mediaEntity, hearing));
        }
        return responseList;
    }

    private AdminMediaSearchResponseItem createResponseItem(MediaEntity mediaEntity, HearingEntity hearing) {
        AdminMediaSearchResponseItem responseItem = new AdminMediaSearchResponseItem();
        responseItem.setId(mediaEntity.getId());
        responseItem.setChannel(mediaEntity.getChannel());
        responseItem.setStartAt(mediaEntity.getStart());
        responseItem.setEndAt(mediaEntity.getEnd());
        responseItem.setCase(createResponseCase(hearing.getCourtCase()));
        responseItem.setHearing(createResponseHearing(hearing));
        responseItem.setCourthouse(createResponseCourthouse(hearing));
        responseItem.setCourtroom(createResponseCourtroom(hearing));
        return responseItem;
    }

    private AdminMediaSearchResponseCase createResponseCase(CourtCaseEntity courtCaseEntity) {
        AdminMediaSearchResponseCase responseCase = new AdminMediaSearchResponseCase();
        responseCase.setId(courtCaseEntity.getId());
        responseCase.setCaseNumber(courtCaseEntity.getCaseNumber());
        return responseCase;
    }

    private AdminMediaSearchResponseHearing createResponseHearing(HearingEntity hearingEntity) {
        AdminMediaSearchResponseHearing responseHearing = new AdminMediaSearchResponseHearing();
        responseHearing.setId(hearingEntity.getId());
        responseHearing.setHearingDate(hearingEntity.getHearingDate());
        return responseHearing;
    }

    private AdminMediaSearchResponseCourthouse createResponseCourthouse(HearingEntity hearingEntity) {
        CourthouseEntity courthouse = hearingEntity.getCourtroom().getCourthouse();
        AdminMediaSearchResponseCourthouse responseCourthouse = new AdminMediaSearchResponseCourthouse();
        responseCourthouse.setId(courthouse.getId());
        responseCourthouse.setDisplayName(courthouse.getDisplayName());
        return responseCourthouse;
    }

    private AdminMediaSearchResponseCourtroom createResponseCourtroom(HearingEntity hearingEntity) {
        CourtroomEntity courtroom = hearingEntity.getCourtroom();
        AdminMediaSearchResponseCourtroom responseCourthouse = new AdminMediaSearchResponseCourtroom();
        responseCourthouse.setId(courtroom.getId());
        responseCourthouse.setDisplayName(courtroom.getName());
        return responseCourthouse;
    }


}
