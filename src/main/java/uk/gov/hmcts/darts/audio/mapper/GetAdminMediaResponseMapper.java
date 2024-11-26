package uk.gov.hmcts.darts.audio.mapper;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.darts.audio.model.AdminActionResponse;
import uk.gov.hmcts.darts.audio.model.GetAdminMediaResponseCase;
import uk.gov.hmcts.darts.audio.model.GetAdminMediaResponseCourthouse;
import uk.gov.hmcts.darts.audio.model.GetAdminMediaResponseCourtroom;
import uk.gov.hmcts.darts.audio.model.GetAdminMediaResponseHearing;
import uk.gov.hmcts.darts.audio.model.GetAdminMediaResponseItem;
import uk.gov.hmcts.darts.audio.model.MediaApproveMarkedForDeletionResponse;
import uk.gov.hmcts.darts.audio.model.MediaHideResponse;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectAdminActionEntity;

import java.util.ArrayList;
import java.util.List;


@Slf4j
@UtilityClass
public class GetAdminMediaResponseMapper {

    public List<GetAdminMediaResponseItem> createResponseItemList(List<MediaEntity> mediaEntities, HearingEntity hearing) {
        List<GetAdminMediaResponseItem> responseList = new ArrayList<>();
        for (MediaEntity mediaEntity : mediaEntities) {
            responseList.add(createResponseItem(mediaEntity, hearing));
        }
        return responseList;
    }

    public GetAdminMediaResponseItem createResponseItem(MediaEntity mediaEntity, HearingEntity hearing) {
        GetAdminMediaResponseItem responseItem = new GetAdminMediaResponseItem();
        responseItem.setId(mediaEntity.getId());
        responseItem.setChannel(mediaEntity.getChannel());
        responseItem.setStartAt(mediaEntity.getStart());
        responseItem.setEndAt(mediaEntity.getEnd());
        responseItem.setIsHidden(mediaEntity.isHidden());
        responseItem.setIsCurrent(mediaEntity.getIsCurrent());
        responseItem.setCase(createResponseCase(hearing.getCourtCase()));
        responseItem.setHearing(createResponseHearing(hearing));
        responseItem.setCourthouse(createResponseCourthouse(hearing));
        responseItem.setCourtroom(createResponseCourtroom(hearing));
        return responseItem;
    }

    private GetAdminMediaResponseCase createResponseCase(CourtCaseEntity courtCaseEntity) {
        GetAdminMediaResponseCase responseCase = new GetAdminMediaResponseCase();
        responseCase.setId(courtCaseEntity.getId());
        responseCase.setCaseNumber(courtCaseEntity.getCaseNumber());
        return responseCase;
    }

    private GetAdminMediaResponseHearing createResponseHearing(HearingEntity hearingEntity) {
        GetAdminMediaResponseHearing responseHearing = new GetAdminMediaResponseHearing();
        responseHearing.setId(hearingEntity.getId());
        responseHearing.setHearingDate(hearingEntity.getHearingDate());
        return responseHearing;
    }

    private GetAdminMediaResponseCourthouse createResponseCourthouse(HearingEntity hearingEntity) {
        CourthouseEntity courthouse = hearingEntity.getCourtroom().getCourthouse();
        GetAdminMediaResponseCourthouse responseCourthouse = new GetAdminMediaResponseCourthouse();
        responseCourthouse.setId(courthouse.getId());
        responseCourthouse.setDisplayName(courthouse.getDisplayName());
        return responseCourthouse;
    }

    private GetAdminMediaResponseCourtroom createResponseCourtroom(HearingEntity hearingEntity) {
        CourtroomEntity courtroom = hearingEntity.getCourtroom();
        GetAdminMediaResponseCourtroom responseCourthouse = new GetAdminMediaResponseCourtroom();
        responseCourthouse.setId(courtroom.getId());
        responseCourthouse.setDisplayName(courtroom.getName());
        return responseCourthouse;
    }

    public MediaHideResponse mapHideOrShowResponse(MediaEntity entity, ObjectAdminActionEntity objectAdminActionEntity) {
        MediaHideResponse response = new MediaHideResponse();
        response.setId(entity.getId());
        response.setIsHidden(entity.isHidden());
        response.setIsDeleted(entity.isDeleted());

        if (objectAdminActionEntity != null) {
            response.setAdminAction(buildAdminActionResponse(objectAdminActionEntity));
        }

        return response;
    }

    private static AdminActionResponse buildAdminActionResponse(ObjectAdminActionEntity objectAdminActionEntity) {
        AdminActionResponse aaResponse = new AdminActionResponse();
        aaResponse.setId(objectAdminActionEntity.getId());
        aaResponse.setReasonId(objectAdminActionEntity.getObjectHiddenReason().getId());
        aaResponse.setHiddenById(objectAdminActionEntity.getHiddenBy().getId());
        aaResponse.setHiddenAt(objectAdminActionEntity.getHiddenDateTime());
        aaResponse.setIsMarkedForManualDeletion(objectAdminActionEntity.isMarkedForManualDeletion());
        aaResponse.setMarkedForManualDeletionById(
            objectAdminActionEntity.getMarkedForManualDelBy() == null ? null : objectAdminActionEntity.getMarkedForManualDelBy().getId());
        aaResponse.setMarkedForManualDeletionAt(
            objectAdminActionEntity.getMarkedForManualDelDateTime() == null ? null : objectAdminActionEntity.getMarkedForManualDelDateTime());
        aaResponse.setTicketReference(objectAdminActionEntity.getTicketReference());
        aaResponse.setComments(objectAdminActionEntity.getComments());
        return aaResponse;
    }

    public MediaApproveMarkedForDeletionResponse mapMediaApproveMarkedForDeletionResponse(MediaEntity entity, ObjectAdminActionEntity objectAdminActionEntity) {
        MediaApproveMarkedForDeletionResponse response = new MediaApproveMarkedForDeletionResponse();
        response.setId(entity.getId());
        response.setIsHidden(entity.isHidden());
        response.setIsDeleted(entity.isDeleted());

        if (objectAdminActionEntity != null) {
            response.setAdminAction(buildAdminActionResponse(objectAdminActionEntity));
        }

        return response;
    }
}