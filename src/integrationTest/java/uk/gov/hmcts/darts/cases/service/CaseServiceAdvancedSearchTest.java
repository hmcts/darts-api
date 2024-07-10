package uk.gov.hmcts.darts.cases.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.cases.model.AdvancedSearchResult;
import uk.gov.hmcts.darts.cases.model.GetCasesSearchRequest;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.DefendantEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.JudgeEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.repository.SecurityGroupRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.test.common.TestUtils;
import uk.gov.hmcts.darts.test.common.data.SecurityGroupTestData;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.APPROVER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.REQUESTER;
import static uk.gov.hmcts.darts.test.common.TestUtils.getContentsFromFile;
import static uk.gov.hmcts.darts.test.common.data.CaseTestData.createCaseAt;
import static uk.gov.hmcts.darts.test.common.data.CourthouseTestData.someMinimalCourthouse;
import static uk.gov.hmcts.darts.test.common.data.CourtroomTestData.createCourtRoomWithNameAtCourthouse;
import static uk.gov.hmcts.darts.test.common.data.DefendantTestData.createDefendantForCaseWithName;
import static uk.gov.hmcts.darts.test.common.data.EventTestData.createEventWith;
import static uk.gov.hmcts.darts.test.common.data.HearingTestData.createHearingWithDefaults;
import static uk.gov.hmcts.darts.test.common.data.JudgeTestData.createJudgeWithName;
import static uk.gov.hmcts.darts.testutils.stubs.UserAccountStub.INTEGRATION_TEST_USER_EMAIL;

@Slf4j
@SuppressWarnings({"PMD.VariableDeclarationUsageDistance", "PMD.NcssCount", "PMD.ExcessiveImports"})
class CaseServiceAdvancedSearchTest extends IntegrationBase {
    @Autowired
    SecurityGroupRepository securityGroupRepository;
    @Autowired
    UserAccountRepository userAccountRepository;
    @Autowired
    CaseService service;
    CourthouseEntity swanseaCourthouse;
    CourthouseEntity londonCourthouse;
    UserAccountEntity user;
    CourtroomEntity courtroom1;
    CourtroomEntity courtroomLondon;

