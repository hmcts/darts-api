package uk.gov.hmcts.darts.arm.model.record.armresponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


class ArmResponseInputUploadFileRecordTest {

    @Test
    void deserlisationTest() throws JsonProcessingException {
        String json = """
            {
              "operation": "input_upload",
              "timestamp": "2024-01-11T12:46:21.215310",
              "status": 1,
              "exception_description": null,
              "error_status": null,
              "filename": "CGITestFilesMalformedManifest_1+2",
              "submission_folder": "/dropzone/A360/submission",
              "file_hash": "fbfec54925d62146aeced724ff9f3c8e"
            }
            """;
        ArmResponseInputUploadFileRecord inputUploadFileRecord = new ObjectMapper()
            .findAndRegisterModules().readValue(json, ArmResponseInputUploadFileRecord.class);

        assertThat(inputUploadFileRecord.getOperation()).isEqualTo("input_upload");
        assertThat(inputUploadFileRecord.getTimestamp()).isEqualTo("2024-01-11T12:46:21.215310");
        assertThat(inputUploadFileRecord.getStatus()).isEqualTo(1);
        assertThat(inputUploadFileRecord.getExceptionDescription()).isNull();
        assertThat(inputUploadFileRecord.getErrorStatus()).isNull();
        assertThat(inputUploadFileRecord.getFilename()).isEqualTo("CGITestFilesMalformedManifest_1+2");
        assertThat(inputUploadFileRecord.getSubmissionFolder()).isEqualTo("/dropzone/A360/submission");
        assertThat(inputUploadFileRecord.getFileHash()).isEqualTo("fbfec54925d62146aeced724ff9f3c8e");
    }
}
