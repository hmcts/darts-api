package uk.gov.hmcts.darts.cases.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import uk.gov.hmcts.darts.cases.model.Hearing;
import uk.gov.hmcts.darts.common.config.ObjectMapperConfig;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.repository.TranscriptionDocumentRepository;
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.test.common.TestUtils.getContentsFromFile;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(MockitoExtension.class)
class HearingEntityToCaseHearingTest {

    @Mock
    private TranscriptionDocumentRepository transcriptionDocumentRepository;

    ObjectMapper objectMapper;

    @BeforeAll
    void beforeAll() {
        ObjectMapperConfig objectMapperConfig = new ObjectMapperConfig();
        objectMapper = objectMapperConfig.objectMapper();
    }

    @BeforeEach
    void beforeEach() {
        lenient().when(transcriptionDocumentRepository.findByTranscriptionIdAndHiddenTrueIncludeDeleted(anyInt()))
            .thenReturn(Collections.emptyList());
    }

    @Test
    void testWorksWithOneHearing() throws Exception {
        List<HearingEntity> hearings = CommonTestDataUtil.createHearings(1);

        List<Hearing> hearingList = HearingEntityToCaseHearing.mapToHearingList(hearings, transcriptionDocumentRepository);

        String actualResponse = objectMapper.writeValueAsString(hearingList);

        String expectedResponse = getContentsFromFile(
            "Tests/cases/HearingEntityToCaseHearingTest/testWithSingleHearing/expectedResponse.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);

        var transcriptionId = hearings.getFirst().getTranscriptions().getFirst().getId();
        verify(transcriptionDocumentRepository).findByTranscriptionIdAndHiddenTrueIncludeDeleted(transcriptionId);
    }

    @Test
    void testMappingToMultipleHearings() throws Exception {
        List<HearingEntity> hearings = CommonTestDataUtil.createHearings(5);

        List<Hearing> hearingList = HearingEntityToCaseHearing.mapToHearingList(hearings, transcriptionDocumentRepository);

        String actualResponse = objectMapper.writeValueAsString(hearingList);

        String expectedResponse = getContentsFromFile(
            "Tests/cases/HearingEntityToCaseHearingTest/testWithMultipleHearings/expectedResponse.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);

        verify(transcriptionDocumentRepository, times(hearings.size())).findByTranscriptionIdAndHiddenTrueIncludeDeleted(anyInt());
    }

    @Test
    void testMappingToHearingsWithAutomatedTranscripts() {
        List<HearingEntity> hearings = CommonTestDataUtil.createHearings(1);
        var hearingTranscripts = hearings.getFirst().getTranscriptions();
        hearingTranscripts.getFirst().setIsManualTranscription(false);
        hearingTranscripts.getFirst().setLegacyObjectId(null);

        List<Hearing> hearingList = HearingEntityToCaseHearing.mapToHearingList(hearings, transcriptionDocumentRepository);

        assertEquals(0, hearingList.getFirst().getTranscriptCount());
        verifyNoInteractions(transcriptionDocumentRepository);
    }

    @Test
    void testMappingToHearingsWithLegacyTranscripts() {
        List<HearingEntity> hearings = CommonTestDataUtil.createHearings(5);
        var hearingTranscripts = hearings.getFirst().getTranscriptions();
        hearingTranscripts.getFirst().setLegacyObjectId("something");

        List<Hearing> hearingList = HearingEntityToCaseHearing.mapToHearingList(hearings, transcriptionDocumentRepository);

        assertEquals(1, hearingList.getFirst().getTranscriptCount());
        verify(transcriptionDocumentRepository, times(hearings.size())).findByTranscriptionIdAndHiddenTrueIncludeDeleted(anyInt());
    }

    @Test
    void testMappingToHearingsWithLegacyAutomatedTranscripts() {
        List<HearingEntity> hearings = CommonTestDataUtil.createHearings(5);
        var hearingTranscripts = hearings.getFirst().getTranscriptions();
        hearingTranscripts.getFirst().setIsManualTranscription(false);
        hearingTranscripts.getFirst().setLegacyObjectId("something");

        List<Hearing> hearingList = HearingEntityToCaseHearing.mapToHearingList(hearings, transcriptionDocumentRepository);

        assertEquals(1, hearingList.getFirst().getTranscriptCount());
        verify(transcriptionDocumentRepository, times(hearings.size())).findByTranscriptionIdAndHiddenTrueIncludeDeleted(anyInt());
    }

    @Test
    void testMappingToHearingsWithTranscriptsWithHiddenDocument() {
        when(transcriptionDocumentRepository.findByTranscriptionIdAndHiddenTrueIncludeDeleted(anyInt()))
            .thenReturn(List.of(new TranscriptionDocumentEntity()));

        List<HearingEntity> hearings = CommonTestDataUtil.createHearings(1);
        var hearingTranscripts = hearings.getFirst().getTranscriptions();
        var transcriptDocs = hearingTranscripts.getFirst().getTranscriptionDocumentEntities();
        transcriptDocs.getFirst().setHidden(true);

        List<Hearing> hearingList = HearingEntityToCaseHearing.mapToHearingList(hearings, transcriptionDocumentRepository);

        assertEquals(0, hearingList.getFirst().getTranscriptCount());
        var transcriptionId = hearings.getFirst().getTranscriptions().getFirst().getId();
        verify(transcriptionDocumentRepository).findByTranscriptionIdAndHiddenTrueIncludeDeleted(transcriptionId);
    }

    @Test
    void testMappingToHearingsWithTranscriptsWithOnlyOneHiddenDocument() {
        when(transcriptionDocumentRepository.findByTranscriptionIdAndHiddenTrueIncludeDeleted(anyInt()))
            .thenReturn(List.of(new TranscriptionDocumentEntity()));

        List<HearingEntity> hearings = CommonTestDataUtil.createHearings(1);
        var hearingTranscripts = hearings.getFirst().getTranscriptions();
        var transcriptDocs = hearingTranscripts.getFirst().getTranscriptionDocumentEntities();
        var transcriptionDocumentEntity = new TranscriptionDocumentEntity();
        transcriptionDocumentEntity.setFileName("test2.doc");
        transcriptionDocumentEntity.setHidden(true);
        transcriptDocs.add(transcriptionDocumentEntity);

        List<Hearing> hearingList = HearingEntityToCaseHearing.mapToHearingList(hearings, transcriptionDocumentRepository);

        assertEquals(0, hearingList.getFirst().getTranscriptCount());
        var transcriptionId = hearings.getFirst().getTranscriptions().getFirst().getId();
        verify(transcriptionDocumentRepository).findByTranscriptionIdAndHiddenTrueIncludeDeleted(transcriptionId);
    }

    @Test
    void mapToHearingList_shouldNotIncludeNonCurrentTranscriptions() {
        List<HearingEntity> hearings = CommonTestDataUtil.createHearings(1);
        hearings.getFirst().getTranscriptions().getFirst().setIsCurrent(false);

        List<Hearing> hearingList = HearingEntityToCaseHearing.mapToHearingList(hearings, transcriptionDocumentRepository);
        assertEquals(1, hearingList.size());
        assertEquals(0, hearingList.getFirst().getTranscriptCount());
    }

    @Test
    void testWithNoHearings() throws Exception {

        List<HearingEntity> hearings = new ArrayList<>();

        List<Hearing> hearingList = HearingEntityToCaseHearing.mapToHearingList(hearings, transcriptionDocumentRepository);

        String actualResponse = objectMapper.writeValueAsString(hearingList);

        String expectedResponse = getContentsFromFile(
            "Tests/cases/HearingEntityToCaseHearingTest/testWithNoHearings/expectedResponse.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);

        verifyNoInteractions(transcriptionDocumentRepository);
    }

}