    @BeforeEach
    void setupData() {
        swanseaCourthouse = someMinimalCourthouse();
        swanseaCourthouse.setCourthouseName("SWANSEA");
        swanseaCourthouse.setDisplayName("SWANSEA");

        londonCourthouse = someMinimalCourthouse();
        londonCourthouse.setCourthouseName("LONDON");
        londonCourthouse.setDisplayName("LONDON");

        CourtCaseEntity case1 = createCaseAt(swanseaCourthouse);
        case1.setCaseNumber("Case1");

        CourtCaseEntity case2 = createCaseAt(swanseaCourthouse);
        case2.setCaseNumber("Case2");

        List<DefendantEntity> defendantList = new ArrayList<>();
        defendantList.add(createDefendantForCaseWithName(case2, "Defendant2"));
        case2.setDefendantList(defendantList);

        CourtCaseEntity case3 = createCaseAt(swanseaCourthouse);
        case3.setCaseNumber("Case3");

        CourtCaseEntity case4 = createCaseAt(swanseaCourthouse);
        case4.setCaseNumber("Case4");

        CourtCaseEntity case5 = createCaseAt(swanseaCourthouse);
        case5.setCaseNumber("case5");

        CourtCaseEntity case6 = createCaseAt(swanseaCourthouse);
        case6.setCaseNumber("case6");

        CourtCaseEntity case7 = createCaseAt(swanseaCourthouse);
        case7.setCaseNumber("case7");

        CourtCaseEntity case8 = createCaseAt(swanseaCourthouse);
        case8.setCaseNumber("case8");

        CourtCaseEntity case9 = createCaseAt(londonCourthouse);
        case9.setCaseNumber("Case9");

        CourtCaseEntity case10 = createCaseAt(swanseaCourthouse);
        case10.setCaseNumber("case10");

        JudgeEntity judge = createJudgeWithName("aJudge");
        courtroom1 = createCourtRoomWithNameAtCourthouse(swanseaCourthouse, "courtroom1");
        HearingEntity hearing1a = createHearingWithDefaults(case1, courtroom1, LocalDate.of(2023, 5, 20), judge);

        HearingEntity hearing1b = createHearingWithDefaults(case1, courtroom1, LocalDate.of(2023, 5, 21), judge);

        HearingEntity hearing1c = createHearingWithDefaults(case1, courtroom1, LocalDate.of(2023, 5, 22), judge);

        HearingEntity hearing2a = createHearingWithDefaults(case2, courtroom1, LocalDate.of(2023, 6, 20), judge);

        HearingEntity hearing2b = createHearingWithDefaults(case2, courtroom1, LocalDate.of(2023, 6, 21), judge);

        HearingEntity hearing2c = createHearingWithDefaults(case2, courtroom1, LocalDate.of(2023, 6, 22), judge);

        HearingEntity hearing3a = createHearingWithDefaults(case3, courtroom1, LocalDate.of(2023, 7, 20), judge);
        JudgeEntity judge3a = createJudgeWithName("Judge3a");
        hearing3a.addJudge(judge3a);

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
        hearing6b.addJudge(createJudgeWithName("Judge6b"));

        HearingEntity hearing6c = createHearingWithDefaults(case6, courtroom1, LocalDate.of(2023, 9, 22), judge);

        HearingEntity hearing7a = createHearingWithDefaults(case7, courtroom1, LocalDate.of(2023, 10, 21), judge);
        HearingEntity hearing7b = createHearingWithDefaults(case7, courtroom1, LocalDate.of(2023, 10, 23), judge);
        HearingEntity hearing8 = createHearingWithDefaults(case8, courtroom1, LocalDate.of(2023, 10, 22), judge);

        courtroomLondon = createCourtRoomWithNameAtCourthouse(londonCourthouse, "courtroomLondon");
        HearingEntity hearing9 = createHearingWithDefaults(case9, courtroomLondon, LocalDate.of(2023, 5, 20), judge3a);

        CourtroomEntity courtroom4 = createCourtRoomWithNameAtCourthouse(swanseaCourthouse, "courtroom4");
        HearingEntity hearing10a = createHearingWithDefaults(case10, courtroom4, LocalDate.of(2023, 10, 23), judge);

        dartsDatabase.saveEntityGraphs(hearing1a, hearing1b, hearing1c,
                                       hearing2a, hearing2b, hearing2c,
                                       hearing3a, hearing3b, hearing3c,
                                       hearing4a, hearing4b, hearing4c,
                                       hearing5a, hearing5b, hearing5c,
                                       hearing6a, hearing6b, hearing6c,
                                       hearing7a, hearing7b,
                                       hearing8,
                                       hearing9,
                                       hearing10a
        );

        EventEntity event4a = createEventWith("eventName", "event4a", hearing4a, OffsetDateTime.now());
        EventEntity event5b = createEventWith("eventName", "event5b", hearing5b, OffsetDateTime.now());
        dartsDatabase.saveEntityGraphs(event4a, event5b);

        givenBearerTokenExists(INTEGRATION_TEST_USER_EMAIL);
        user = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
    }

    @AfterEach
    void deleteUser() {
        dartsDatabase.addToUserAccountTrash(INTEGRATION_TEST_USER_EMAIL);
    }

    @Test
    void getWithCaseNumber() throws IOException {

        GetCasesSearchRequest request = GetCasesSearchRequest.builder()
            .caseNumber("sE1")
            .build();

        setupUserAccountSecurityGroup(APPROVER, swanseaCourthouse);
        userAccountRepository.save(user);

        List<AdvancedSearchResult> resultList = service.advancedSearch(request);
        String actualResponse = TestUtils.removeIds(objectMapper.writeValueAsString(resultList));
        String expectedResponse = TestUtils.removeIds(getContentsFromFile(
            "tests/cases/CaseServiceAdvancedSearchTest/getWithCaseNumber/expectedResponse.json"));
        compareJson(actualResponse, expectedResponse);
    }

    @Test
    void getWithCaseNumberWithoutAccess() throws IOException {

        GetCasesSearchRequest request = GetCasesSearchRequest.builder()
            .caseNumber("sE1")
            .build();

        setupUserAccountSecurityGroup(APPROVER, londonCourthouse);
        userAccountRepository.save(user);

        List<AdvancedSearchResult> resultList = service.advancedSearch(request);
        assertTrue(resultList.isEmpty());
    }

    @Test
    void getWithCaseNumberGlobalAccess() throws IOException {

        GetCasesSearchRequest request = GetCasesSearchRequest.builder()
            .caseNumber("sE1")
            .build();

        setupUserAccountSecurityGroupGlobal(APPROVER, londonCourthouse);
        userAccountRepository.save(user);

        List<AdvancedSearchResult> resultList = service.advancedSearch(request);
        String actualResponse = TestUtils.removeIds(objectMapper.writeValueAsString(resultList));
        String expectedResponse = TestUtils.removeIds(getContentsFromFile(
            "tests/cases/CaseServiceAdvancedSearchTest/getWithCaseNumber/expectedResponse.json"));
        compareJson(actualResponse, expectedResponse);
    }

