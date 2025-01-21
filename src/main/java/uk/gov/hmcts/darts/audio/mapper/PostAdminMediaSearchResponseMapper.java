package uk.gov.hmcts.darts.audio.mapper;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.darts.audio.model.AdminMediaCourthouseResponse;
import uk.gov.hmcts.darts.audio.model.AdminMediaCourtroomResponse;
import uk.gov.hmcts.darts.audio.model.PostAdminMediasSearchResponseItem;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;

import java.util.ArrayList;
import java.util.List;


@Slf4j
@UtilityClass
public class PostAdminMediaSearchResponseMapper {

    public List<PostAdminMediasSearchResponseItem> createResponseItemList(List<MediaEntity> mediaEntities) {
        List<PostAdminMediasSearchResponseItem> responseList = new ArrayList<>();
        for (MediaEntity mediaEntity : mediaEntities) {
            responseList.add(createResponseItem(mediaEntity));
        }
        return responseList;
    }

    private PostAdminMediasSearchResponseItem createResponseItem(MediaEntity mediaEntity) {
        PostAdminMediasSearchResponseItem responseItem = new PostAdminMediasSearchResponseItem();
        responseItem.setId(mediaEntity.getId());
        responseItem.setCourthouse(createCourthouse(mediaEntity.getCourtroom().getCourthouse()));
        responseItem.setCourtroom(createCourtroom(mediaEntity.getCourtroom()));
        responseItem.setStartAt(mediaEntity.getStart());
        responseItem.setEndAt(mediaEntity.getEnd());
        responseItem.setChannel(mediaEntity.getChannel());
        responseItem.setIsHidden(mediaEntity.isHidden());
        return responseItem;
    }

    private AdminMediaCourthouseResponse createCourthouse(CourthouseEntity courthouse) {
        AdminMediaCourthouseResponse responseCourthouse = new AdminMediaCourthouseResponse();
        responseCourthouse.setId(courthouse.getId());
        responseCourthouse.setDisplayName(courthouse.getDisplayName());
        return responseCourthouse;
    }

    private AdminMediaCourtroomResponse createCourtroom(CourtroomEntity courtroomEntity) {
        AdminMediaCourtroomResponse courtroomResponseObject = new AdminMediaCourtroomResponse();
        courtroomResponseObject.setId(courtroomEntity.getId());
        courtroomResponseObject.setName(courtroomEntity.getName());
        return courtroomResponseObject;
    }
}