package uk.gov.hmcts.darts.common.service;

import com.azure.storage.blob.BlobClientBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.helper.TransformedMediaHelper;
import uk.gov.hmcts.darts.audio.service.MediaRequestService;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestType;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.testutils.stubs.DartsDatabaseStub;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.darts.common.enums.ObjectDirectoryStatusEnum.STORED;

@SpringBootTest
@ActiveProfiles({"intTest", "h2db"})
class TransientObjectDirectoryServiceTest {

    @Autowired
    private DartsDatabaseStub dartsDatabase;

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
        var mediaRequestEntity1 = dartsDatabase.createAndLoadCurrentMediaRequestEntity(requestor, AudioRequestType.DOWNLOAD);

        MediaRequestEntity mediaRequestEntity = mediaRequestService.getMediaRequestById(mediaRequestEntity1.getId());
        String blodId = "f744a74f-83c0-47e4-8bb2-2fd4d2b68647";
        BlobClientBuilder blobClientBuilder = new BlobClientBuilder();
        blobClientBuilder.blobName(blodId);
        blobClientBuilder.endpoint("http://127.0.0.1:10000/devstoreaccount1");

        TransformedMediaEntity transformedMediaEntity = transformedMediaHelper.createTransformedMediaEntity(mediaRequestEntity, "aFilename");
        TransientObjectDirectoryEntity transientObjectDirectoryEntity = transientObjectDirectoryService.saveTransientObjectDirectoryEntity(
            transformedMediaEntity,
            blobClientBuilder.buildClient()
                                                                                                                                          );

        assertNotNull(transientObjectDirectoryEntity);
        assertTrue(transientObjectDirectoryEntity.getId() > 0);
        assertEquals(mediaRequestEntity1.getId(), transientObjectDirectoryEntity.getTransformedMedia().getMediaRequest().getId());
        assertEquals(STORED.getId(), transientObjectDirectoryEntity.getStatus().getId());
        assertEquals(blodId, transientObjectDirectoryEntity.getExternalLocation().toString());
        assertNull(transientObjectDirectoryEntity.getChecksum());
        assertTrue(transientObjectDirectoryEntity.getCreatedDateTime()
                       .isAfter(OffsetDateTime.parse("2023-07-06T16:00:00.000Z")));
        assertTrue(transientObjectDirectoryEntity.getLastModifiedDateTime()
                       .isAfter(OffsetDateTime.parse("2023-07-06T16:05:00.000Z")));
        assertNotNull(transientObjectDirectoryEntity.getLastModifiedBy());
    }

}
