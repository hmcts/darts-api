package uk.gov.hmcts.darts.audio.mapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.audio.model.AdminActionResponse;
import uk.gov.hmcts.darts.audio.model.AdminMediaVersionResponse;
import uk.gov.hmcts.darts.audio.model.AdminVersionedMediaResponse;
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
import java.util.Optional;


@Slf4j
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class GetAdminMediaResponseMapper {

    private final CourtroomMapper courtroomMapper;
    private final CourthouseMapper courthouseMapper;

    public static List<GetAdminMediaResponseItem> createResponseItemList(List<MediaEntity> mediaEntities, HearingEntity hearing) {
        List<GetAdminMediaResponseItem> responseList = new ArrayList<>();
        for (MediaEntity mediaEntity : mediaEntities) {
            responseList.add(createResponseItem(mediaEntity, hearing));
        }
        return responseList;
    }

    public static GetAdminMediaResponseItem createResponseItem(MediaEntity mediaEntity, HearingEntity hearing) {
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

    private static GetAdminMediaResponseCase createResponseCase(CourtCaseEntity courtCaseEntity) {
        GetAdminMediaResponseCase responseCase = new GetAdminMediaResponseCase();
        responseCase.setId(courtCaseEntity.getId());
        responseCase.setCaseNumber(courtCaseEntity.getCaseNumber());
        return responseCase;
    }

    private static GetAdminMediaResponseHearing createResponseHearing(HearingEntity hearingEntity) {
        GetAdminMediaResponseHearing responseHearing = new GetAdminMediaResponseHearing();
        responseHearing.setId(hearingEntity.getId());
        responseHearing.setHearingDate(hearingEntity.getHearingDate());
        return responseHearing;
    }

    private static GetAdminMediaResponseCourthouse createResponseCourthouse(HearingEntity hearingEntity) {
        CourthouseEntity courthouse = hearingEntity.getCourtroom().getCourthouse();
        GetAdminMediaResponseCourthouse responseCourthouse = new GetAdminMediaResponseCourthouse();
        responseCourthouse.setId(courthouse.getId());
        responseCourthouse.setDisplayName(courthouse.getDisplayName());
        return responseCourthouse;
    }

    private static GetAdminMediaResponseCourtroom createResponseCourtroom(HearingEntity hearingEntity) {
        CourtroomEntity courtroom = hearingEntity.getCourtroom();
        GetAdminMediaResponseCourtroom responseCourthouse = new GetAdminMediaResponseCourtroom();
        responseCourthouse.setId(courtroom.getId());
        responseCourthouse.setDisplayName(courtroom.getName());
        return responseCourthouse;
    }

    public static MediaHideResponse mapHideOrShowResponse(MediaEntity entity, ObjectAdminActionEntity objectAdminActionEntity) {
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

    public static MediaApproveMarkedForDeletionResponse mapMediaApproveMarkedForDeletionResponse(MediaEntity entity,
                                                                                                 ObjectAdminActionEntity objectAdminActionEntity) {
        MediaApproveMarkedForDeletionResponse response = new MediaApproveMarkedForDeletionResponse();
        response.setId(entity.getId());
        response.setIsHidden(entity.isHidden());
        response.setIsDeleted(entity.isDeleted());

        if (objectAdminActionEntity != null) {
            response.setAdminAction(buildAdminActionResponse(objectAdminActionEntity));
        }

        return response;
    }

    public AdminVersionedMediaResponse mapAdminVersionedMediaResponse(MediaEntity mediaEntity, List<MediaEntity> mediaVersions) {
        AdminVersionedMediaResponse response = new AdminVersionedMediaResponse();
        response.setMediaObjectId(mediaEntity.getLegacyObjectId());
        response.setCurrentVersion(mapAdminMediaVersionResponse(mediaEntity));
        response.setPreviousVersions(
            mediaVersions.stream()
                .map(this::mapAdminMediaVersionResponse)
                .filter(adminMediaVersionResponse -> adminMediaVersionResponse != null)
                .toList()
        );
        return response;
    }

    AdminMediaVersionResponse mapAdminMediaVersionResponse(MediaEntity mediaEntity) {
        if (mediaEntity == null) {
            return null;
        }
        AdminMediaVersionResponse response = new AdminMediaVersionResponse();
        response.setId(mediaEntity.getId());
        response.setCourtroom(courtroomMapper.toApiModel(mediaEntity.getCourtroom()));
        response.setCourthouse(courthouseMapper.toApiModel(
            Optional.ofNullable(mediaEntity.getCourtroom())
                .map(courtroomEntity -> courtroomEntity.getCourthouse())
                .orElse(null)));
        response.setStartAt(mediaEntity.getStart());
        response.setEndAt(mediaEntity.getEnd());
        response.setChannel(mediaEntity.getChannel());
        response.setChronicleId(mediaEntity.getChronicleId());
        response.setAntecedentId(mediaEntity.getAntecedentId());
        response.setIsCurrent(mediaEntity.getIsCurrent());
        response.setCreatedAt(mediaEntity.getCreatedDateTime());
        return response;
    }
}