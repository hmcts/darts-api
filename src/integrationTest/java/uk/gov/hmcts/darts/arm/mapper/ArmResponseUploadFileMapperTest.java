package uk.gov.hmcts.darts.arm.mapper;

import com.fasterxml.jackson.core.JsonParseException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.arm.exception.UnableToReadArmFileException;
import uk.gov.hmcts.darts.arm.model.record.armresponse.ArmResponseUploadFileRecordObject;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.hmcts.darts.test.common.TestUtils.getContentsFromFile;

class ArmResponseUploadFileMapperTest extends IntegrationBase {

    @Autowired
    private ArmResponseUploadFileMapper armResponseUploadFileMapper;

    @Test
    public void parseOk() throws IOException, UnableToReadArmFileException {
        String fileContents = getContentsFromFile("tests/arm/mapper/ArmResponseUploadFileMapperTest/parseOk.json");
        ArmResponseUploadFileRecordObject result = armResponseUploadFileMapper.map(fileContents);
        assertEquals("0002", result.getInput().getRelationId());
    }

    @Test
    public void parseFailInputJson() throws IOException {
        String fileContents = getContentsFromFile("tests/arm/mapper/ArmResponseUploadFileMapperTest/parseFailInputJson.json");
        assertThrows(JsonParseException.class,
                     () -> armResponseUploadFileMapper.map(fileContents));

    }

    @Test
    public void parseFailInputBlank() throws IOException {
        String fileContents = getContentsFromFile("tests/arm/mapper/ArmResponseUploadFileMapperTest/parseFailInputBlank.json");
        assertThrows(UnableToReadArmFileException.class,
                     () -> armResponseUploadFileMapper.map(fileContents));

    }

}