    @Test
    void getWithDateRangeFrom() throws IOException {
        GetCasesSearchRequest request = GetCasesSearchRequest.builder()
            .dateFrom(LocalDate.of(2023, 7, 21))
            .build();

        setupUserAccountSecurityGroup(APPROVER, swanseaCourthouse);
        userAccountRepository.save(user);

        List<AdvancedSearchResult> resultList = service.advancedSearch(request);
        String actualResponse = TestUtils.removeIds(objectMapper.writeValueAsString(resultList));
        String expectedResponse = TestUtils.removeIds(getContentsFromFile(
            "tests/cases/CaseServiceAdvancedSearchTest/getWithDateRangeFrom/expectedResponse.json"));
        compareJson(actualResponse, expectedResponse);
    }

    @Test
    void getWithDateRangeTo() throws IOException {

        GetCasesSearchRequest request = GetCasesSearchRequest.builder()
            .dateTo(LocalDate.of(2023, 6, 21))
            .build();

        setupUserAccountSecurityGroup(APPROVER, swanseaCourthouse);
        userAccountRepository.save(user);

        List<AdvancedSearchResult> resultList = service.advancedSearch(request);
        String actualResponse = TestUtils.removeIds(objectMapper.writeValueAsString(resultList));
        String expectedResponse = TestUtils.removeIds(getContentsFromFile(
            "tests/cases/CaseServiceAdvancedSearchTest/getWithDateRangeTo/expectedResponse.json"));
        compareJson(actualResponse, expectedResponse);
    }

    @Test
    void getWithDateRangeFromTo() throws IOException {

        GetCasesSearchRequest request = GetCasesSearchRequest.builder()
            .dateFrom(LocalDate.of(2023, 6, 21))
            .dateTo(LocalDate.of(2023, 7, 21))
            .build();

        setupUserAccountSecurityGroup(APPROVER, swanseaCourthouse);
        userAccountRepository.save(user);

        List<AdvancedSearchResult> resultList = service.advancedSearch(request);
        String actualResponse = TestUtils.removeIds(objectMapper.writeValueAsString(resultList));
        String expectedResponse = TestUtils.removeIds(getContentsFromFile(
            "tests/cases/CaseServiceAdvancedSearchTest/getWithDateRangeFromTo/expectedResponse.json"));
        compareJson(actualResponse, expectedResponse);
    }

    @Test
    void getWithDateRangeFromToSameDate() throws IOException {

        GetCasesSearchRequest request = GetCasesSearchRequest.builder()
            .dateFrom(LocalDate.of(2023, 10, 22))
            .dateTo(LocalDate.of(2023, 10, 22))
            .build();

        setupUserAccountSecurityGroup(APPROVER, swanseaCourthouse);
        userAccountRepository.save(user);

        List<AdvancedSearchResult> resultList = service.advancedSearch(request);
        String actualResponse = TestUtils.removeIds(objectMapper.writeValueAsString(resultList));
        String expectedResponse = TestUtils.removeIds(getContentsFromFile(
            "tests/cases/CaseServiceAdvancedSearchTest/getWithDateRangeFromToSameDate/expectedResponse.json"));
        compareJson(actualResponse, expectedResponse);
    }

    @Test
    void getWithJudge() throws IOException {

        GetCasesSearchRequest request = GetCasesSearchRequest.builder()
            .judgeName("3A")
            .build();

        setupUserAccountSecurityGroup(APPROVER, swanseaCourthouse);
        userAccountRepository.save(user);

        List<AdvancedSearchResult> resultList = service.advancedSearch(request);
        String actualResponse = TestUtils.removeIds(objectMapper.writeValueAsString(resultList));
        String expectedResponse = TestUtils.removeIds(getContentsFromFile(
            "tests/cases/CaseServiceAdvancedSearchTest/getWithJudge/expectedResponse.json"));
        compareJson(actualResponse, expectedResponse);
    }

    @Test
    void getWithJudgeMultipleCourtHouses() throws IOException {

        GetCasesSearchRequest request = GetCasesSearchRequest.builder()
            .judgeName("3A")
            .build();

        setupUserAccountSecurityGroup(APPROVER, swanseaCourthouse);
        setupUserAccountSecurityGroup(REQUESTER, londonCourthouse);
        userAccountRepository.save(user);

        List<AdvancedSearchResult> resultList = service.advancedSearch(request);
        String actualResponse = TestUtils.removeIds(objectMapper.writeValueAsString(resultList));
        String expectedResponse = TestUtils.removeIds(getContentsFromFile(
            "tests/cases/CaseServiceAdvancedSearchTest/getWithJudgeMultipleCourtHouses/expectedResponse.json"));
        compareJson(actualResponse, expectedResponse);
    }

