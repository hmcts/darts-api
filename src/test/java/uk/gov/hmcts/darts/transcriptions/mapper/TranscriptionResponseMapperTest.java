package uk.gov.hmcts.darts.transcriptions.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import uk.gov.hmcts.darts.common.config.ObjectMapperConfig;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionTypeEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionUrgencyEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.HearingReportingRestrictionsRepository;
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionTypeEnum;
import uk.gov.hmcts.darts.transcriptions.enums.TranscriptionUrgencyEnum;
import uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError;
import uk.gov.hmcts.darts.transcriptions.model.GetTranscriptionByIdResponse;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionTypeResponse;
import uk.gov.hmcts.darts.transcriptions.model.TranscriptionUrgencyResponse;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.hmcts.darts.common.util.TestUtils.getContentsFromFile;


@ExtendWith(MockitoExtension.class)
class TranscriptionResponseMapperTest {

    private final ObjectMapper objectMapper = new ObjectMapperConfig().objectMapper();

    private TranscriptionResponseMapper transcriptionResponseMapper;

    @Mock
    private HearingReportingRestrictionsRepository hearingReportingRestrictionsRepository;

    @BeforeEach
    void setUp() {
        transcriptionResponseMapper = new TranscriptionResponseMapper(hearingReportingRestrictionsRepository);
    }

