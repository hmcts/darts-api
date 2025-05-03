package uk.gov.hmcts.darts.cases.controller;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.util.DateConverterUtil;
import uk.gov.hmcts.darts.test.common.TestUtils;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static uk.gov.hmcts.darts.test.common.TestUtils.getContentsFromFile;

@AutoConfigureMockMvc
class CasesControllerGetTranscriptsTest extends IntegrationBase {
    private static final String ENDPOINT_URL_CASE = "/cases/{case_id}/transcripts";
    private static final OffsetDateTime SOME_DATE_TIME = OffsetDateTime.parse("2023-01-01T12:00Z");
    private static final String SOME_COURTHOUSE = "SOME-COURTHOUSE";
    private static final String SOME_COURTROOM = "some-courtroom";
    private static final String SOME_CASE_ID = "1";
    private static final List<String> TAGS_TO_IGNORE = List.of("tra_id", "hea_id", "transcription_id", "hearing_id");
    @Autowired
    private transient MockMvc mockMvc;
    @MockitoBean
    private UserIdentity mockUserIdentity;

    @SuppressWarnings("PMD.UnitTestShouldUseBeforeAnnotation")//False positive needs to be called within same trasnaction
    private void setUp() {
        HearingEntity hearingEntity = dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            SOME_CASE_ID,
            SOME_COURTHOUSE,
            SOME_COURTROOM,
            DateConverterUtil.toLocalDateTime(SOME_DATE_TIME)
        );

        CourthouseEntity courthouseEntity = hearingEntity.getCourtroom().getCourthouse();
        assertEquals(SOME_COURTHOUSE.toUpperCase(Locale.ROOT), courthouseEntity.getCourthouseName());
        assertEquals(SOME_COURTHOUSE, courthouseEntity.getDisplayName());

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub()
            .createAuthorisedIntegrationTestUser(courthouseEntity);
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);

    }

    @Test
    void caseGetTranscriptEndpointNotFound() throws Exception {
        setUp();
        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL_CASE, "25");

        mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().isNotFound());

    }


    @Test
    void casesGetTranscriptEndpointOneObjectReturned() throws Exception {
        final TranscriptionEntity[] transcription = new TranscriptionEntity[1];
        HearingEntity hearing = transactionalUtil.executeInTransaction(() -> {
            setUp();
            HearingEntity hearingEntity = dartsDatabase.getHearingRepository().findAll().getFirst();
            transcription[0] = dartsDatabase.getTranscriptionStub().createTranscription(hearingEntity);
            dartsDatabase.save(transcription[0]);
            return hearingEntity;
        });
        dartsDatabase.updateCreatedBy(transcription[0], OffsetDateTime.of(2023, 6, 20, 10, 0, 0, 0, ZoneOffset.UTC));

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL_CASE, hearing.getCourtCase().getId());
        String expected = TestUtils.removeTags(TAGS_TO_IGNORE, getContentsFromFile(
            "tests/cases/CaseControllerGetCaseTranscriptsTest/casesSearchGetEndpointOneObjectReturned.json"));
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();
        String actualResponse = TestUtils.removeTags(TAGS_TO_IGNORE, mvcResult.getResponse().getContentAsString());
        JSONAssert.assertEquals(expected, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void casesGetTranscriptEndpointTwoObjectsReturned() throws Exception {
        final TranscriptionEntity[] transcription = new TranscriptionEntity[2];
        HearingEntity hearing = transactionalUtil.executeInTransaction(() -> {
            setUp();
            HearingEntity hearingEntity = dartsDatabase.getHearingRepository().findAll().getFirst();
            transcription[0] = dartsDatabase.getTranscriptionStub().createTranscription(hearingEntity);
            dartsDatabase.save(transcription[0]);

            transcription[1] = dartsDatabase.getTranscriptionStub().createTranscription(hearingEntity);
            dartsDatabase.save(transcription[1]);
            return hearingEntity;
        });
        dartsDatabase.updateCreatedBy(transcription[0], OffsetDateTime.of(2023, 6, 20, 10, 0, 0, 0, ZoneOffset.UTC));
        dartsDatabase.updateCreatedBy(transcription[1], OffsetDateTime.of(2023, 6, 20, 10, 1, 0, 0, ZoneOffset.UTC));


        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL_CASE, hearing.getCourtCase().getId());
        String expected = TestUtils.removeTags(TAGS_TO_IGNORE, getContentsFromFile(
            "tests/cases/CaseControllerGetCaseTranscriptsTest/casesSearchGetEndpointTwoObjectsReturned.json"));
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();
        String actualResponse = TestUtils.removeTags(TAGS_TO_IGNORE, mvcResult.getResponse().getContentAsString());
        JSONAssert.assertEquals(expected, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    void ignoreAutomaticTranscripts() throws Exception {
        final TranscriptionEntity[] transcription = new TranscriptionEntity[4];
        HearingEntity hearing = transactionalUtil.executeInTransaction(() -> {
            setUp();
            HearingEntity hearingEntity = dartsDatabase.getHearingRepository().findAll().getFirst();

            //modernised manual transcription
            transcription[0] = dartsDatabase.getTranscriptionStub().createTranscription(hearingEntity);
            transcription[0].setIsManualTranscription(true);
            dartsDatabase.save(transcription[0]);

            //modernised automatic transcription
            transcription[1] = dartsDatabase.getTranscriptionStub().createTranscription(hearingEntity);
            transcription[1].setIsManualTranscription(false);
            transcription[1].setLegacyObjectId(null);
            dartsDatabase.save(transcription[1]);

            //legacy manual transcription
            transcription[2] = dartsDatabase.getTranscriptionStub().createTranscription(hearingEntity);
            transcription[2].setIsManualTranscription(true);
            transcription[2].setLegacyObjectId("Something");
            dartsDatabase.save(transcription[2]);

            //legacy automatic transcription
            transcription[3] = dartsDatabase.getTranscriptionStub().createTranscription(hearingEntity);
            transcription[3].setIsManualTranscription(false);
            transcription[3].setLegacyObjectId("Something");
            dartsDatabase.save(transcription[3]);
            dartsDatabase.getTranscriptionDocumentStub().createTranscriptionDocumentForTranscription(transcription[3]);
            return hearingEntity;
        });
        dartsDatabase.updateCreatedBy(transcription[0], OffsetDateTime.of(2023, 6, 20, 10, 1, 0, 0, ZoneOffset.UTC));
        dartsDatabase.updateCreatedBy(transcription[1], OffsetDateTime.of(2023, 6, 20, 10, 2, 0, 0, ZoneOffset.UTC));
        dartsDatabase.updateCreatedBy(transcription[2], OffsetDateTime.of(2023, 6, 20, 10, 3, 0, 0, ZoneOffset.UTC));
        dartsDatabase.updateCreatedBy(transcription[3], OffsetDateTime.of(2023, 6, 20, 10, 4, 0, 0, ZoneOffset.UTC));


        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL_CASE, hearing.getCourtCase().getId());
        String expected = TestUtils.removeTags(TAGS_TO_IGNORE, getContentsFromFile(
            "tests/cases/CaseControllerGetCaseTranscriptsTest/ignoreAutomaticTranscripts.json"));
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();
        String actualResponse = TestUtils.removeTags(TAGS_TO_IGNORE, mvcResult.getResponse().getContentAsString());
        JSONAssert.assertEquals(expected, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }
}