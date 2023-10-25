package uk.gov.hmcts.darts.transcriptions.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import uk.gov.hmcts.darts.common.config.ObjectMapperConfig;
import uk.gov.hmcts.darts.common.entity.TranscriptionUrgencyEntity;
import uk.gov.hmcts.darts.common.repository.TranscriptionUrgencyRepository;
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionUrgencyResponse;

import java.util.List;

import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.util.TestUtils.getContentsFromFile;

@ExtendWith(MockitoExtension.class)
class TranscriptionServiceGetTranscriptionUrgencyTest {
    ObjectMapper objectMapper;

    @Mock
    private TranscriptionUrgencyRepository mockTranscriptionUrgencyRepository;

    @InjectMocks
    private TranscriptionServiceImpl transcriptionService;

    @BeforeEach
    void setUp() {
        ObjectMapperConfig objectMapperConfig = new ObjectMapperConfig();
        objectMapper = objectMapperConfig.objectMapper();
    }

    @Test
    void getTranscriptionUrgency() throws Exception {
        List<TranscriptionUrgencyEntity> transcriptionUrgencyEntities = CommonTestDataUtil.createTranscriptionUrgencyEntities();

        when(mockTranscriptionUrgencyRepository.findAll()).thenReturn(transcriptionUrgencyEntities);

        List<TranscriptionUrgencyResponse> transcriptionUrgencyResponses = transcriptionService.getTranscriptionUrgencies();
        String actualResponse = objectMapper.writeValueAsString(transcriptionUrgencyResponses);

        String expectedResponse = getContentsFromFile(
            "Tests/transcriptions/service/TranscriptionUrgencyResponse/expectedResponseMultipleEntities.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }
}
