package uk.gov.hmcts.darts.hearings.controller;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import uk.gov.hmcts.darts.test.common.data.CourtroomTestData;
import uk.gov.hmcts.darts.test.common.data.JudgeTestData;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.SuperAdminUserStub;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;


@AutoConfigureMockMvc
@Slf4j
class HearingsControllerAdminGetHearingIntTest extends IntegrationBase {

    @Autowired
    private transient MockMvc mockMvc;

    public static final String ENDPOINT_URL = "/admin/hearings/{hearingId}";

    @MockitoBean
    private UserIdentity userIdentity;
    @Autowired
    private SuperAdminUserStub superAdminUserStub;

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
    void beforeEach() {
        var courtCase = PersistableFactory.getCourtCaseTestData().someMinimalCase();
        var hearing = PersistableFactory.getHearingTestData().createHearingWithDefaults(courtCase,
                                                                                        CourtroomTestData
                                                                                            .createCourtRoomWithNameAtCourthouse(
                                                                                                courtCase.getCourthouse(), "room"),
                                                                                        LocalDate.parse(SOME_DATE_TIME),
                                                                                        JudgeTestData.createJudgeWithName("1JUDGE1"));

        hearingEntity = dartsPersistence.save(hearing);
    }

    @ParameterizedTest(name = "Hearing is actual: {0}")
    @ValueSource(booleans = {true, false})
    void adminGetHearing_usingAValidHearingId_shouldReturnCorrectData(boolean hearingIsActual) throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);
        hearingEntity.setHearingIsActual(hearingIsActual);
        dartsPersistence.save(hearingEntity);
        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL, hearingEntity.getId());
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(MockMvcResultMatchers.status().isOk()).andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();
        String expectedJson = """
            {
                "id": 1,
                "hearing_date": "2023-01-01",
                "hearing_is_actual": <hearing_is_actual>,
                "courtroom": {
                    "id": 1,
                    "name": "ROOM"
                },
                "judges": [
                    "1JUDGE1"
                ],
                "created_at": "<created_at>",
                "created_by": 15008,
                "last_modified_at": "<last_modified_at>",
                "last_modified_by": 15008,
                "case": {
                    "id": 1,
                    "case_number": "case-1",
                    "courthouse": {
                        "id": 1,
                        "display_name": "<courthouse>"
                    },
                    "defendants": [
                        "aDefendant"
                    ],
                    "prosecutors": [
                        "aProsecutor"
                    ],
                    "defenders": [
                        "aDefence"
                    ],
                    "judges": [
                        "1JUDGE1"
                    ]
                }
            }
            """
            .replace("<courthouse>", hearingEntity.getCourtroom().getCourthouse().getDisplayName())
            .replace("<created_at>", hearingEntity.getCreatedDateTime().format(DateTimeFormatter.ISO_DATE_TIME))
            .replace("<last_modified_at>", hearingEntity.getLastModifiedDateTime().format(DateTimeFormatter.ISO_DATE_TIME))
            .replaceAll("<hearing_is_actual>", String.valueOf(hearingIsActual));
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void adminGetHearing_usingAHearingIdThatDoesNotExist_shouldReturnNotFound() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(userIdentity);
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
    void adminGetHearing_whenNotAuthentication_shouldReturnForbiddenError() throws Exception {

        HearingEntity hearing = dartsDatabase.createHearing(
            "testCourthouse",
            "testCourtroom",
            "testCaseNumber",
            LocalDateTime.of(2020, 6, 20, 10, 0, 0)
        );

        JudgeEntity testJudge = dartsDatabase.createSimpleJudge("testJudge");
        hearing.addJudge(testJudge, false);
        dartsDatabase.save(hearing);


        MockHttpServletRequestBuilder requestBuilder = get(ENDPOINT_URL, hearing.getId());

        MvcResult response = mockMvc.perform(requestBuilder)
            .andExpect(MockMvcResultMatchers.status().isForbidden())
            .andReturn();

        String actualResponse = response.getResponse().getContentAsString();

        String expectedResponse = """
            {"type":"AUTHORISATION_109","title":"User is not authorised for this endpoint","status":403}
            """;
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }
}