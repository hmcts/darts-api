package uk.gov.hmcts.darts.arm.mapper;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.arm.exception.UnableToReadArmFileException;
import uk.gov.hmcts.darts.arm.model.record.armresponse.ArmResponseUploadFileRecordObject;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ArmResponseUploadFileMapperTest extends IntegrationBase {

    @Autowired
    private ArmResponseUploadFileMapper armResponseUploadFileMapper;

    @Test
    public void parseOk() throws JsonProcessingException, UnableToReadArmFileException {
        String fileContents = """
            {
                "operation": "upload_new_file",
                "transaction_id": "f11e1453-27ef-75ec-9322-41af570e6502",
                "relation_id": "0001",
                "a360_record_id": "1cf976c7-cedd-703f-ab70-01588bd56d50",
                "process_time": "2023-07-11T11:41:27.873000",
                "status": 1,
                "input": "{\\"operation\\": \\"upload_new_file\\",\\"relation_id\\": \\"0002\\",\\"file_metadata\\":{\\"publisher\\": \\"A360\\",\\"dz_file_name\\": \\"A360230516_TestIngestion_1.docx\\",\\"file_tag\\": \\"docx\\"}}",
                "exception_description": null,
                "error_status": null,
                "a360_file_id": "e7cde7c6-15d7-4c7e-a85d-a468c7ea72b9",
                "file_size": 11997,
                "s_md5": "<CHECKSUM>",
                "s_sha256": "33054BD335175AE9CAFEBA794E468F2EC1C3F999CD8E0B314432A2C893EE4775"
            }""";
        ArmResponseUploadFileRecordObject result = armResponseUploadFileMapper.map(fileContents);
        assertEquals("0002", result.getInput().getRelationId());
    }

    @Test
    public void parseFailInputJson() {
        String fileContents = """
            {
                "operation": "upload_new_file",
                "transaction_id": "f11e1453-27ef-75ec-9322-41af570e6502",
                "relation_id": "0001",
                "a360_record_id": "1cf976c7-cedd-703f-ab70-01588bd56d50",
                "process_time": "2023-07-11T11:41:27.873000",
                "status": 1,
                "input": "{\\"operation\\": \\"upload_new_file,\\"relation_id\\": \\"0002\\",\\"file_metadata\\":{\\"publisher\\": \\"A360\\",\\"dz_file_name\\": \\"A360230516_TestIngestion_1.docx\\",\\"file_tag\\": \\"docx\\"}}",
                "exception_description": null,
                "error_status": null,
                "a360_file_id": "e7cde7c6-15d7-4c7e-a85d-a468c7ea72b9",
                "file_size": 11997,
                "s_md5": "<CHECKSUM>",
                "s_sha256": "33054BD335175AE9CAFEBA794E468F2EC1C3F999CD8E0B314432A2C893EE4775"
            }""";
        assertThrows(JsonParseException.class,
                     () -> armResponseUploadFileMapper.map(fileContents));

    }

    @Test
    public void parseFailInputBlank() {
        String fileContents = """
            {
                "operation": "upload_new_file",
                "transaction_id": "f11e1453-27ef-75ec-9322-41af570e6502",
                "relation_id": "0001",
                "a360_record_id": "1cf976c7-cedd-703f-ab70-01588bd56d50",
                "process_time": "2023-07-11T11:41:27.873000",
                "status": 1,
                "input": "",
                "exception_description": null,
                "error_status": null,
                "a360_file_id": "e7cde7c6-15d7-4c7e-a85d-a468c7ea72b9",
                "file_size": 11997,
                "s_md5": "<CHECKSUM>",
                "s_sha256": "33054BD335175AE9CAFEBA794E468F2EC1C3F999CD8E0B314432A2C893EE4775"
            }""";
        assertThrows(UnableToReadArmFileException.class,
                     () -> armResponseUploadFileMapper.map(fileContents));

    }

}