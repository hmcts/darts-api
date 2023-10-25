package uk.gov.hmcts.darts.transcriptions.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import uk.gov.hmcts.darts.common.config.ObjectMapperConfig;
import uk.gov.hmcts.darts.common.entity.TranscriptionTypeEntity;
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionTypeResponse;

import java.util.List;

import static uk.gov.hmcts.darts.common.util.TestUtils.getContentsFromFile;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Slf4j
class TranscriptionResponseMapperTest {
    ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        ObjectMapperConfig objectMapperConfig = new ObjectMapperConfig();
        objectMapper = objectMapperConfig.objectMapper();
    }

    @Test
    void mapToTranscriptionTypeResponses() throws Exception {
        List<TranscriptionTypeEntity> transcriptionTypeEntities = CommonTestDataUtil.createTranscriptionTypeEntities();

        List<TranscriptionTypeResponse> transcriptionTypeResponses =
            TranscriptionResponseMapper.mapToTranscriptionTypeResponses(transcriptionTypeEntities);
        String actualResponse = objectMapper.writeValueAsString(transcriptionTypeResponses);

        String expectedResponse = getContentsFromFile(
            "Tests/transcriptions/mapper/TranscriptionTypeResponseMapper/expectedResponseMultipleEntities.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    void mapToTranscriptionTypeResponse() throws Exception {
        TranscriptionTypeEntity transcriptionTypeEntity =
            CommonTestDataUtil.createTranscriptionTypeEntityFromEnum(TranscriptionTypeEnum.SENTENCING_REMARKS);

        TranscriptionTypeResponse transcriptionTypeResponse =
            TranscriptionResponseMapper.mapToTranscriptionTypeResponse(transcriptionTypeEntity);
        String actualResponse = objectMapper.writeValueAsString(transcriptionTypeResponse);

        String expectedResponse = getContentsFromFile(
            "Tests/transcriptions/mapper/TranscriptionTypeResponseMapper/expectedResponseSingleEntity.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

}
