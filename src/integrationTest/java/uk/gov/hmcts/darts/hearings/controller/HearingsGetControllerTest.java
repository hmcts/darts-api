package uk.gov.hmcts.darts.hearings.controller;

import lombok.extern.slf4j.Slf4j;
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
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.JudgeEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@AutoConfigureMockMvc
@Transactional
@Slf4j
class HearingsGetControllerTest extends IntegrationBase {

    @Autowired
    private transient MockMvc mockMvc;

    public static final String ENDPOINT_URL = "/hearings/{hearingId}";

    @MockBean
    private UserIdentity mockUserIdentity;

    private HearingEntity hearingEntity;

    private static final OffsetDateTime SOME_DATE_TIME = OffsetDateTime.parse("2023-01-01T12:00Z");
    private static final String SOME_COURTHOUSE = "some-courthouse";
    private static final String SOME_COURTROOM = "some-courtroom";
    private static final String SOME_CASE_NUMBER = "1";

    @BeforeEach
    void setUp() {

        hearingEntity = dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            SOME_CASE_NUMBER,
            SOME_COURTHOUSE,
            SOME_COURTROOM,
            SOME_DATE_TIME.toLocalDate()
        );
        CourtCaseEntity courtCase = hearingEntity.getCourtCase();
        courtCase.addProsecutor("aProsecutor");
        courtCase.addDefendant("aDefendant");
        courtCase.addDefence("aDefence");
        dartsDatabase.save(courtCase);

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub()
            .createAuthorisedIntegrationTestUser(hearingEntity.getCourtroom().getCourthouse());
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);
    }

    @Test
    void okGet() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL, hearingEntity.getId());
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();
        String expectedJson = """
            {
               "hearing_id": <hearing-id>,
               "courthouse": "some-courthouse",
               "courtroom": "some-courtroom",
               "hearing_date": "<hearing-date>",
               "case_id": <case-id>,
               "case_number": "1",
               "judges": [
                 "1judge1"
               ],
               "transcription_count": 0,
               "case_reporting_restrictions":[]
             }
            """;
        log.info(actualJson);
        expectedJson = expectedJson.replace("<hearing-id>", hearingEntity.getId().toString());
        expectedJson = expectedJson.replace("<case-id>", hearingEntity.getCourtCase().getId().toString());
        expectedJson = expectedJson.replace("<hearing-date>", hearingEntity.getHearingDate().toString());
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.NON_EXTENSIBLE);

    }

    @Test
    void errorGetNotFound() throws Exception {
        int hearingId = -1;

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL, hearingId);
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().isNotFound()).andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();
        String expectedJson = """
            {
              "type": "100",
              "title": "The requested hearing cannot be found",
              "status": 404
            }
            """;

        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.NON_EXTENSIBLE);

    }

    @Test
    void hearingsGetEndpointShouldReturnForbiddenError() throws Exception {

        HearingEntity hearing = dartsDatabase.createHearing(
            "testCourthouse",
            "testCourtroom",
            "testCaseNumber",
            LocalDate.of(2020, 6, 20)
        );

        JudgeEntity testJudge = dartsDatabase.createSimpleJudge("testJudge");
        hearing.addJudge(testJudge);
        dartsDatabase.save(hearing);

        when(mockUserIdentity.getUserAccount()).thenReturn(null);

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL, hearing.getId());

        MvcResult response = mockMvc.perform(requestBuilder)
            .andExpect(MockMvcResultMatchers.status().isForbidden())
            .andReturn();

        String actualResponse = response.getResponse().getContentAsString();

        String expectedResponse = """
            {"type":"106","title":"Could not obtain user details","status":403}
            """;
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

}
