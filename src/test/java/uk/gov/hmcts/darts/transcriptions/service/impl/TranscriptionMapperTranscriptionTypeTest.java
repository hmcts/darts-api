package uk.gov.hmcts.darts.transcriptions.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import uk.gov.hmcts.darts.common.config.ObjectMapperConfig;
import uk.gov.hmcts.darts.common.entity.TranscriptionTypeEntity;
import uk.gov.hmcts.darts.common.repository.HearingReportingRestrictionsRepository;
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;
import uk.gov.hmcts.darts.transcriptions.mapper.TranscriptionResponseMapper;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionTypeResponse;

import java.util.List;

import static uk.gov.hmcts.darts.common.util.TestUtils.getContentsFromFile;

@ExtendWith(MockitoExtension.class)
class TranscriptionMapperTranscriptionTypeTest {
    private final ObjectMapper objectMapper = new ObjectMapperConfig().objectMapper();

    @Mock
    private HearingReportingRestrictionsRepository hearingReportingRestrictionsRepository;

    private TranscriptionResponseMapper transcriptionResponseMapper;

    @BeforeEach
    void setUp() {
        transcriptionResponseMapper = new TranscriptionResponseMapper(hearingReportingRestrictionsRepository);
    }

    @Test
    void getTranscriptionTypes() throws Exception {
        List<TranscriptionTypeEntity> transcriptionTypeEntities = CommonTestDataUtil.createTranscriptionTypeEntities();

        List<TranscriptionTypeResponse> transcriptionTypes =
            transcriptionResponseMapper.mapToTranscriptionTypeResponses(transcriptionTypeEntities);

        String actualResponse = objectMapper.writeValueAsString(transcriptionTypes);

        String expectedResponse = getContentsFromFile(
            "Tests/transcriptions/service/TranscriptionTypeResponse/expectedResponseMultipleEntities.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }
}
