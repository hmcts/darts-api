package uk.gov.hmcts.darts.transcriptions.controller;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.testutils.TestUtils.getContentsFromFile;

@AutoConfigureMockMvc
@Transactional
class TranscriptionControllerGetTranscriptionTest extends IntegrationBase {

    private static final String ENDPOINT_URL_TRANSCRIPTION = "/transcriptions/{transcription_id}";
    private static final OffsetDateTime SOME_DATE_TIME = OffsetDateTime.parse("2023-01-01T12:00Z");
    private static final String SOME_COURTHOUSE = "some-courthouse";
    private static final String SOME_COURTROOM = "some-courtroom";
    private static final String SOME_CASE_ID = "1";
    private static final List<String> TAGS_TO_IGNORE = List.of("case_id");

    @Autowired
    private MockMvc mockMvc;
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
    void getTranscription() throws Exception {
        HearingEntity hearingEntity = dartsDatabase.getHearingRepository().findAll().get(0);
        TranscriptionEntity transcription = dartsDatabase.getTranscriptionStub().createTranscription(hearingEntity);
        transcription.setCreatedDateTime(OffsetDateTime.of(2023, 6, 20, 10, 0, 0, 0, ZoneOffset.UTC));
        transcription.setStartTime(SOME_DATE_TIME);
        transcription.setEndTime(SOME_DATE_TIME);
        transcription = dartsDatabase.save(transcription);

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL_TRANSCRIPTION, transcription.getId());
        String expected = TestUtils.removeTags(TAGS_TO_IGNORE,
                                               getContentsFromFile(
                                                   "tests/transcriptions/transcription/expectedResponse.json")
        );
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();
        String actualResponse = TestUtils.removeTags(TAGS_TO_IGNORE, mvcResult.getResponse().getContentAsString());
        JSONAssert.assertEquals(expected, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void getTranscriptionNotFound() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL_TRANSCRIPTION, -999);
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isNotFound()).andReturn();
        String actualResponse = response.getResponse().getContentAsString();
        String expectedResponse = getContentsFromFile("tests/transcriptions/transcription/expectedResponseNotFound.json");
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

}
