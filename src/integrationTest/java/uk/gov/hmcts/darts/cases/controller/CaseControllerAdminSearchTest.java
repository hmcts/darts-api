package uk.gov.hmcts.darts.cases.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
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
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;
import static uk.gov.hmcts.darts.test.common.TestUtils.getContentsFromFile;
import static uk.gov.hmcts.darts.test.common.data.CourthouseTestData.someMinimalCourthouse;
import static uk.gov.hmcts.darts.test.common.data.CourtroomTestData.createCourtRoomWithNameAtCourthouse;
import static uk.gov.hmcts.darts.test.common.data.DefendantTestData.createDefendantForCaseWithName;
import static uk.gov.hmcts.darts.test.common.data.EventTestData.createEventWith;
import static uk.gov.hmcts.darts.test.common.data.JudgeTestData.createJudgeWithName;
import static uk.gov.hmcts.darts.testutils.stubs.UserAccountStub.INTEGRATION_TEST_USER_EMAIL;


@AutoConfigureMockMvc
class CaseControllerAdminSearchTest extends IntegrationBase {

    private static final String ENDPOINT_URL = "/admin/cases/search";

    @Autowired
    private SecurityGroupRepository securityGroupRepository;
    @Autowired
    private UserAccountRepository userAccountRepository;
    @Autowired
    private MockMvc mockMvc;
    private CourthouseEntity swanseaCourthouse;
    private UserAccountEntity user;

    @BeforeEach
    void setupData() {
        swanseaCourthouse = someMinimalCourthouse();
        swanseaCourthouse.setCourthouseName("SWANSEA");
        swanseaCourthouse.setDisplayName("SWANSEA");

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

        JudgeEntity judge = createJudgeWithName("aJudge");
        CourtroomEntity courtroom1 = createCourtRoomWithNameAtCourthouse(swanseaCourthouse, "courtroom1");

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
        user = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        setupUserAccountAndSecurityGroup();
    }

    @Test
    void adminCaseSearch_ShouldReturnOk() throws Exception {

        String requestBody = """
            {
              "courthouse_ids": [
                <<courthouseId>>
              ],
              "case_number": "Case1",
              "courtroom_name": "COURTROOM1",
              "hearing_start_at": "2023-04-18",
              "hearing_end_at": "2024-04-17"
            }""";

        requestBody = requestBody.replace("<<courthouseId>>", swanseaCourthouse.getId().toString());
        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT_URL)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(requestBody);
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();

        String actualResponse = TestUtils.removeIds(response.getResponse().getContentAsString());

        String expectedResponse = getContentsFromFile(
            "tests/cases/CaseControllerAdminSearchTest/testOk/expectedResponse.json");
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void adminCaseSearch_ShouldReturnMultipleOrderedByCaseId() throws Exception {

        String requestBody = """
            {
              "courthouse_ids": [
                <<courthouseId>>
              ],
              "case_number": "Case",
              "courtroom_name": "COURTROOM1",
              "hearing_start_at": "2023-04-18",
              "hearing_end_at": "2024-04-17"
            }""";

        requestBody = requestBody.replace("<<courthouseId>>", swanseaCourthouse.getId().toString());
        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT_URL)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(requestBody);
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();

        String actualResponse = TestUtils.removeIds(response.getResponse().getContentAsString());

        String expectedResponse = getContentsFromFile(
            "tests/cases/CaseControllerAdminSearchTest/testOk/expectedResponseMultiple.json");
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void adminCaseSearch_ShouldReturnOkIsAnonymised() throws Exception {
        CourtCaseEntity caseEntity = dartsDatabase.getCaseRepository()
            .findByCaseNumberAndCourthouse_CourthouseName("Case1", "SWANSEA").orElseThrow();
        OffsetDateTime dataAnonymisedTs = OffsetDateTime.parse("2023-01-01T12:00:00Z");
        caseEntity.setDataAnonymisedTs(dataAnonymisedTs);
        caseEntity.setDataAnonymised(true);
        dartsDatabase.getCaseRepository().save(caseEntity);

        String requestBody = """
            {
              "courthouse_ids": [
                <<courthouseId>>
              ],
              "case_number": "Case1",
              "courtroom_name": "COURTROOM1",
              "hearing_start_at": "2023-04-18",
              "hearing_end_at": "2024-04-17"
            }""";

        requestBody = requestBody.replace("<<courthouseId>>", swanseaCourthouse.getId().toString());
        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT_URL)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(requestBody);
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();

