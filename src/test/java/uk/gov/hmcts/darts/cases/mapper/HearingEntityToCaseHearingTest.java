package uk.gov.hmcts.darts.cases.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import uk.gov.hmcts.darts.cases.model.Hearing;
import uk.gov.hmcts.darts.common.config.ObjectMapperConfig;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.hmcts.darts.test.common.TestUtils.getContentsFromFile;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HearingEntityToCaseHearingTest {

    ObjectMapper objectMapper;

    @BeforeAll
    void beforeAll() {
        ObjectMapperConfig objectMapperConfig = new ObjectMapperConfig();
        objectMapper = objectMapperConfig.objectMapper();
    }

    @Test
    void testWorksWithOneHearing() throws Exception {

        List<HearingEntity> hearings = CommonTestDataUtil.createHearings(1);

        List<Hearing> hearingList = HearingEntityToCaseHearing.mapToHearingList(hearings);

        String actualResponse = objectMapper.writeValueAsString(hearingList);

        String expectedResponse = getContentsFromFile(
            "Tests/cases/HearingEntityToCaseHearingTest/testWithSingleHearing/expectedResponse.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);

    }

    @Test
    void testMappingToMultipleHearings() throws Exception {

        List<HearingEntity> hearings = CommonTestDataUtil.createHearings(5);

        List<Hearing> hearingList = HearingEntityToCaseHearing.mapToHearingList(hearings);

        String actualResponse = objectMapper.writeValueAsString(hearingList);

        String expectedResponse = getContentsFromFile(
            "Tests/cases/HearingEntityToCaseHearingTest/testWithMultipleHearings/expectedResponse.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);

    }

    @Test
    void testMappingToHearingsWithAutomatedTranscripts() {
        List<HearingEntity> hearings = CommonTestDataUtil.createHearings(1);
        var hearingTranscripts = hearings.get(0).getTranscriptions();
        hearingTranscripts.get(0).setIsManualTranscription(false);
        hearingTranscripts.get(0).setLegacyObjectId(null);

        List<Hearing> hearingList = HearingEntityToCaseHearing.mapToHearingList(hearings);

        assertEquals(0, hearingList.get(0).getTranscriptCount());
    }

    @Test
    void testMappingToHearingsWithLegacyTranscripts() {
        List<HearingEntity> hearings = CommonTestDataUtil.createHearings(5);
        var hearingTranscripts = hearings.get(0).getTranscriptions();
        hearingTranscripts.get(0).setLegacyObjectId("something");

        List<Hearing> hearingList = HearingEntityToCaseHearing.mapToHearingList(hearings);

        assertEquals(1, hearingList.get(0).getTranscriptCount());
    }

    @Test
    void testMappingToHearingsWithLegacyAutomatedTranscripts() {
        List<HearingEntity> hearings = CommonTestDataUtil.createHearings(5);
        var hearingTranscripts = hearings.get(0).getTranscriptions();
        hearingTranscripts.get(0).setIsManualTranscription(false);
        hearingTranscripts.get(0).setLegacyObjectId("something");

        List<Hearing> hearingList = HearingEntityToCaseHearing.mapToHearingList(hearings);

        assertEquals(1, hearingList.get(0).getTranscriptCount());
    }

    @Test
    void testMappingToHearingsWithTranscriptsWithHiddenDocument() {
        List<HearingEntity> hearings = CommonTestDataUtil.createHearings(5);
        var hearingTranscripts = hearings.get(0).getTranscriptions();
        var transcriptDocs = hearingTranscripts.get(0).getTranscriptionDocumentEntities();
        transcriptDocs.get(0).setHidden(true);

        List<Hearing> hearingList = HearingEntityToCaseHearing.mapToHearingList(hearings);

        assertEquals(0, hearingList.get(0).getTranscriptCount());
    }

    @Test
    void testMappingToHearingsWithTranscriptsWithOnlyOneHiddenDocument() {
        List<HearingEntity> hearings = CommonTestDataUtil.createHearings(5);
        var hearingTranscripts = hearings.get(0).getTranscriptions();
        var transcriptDocs = hearingTranscripts.get(0).getTranscriptionDocumentEntities();
        var transcriptionDocumentEntity = new TranscriptionDocumentEntity();
        transcriptionDocumentEntity.setFileName("test2.doc");
        transcriptionDocumentEntity.setHidden(true);
        transcriptDocs.add(transcriptionDocumentEntity);

        List<Hearing> hearingList = HearingEntityToCaseHearing.mapToHearingList(hearings);

        assertEquals(0, hearingList.get(0).getTranscriptCount());
    }

    @Test
    void testWithNoHearings() throws Exception {

        List<HearingEntity> hearings = new ArrayList<>();

        List<Hearing> hearingList = HearingEntityToCaseHearing.mapToHearingList(hearings);

        String actualResponse = objectMapper.writeValueAsString(hearingList);

        String expectedResponse = getContentsFromFile(
            "Tests/cases/HearingEntityToCaseHearingTest/testWithNoHearings/expectedResponse.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);

    }

}