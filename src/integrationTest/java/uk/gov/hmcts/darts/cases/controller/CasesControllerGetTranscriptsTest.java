package uk.gov.hmcts.darts.cases.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.TestUtils;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static uk.gov.hmcts.darts.testutils.TestUtils.getContentsFromFile;

@AutoConfigureMockMvc
@Transactional
class CasesControllerGetTranscriptsTest extends IntegrationBase {
    @Autowired
    private transient MockMvc mockMvc;
    private static final String ENDPOINT_URL_CASE = "/cases/{case_id}/transcripts";
    private static final OffsetDateTime SOME_DATE_TIME = OffsetDateTime.parse("2023-01-01T12:00Z");
    private static final String SOME_COURTHOUSE = "some-courthouse";
    private static final String SOME_COURTROOM = "some-courtroom";
    private static final String SOME_CASE_ID = "1";

    private static final List<String> TAGS_TO_IGNORE = List.of("tra_id", "hea_id");

    @MockBean
    private UserIdentity mockUserIdentity;

    @BeforeEach
    void setUp() {

        HearingEntity hearingEntity = dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            SOME_CASE_ID,
            SOME_COURTHOUSE,
            SOME_COURTROOM,
            SOME_DATE_TIME.toLocalDate()
        );

        CourthouseEntity courthouseEntity = hearingEntity.getCourtroom().getCourthouse();
        assertEquals(SOME_COURTHOUSE, courthouseEntity.getCourthouseName());

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub()
            .createAuthorisedIntegrationTestUser(courthouseEntity);
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);

    }

    @Test
    void caseGetTranscriptEndpointNotFound() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL_CASE, "25");

        mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().isNotFound());

    }

    @Test
    void casesGetTranscriptEndpointOneObjectReturned() throws Exception {
        HearingEntity hearingEntity = dartsDatabase.getHearingRepository().findAll().get(0);
        TranscriptionEntity transcription = dartsDatabase.getTranscriptionStub().createTranscription(hearingEntity);
        transcription.setCreatedDateTime(OffsetDateTime.of(2023, 6, 20, 10, 0, 0, 0, ZoneOffset.UTC));
        dartsDatabase.save(transcription);

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL_CASE, hearingEntity.getCourtCase().getId());
        String expected = TestUtils.removeTags(TAGS_TO_IGNORE, getContentsFromFile(
            "tests/cases/CaseControllerGetCaseTranscriptsTest/casesSearchGetEndpointOneObjectReturned.json"));
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();
        String actualResponse = TestUtils.removeTags(TAGS_TO_IGNORE, mvcResult.getResponse().getContentAsString());
        JSONAssert.assertEquals(expected, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void casesGetTranscriptEndpointTwoObjectsReturned() throws Exception {
        HearingEntity hearingEntity = dartsDatabase.getHearingRepository().findAll().get(0);
        TranscriptionEntity transcription = dartsDatabase.getTranscriptionStub().createTranscription(hearingEntity);
        transcription.setCreatedDateTime(OffsetDateTime.of(2023, 6, 20, 10, 0, 0, 0, ZoneOffset.UTC));
        dartsDatabase.save(transcription);
        TranscriptionEntity transcription2 = dartsDatabase.getTranscriptionStub().createTranscription(hearingEntity);
        transcription2.setCreatedDateTime(OffsetDateTime.of(2023, 6, 20, 10, 0, 0, 0, ZoneOffset.UTC));
        dartsDatabase.save(transcription2);

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL_CASE, hearingEntity.getCourtCase().getId());
        String expected = TestUtils.removeTags(TAGS_TO_IGNORE, getContentsFromFile(
            "tests/cases/CaseControllerGetCaseTranscriptsTest/casesSearchGetEndpointTwoObjectsReturned.json"));
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().isOk())
            .andReturn();
        String actualResponse = TestUtils.removeTags(TAGS_TO_IGNORE, mvcResult.getResponse().getContentAsString());
        JSONAssert.assertEquals(expected, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }


}