        String actualResponse = TestUtils.removeIds(response.getResponse().getContentAsString());

        String expectedResponse = getContentsFromFile(
            "tests/cases/CaseControllerAdminSearchTest/testOkIsAnonymised/expectedResponse.json");
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void adminCaseSearch_ShouldReturnMultiples_WithSearchByCourthouseIdsAndHearingDates() throws Exception {

        String requestBody = """
            {
              "courthouse_ids": [
                <<courthouseId>>
              ],
              "case_number": null,
              "courtroom_name": null,
              "hearing_start_at": "2023-03-02",
              "hearing_end_at": "2024-03-01"
            }""";

        requestBody = requestBody.replace("<<courthouseId>>", swanseaCourthouse.getId().toString());
        MockHttpServletRequestBuilder requestBuilder = post(ENDPOINT_URL)
            .contentType(MediaType.APPLICATION_JSON_VALUE)
            .content(requestBody);
        MvcResult response = mockMvc.perform(requestBuilder).andExpect(status().isOk()).andReturn();

        String actualResponse = TestUtils.removeIds(response.getResponse().getContentAsString());

        String expectedResponse = getContentsFromFile(
            "tests/cases/CaseControllerAdminSearchTest/testOk/expectedResponseMultiple.json");
        assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void adminCasesSearchPost_ShouldReturnUnprocessableEntity_WhenCourtroomNameIsLowercase() throws Exception {
        // Given
        AdminCasesSearchRequest request = new AdminCasesSearchRequest();
        request.setCourtroomName("courtroom1");  // lowercase value
        request.setCaseNumber("Case1");

        // When/Then
        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT_URL)
                                                  .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                  .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnprocessableEntity())
            .andReturn();

        String actualResponse = mvcResult.getResponse().getContentAsString();
        String expectedResponse = """
            {
               "type": "CASE_103",
               "title": "The request is not valid",
               "status": 422,
               "detail": "Courthouse and courtroom must be uppercase."
             }""";

        assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void adminCasesSearchPost_ShouldReturnUnprocessableEntity_WhenHearingDateStartIsAfterHearingDateEnd() throws Exception {
        // Given
        AdminCasesSearchRequest request = new AdminCasesSearchRequest();
        request.setCaseNumber("Case1");
        request.setHearingStartAt(LocalDate.of(2023, 04, 11));
        request.setHearingEndAt(LocalDate.of(2023, 03, 11));

        // When/Then
        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT_URL)
                                                  .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                  .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnprocessableEntity())
            .andReturn();

        String actualResponse = mvcResult.getResponse().getContentAsString();
        String expectedResponse = """
            {
               "type": "COMMON_104",
               "title": "Invalid request",
               "status": 422,
               "detail": "The hearing start date cannot be after the end date."
             }
            """;

        assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void adminCasesSearchPost_ShouldReturnUnprocessableEntity_WhenHearingDateEndIsNull() throws Exception {
        // Given
        AdminCasesSearchRequest request = new AdminCasesSearchRequest();
        request.setCourthouseIds(List.of(1));
        request.setHearingStartAt(LocalDate.of(2023, 04, 11));

        // When/Then
        MvcResult mvcResult = mockMvc.perform(post(ENDPOINT_URL)
                                                  .contentType(MediaType.APPLICATION_JSON_VALUE)
                                                  .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnprocessableEntity())
            .andReturn();

        String actualResponse = mvcResult.getResponse().getContentAsString();
        String expectedResponse = """
            {
              "type": "COMMON_105",
              "title": "The search criteria is too broad",
              "status": 422
            }
            """;

        assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    private void setupUserAccountAndSecurityGroup() {
        var securityGroup = SecurityGroupTestData.createGroupForRole(SUPER_ADMIN);
        securityGroup.setGlobalAccess(true);
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