    @Test
    void getWithJudgeGlobalAccess() throws IOException {

        GetCasesSearchRequest request = GetCasesSearchRequest.builder()
            .judgeName("3A")
            .build();

        setupUserAccountSecurityGroupGlobal(APPROVER, swanseaCourthouse);
        userAccountRepository.save(user);

        List<AdvancedSearchResult> resultList = service.advancedSearch(request);
        String actualResponse = TestUtils.removeIds(objectMapper.writeValueAsString(resultList));
        String expectedResponse = TestUtils.removeIds(getContentsFromFile(
            "tests/cases/CaseServiceAdvancedSearchTest/getWithJudgeMultipleCourtHouses/expectedResponse.json"));
        compareJson(actualResponse, expectedResponse);
    }

    @Test
    void getWithCourthouseCourtroom() throws IOException {

        GetCasesSearchRequest request = GetCasesSearchRequest.builder()
            .courthouse(swanseaCourthouse.getCourthouseName())
            .courtroom("roOm2")
            .build();

        setupUserAccountSecurityGroup(APPROVER, swanseaCourthouse);
        userAccountRepository.save(user);

        List<AdvancedSearchResult> resultList = service.advancedSearch(request);
        String actualResponse = TestUtils.removeIds(objectMapper.writeValueAsString(resultList));
        String expectedResponse = TestUtils.removeIds(getContentsFromFile(
            "tests/cases/CaseServiceAdvancedSearchTest/getWithCourtroom/expectedResponse.json"));
        compareJson(actualResponse, expectedResponse);
    }

    @Test
    void getWithCourthouseCourtroomWithoutAccess() throws IOException {

        GetCasesSearchRequest request = GetCasesSearchRequest.builder()
            .courthouse(swanseaCourthouse.getCourthouseName())
            .courtroom("roOm2")
            .build();

        setupUserAccountSecurityGroup(APPROVER, londonCourthouse);
        userAccountRepository.save(user);

        List<AdvancedSearchResult> resultList = service.advancedSearch(request);
        assertTrue(resultList.isEmpty());
    }

    @Test
    void getWithCourthouseNotExist() {

        GetCasesSearchRequest request = GetCasesSearchRequest.builder()
            .courthouse("jyguiyfgiytfuiytfuyrt")
            .build();

        setupUserAccountSecurityGroup(APPROVER, swanseaCourthouse);
        userAccountRepository.save(user);

        List<AdvancedSearchResult> resultList = service.advancedSearch(request);
        assertTrue(resultList.isEmpty());
    }

    @Test
    void getWithCourthouse() {

        GetCasesSearchRequest request = GetCasesSearchRequest.builder()
            .courthouse(swanseaCourthouse.getCourthouseName())
            .build();

        setupUserAccountSecurityGroup(APPROVER, swanseaCourthouse);
        userAccountRepository.save(user);

        List<AdvancedSearchResult> resultList = service.advancedSearch(request);
        assertEquals(9, resultList.size());
    }

    @Test
    void getWithCourthouseWithoutAccess() {

        GetCasesSearchRequest request = GetCasesSearchRequest.builder()
            .courthouse(swanseaCourthouse.getCourthouseName())
            .build();

        setupUserAccountSecurityGroup(APPROVER, londonCourthouse);
        userAccountRepository.save(user);

        List<AdvancedSearchResult> resultList = service.advancedSearch(request);
        assertTrue(resultList.isEmpty());
    }

    @Test
    void getWithCourthouseId() {

        GetCasesSearchRequest request = GetCasesSearchRequest.builder()
            .courthouseId(swanseaCourthouse.getId())
            .build();

        setupUserAccountSecurityGroup(APPROVER, swanseaCourthouse);
        userAccountRepository.save(user);

        List<AdvancedSearchResult> resultList = service.advancedSearch(request);
        assertEquals(9, resultList.size());
    }

    @Test
    void getWithCourtroom() throws IOException {

        GetCasesSearchRequest request = GetCasesSearchRequest.builder()
            .courtroom("roOm2")
            .build();

        setupUserAccountSecurityGroup(APPROVER, swanseaCourthouse);
        userAccountRepository.save(user);

        List<AdvancedSearchResult> resultList = service.advancedSearch(request);
        String actualResponse = TestUtils.removeIds(objectMapper.writeValueAsString(resultList));
        String expectedResponse = TestUtils.removeIds(getContentsFromFile(
            "tests/cases/CaseServiceAdvancedSearchTest/getWithCourtroom/expectedResponse.json"));
        compareJson(actualResponse, expectedResponse);
    }

