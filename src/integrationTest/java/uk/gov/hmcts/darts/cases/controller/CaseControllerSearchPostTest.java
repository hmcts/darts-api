package uk.gov.hmcts.darts.cases.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.authorisation.exception.AuthorisationError;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.JudgeEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.SecurityGroupRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.test.common.TestUtils;
import uk.gov.hmcts.darts.test.common.data.SecurityGroupTestData;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.cases.CasesConstants.GetSearchCasesParams.ENDPOINT_URL;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.APPROVER;
import static uk.gov.hmcts.darts.test.common.TestUtils.getContentsFromFile;
import static uk.gov.hmcts.darts.test.common.data.CaseTestData.createCaseAt;
import static uk.gov.hmcts.darts.test.common.data.CourthouseTestData.createCourthouseWithName;
import static uk.gov.hmcts.darts.test.common.data.CourtroomTestData.createCourtRoomWithNameAtCourthouse;
import static uk.gov.hmcts.darts.test.common.data.DefendantTestData.createDefendantForCaseWithName;
import static uk.gov.hmcts.darts.test.common.data.EventTestData.createEventWith;
import static uk.gov.hmcts.darts.test.common.data.HearingTestData.createHearingWithDefaults;
import static uk.gov.hmcts.darts.test.common.data.JudgeTestData.createJudgeWithName;
import static uk.gov.hmcts.darts.testutils.stubs.UserAccountStub.INTEGRATION_TEST_USER_EMAIL;

@AutoConfigureMockMvc
@SuppressWarnings({"PMD.VariableDeclarationUsageDistance", "PMD.NcssCount", "PMD.ExcessiveImports"})
class CaseControllerSearchPostTest extends IntegrationBase {

    @Autowired
    SecurityGroupRepository securityGroupRepository;
    @Autowired
    UserAccountRepository userAccountRepository;
    @Autowired
    private transient MockMvc mockMvc;
    CourthouseEntity swanseaCourthouse;
    UserAccountEntity user;

