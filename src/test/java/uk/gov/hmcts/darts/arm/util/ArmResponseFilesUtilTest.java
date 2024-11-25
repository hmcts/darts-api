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
        externalObjectDirectoryEntity.setId(1);
        externalObjectDirectoryEntity.setTransferAttempts(2);
        MediaEntity media = new MediaEntity();
        media.setId(3);
        externalObjectDirectoryEntity.setMedia(media);

        String expectedPrefix = "1_3_2";
        String actualPrefix = ArmResponseFilesUtil.getPrefix(externalObjectDirectoryEntity);
        assertEquals(expectedPrefix, actualPrefix);
    }

    @Test
    void getObjectTypeId_shouldReturnCorrectObjectTypeId() {
        ExternalObjectDirectoryEntity externalObjectDirectoryEntity = new ExternalObjectDirectoryEntity();
        MediaEntity media = new MediaEntity();
        media.setId(3);
        externalObjectDirectoryEntity.setMedia(media);

        String expectedObjectTypeId = "3";
        String actualObjectTypeId = ArmResponseFilesUtil.getObjectTypeId(externalObjectDirectoryEntity);
        assertEquals(expectedObjectTypeId, actualObjectTypeId);
    }

    @Test
    void getRelationIdFromArmResponseFilesInputField_shouldReturnCorrectRelationId() {
        String input = "{\"relation_id\":\"300923629\"}";
        Integer expectedRelationId = 300_923_629;
        Integer actualRelationId = ArmResponseFilesUtil.getRelationIdFromArmResponseFilesInputField(input);
        assertEquals(expectedRelationId, actualRelationId);
    }

    @Test
    void getRelationIdFromArmResponseFilesInputField_shouldReturnCorrectRelationIdWithInvalidCharsBetweenColon() {
        String input = "{\"relation_id\"^%$%:8978\"300923629\"}";
        Integer expectedRelationId = 300_923_629;
        Integer actualRelationId = ArmResponseFilesUtil.getRelationIdFromArmResponseFilesInputField(input);
        assertEquals(expectedRelationId, actualRelationId);
    }

    @Test
    void getRelationIdFromArmResponseFilesInputField_shouldReturnNullWithInvalidInput() {
        String input = "{\"relation_id\":\"\"}";
        Integer actualRelationId = ArmResponseFilesUtil.getRelationIdFromArmResponseFilesInputField(input);
        assertEquals(null, actualRelationId);
    }

    @Test
    void getRelationIdFromArmResponseFilesInputField_shouldReturnValueWithMultipleMatches() {
        String input = "{@-@\"operation\":\"create_record\",\"relation_id\":\"300830603\",\"record_metadata\":{\"record_class\":\"A360TEST\"";
        Integer actualRelationId = ArmResponseFilesUtil.getRelationIdFromArmResponseFilesInputField(input);
        assertEquals(300_830_603, actualRelationId);
    }

    @Test
    void getOperationFromArmResponseFilesInputField_shouldReturnCorrectOperation() {
        String input = "{\"operation_id\":\"create_record\"}";
        String expectedOperation = "create_record";
        String actualOperation = ArmResponseFilesUtil.getOperationFromArmResponseFilesInputField(input);
        assertEquals(expectedOperation, actualOperation);
    }

    @Test
    void getOperationFromArmResponseFilesInputField_shouldReturnCorrectOperationWithInvalidCharsAtBeginning() {
        String input = "@@{\"operation_id\":\"create_record\"}";
        String expectedOperation = "create_record";
        String actualOperation = ArmResponseFilesUtil.getOperationFromArmResponseFilesInputField(input);
        assertEquals(expectedOperation, actualOperation);
    }

    @Test
    void findFirstPatternMatch_shouldReturnCorrectMatch() {
        String regex = "\"relation_id\".*?:.*?\"([^\"]+)\"";
        String text = "{\"relation_id\"$$:56\"300923629\"}";
        String expectedMatch = "300923629";
        String actualMatch = ArmResponseFilesUtil.findFirstPatternMatch(regex, text);
        assertEquals(expectedMatch, actualMatch);
    }

}