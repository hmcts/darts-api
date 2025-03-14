package uk.gov.hmcts.darts.hearings.controller;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.JudgeEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.test.common.data.CourtroomTestData;
import uk.gov.hmcts.darts.test.common.data.JudgeTestData;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@AutoConfigureMockMvc
@Slf4j
class HearingsGetControllerTest extends IntegrationBase {

    @Autowired
    private transient MockMvc mockMvc;

    public static final String ENDPOINT_URL = "/hearings/{hearingId}";

    @MockitoBean
    private UserIdentity mockUserIdentity;

    private HearingEntity hearingEntity;

    private static final String SOME_DATE_TIME = "2023-01-01";

    @BeforeEach
    void setupOpenInView() {
        openInViewUtil.openEntityManager();
    }

    @AfterEach
    void closeOpenInView() {
        openInViewUtil.closeEntityManager();
    }

    @BeforeEach
    void setUp() {
        var courtCase = PersistableFactory.getCourtCaseTestData().someMinimalCase();
        var hearing = PersistableFactory.getHearingTestData().createHearingWithDefaults(courtCase,
                                                                                        CourtroomTestData
                                                                                            .createCourtRoomWithNameAtCourthouse(
                                                                                                courtCase.getCourthouse(), "room"),
                                                                                        LocalDate.parse(SOME_DATE_TIME),
                                                                                        JudgeTestData.createJudgeWithName("1JUDGE1"));

        hearingEntity = dartsPersistence.save(hearing);

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
               "courthouse_id": <courthouse-id>,
               "courthouse": "<COURTHOUSE>",
               "courtroom": "<COURTROOM>",
               "hearing_date": "<hearing-date>",
               "case_id": <case-id>,
               "case_number": "<case_number>",
               "judges": [
                 "1JUDGE1"
               ],
               "transcription_count": 0,
               "case_reporting_restrictions":[]
             }
            """.replace("<COURTROOM>", hearingEntity.getCourtroom().getName())
            .replace("<COURTHOUSE>", hearingEntity.getCourtroom().getCourthouse().getDisplayName());
        log.info(actualJson);
        expectedJson = expectedJson.replace("<hearing-id>", hearingEntity.getId().toString());
        expectedJson = expectedJson.replace("<courthouse-id>", hearingEntity.getCourtCase().getCourthouse().getId().toString());
        expectedJson = expectedJson.replace("<case-id>", hearingEntity.getCourtCase().getId().toString());
        expectedJson = expectedJson.replace("<case_number>", hearingEntity.getCourtCase().getCaseNumber());
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
              "type": "HEARING_100",
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
            LocalDateTime.of(2020, 6, 20, 10, 0, 0)
        );

        JudgeEntity testJudge = dartsDatabase.createSimpleJudge("testJudge");
        hearing.addJudge(testJudge, false);
        dartsDatabase.save(hearing);

        when(mockUserIdentity.getUserAccount()).thenReturn(null);

        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL, hearing.getId());

        MvcResult response = mockMvc.perform(requestBuilder)
            .andExpect(MockMvcResultMatchers.status().isForbidden())
            .andReturn();

        String actualResponse = response.getResponse().getContentAsString();

        String expectedResponse = """
            {"type":"AUTHORISATION_106","title":"Could not obtain user details","status":403}
            """;
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

}