    @BeforeEach
    void setupData() {
        swanseaCourthouse = createCourthouseWithName("SWANSEA");

        CourtCaseEntity case1 = createCaseAt(swanseaCourthouse);
        case1.setCaseNumber("Case1");

        CourtCaseEntity case2 = createCaseAt(swanseaCourthouse);
        case2.setCaseNumber("Case2");
        case2.setDefendantList(new ArrayList<>(List.of(createDefendantForCaseWithName(case2, "Defendant2"))));

        CourtCaseEntity case3 = createCaseAt(swanseaCourthouse);
        case3.setCaseNumber("Case3");

        CourtCaseEntity case4 = createCaseAt(swanseaCourthouse);
        case4.setCaseNumber("Case4");

        CourtCaseEntity case5 = createCaseAt(swanseaCourthouse);
        case5.setCaseNumber("case5");

        CourtCaseEntity case6 = createCaseAt(swanseaCourthouse);
        case6.setCaseNumber("case6");

        JudgeEntity judge = createJudgeWithName("aJudge");
        CourtroomEntity courtroom1 = createCourtRoomWithNameAtCourthouse(swanseaCourthouse, "courtroom1");

        HearingEntity hearing1a = createHearingWithDefaults(case1, courtroom1, LocalDate.of(2023, 5, 20), judge);

        HearingEntity hearing1b = createHearingWithDefaults(case1, courtroom1, LocalDate.of(2023, 5, 21), judge);

        HearingEntity hearing1c = createHearingWithDefaults(case1, courtroom1, LocalDate.of(2023, 5, 22), judge);

        HearingEntity hearing2a = createHearingWithDefaults(case2, courtroom1, LocalDate.of(2023, 6, 20), judge);

        HearingEntity hearing2b = createHearingWithDefaults(case2, courtroom1, LocalDate.of(2023, 6, 21), judge);

        HearingEntity hearing2c = createHearingWithDefaults(case2, courtroom1, LocalDate.of(2023, 6, 22), judge);

        HearingEntity hearing3a = createHearingWithDefaults(case3, courtroom1, LocalDate.of(2023, 7, 20), judge);
        JudgeEntity judge3a = createJudgeWithName("Judge3a");
        hearing3a.addJudge(judge3a, false);

        HearingEntity hearing3b = createHearingWithDefaults(case3, courtroom1, LocalDate.of(2023, 7, 21), judge);

        HearingEntity hearing3c = createHearingWithDefaults(case3, courtroom1, LocalDate.of(2023, 7, 22), judge);

        CourtroomEntity courtroom2 = createCourtRoomWithNameAtCourthouse(swanseaCourthouse, "courtroom2");

        HearingEntity hearing4a = createHearingWithDefaults(case4, courtroom2, LocalDate.of(2023, 8, 20), judge);

        HearingEntity hearing4b = createHearingWithDefaults(case4, courtroom1, LocalDate.of(2023, 8, 21), judge);

        HearingEntity hearing4c = createHearingWithDefaults(case4, courtroom1, LocalDate.of(2023, 8, 22), judge);

        HearingEntity hearing5a = createHearingWithDefaults(case5, courtroom2, LocalDate.of(2023, 9, 20), judge);

        HearingEntity hearing5b = createHearingWithDefaults(case5, courtroom1, LocalDate.of(2023, 9, 21), judge);

        CourtroomEntity courtroom3 = createCourtRoomWithNameAtCourthouse(swanseaCourthouse, "courtroom3");
        HearingEntity hearing5c = createHearingWithDefaults(case5, courtroom3, LocalDate.of(2023, 9, 22), judge);

        HearingEntity hearing6a = createHearingWithDefaults(case6, courtroom2, LocalDate.of(2023, 9, 20), judge);

        HearingEntity hearing6b = createHearingWithDefaults(case6, courtroom3, LocalDate.of(2023, 9, 21), judge);
        hearing6b.addJudge(createJudgeWithName("Judge6b"), false);

        HearingEntity hearing6c = createHearingWithDefaults(case6, courtroom1, LocalDate.of(2023, 9, 22), judge);

        dartsDatabase.saveAll(hearing1a, hearing1b, hearing1c,
                              hearing2a, hearing2b, hearing2c,
                              hearing3a, hearing3b, hearing3c,
                              hearing4a, hearing4b, hearing4c,
                              hearing5a, hearing5b, hearing5c,
                              hearing6a, hearing6b, hearing6c

        );

        EventEntity event4a = createEventWith("eventName", "event4a", hearing4a, OffsetDateTime.now());
        EventEntity event5b = createEventWith("eventName", "event5b", hearing5b, OffsetDateTime.now());
        dartsDatabase.saveAll(event4a, event5b);

        givenBearerTokenExists(INTEGRATION_TEST_USER_EMAIL);
    }

    @Test
    void casesSearchPostEndpoint() throws Exception {
        user = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        setupUserAccountAndSecurityGroup();
        String requestBody = """
            {
              "courthouse": "SWANSEA",
              "courtroom": "1",
              "date_to": "2023-05-20"
            }""";

        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT_URL)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(requestBody);
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();

        String actualResponse = TestUtils.removeIds(response.getResponse().getContentAsString());

        String expectedResponse = getContentsFromFile(
            "tests/cases/CaseControllerSearchGetTest/casesSearchGetEndpoint/expectedResponse.json");
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void casesSearchPostEndpointDateRange() throws Exception {
        user = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        setupUserAccountAndSecurityGroup();
        String requestBody = """
            {
              "courthouse": "SWANSEA",
              "courtroom": "courtroom",
              "date_to": "2023-09-20",
              "date_from": "2023-05-20"
            }""";

        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT_URL)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(requestBody);
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();

        String actualResponse = TestUtils.removeIds(response.getResponse().getContentAsString());

        String expectedResponse = getContentsFromFile(
            "tests/cases/CaseControllerSearchGetTest/casesSearchGetEndpoint/expectedResponseDateRange.json");
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void courthouseAndSpecificDate() throws Exception {
        user = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        setupUserAccountAndSecurityGroup();
        String requestBody = """
            {
              "courthouse": "SWANSEA",
              "date_to": "2023-05-20",
              "date_from": "2023-05-20"
            }""";

        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT_URL)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(requestBody);
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();

        String actualResponse = TestUtils.removeIds(response.getResponse().getContentAsString());

