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
import uk.gov.hmcts.darts.common.entity.TranscriptionUrgencyEntity;
import uk.gov.hmcts.darts.common.repository.HearingReportingRestrictionsRepository;
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;
import uk.gov.hmcts.darts.transcriptions.mapper.TranscriptionResponseMapper;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionUrgencyResponse;

import java.util.List;

import static uk.gov.hmcts.darts.common.util.TestUtils.getContentsFromFile;

@ExtendWith(MockitoExtension.class)
class TranscriptionServiceGetTranscriptionUrgencyTest {
    private final ObjectMapper objectMapper = new ObjectMapperConfig().objectMapper();

    @Mock
    private HearingReportingRestrictionsRepository hearingReportingRestrictionsRepository;

    private TranscriptionResponseMapper transcriptionResponseMapper;

    @BeforeEach
    void setUp() {
        transcriptionResponseMapper = new TranscriptionResponseMapper(hearingReportingRestrictionsRepository);
    }

    @Test
    void getTranscriptionUrgency() throws Exception {
        List<TranscriptionUrgencyEntity> transcriptionUrgencyEntities = CommonTestDataUtil.createTranscriptionUrgencyEntities();

        List<TranscriptionUrgencyResponse> transcriptionUrgencyResponses =
            transcriptionResponseMapper.mapToTranscriptionUrgencyResponses(transcriptionUrgencyEntities);
        String actualResponse = objectMapper.writeValueAsString(transcriptionUrgencyResponses);

        String expectedResponse = getContentsFromFile(
            "Tests/transcriptions/service/TranscriptionUrgencyResponse/expectedResponseMultipleEntities.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }
}
