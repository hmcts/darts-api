package uk.gov.hmcts.darts.cases.controller;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.darts.authorisation.exception.AuthorisationError;
import uk.gov.hmcts.darts.cases.model.AdminCasesSearchRequest;
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
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.test.common.data.SecurityGroupTestData;
import uk.gov.hmcts.darts.testutils.GivenBuilder;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.cases.CasesConstants.GetSearchCasesParams.ENDPOINT_URL;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.APPROVER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;
import static uk.gov.hmcts.darts.test.common.TestUtils.getContentsFromFile;
import static uk.gov.hmcts.darts.test.common.data.CourthouseTestData.createCourthouseWithDifferentNameAndDisplayName;
import static uk.gov.hmcts.darts.test.common.data.CourthouseTestData.createCourthouseWithName;
import static uk.gov.hmcts.darts.test.common.data.CourtroomTestData.createCourtRoomWithNameAtCourthouse;
import static uk.gov.hmcts.darts.test.common.data.DefendantTestData.createDefendantForCaseWithName;
import static uk.gov.hmcts.darts.test.common.data.EventTestData.createEventWith;
import static uk.gov.hmcts.darts.test.common.data.JudgeTestData.createJudgeWithName;
import static uk.gov.hmcts.darts.test.common.data.SecurityGroupTestData.createGroupForRole;
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
    CourthouseEntity myCourthouseWithDifferentDisplayName;
    UserAccountEntity user;


    @BeforeEach
    void setupData() {
        swanseaCourthouse = createCourthouseWithName("SWANSEA");
        myCourthouseWithDifferentDisplayName = createCourthouseWithDifferentNameAndDisplayName("TS0001", "MyCourthouseDisplayName");

        CourtCaseEntity case1 = PersistableFactory.getCourtCaseTestData().createCaseAt(swanseaCourthouse);
        case1.setCaseNumber("Case1");

        CourtCaseEntity case2 = PersistableFactory.getCourtCaseTestData().createCaseAt(swanseaCourthouse);
        case2.setCaseNumber("Case2");
        case2.setDefendantList(new ArrayList<>(List.of(createDefendantForCaseWithName(case2, "Defendant2"))));

        CourtCaseEntity case3 = PersistableFactory.getCourtCaseTestData().createCaseAt(swanseaCourthouse);
        case3.setCaseNumber("Case3");

        CourtCaseEntity case4 = PersistableFactory.getCourtCaseTestData().createCaseAt(swanseaCourthouse);
        case4.setCaseNumber("Case4");

        CourtCaseEntity case5 = PersistableFactory.getCourtCaseTestData().createCaseAt(swanseaCourthouse);
        case5.setCaseNumber("case5");

        CourtCaseEntity case6 = PersistableFactory.getCourtCaseTestData().createCaseAt(swanseaCourthouse);
        case6.setCaseNumber("case6");

        CourtCaseEntity case7 = PersistableFactory.getCourtCaseTestData().createCaseAt(myCourthouseWithDifferentDisplayName);
        case7.setCaseNumber("Case7");

        JudgeEntity judge = createJudgeWithName("aJudge");
        CourtroomEntity courtroom1 = createCourtRoomWithNameAtCourthouse(swanseaCourthouse, "1");

        HearingEntity hearing1a = PersistableFactory.getHearingTestData().createHearingWithDefaults(case1, courtroom1, LocalDate.of(2023, 5, 20), judge);

        HearingEntity hearing1b = PersistableFactory.getHearingTestData().createHearingWithDefaults(case1, courtroom1, LocalDate.of(2023, 5, 21), judge);

        HearingEntity hearing1c = PersistableFactory.getHearingTestData().createHearingWithDefaults(case1, courtroom1, LocalDate.of(2023, 5, 22), judge);

        HearingEntity hearing2a = PersistableFactory.getHearingTestData().createHearingWithDefaults(case2, courtroom1, LocalDate.of(2023, 6, 20), judge);

        HearingEntity hearing2b = PersistableFactory.getHearingTestData().createHearingWithDefaults(case2, courtroom1, LocalDate.of(2023, 6, 21), judge);

        HearingEntity hearing2c = PersistableFactory.getHearingTestData().createHearingWithDefaults(case2, courtroom1, LocalDate.of(2023, 6, 22), judge);

        HearingEntity hearing3a = PersistableFactory.getHearingTestData().createHearingWithDefaults(case3, courtroom1, LocalDate.of(2023, 7, 20), judge);
        JudgeEntity judge3a = createJudgeWithName("Judge3a");
        hearing3a.addJudge(judge3a, false);

        HearingEntity hearing3b = PersistableFactory.getHearingTestData().createHearingWithDefaults(case3, courtroom1, LocalDate.of(2023, 7, 21), judge);

        HearingEntity hearing3c = PersistableFactory.getHearingTestData().createHearingWithDefaults(case3, courtroom1, LocalDate.of(2023, 7, 22), judge);

        CourtroomEntity courtroom2 = createCourtRoomWithNameAtCourthouse(swanseaCourthouse, "courtroom2");

        HearingEntity hearing4a = PersistableFactory.getHearingTestData().createHearingWithDefaults(case4, courtroom2, LocalDate.of(2023, 8, 20), judge);

        HearingEntity hearing4b = PersistableFactory.getHearingTestData().createHearingWithDefaults(case4, courtroom1, LocalDate.of(2023, 8, 21), judge);

        HearingEntity hearing4c = PersistableFactory.getHearingTestData().createHearingWithDefaults(case4, courtroom1, LocalDate.of(2023, 8, 22), judge);

        HearingEntity hearing5a = PersistableFactory.getHearingTestData().createHearingWithDefaults(case5, courtroom2, LocalDate.of(2023, 9, 20), judge);

        HearingEntity hearing5b = PersistableFactory.getHearingTestData().createHearingWithDefaults(case5, courtroom1, LocalDate.of(2023, 9, 21), judge);

        CourtroomEntity courtroom3 = createCourtRoomWithNameAtCourthouse(swanseaCourthouse, "courtroom3");
        HearingEntity hearing5c = PersistableFactory.getHearingTestData().createHearingWithDefaults(case5, courtroom3, LocalDate.of(2023, 9, 22), judge);

        HearingEntity hearing6a = PersistableFactory.getHearingTestData().createHearingWithDefaults(case6, courtroom2, LocalDate.of(2023, 9, 20), judge);

        HearingEntity hearing6b = PersistableFactory.getHearingTestData().createHearingWithDefaults(case6, courtroom3, LocalDate.of(2023, 9, 21), judge);
        hearing6b.addJudge(createJudgeWithName("Judge6b"), false);

        HearingEntity hearing6c = PersistableFactory.getHearingTestData().createHearingWithDefaults(case6, courtroom1, LocalDate.of(2023, 9, 22), judge);

        CourtroomEntity courtroom7 = createCourtRoomWithNameAtCourthouse(myCourthouseWithDifferentDisplayName, "courtroom7");

        HearingEntity hearing7a = PersistableFactory.getHearingTestData().createHearingWithDefaults(case7, courtroom7, LocalDate.of(2023, 5, 20), judge);

        HearingEntity hearing7b = PersistableFactory.getHearingTestData().createHearingWithDefaults(case7, courtroom7, LocalDate.of(2023, 5, 21), judge);

        HearingEntity hearing7c = PersistableFactory.getHearingTestData().createHearingWithDefaults(case7, courtroom7, LocalDate.of(2023, 5, 22), judge);

        dartsDatabase.saveAll(hearing1a, hearing1b, hearing1c,
                              hearing2a, hearing2b, hearing2c,
                              hearing3a, hearing3b, hearing3c,
                              hearing4a, hearing4b, hearing4c,
                              hearing5a, hearing5b, hearing5c,
                              hearing6a, hearing6b, hearing6c,
                              hearing7a, hearing7b, hearing7c

        );

        EventEntity event4a = createEventWith("eventName", "event4a", hearing4a, OffsetDateTime.now());
        EventEntity event5b = createEventWith("eventName", "event5b", hearing5b, OffsetDateTime.now());
        dartsDatabase.saveAll(event4a, event5b);

        givenBearerTokenExists(INTEGRATION_TEST_USER_EMAIL);
    }

    @Test
    void casesSearchByCourthouseIdsPost_shouldReturnCasesMatchingSearchCriteria_whenCourthouseIdProvided() throws Exception {
        setupUserAndSecurityGroupForCourthouses(List.of(swanseaCourthouse));
        String requestBody = """
            {
              "courthouse_ids": [<courthouse-id>],
              "courtroom": "1",
              "date_to": "2023-05-20"
            }""";

        requestBody = requestBody.replace("<courthouse-id>", swanseaCourthouse.getId().toString());

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
    void casesSearchByCourthouseIdsPost_shouldReturnCasesMatchingSearchCriteria_whenMultipleCourthouseIdsProvided() throws Exception {

        setupUserAndSecurityGroupForCourthouses(List.of(swanseaCourthouse, myCourthouseWithDifferentDisplayName));
        String requestBody = """
            {
              "courthouse_ids": [<courthouse-id1>, <courthouse-id2>],
              "date_from": "2023-05-19",
              "date_to": "2023-05-20"
            }""";

        requestBody = requestBody.replace("<courthouse-id1>", swanseaCourthouse.getId().toString());
        requestBody = requestBody.replace("<courthouse-id2>", myCourthouseWithDifferentDisplayName.getId().toString());

        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT_URL)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(requestBody);
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();

        String actualResponse = TestUtils.removeIds(response.getResponse().getContentAsString());

        String expectedResponse = getContentsFromFile(
            "tests/cases/CaseControllerSearchGetTest/casesSearchGetEndpoint/expectedResponseMultipleCourthouses.json");
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void casesSearchPostEndpoint() throws Exception {
        //FIXME: Remove this test once move to courthouse_ids has been merged (DMP-4912)
        user = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        setupUserAndSecurityGroupForCourthouses(List.of(swanseaCourthouse));
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
    void caseSearch_multipleReturned_shouldBeOrderedByCaseId() throws Exception {
        user = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        setupUserAndSecurityGroupForCourthouses(List.of(swanseaCourthouse));
        String requestBody = """
            {
              "courthouse_ids": [<courthouse-id>],
              "courtroom": "1",
              "case_number": "case"
            }""";

        requestBody = requestBody.replace("<courthouse-id>", swanseaCourthouse.getId().toString());

        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT_URL)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(requestBody);
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();

        String actualResponse = response.getResponse().getContentAsString();

        String expectedResponse = getContentsFromFile(
            "tests/cases/CaseControllerSearchGetTest/casesSearchGetEndpoint/expectedResponseMultiple.json");
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.STRICT);
    }

    @Test
    void casesSearchPostEndpointDateRange() throws Exception {
        user = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        setupUserAndSecurityGroupForCourthouses(List.of(swanseaCourthouse));
        String requestBody = """
            {
              "courthouse_ids": [<courthouse-id>],
              "courtroom": "1",
              "date_to": "2023-09-20",
              "date_from": "2023-05-20"
            }""";

        requestBody = requestBody.replace("<courthouse-id>", swanseaCourthouse.getId().toString());

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
        setupUserAndSecurityGroupForCourthouses(List.of(swanseaCourthouse));
        String requestBody = """
            {
              "courthouse_ids": [<courthouse-id>],
              "date_to": "2023-05-20",
              "date_from": "2023-05-20"
            }""";

        requestBody = requestBody.replace("<courthouse-id>", swanseaCourthouse.getId().toString());

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
                    "courtroom": "1",
                    "judges": [
                      "AJUDGE"
                    ]
                  },
                  {
                    "date": "2023-05-21",
                    "courtroom": "1",
                    "judges": [
                      "AJUDGE"
                    ]
                  },
                  {
                    "date": "2023-05-22",
                    "courtroom": "1",
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
        setupUserAndSecurityGroupForCourthouses(List.of(swanseaCourthouse));
        String requestBody = """
            {
              "courthouse_ids": [<courthouse-id>],
              "date_from": "2023-05-19",
              "date_to": "2023-05-20"
            }""";

        requestBody = requestBody.replace("<courthouse-id>", swanseaCourthouse.getId().toString());

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
                    "courtroom": "1",
                    "judges": [
                      "AJUDGE"
                    ]
                  },
                  {
                    "date": "2023-05-21",
                    "courtroom": "1",
                    "judges": [
                      "AJUDGE"
                    ]
                  },
                  {
                    "date": "2023-05-22",
                    "courtroom": "1",
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
    void courthouseAndCourtroomAndDateRangeWhenCourthouseDisplayNameDoesNotMatchCourthouseName() throws Exception {
        //FIXME: Remove this test once move to courthouse_ids has been merged (DMP-4912)
        user = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();

        setupUserAndSecurityGroupForCourthouses(List.of(myCourthouseWithDifferentDisplayName));
        String requestBody = """
            {
              "courthouse": "MYCOURTHOUSEDISPLAYNAME",
              "courtroom": "COURTROOM7",
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
                "case_number": "Case7",
                "courthouse": "MyCourthouseDisplayName",
                "defendants": [
                  "aDefendant"
                ],
                "judges": [
                  "AJUDGE"
                ],
                "hearings": [
                  {
                    "date": "2023-05-20",
                    "courtroom": "COURTROOM7",
                    "judges": [
                      "AJUDGE"
                    ]
                  },
                  {
                    "date": "2023-05-21",
                    "courtroom": "COURTROOM7",
                    "judges": [
                      "AJUDGE"
                    ]
                  },
                  {
                    "date": "2023-05-22",
                    "courtroom": "COURTROOM7",
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
    void courthouseAndDateRangeIsAnonymised() throws Exception {
        user = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        CourtCaseEntity caseEntity = dartsDatabase.getCaseRepository()
            .findByCaseNumberAndCourthouse_CourthouseName("Case1", "SWANSEA").orElseThrow();
        OffsetDateTime dataAnonymisedTs = OffsetDateTime.parse("2023-01-01T12:00:00Z");
        caseEntity.setDataAnonymisedTs(dataAnonymisedTs);
        caseEntity.setDataAnonymised(true);
        dartsDatabase.getCaseRepository().save(caseEntity);

        setupUserAndSecurityGroupForCourthouses(List.of(swanseaCourthouse));
        String requestBody = """
            {
              "courthouse_ids": [<courthouse-id>],
              "date_from": "2023-05-19",
              "date_to": "2023-05-20"
            }""";

        requestBody = requestBody.replace("<courthouse-id>", swanseaCourthouse.getId().toString());

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
                    "courtroom": "1",
                    "judges": [
                      "AJUDGE"
                    ]
                  },
                  {
                    "date": "2023-05-21",
                    "courtroom": "1",
                    "judges": [
                      "AJUDGE"
                    ]
                  },
                  {
                    "date": "2023-05-22",
                    "courtroom": "1",
                    "judges": [
                      "AJUDGE"
                    ]
                  }
                ],
                "is_data_anonymised": true,
                "data_anonymised_at":"2023-01-01T12:00:00Z"
              }
            ]
            """;
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void casesSearchPostEndpointEventText() throws Exception {
        user = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        setupUserAndSecurityGroupForCourthouses(List.of(swanseaCourthouse));
        String requestBody = """
            {
              "courthouse_ids": [<courthouse-id>],
              "courtroom": "1",
              "event_text_contains": "t5b"
            }""";

        requestBody = requestBody.replace("<courthouse-id>", swanseaCourthouse.getId().toString());

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
        setupUserAndSecurityGroupForCourthouses(List.of(swanseaCourthouse));
        String requestBody = """
            {
              "courthouse_ids": [<courthouse-id>],
              "courtroom": "1",
              "judge_name": "e3a"
            }""";

        requestBody = requestBody.replace("<courthouse-id>", swanseaCourthouse.getId().toString());

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
        setupUserAccountAndSecurityGroup(swanseaCourthouse);

        String requestBody = """
            {
              "courthouse_ids": [<courthouse-id>],
              "courtroom": "1",
              "judge_name": "e3a"
            }""";

        requestBody = requestBody.replace("<courthouse-id>", swanseaCourthouse.getId().toString());

        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT_URL)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(requestBody);
        mockMvc.perform(requestBuilder).andExpect(status().isForbidden()).andExpect(jsonPath("$.type").value(
            AuthorisationError.USER_DETAILS_INVALID.getType()));

    }

    @Test
    void casesSearchPost_shouldReturn400_whenCourtroomIsLowercase() throws Exception {
        setupUserAndSecurityGroupForCourthouses(List.of(swanseaCourthouse));
        String requestBody = """
            {
              "courthouse_ids": [<courthouse-id>],
              "courtroom": "courtroom1",
              "date_to": "2023-05-20"
            }""";

        MockHttpServletRequestBuilder requestBuilder = post("/cases/search")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(requestBody);
        mockMvc.perform(requestBuilder)
            .andExpect(status().isBadRequest());
    }

    @Test
    void casesSearchPost_shouldReturn400_whenEventTextLengthIs2() throws Exception {
        user = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        setupUserAccountAndSecurityGroup(swanseaCourthouse);
        String requestBody = """
            {
             "courthouse": "SWANSEA",
              "courtroom": "1",
              "event_text_contains": "t5"
            }""";

        MockHttpServletRequestBuilder requestBuilder = post("/cases/search")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(requestBody);
        MvcResult response = mockMvc.perform(requestBuilder)
            .andExpect(status().isBadRequest())
            .andReturn();


        String expectedResponse = """
            {
              "violations": [
                {
                  "field": "eventTextContains",
                  "message": "size must be between 3 and 2000"
                }
              ],
              "type": "https://zalando.github.io/problem/constraint-violation",
              "status": 400,
              "title": "Constraint Violation"
            }
            """;
        String actualResponse = response.getResponse().getContentAsString();
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void casesSearchPost_shouldReturn400_whenJudgeNameLengthIs2() throws Exception {
        user = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        setupUserAccountAndSecurityGroup(swanseaCourthouse);
        String requestBody = """
            {
             "courthouse": "SWANSEA",
              "courtroom": "1",
              "judge_name": "t5"
            }""";

        MockHttpServletRequestBuilder requestBuilder = post("/cases/search")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(requestBody);
        MvcResult response = mockMvc.perform(requestBuilder)
            .andExpect(status().isBadRequest())
            .andReturn();


        String expectedResponse = """
            {
              "violations": [
                {
                  "field": "judgeName",
                  "message": "size must be between 3 and 2000"
                }
              ],
              "type": "https://zalando.github.io/problem/constraint-violation",
              "status": 400,
              "title": "Constraint Violation"
            }
            """;
        String actualResponse = response.getResponse().getContentAsString();
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void casesSearchPost_shouldReturn400_whenDefendantNameLengthIs2() throws Exception {
        user = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        setupUserAccountAndSecurityGroup(swanseaCourthouse);
        String requestBody = """
            {
             "courthouse": "SWANSEA",
              "courtroom": "1",
              "defendant_name": "t5"
            }""";

        MockHttpServletRequestBuilder requestBuilder = post("/cases/search")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(requestBody);
        MvcResult response = mockMvc.perform(requestBuilder)
            .andExpect(status().isBadRequest())
            .andReturn();


        String expectedResponse = """
            {
              "violations": [
                {
                  "field": "defendantName",
                  "message": "size must be between 3 and 2000"
                }
              ],
              "type": "https://zalando.github.io/problem/constraint-violation",
              "status": 400,
              "title": "Constraint Violation"
            }
            """;
        String actualResponse = response.getResponse().getContentAsString();
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void casesSearchPost_shouldReturn400_whenCourthouseNameLengthIs2() throws Exception {
        user = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        setupUserAccountAndSecurityGroup(swanseaCourthouse);
        String requestBody = """
            {
             "courthouse": "SW",
              "courtroom": "1"
            }""";

        MockHttpServletRequestBuilder requestBuilder = post("/cases/search")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(requestBody);
        MvcResult response = mockMvc.perform(requestBuilder)
            .andExpect(status().isBadRequest())
            .andReturn();


        String expectedResponse = """
            {
              "violations": [
                {
                  "field": "courthouse",
                  "message": "size must be between 3 and 2000"
                }
              ],
              "type": "https://zalando.github.io/problem/constraint-violation",
              "status": 400,
              "title": "Constraint Violation"
            }
            """;
        String actualResponse = response.getResponse().getContentAsString();
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void casesSearchPost_shouldReturn400_whenCourthouseIsLowercase() throws Exception {
        //FIXME: Remove this test once move to courthouse_ids has been merged (DMP-4912)
        user = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        setupUserAndSecurityGroupForCourthouses(List.of(swanseaCourthouse));
        String requestBody = """
            {
              "courthouse": "swansea",
              "courtroom": "COURTROOM1",
              "date_to": "2023-05-20"
            }""";

        MockHttpServletRequestBuilder requestBuilder = post("/cases/search")
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(requestBody);
        mockMvc.perform(requestBuilder)
            .andExpect(status().isBadRequest());
    }

    @Test
    void adminCasesSearchPost_shouldReturnBadRequest_whenCourtroomNameIsLowercase() throws Exception {
        // Given
        user = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        SecurityGroupEntity securityGroup = SecurityGroupTestData.buildGroupForRoleAndCourthouse(SUPER_ADMIN, swanseaCourthouse);
        securityGroup.setGlobalAccess(true);
        assignSecurityGroupToUser(user, securityGroup);

        AdminCasesSearchRequest request = new AdminCasesSearchRequest();
        request.setCourtroomName("courtroom1");  // lowercase value

        // When/Then
        mockMvc.perform(post("/admin/cases/search")
                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                            .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(result -> {
                String response = result.getResponse().getContentAsString();
                Assertions.assertTrue(response.contains("Courthouse and courtroom must be uppercase"));
            });
    }

    private void setupUserAccountAndSecurityGroup(CourthouseEntity courthouse) {
        var securityGroup = SecurityGroupTestData.buildGroupForRoleAndCourthouse(APPROVER, courthouse);
        securityGroup.setGlobalAccess(false);
        securityGroup.setUseInterpreter(false);
        assignSecurityGroupToUser(user, securityGroup);
    }

    private void setupUserAndSecurityGroupForCourthouses(List<CourthouseEntity> courthouseEntities) {
        String guid = UUID.randomUUID().toString();
        var securityGroup = createGroupForRole(APPROVER);
        securityGroup.setGlobalAccess(false);
        securityGroup.setUseInterpreter(false);
        securityGroup = dartsDatabase.save(securityGroup);
        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().createExternalUser(guid, securityGroup, courthouseEntities);
        GivenBuilder.anAuthenticatedUserFor(testUser);
    }

    private void assignSecurityGroupToUser(UserAccountEntity user, SecurityGroupEntity securityGroup) {
        securityGroup.getUsers().add(user);
        user.getSecurityGroupEntities().add(securityGroup);
        securityGroupRepository.save(securityGroup);
        userAccountRepository.save(user);
    }

}