        String expectedResponse = """
            [
              {
                "case_number": "Case1",
                "courthouse": "SWANSEA",
                "defendants": [
                  "aDefendant"
                ],
                "judges": [
                  "AJUDGE"
                ],
                "hearings": [
                  {
                    "date": "2023-05-20",
                    "courtroom": "COURTROOM1",
                    "judges": [
                      "AJUDGE"
                    ]
                  },
                  {
                    "date": "2023-05-21",
                    "courtroom": "COURTROOM1",
                    "judges": [
                      "AJUDGE"
                    ]
                  },
                  {
                    "date": "2023-05-22",
                    "courtroom": "COURTROOM1",
                    "judges": [
                      "AJUDGE"
                    ]
                  }
                ],
                "is_data_anonymised": false
              }
            ]
            """;
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void courthouseAndDateRange() throws Exception {
        user = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        setupUserAccountAndSecurityGroup();
        String requestBody = """
            {
              "courthouse": "SWANSEA",
              "date_from": "2023-05-19",
              "date_to": "2023-05-20"
            }""";

        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT_URL)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(requestBody);
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();

        String actualResponse = TestUtils.removeIds(response.getResponse().getContentAsString());

        String expectedResponse = """
            [
              {
                "case_number": "Case1",
                "courthouse": "SWANSEA",
                "defendants": [
                  "aDefendant"
                ],
                "judges": [
                  "AJUDGE"
                ],
                "hearings": [
                  {
                    "date": "2023-05-20",
                    "courtroom": "COURTROOM1",
                    "judges": [
                      "AJUDGE"
                    ]
                  },
                  {
                    "date": "2023-05-21",
                    "courtroom": "COURTROOM1",
                    "judges": [
                      "AJUDGE"
                    ]
                  },
                  {
                    "date": "2023-05-22",
                    "courtroom": "COURTROOM1",
                    "judges": [
                      "AJUDGE"
                    ]
                  }
                ],
                "is_data_anonymised": false
              }
            ]
            """;
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void casesSearchPostEndpointEventText() throws Exception {
        user = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        setupUserAccountAndSecurityGroup();
        String requestBody = """
            {
              "courthouse": "SWANSEA",
              "courtroom": "1",
              "event_text_contains": "t5b"
            }""";

        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT_URL)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(requestBody);
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();

        String actualResponse = TestUtils.removeIds(response.getResponse().getContentAsString());

        String expectedResponse = getContentsFromFile(
            "tests/cases/CaseControllerSearchGetTest/casesSearchGetEndpoint/expectedResponseEventText.json");
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void casesSearchPostEndpointJudgeName() throws Exception {
        user = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        setupUserAccountAndSecurityGroup();
        String requestBody = """
            {
              "courthouse": "SWANSEA",
              "courtroom": "1",
              "judge_name": "e3a"
            }""";

        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT_URL)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(requestBody);
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();

        String actualResponse = TestUtils.removeIds(response.getResponse().getContentAsString());

        String expectedResponse = getContentsFromFile(
            "tests/cases/CaseControllerSearchGetTest/casesSearchGetEndpoint/expectedResponseJudgeName.json");
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void casesSearchPostEndpointJudgeNameInactive() throws Exception {
        user = dartsDatabase.getUserAccountStub().createJudgeUser();
        user.setActive(false);
        setupUserAccountAndSecurityGroup();

        String requestBody = """
            {
              "courthouse": "SWANSEA",
              "courtroom": "1",
              "judge_name": "e3a"
            }""";

        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT_URL)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(requestBody);
        mockMvc.perform(requestBuilder).andExpect(status().isForbidden()).andExpect(jsonPath("$.type").value(
            AuthorisationError.USER_DETAILS_INVALID.getType().toString()));

    }

    private void setupUserAccountAndSecurityGroup() {
        var securityGroup = SecurityGroupTestData.buildGroupForRoleAndCourthouse(APPROVER, swanseaCourthouse);
        securityGroup.setGlobalAccess(false);
        securityGroup.setUseInterpreter(false);
        assignSecurityGroupToUser(user, securityGroup);
    }

    private void assignSecurityGroupToUser(UserAccountEntity user, SecurityGroupEntity securityGroup) {
        securityGroup.getUsers().add(user);
        user.getSecurityGroupEntities().add(securityGroup);
        securityGroupRepository.save(securityGroup);
        userAccountRepository.save(user);
    }

}
