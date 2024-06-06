package uk.gov.hmcts.darts.arm.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.model.record.UploadNewFileRecord;
import uk.gov.hmcts.darts.arm.model.record.armresponse.ArmResponseUploadFileRecord;
import uk.gov.hmcts.darts.arm.model.record.armresponse.ArmResponseUploadFileRecordObject;

@Component
@RequiredArgsConstructor
@Slf4j
public class ArmResponseUploadFileMapper {
    private final ObjectMapper objectMapper;
    private final ArmResponseUploadFileRecordObjectMapper armResponseUploadFileRecordObjectMapper;

    public ArmResponseUploadFileRecordObject map(String fileContents) throws JsonProcessingException {
        ArmResponseUploadFileRecord armResponseUploadFileRecord = objectMapper.readValue(fileContents, ArmResponseUploadFileRecord.class);
        ArmResponseUploadFileRecordObject response = armResponseUploadFileRecordObjectMapper.map(armResponseUploadFileRecord);
        UploadNewFileRecord uploadNewFileRecord = readInputJson(armResponseUploadFileRecord.getInput());
        response.setInput(uploadNewFileRecord);
        return response;
    }

    private UploadNewFileRecord readInputJson(String input) {
        UploadNewFileRecord uploadNewFileRecord = null;
        if (StringUtils.isNotEmpty(input)) {
            String unescapedJson = StringEscapeUtils.unescapeJson(input);
            try {
                uploadNewFileRecord = objectMapper.readValue(unescapedJson, UploadNewFileRecord.class);
            } catch (JsonMappingException e) {
                log.error("Unable to map the input field {}", e.getMessage());
            } catch (JsonProcessingException e) {
                log.error("Unable to parse the upload new file record {}", e.getMessage());
            }
        } else {
            log.warn("Unable to parse the input field upload new file record");
        }
        return uploadNewFileRecord;
    }

}