    @Test
    void getWithCourtroomWithoutAccess() throws IOException {

        GetCasesSearchRequest request = GetCasesSearchRequest.builder()
            .courtroom("roOm2")
            .build();

        setupUserAccountSecurityGroup(APPROVER, londonCourthouse);
        userAccountRepository.save(user);

        List<AdvancedSearchResult> resultList = service.advancedSearch(request);
        assertTrue(resultList.isEmpty());
    }

    @Test
    void getWithCourtroomId() throws IOException {


        GetCasesSearchRequest request = GetCasesSearchRequest.builder()
            .courtroomId(courtroom1.getId())
            .build();

        setupUserAccountSecurityGroup(APPROVER, swanseaCourthouse);
        userAccountRepository.save(user);

        List<AdvancedSearchResult> resultList = service.advancedSearch(request);
        assertEquals(8, resultList.size());
    }

    @Test
    void getWithEventText() throws IOException {

        GetCasesSearchRequest request = GetCasesSearchRequest.builder()
            .eventTextContains("nT5b")
            .build();

        setupUserAccountSecurityGroup(APPROVER, swanseaCourthouse);
        userAccountRepository.save(user);

        List<AdvancedSearchResult> resultList = service.advancedSearch(request);
        String actualResponse = TestUtils.removeIds(objectMapper.writeValueAsString(resultList));
        String expectedResponse = TestUtils.removeIds(getContentsFromFile(
            "tests/cases/CaseServiceAdvancedSearchTest/getWithEventText/expectedResponse.json"));
        compareJson(actualResponse, expectedResponse);
    }


    @Test
    void getWithCourtroomJudge() throws IOException {

        GetCasesSearchRequest request = GetCasesSearchRequest.builder()
            .courtroom("roOm3")
            .judgeName("dGe6B")
            .build();

        setupUserAccountSecurityGroup(APPROVER, swanseaCourthouse);
        userAccountRepository.save(user);

        List<AdvancedSearchResult> resultList = service.advancedSearch(request);
        String actualResponse = TestUtils.removeIds(objectMapper.writeValueAsString(resultList));
        String expectedResponse = TestUtils.removeIds(getContentsFromFile(
            "tests/cases/CaseServiceAdvancedSearchTest/getWithCourtroomJudge/expectedResponse.json"));
        compareJson(actualResponse, expectedResponse);
    }

    @Test
    void whenAdvancedSearchIsRunForUserWithoutGlobalAccessWithNoCourthouseAccess_thenShouldReturnEmptyArray() throws IOException {

        GetCasesSearchRequest request = GetCasesSearchRequest.builder()
            .caseNumber("sE1")
            .build();
        user.getSecurityGroupEntities().clear();

        List<AdvancedSearchResult> resultList = service.advancedSearch(request);
        String actualResponse = TestUtils.removeIds(objectMapper.writeValueAsString(resultList));
        String expectedResponse = "[]";

        compareJson(actualResponse, expectedResponse);
    }

    private void setupUserAccountSecurityGroup(SecurityRoleEnum securityRole, CourthouseEntity courthouse) {
        var securityGroup = SecurityGroupTestData.buildGroupForRoleAndCourthouse(securityRole, courthouse);
        securityGroup.setGlobalAccess(false);
        securityGroup.setUseInterpreter(false);
        assignSecurityGroupToUser(user, securityGroup);
    }

    private void setupUserAccountSecurityGroupGlobal(SecurityRoleEnum securityRole, CourthouseEntity courthouse) {
        var securityGroup = SecurityGroupTestData.buildGroupForRoleAndCourthouse(securityRole, courthouse);
        securityGroup.setGlobalAccess(true);
        securityGroup.setUseInterpreter(false);
        assignSecurityGroupToUser(user, securityGroup);
    }

    private void assignSecurityGroupToUser(UserAccountEntity user, SecurityGroupEntity securityGroup) {
        securityGroup.getUsers().add(user);
        user.getSecurityGroupEntities().add(securityGroup);
        securityGroupRepository.save(securityGroup);
    }

    private static void compareJson(String actualResponse, String expectedResponse) {
        try {
            JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
        } catch (AssertionError ae) {
            log.error("expected\r\n{}to match\r\n{}", expectedResponse, actualResponse);
            throw ae;
        }
    }
}
