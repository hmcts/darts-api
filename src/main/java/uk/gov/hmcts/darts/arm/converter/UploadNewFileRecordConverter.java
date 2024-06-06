package uk.gov.hmcts.darts.arm.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.arm.model.record.UploadNewFileRecord;

@Slf4j
@Component
@RequiredArgsConstructor
public class UploadNewFileRecordConverter implements Converter<String, UploadNewFileRecord> {

    private final ObjectMapper objectMapper;

    @Override
    public UploadNewFileRecord convert(String input) {
        if (StringUtils.isBlank(input)) {
            return null;
        }
        String unescapedJson = StringEscapeUtils.unescapeJson(input);
        try {
            return objectMapper.readValue(unescapedJson, UploadNewFileRecord.class);
        } catch (JsonMappingException e) {
            log.error("Unable to map the input field {}", e.getMessage());
        } catch (JsonProcessingException e) {
            log.error("Unable to parse the upload new file record {}", e.getMessage());
        }
        return null;
    }
}
