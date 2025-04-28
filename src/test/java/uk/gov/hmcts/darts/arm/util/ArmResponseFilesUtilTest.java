package uk.gov.hmcts.darts.arm.util;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArmResponseFilesUtilTest {

    @Test
    void generateSuffix_shouldReturnCorrectSuffix() {
        String filenameKey = "cr";
        String expectedSuffix = "_cr.rsp";
        String actualSuffix = ArmResponseFilesUtil.generateSuffix(filenameKey);
        assertEquals(expectedSuffix, actualSuffix);
    }

    @Test
    void getPrefix_shouldReturnCorrectPrefix() {
        ExternalObjectDirectoryEntity externalObjectDirectoryEntity = new ExternalObjectDirectoryEntity();
        externalObjectDirectoryEntity.setId(1L);
        externalObjectDirectoryEntity.setTransferAttempts(2);
        MediaEntity media = new MediaEntity();
        media.setId(3L);
        externalObjectDirectoryEntity.setMedia(media);

        String expectedPrefix = "1_3_2";
        String actualPrefix = ArmResponseFilesUtil.getPrefix(externalObjectDirectoryEntity);
        assertEquals(expectedPrefix, actualPrefix);
    }

    @Test
    void getObjectTypeId_shouldReturnCorrectObjectTypeId() {
        ExternalObjectDirectoryEntity externalObjectDirectoryEntity = new ExternalObjectDirectoryEntity();
        MediaEntity media = new MediaEntity();
        media.setId(3L);
        externalObjectDirectoryEntity.setMedia(media);

        String expectedObjectTypeId = "3";
        String actualObjectTypeId = ArmResponseFilesUtil.getObjectTypeId(externalObjectDirectoryEntity);
        assertEquals(expectedObjectTypeId, actualObjectTypeId);
    }
    
}