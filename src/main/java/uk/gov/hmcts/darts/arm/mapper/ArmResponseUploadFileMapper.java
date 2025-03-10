package uk.gov.hmcts.darts.arm.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.exception.UnableToReadArmFileException;
import uk.gov.hmcts.darts.arm.model.record.UploadNewFileRecord;
import uk.gov.hmcts.darts.arm.model.record.armresponse.ArmResponseUploadFileRecord;
import uk.gov.hmcts.darts.arm.model.record.armresponse.ArmResponseUploadFileRecordObject;

@Component
@RequiredArgsConstructor
@Slf4j
public class ArmResponseUploadFileMapper {
    private final ObjectMapper objectMapper;
    private final ArmResponseUploadFileRecordObjectMapper armResponseUploadFileRecordObjectMapper;

    public ArmResponseUploadFileRecordObject map(String fileContents) throws JsonProcessingException, UnableToReadArmFileException {
        ArmResponseUploadFileRecord armResponseUploadFileRecord = objectMapper.readValue(fileContents, ArmResponseUploadFileRecord.class);
        ArmResponseUploadFileRecordObject response = armResponseUploadFileRecordObjectMapper.map(armResponseUploadFileRecord);
        UploadNewFileRecord uploadNewFileRecord = readInputJson(armResponseUploadFileRecord.getInput());
        response.setInput(uploadNewFileRecord);
        return response;
    }

    private UploadNewFileRecord readInputJson(String input) throws JsonProcessingException, UnableToReadArmFileException {
        if (StringUtils.isEmpty(input)) {
            String errorMessage = "\"input\" String is empty, so cannot parse.";
            log.warn(errorMessage);
            throw new UnableToReadArmFileException(null);
        }
        UploadNewFileRecord uploadNewFileRecord;
        try {
            uploadNewFileRecord = objectMapper.readValue(input, UploadNewFileRecord.class);
            return uploadNewFileRecord;
        } catch (JsonProcessingException e) {
            log.error("Unable to parse the upload new file record {}", e.getMessage());
            throw e;
        }

    }

}