    @Test
    void mapToTranscriptionTypeResponses() throws Exception {
        List<TranscriptionTypeEntity> transcriptionTypeEntities = CommonTestDataUtil.createTranscriptionTypeEntities();

        List<TranscriptionTypeResponse> transcriptionTypeResponses =
              transcriptionResponseMapper.mapToTranscriptionTypeResponses(transcriptionTypeEntities);
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
              transcriptionResponseMapper.mapToTranscriptionTypeResponse(transcriptionTypeEntity);
        String actualResponse = objectMapper.writeValueAsString(transcriptionTypeResponse);

        String expectedResponse = getContentsFromFile(
              "Tests/transcriptions/mapper/TranscriptionTypeResponseMapper/expectedResponseSingleEntity.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }


    @Test
    void mapToTranscriptionUrgencyResponses() throws Exception {
        List<TranscriptionUrgencyEntity> transcriptionUrgencyEntities = CommonTestDataUtil.createTranscriptionUrgencyEntities();

        List<TranscriptionUrgencyResponse> transcriptionUrgencyResponses =
              transcriptionResponseMapper.mapToTranscriptionUrgencyResponses(transcriptionUrgencyEntities);
        String actualResponse = objectMapper.writeValueAsString(transcriptionUrgencyResponses);

        String expectedResponse = getContentsFromFile(
              "Tests/transcriptions/mapper/TranscriptionUrgencyResponseMapper/expectedResponseMultipleEntities.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    void mapToTranscriptionUrgencyResponse() throws Exception {
        TranscriptionUrgencyEntity transcriptionUrgencyEntity =
              CommonTestDataUtil.createTranscriptionUrgencyEntityFromEnum(TranscriptionUrgencyEnum.STANDARD, 999);

        TranscriptionUrgencyResponse transcriptionUrgencyResponse =
              transcriptionResponseMapper.mapToTranscriptionUrgencyResponse(transcriptionUrgencyEntity);
        String actualResponse = objectMapper.writeValueAsString(transcriptionUrgencyResponse);

        String expectedResponse = getContentsFromFile(
              "Tests/transcriptions/mapper/TranscriptionUrgencyResponseMapper/expectedResponseSingleEntity.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    void mapToTranscriptionResponseWithNoStatus() throws Exception {
        HearingEntity hearing1 = CommonTestDataUtil.createHearing("case1", LocalTime.NOON);
        List<TranscriptionEntity> transcriptionList = CommonTestDataUtil.createTranscriptionList(hearing1, false);
        TranscriptionEntity transcriptionEntity = transcriptionList.get(0);

        GetTranscriptionByIdResponse transcriptionResponse =
              transcriptionResponseMapper.mapToTranscriptionResponse(transcriptionEntity);
        String actualResponse = objectMapper.writeValueAsString(transcriptionResponse);

        String expectedResponse = getContentsFromFile(
              "Tests/transcriptions/mapper/TranscriptionResponseMapper/expectedResponseNoStatusSingleEntity.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    void mapToTranscriptionResponseWithNoHearingId() throws Exception {
        HearingEntity hearing1 = CommonTestDataUtil.createHearing("case1", LocalTime.NOON);
        List<TranscriptionEntity> transcriptionList = CommonTestDataUtil.createTranscriptionList(hearing1, true, false);
        TranscriptionEntity transcriptionEntity = transcriptionList.get(0);
        transcriptionEntity.setHearings(new ArrayList<>());
        transcriptionEntity.setHearingDate(LocalDate.of(2023, 6, 20));

        GetTranscriptionByIdResponse transcriptionResponse =
              transcriptionResponseMapper.mapToTranscriptionResponse(transcriptionEntity);
        String actualResponse = objectMapper.writeValueAsString(transcriptionResponse);

        String expectedResponse = getContentsFromFile(
              "Tests/transcriptions/mapper/TranscriptionResponseMapper/expectedResponseSingleEntityNoHearing.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    void mapToTranscriptionResponseWithWorkflow() throws Exception {
        HearingEntity hearing1 = CommonTestDataUtil.createHearing("case1", LocalTime.NOON);
        List<TranscriptionEntity> transcriptionList = CommonTestDataUtil.createTranscriptionList(hearing1, true, false);
        TranscriptionEntity transcriptionEntity = transcriptionList.get(0);

        GetTranscriptionByIdResponse transcriptionResponse =
              transcriptionResponseMapper.mapToTranscriptionResponse(transcriptionEntity);
        String actualResponse = objectMapper.writeValueAsString(transcriptionResponse);

        String expectedResponse = getContentsFromFile(
              "Tests/transcriptions/mapper/TranscriptionResponseMapper/expectedResponseSingleEntityWithWorkflow.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    void mapToTranscriptionResponse() throws Exception {
        HearingEntity hearing1 = CommonTestDataUtil.createHearing("case1", LocalTime.NOON);
        List<TranscriptionEntity> transcriptionList = CommonTestDataUtil.createTranscriptionList(hearing1, true, false);
        TranscriptionEntity transcriptionEntity = transcriptionList.get(0);

        GetTranscriptionByIdResponse transcriptionResponse =
              transcriptionResponseMapper.mapToTranscriptionResponse(transcriptionEntity);
        String actualResponse = objectMapper.writeValueAsString(transcriptionResponse);

        String expectedResponse = getContentsFromFile(
              "Tests/transcriptions/mapper/TranscriptionResponseMapper/expectedResponseSingleEntity.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    void mapToTranscriptionResponseIsAutomated() throws Exception {
        HearingEntity hearing1 = CommonTestDataUtil.createHearing("case1", LocalTime.NOON);
        List<TranscriptionEntity> transcriptionList = CommonTestDataUtil.createTranscriptionList(hearing1, true, false);
        TranscriptionEntity transcriptionEntity = transcriptionList.get(0);
        transcriptionEntity.setIsManualTranscription(false);

        GetTranscriptionByIdResponse transcriptionResponse =
              transcriptionResponseMapper.mapToTranscriptionResponse(transcriptionEntity);
        String actualResponse = objectMapper.writeValueAsString(transcriptionResponse);

        String expectedResponse = getContentsFromFile(
              "Tests/transcriptions/mapper/TranscriptionResponseMapper/expectedResponseSingleEntityAutomated.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    void mapToTranscriptionResponseInternalServerError() throws Exception {
        HearingEntity hearing1 = CommonTestDataUtil.createHearing("case1", LocalTime.NOON);
        hearing1.setCourtCase(null);
        List<TranscriptionEntity> transcriptionList = CommonTestDataUtil.createTranscriptionList(hearing1);
        TranscriptionEntity transcriptionEntity = transcriptionList.get(0);

        var exception = assertThrows(
              DartsApiException.class,
              () -> transcriptionResponseMapper.mapToTranscriptionResponse(transcriptionEntity)
        );

        assertEquals(TranscriptionApiError.TRANSCRIPTION_NOT_FOUND, exception.getError());
    }

}
