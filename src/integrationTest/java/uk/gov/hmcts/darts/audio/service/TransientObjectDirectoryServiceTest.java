package uk.gov.hmcts.darts.audio.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.common.entity.ObjectDirectoryStatusEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.service.TransientObjectDirectoryService;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.darts.common.entity.ObjectDirectoryStatusEnum.NEW;

@SpringBootTest
@ActiveProfiles({"intTest", "postgresTestContainer"})
@Transactional
class TransientObjectDirectoryServiceTest {

    @Autowired
    private MediaRequestService mediaRequestService;
    @Autowired
    private TransientObjectDirectoryService transientObjectDirectoryService;

    @Test
    void shouldGetTransientObjectDirectoryListByMediaRequest() {

        MediaRequestEntity mediaRequestEntity = mediaRequestService.getMediaRequestById(-1);

        List<TransientObjectDirectoryEntity> transientObjectDirectoryList = mediaRequestEntity.getTransientObjectDirectoryList();

        assertNotNull(transientObjectDirectoryList);
        assertEquals(1, transientObjectDirectoryList.size());
        TransientObjectDirectoryEntity transientObjectDirectoryEntity = transientObjectDirectoryList.get(0);
        assertEquals(-1, transientObjectDirectoryEntity.getId());
        ObjectDirectoryStatusEntity objectDirectoryStatusEntity = transientObjectDirectoryEntity.getStatus();
        assertNotNull(objectDirectoryStatusEntity);
        assertEquals(NEW.getId(), objectDirectoryStatusEntity.getId());
        assertEquals(
            UUID.fromString("f744a74f-83c0-47e4-8bb2-2fd4d2b68647"),
            transientObjectDirectoryEntity.getExternalLocation()
        );
    }

    @Test
    void shouldSaveTransientDataLocation() {

        MediaRequestEntity mediaRequestEntity = mediaRequestService.getMediaRequestById(-1);
        UUID externalLocation = UUID.randomUUID();

        TransientObjectDirectoryEntity transientObjectDirectoryEntity = transientObjectDirectoryService.saveTransientDataLocation(
            mediaRequestEntity,
            externalLocation
        );

        assertNotNull(transientObjectDirectoryEntity);
        assertTrue(transientObjectDirectoryEntity.getId() > 0);
        assertEquals(-1, transientObjectDirectoryEntity.getMediaRequest().getRequestId());
        assertEquals(NEW.getId(), transientObjectDirectoryEntity.getStatus().getId());
        assertEquals(externalLocation, transientObjectDirectoryEntity.getExternalLocation());
        assertNull(transientObjectDirectoryEntity.getChecksum());
        assertTrue(transientObjectDirectoryEntity.getCreatedTimestamp()
                       .isAfter(OffsetDateTime.parse("2023-06-30T17:00:00.000Z")));
        assertTrue(transientObjectDirectoryEntity.getModifiedTimestamp()
                       .isAfter(OffsetDateTime.parse("2023-06-30T17:05:00.000Z")));
        assertNull(transientObjectDirectoryEntity.getModifiedBy());
    }

}
