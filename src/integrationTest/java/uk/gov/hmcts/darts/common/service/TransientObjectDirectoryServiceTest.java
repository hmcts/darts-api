package uk.gov.hmcts.darts.common.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.helper.TransformedMediaHelper;
import uk.gov.hmcts.darts.audio.service.MediaRequestService;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestType;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;

class TransientObjectDirectoryServiceTest extends IntegrationBase {

    @Autowired
    private MediaRequestService mediaRequestService;
    @Autowired
    private TransientObjectDirectoryService transientObjectDirectoryService;

    @Autowired
    private TransformedMediaHelper transformedMediaHelper;

    @Test
    void shouldSaveTransientDataLocation() {
        dartsDatabase.getUserAccountStub().getSystemUserAccountEntity();
        var requestor = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        var mediaRequestEntity1 = dartsDatabase.createAndLoadOpenMediaRequestEntity(requestor, AudioRequestType.DOWNLOAD);

        MediaRequestEntity mediaRequestEntity = mediaRequestService.getMediaRequestEntityById(mediaRequestEntity1.getId());
        String blobId = "f744a74f-83c0-47e4-8bb2-2fd4d2b68647";
        UserAccountEntity userAccount = dartsDatabase.getUserAccountStub().createAuthorisedIntegrationTestUser(false, "NEWCASTLE");
        givenBearerTokenExists(userAccount.getEmailAddress());
        TransformedMediaEntity transformedMediaEntity = transformedMediaHelper.createTransformedMediaEntity(
            mediaRequestEntity,
            "aFilename",
            mediaRequestEntity.getStartTime(),
            mediaRequestEntity.getEndTime(),
            1000L
        );
        assertThat(transformedMediaEntity.getCreatedBy()).isEqualTo(mediaRequestEntity.getCreatedBy());
        assertThat(transformedMediaEntity.getLastModifiedById()).isEqualTo(mediaRequestEntity.getCreatedById());
        TransientObjectDirectoryEntity transientObjectDirectoryEntity = transientObjectDirectoryService.saveTransientObjectDirectoryEntity(
            transformedMediaEntity,
            blobId
        );

        assertNotNull(transientObjectDirectoryEntity);
        assertTrue(transientObjectDirectoryEntity.getId() > 0);
        assertEquals(mediaRequestEntity1.getId(), transientObjectDirectoryEntity.getTransformedMedia().getMediaRequest().getId());
        assertEquals(STORED.getId(), transientObjectDirectoryEntity.getStatus().getId());
        assertEquals(blobId, transientObjectDirectoryEntity.getExternalLocation());
        assertNull(transientObjectDirectoryEntity.getChecksum());
        assertTrue(transientObjectDirectoryEntity.getCreatedDateTime()
                       .isAfter(OffsetDateTime.parse("2023-07-06T16:00:00.000Z")));
        assertTrue(transientObjectDirectoryEntity.getLastModifiedDateTime()
                       .isAfter(OffsetDateTime.parse("2023-07-06T16:05:00.000Z")));
        assertNotNull(transientObjectDirectoryEntity.getLastModifiedById());
    }
}
