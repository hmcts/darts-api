package uk.gov.hmcts.darts.audio.mapper;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.audio.model.MediaHideResponse;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectAdminActionEntity;
import uk.gov.hmcts.darts.common.entity.ObjectHiddenReasonEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MediaResponseMapperTest {

    @Test
    void mapHideResponse() {
        Long mediaId = 100L;
        boolean hide = true;
        boolean deleted = true;

        MediaEntity mediaEntity = new MediaEntity();
        mediaEntity.setId(mediaId);
        mediaEntity.setHidden(hide);
        mediaEntity.setDeleted(deleted);

        Integer objectAdminActionId = 101;
        String comments = "comments";
        String reference = "reference";

        UserAccountEntity userAccountEntity = new UserAccountEntity();

        ObjectHiddenReasonEntity reasonEntity = mock(ObjectHiddenReasonEntity.class);
        when(reasonEntity.getId()).thenReturn(2332);

        OffsetDateTime creationDate = OffsetDateTime.now();
        ObjectAdminActionEntity objectAdminActionEntity = new ObjectAdminActionEntity();
        objectAdminActionEntity.setId(objectAdminActionId);
        objectAdminActionEntity.setComments(comments);
        objectAdminActionEntity.setTicketReference(reference);
        objectAdminActionEntity.setId(objectAdminActionId);
        objectAdminActionEntity.setHiddenBy(userAccountEntity);
        objectAdminActionEntity.setHiddenDateTime(creationDate);
        objectAdminActionEntity.setMarkedForManualDelBy(userAccountEntity);
        objectAdminActionEntity.setMarkedForManualDelDateTime(creationDate);
        objectAdminActionEntity.setObjectHiddenReason(reasonEntity);

        MediaHideResponse response = GetAdminMediaResponseMapper.mapHideOrShowResponse(mediaEntity, objectAdminActionEntity);

        assertEquals(response.getId(), mediaEntity.getId());
        assertEquals(response.getIsHidden(), mediaEntity.isHidden());
        assertEquals(response.getIsDeleted(), mediaEntity.isHidden());
        assertEquals(response.getAdminAction().getReasonId(), objectAdminActionEntity.getObjectHiddenReason().getId());
        assertEquals(response.getAdminAction().getComments(), objectAdminActionEntity.getComments());
        assertEquals(response.getAdminAction().getTicketReference(), objectAdminActionEntity.getTicketReference());
        assertEquals(response.getAdminAction().getId(), objectAdminActionEntity.getId());
        assertEquals(response.getAdminAction().getHiddenAt(), objectAdminActionEntity.getHiddenDateTime());
        assertEquals(response.getAdminAction().getHiddenById(), objectAdminActionEntity.getHiddenBy().getId());
        assertEquals(response.getAdminAction().getHiddenById(), userAccountEntity.getId());
        assertEquals(response.getAdminAction().getMarkedForManualDeletionById(), objectAdminActionEntity.getMarkedForManualDelBy().getId());
        assertEquals(response.getAdminAction().getMarkedForManualDeletionAt(), objectAdminActionEntity.getMarkedForManualDelDateTime());
        assertEquals(response.getAdminAction().getIsMarkedForManualDeletion(), objectAdminActionEntity.isMarkedForManualDeletion());
    }

    @Test
    void mapShowResponseWithNoObjectAdminAction() {
        Long mediaId = 100L;
        boolean hide = true;

        MediaEntity mediaEntity = new MediaEntity();
        mediaEntity.setId(mediaId);
        mediaEntity.setHidden(hide);

        MediaHideResponse response = GetAdminMediaResponseMapper.mapHideOrShowResponse(mediaEntity, null);

        assertEquals(response.getId(), mediaEntity.getId());
        assertEquals(response.getIsHidden(), mediaEntity.isHidden());
    }
}