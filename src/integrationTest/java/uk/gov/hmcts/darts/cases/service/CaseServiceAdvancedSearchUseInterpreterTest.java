package uk.gov.hmcts.darts.cases.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import uk.gov.hmcts.darts.cases.model.AdvancedSearchResult;
import uk.gov.hmcts.darts.cases.model.GetCasesSearchRequest;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.SecurityGroupRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.data.SecurityGroupTestData;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.APPROVER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSLATION_QA;
import static uk.gov.hmcts.darts.testutils.data.CaseTestData.createCaseAt;
import static uk.gov.hmcts.darts.testutils.data.CourthouseTestData.createCourthouse;
import static uk.gov.hmcts.darts.testutils.data.CourtroomTestData.createCourtRoomWithNameAtCourthouse;
import static uk.gov.hmcts.darts.testutils.data.HearingTestData.createHearingWith;
import static uk.gov.hmcts.darts.testutils.stubs.UserAccountStub.INTEGRATION_TEST_USER_EMAIL;

@Slf4j
class CaseServiceAdvancedSearchUseInterpreterTest extends IntegrationBase {

    @Autowired
    SecurityGroupRepository securityGroupRepository;
    @Autowired
    UserAccountRepository userAccountRepository;
    @Autowired
    CaseService service;
    CourthouseEntity swanseaCourthouse;
    CourthouseEntity cardiffCourthouse;
    CourthouseEntity liverpoolCourthouse;
    UserAccountEntity user;


    @BeforeEach
    void setupData() {
        // swansea cases
        swanseaCourthouse = createCourthouse("SWANSEA");

        CourtCaseEntity case1 = createCaseAt(swanseaCourthouse, "Case1");
        CourtCaseEntity case2 = createCaseAt(swanseaCourthouse, "Case2");
        case2.setInterpreterUsed(true);

        CourtroomEntity courtroom1 = createCourtRoomWithNameAtCourthouse(swanseaCourthouse, "courtroom1");
        var hearing1 = createHearingWith(case1, courtroom1);
        var hearing2 = createHearingWith(case2, courtroom1);

        dartsDatabase.saveAll(hearing1, hearing2);

        // cardiff cases
        cardiffCourthouse = createCourthouse("CARDIFF");

        CourtCaseEntity case3 = createCaseAt(cardiffCourthouse, "Case3");
        CourtCaseEntity case4 = createCaseAt(cardiffCourthouse, "Case4");
        case4.setInterpreterUsed(true);

        CourtroomEntity courtroom2 = createCourtRoomWithNameAtCourthouse(cardiffCourthouse, "courtroom2");
        var hearing3 = createHearingWith(case3, courtroom2);
        var hearing4 = createHearingWith(case4, courtroom2);

        dartsDatabase.saveAll(hearing3, hearing4);

        // liverpool cases
        liverpoolCourthouse = createCourthouse("LIVERPOOL");

        CourtCaseEntity case5 = createCaseAt(liverpoolCourthouse, "Case5");

        CourtCaseEntity case6 = createCaseAt(liverpoolCourthouse, "Case6");
        case6.setInterpreterUsed(true);

        CourtroomEntity courtroom3 = createCourtRoomWithNameAtCourthouse(liverpoolCourthouse, "courtroom3");
        var hearing5 = createHearingWith(case5, courtroom3);
        var hearing6 = createHearingWith(case6, courtroom3);

        dartsDatabase.saveAll(hearing5, hearing6);

        givenBearerTokenExists(INTEGRATION_TEST_USER_EMAIL);
        user = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        user.getSecurityGroupEntities().clear();
    }

    @AfterEach
    void deleteUser() {
        dartsDatabase.addToUserAccountTrash(INTEGRATION_TEST_USER_EMAIL);
    }

    @Test
    void testSearchCasesWithPermissionsGlobalTrueUseInterpreterTrue() {
        // given
        var securityGroup = SecurityGroupTestData.buildGroupForRole(TRANSLATION_QA);
        securityGroup.setGlobalAccess(true);
        securityGroup.setUseInterpreter(true);
        assignSecurityGroupToUser(user, securityGroup);

        userAccountRepository.save(user);

        // when
        GetCasesSearchRequest allCasesRequest = GetCasesSearchRequest.builder().build();
        List<AdvancedSearchResult> resultList = service.advancedSearch(allCasesRequest);

        // then
        var caseNumbers = resultList.stream().map(AdvancedSearchResult::getCaseNumber).toList();
        assertThat(caseNumbers).containsExactlyInAnyOrder("Case2", "Case4", "Case6");
    }

    @Test
    void testSearchCasesWithPermissionsGlobalTrueUseInterpreterFalse() {
        // given
        var securityGroup = SecurityGroupTestData.buildGroupForRole(TRANSLATION_QA);
        securityGroup.setGlobalAccess(true);
        securityGroup.setUseInterpreter(false);
        assignSecurityGroupToUser(user, securityGroup);

        userAccountRepository.save(user);

        // when
        GetCasesSearchRequest allCasesRequest = GetCasesSearchRequest.builder().build();
        List<AdvancedSearchResult> resultList = service.advancedSearch(allCasesRequest);

        // then
        var caseNumbers = resultList.stream().map(AdvancedSearchResult::getCaseNumber).toList();
        assertThat(caseNumbers).containsExactlyInAnyOrder("Case1", "Case2", "Case3", "Case4", "Case5", "Case6");
    }

    @Test
    void testSearchCasesWithPermissionsGlobalFalseUseInterpreterTrue() {
        // given
        var securityGroup = SecurityGroupTestData.buildGroupForRole(TRANSLATION_QA);
        securityGroup.setGlobalAccess(false);
        securityGroup.setUseInterpreter(true);
        assignSecurityGroupToUser(user, securityGroup);

        userAccountRepository.save(user);

        // when
        GetCasesSearchRequest allCasesRequest = GetCasesSearchRequest.builder().build();
        List<AdvancedSearchResult> resultList = service.advancedSearch(allCasesRequest);

        // then
        var caseNumbers = resultList.stream().map(AdvancedSearchResult::getCaseNumber).toList();
        assertThat(caseNumbers).isEmpty();
    }

    @Test
    void testSearchCasesWithPermissionsGlobalTrueFalseInterpreterFalse() {
        // given
        var securityGroup = SecurityGroupTestData.buildGroupForRole(TRANSLATION_QA);
        securityGroup.setGlobalAccess(false);
        securityGroup.setUseInterpreter(false);
        assignSecurityGroupToUser(user, securityGroup);

        userAccountRepository.save(user);

        // when
        GetCasesSearchRequest allCasesRequest = GetCasesSearchRequest.builder().build();
        List<AdvancedSearchResult> resultList = service.advancedSearch(allCasesRequest);

        // then
        var caseNumbers = resultList.stream().map(AdvancedSearchResult::getCaseNumber).toList();
        assertThat(caseNumbers).isEmpty();
    }

    @Test
    void testSearchCasesWithPermissionsGlobalFalseUseInterpreterFalseOnCourthouse() {
        // given
        var securityGroupSwansea = SecurityGroupTestData.buildGroupForRoleAndCourthouse(TRANSLATION_QA, swanseaCourthouse);
        securityGroupSwansea.setGlobalAccess(false);
        securityGroupSwansea.setUseInterpreter(false);
        assignSecurityGroupToUser(user, securityGroupSwansea);

        userAccountRepository.save(user);

        // when
        GetCasesSearchRequest allCasesRequest = GetCasesSearchRequest.builder().build();
        List<AdvancedSearchResult> resultList = service.advancedSearch(allCasesRequest);

        // then
        var caseNumbers = resultList.stream().map(AdvancedSearchResult::getCaseNumber).toList();
        assertThat(caseNumbers).containsExactlyInAnyOrder("Case1", "Case2");
    }

    @Test
    void testSearchCasesWithPermissionsGlobalTrueUseInterpreterTrueOnCourthouse() {
        // given
        var securityGroupSwansea = SecurityGroupTestData.buildGroupForRoleAndCourthouse(TRANSLATION_QA, swanseaCourthouse);
        securityGroupSwansea.setGlobalAccess(true);
        securityGroupSwansea.setUseInterpreter(true);
        assignSecurityGroupToUser(user, securityGroupSwansea);

        userAccountRepository.save(user);

        // when
        GetCasesSearchRequest allCasesRequest = GetCasesSearchRequest.builder().build();
        List<AdvancedSearchResult> resultList = service.advancedSearch(allCasesRequest);

        // then
        var caseNumbers = resultList.stream().map(AdvancedSearchResult::getCaseNumber).toList();
        assertThat(caseNumbers).containsExactlyInAnyOrder("Case2", "Case4", "Case6");
    }

    @Test
    void testSearchCasesWithPermissionsGlobalTrueUseInterpreterFalseOnCourthouse() {
        // given
        var securityGroupSwansea = SecurityGroupTestData.buildGroupForRoleAndCourthouse(TRANSLATION_QA, swanseaCourthouse);
        securityGroupSwansea.setGlobalAccess(true);
        securityGroupSwansea.setUseInterpreter(false);
        assignSecurityGroupToUser(user, securityGroupSwansea);

        userAccountRepository.save(user);

        // when
        GetCasesSearchRequest allCasesRequest = GetCasesSearchRequest.builder().build();
        List<AdvancedSearchResult> resultList = service.advancedSearch(allCasesRequest);

        // then
        var caseNumbers = resultList.stream().map(AdvancedSearchResult::getCaseNumber).toList();
        assertThat(caseNumbers).containsExactlyInAnyOrder("Case1", "Case2", "Case3", "Case4", "Case5", "Case6");
    }

    @Test
    void testSearchCasesWithPermissionsGlobalFalseUseInterpreterTrueOnCourthouse() {
        // given
        var securityGroupSwansea = SecurityGroupTestData.buildGroupForRoleAndCourthouse(TRANSLATION_QA, swanseaCourthouse);
        securityGroupSwansea.setGlobalAccess(false);
        securityGroupSwansea.setUseInterpreter(true);
        assignSecurityGroupToUser(user, securityGroupSwansea);

        userAccountRepository.save(user);

        // when
        GetCasesSearchRequest allCasesRequest = GetCasesSearchRequest.builder().build();
        List<AdvancedSearchResult> resultList = service.advancedSearch(allCasesRequest);

        // then
        var caseNumbers = resultList.stream().map(AdvancedSearchResult::getCaseNumber).toList();
        assertThat(caseNumbers).containsExactlyInAnyOrder("Case2");
    }

    @Test
    void testSearchCasesWithPermissionsGlobalTrueOnCourthouselessUserGroupUseInterpreterFalseOnCourthouse() {
        // given
        var securityGroup = SecurityGroupTestData.buildGroupForRole(APPROVER);
        securityGroup.setGlobalAccess(true);
        securityGroup.setUseInterpreter(true);
        assignSecurityGroupToUser(user, securityGroup);

        var securityGroupSwansea = SecurityGroupTestData.buildGroupForRoleAndCourthouse(TRANSLATION_QA, swanseaCourthouse);
        securityGroupSwansea.setGlobalAccess(false);
        securityGroupSwansea.setUseInterpreter(false);
        assignSecurityGroupToUser(user, securityGroupSwansea);

        userAccountRepository.save(user);

        // when
        GetCasesSearchRequest allCasesRequest = GetCasesSearchRequest.builder().build();
        List<AdvancedSearchResult> resultList = service.advancedSearch(allCasesRequest);

        // then
        var caseNumbers = resultList.stream().map(AdvancedSearchResult::getCaseNumber).toList();
        assertThat(caseNumbers).containsExactlyInAnyOrder("Case1", "Case2", "Case4", "Case6");
    }

    @Test
    void testSearchCasesWithPermissionsGlobalTrueAndUseInterpreterWithDifferentValuesForCourthouses() {
        // given
        var securityGroupSwansea = SecurityGroupTestData.buildGroupForRoleAndCourthouse(TRANSLATION_QA, cardiffCourthouse);
        securityGroupSwansea.setGlobalAccess(true);
        securityGroupSwansea.setUseInterpreter(true);
        assignSecurityGroupToUser(user, securityGroupSwansea);

        var securityGroupCardiff = SecurityGroupTestData.buildGroupForRoleAndCourthouse(TRANSLATION_QA, swanseaCourthouse);
        securityGroupCardiff.setGlobalAccess(false);
        securityGroupCardiff.setUseInterpreter(false);
        assignSecurityGroupToUser(user, securityGroupCardiff);

        userAccountRepository.save(user);

        // when
        GetCasesSearchRequest allCasesRequest = GetCasesSearchRequest.builder().build();
        List<AdvancedSearchResult> resultList = service.advancedSearch(allCasesRequest);

        // then
        var caseNumbers = resultList.stream().map(AdvancedSearchResult::getCaseNumber).toList();
        assertThat(caseNumbers).containsExactlyInAnyOrder("Case1", "Case2", "Case4", "Case6");
    }

    @Test
    void testSearchCasesWithPermissionsGlobalTrueAndUseInterpreterWithDifferentValuesForCourthouses2() {
        // given
        var securityGroupSwansea = SecurityGroupTestData.buildGroupForRoleAndCourthouse(TRANSLATION_QA, swanseaCourthouse);
        securityGroupSwansea.setGlobalAccess(true);
        securityGroupSwansea.setUseInterpreter(true);
        assignSecurityGroupToUser(user, securityGroupSwansea);

        var securityGroupCardiff = SecurityGroupTestData.buildGroupForRoleAndCourthouse(TRANSLATION_QA, cardiffCourthouse);
        securityGroupCardiff.setGlobalAccess(true);
        securityGroupCardiff.setUseInterpreter(false);
        assignSecurityGroupToUser(user, securityGroupCardiff);

        userAccountRepository.save(user);

        // when
        GetCasesSearchRequest allCasesRequest = GetCasesSearchRequest.builder().build();
        List<AdvancedSearchResult> resultList = service.advancedSearch(allCasesRequest);

        // then
        var caseNumbers = resultList.stream().map(AdvancedSearchResult::getCaseNumber).toList();
        assertThat(caseNumbers).containsExactlyInAnyOrder("Case1", "Case2", "Case3", "Case4", "Case5", "Case6");
    }

    @Test
    void testSearchCasesWithPermissionsGlobalTrueUseInterpreterFalseForAllCourthouses() {
        // given
        var securityGroupSwansea = SecurityGroupTestData.buildGroupForRoleAndCourthouse(TRANSLATION_QA, swanseaCourthouse);
        securityGroupSwansea.setGlobalAccess(true);
        securityGroupSwansea.setUseInterpreter(false);
        assignSecurityGroupToUser(user, securityGroupSwansea);

        var securityGroupCardiff = SecurityGroupTestData.buildGroupForRoleAndCourthouse(TRANSLATION_QA, cardiffCourthouse);
        securityGroupCardiff.setGlobalAccess(false);
        securityGroupCardiff.setUseInterpreter(false);
        assignSecurityGroupToUser(user, securityGroupCardiff);

        userAccountRepository.save(user);

        // when
        GetCasesSearchRequest allCasesRequest = GetCasesSearchRequest.builder().build();
        List<AdvancedSearchResult> resultList = service.advancedSearch(allCasesRequest);

        // then
        var caseNumbers = resultList.stream().map(AdvancedSearchResult::getCaseNumber).toList();
        assertThat(caseNumbers).containsExactlyInAnyOrder("Case1", "Case2", "Case3", "Case4", "Case5", "Case6");
    }

    @Test
    void testSearchCasesWithPermissionsGlobalFalseUseInterpreterTrueForAllCourthouses() {
        // given
        var securityGroupSwansea = SecurityGroupTestData.buildGroupForRoleAndCourthouse(TRANSLATION_QA, swanseaCourthouse);
        securityGroupSwansea.setGlobalAccess(false);
        securityGroupSwansea.setUseInterpreter(true);
        assignSecurityGroupToUser(user, securityGroupSwansea);

        var securityGroupCardiff = SecurityGroupTestData.buildGroupForRoleAndCourthouse(TRANSLATION_QA, cardiffCourthouse);
        securityGroupCardiff.setGlobalAccess(false);
        securityGroupCardiff.setUseInterpreter(true);
        assignSecurityGroupToUser(user, securityGroupCardiff);

        userAccountRepository.save(user);

        // when
        GetCasesSearchRequest allCasesRequest = GetCasesSearchRequest.builder().build();
        List<AdvancedSearchResult> resultList = service.advancedSearch(allCasesRequest);

        // then
        var caseNumbers = resultList.stream().map(AdvancedSearchResult::getCaseNumber).toList();
        assertThat(caseNumbers).containsExactlyInAnyOrder("Case2", "Case4");
    }

    @Test
    void testSearchCasesWithPermissionsGlobalTrueAndUseInterpreterWithDifferentValuesForCourthouses3() {
        var securityGroupSwansea = SecurityGroupTestData.buildGroupForRole(TRANSLATION_QA);
        securityGroupSwansea.setGlobalAccess(true);
        securityGroupSwansea.setUseInterpreter(true);
        assignSecurityGroupToUser(user, securityGroupSwansea);

        var securityGroupCardiff = SecurityGroupTestData.buildGroupForRoleAndCourthouse(TRANSLATION_QA, cardiffCourthouse);
        securityGroupCardiff.setGlobalAccess(false);
        securityGroupCardiff.setUseInterpreter(false);
        assignSecurityGroupToUser(user, securityGroupCardiff);

        userAccountRepository.save(user);

        // when
        GetCasesSearchRequest allCasesRequest = GetCasesSearchRequest.builder().build();
        List<AdvancedSearchResult> resultList = service.advancedSearch(allCasesRequest);

        // then
        var caseNumbers = resultList.stream().map(AdvancedSearchResult::getCaseNumber).toList();
        assertThat(caseNumbers).containsExactlyInAnyOrder("Case2", "Case3", "Case4", "Case6");
    }

    private static void givenBearerTokenExists(String email) {
        Jwt jwt = Jwt.withTokenValue("test")
            .header("alg", "RS256")
            .claim("emails", List.of(email))
            .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    private void assignSecurityGroupToUser(UserAccountEntity user, SecurityGroupEntity securityGroup) {
        securityGroup.getUsers().add(user);
        user.getSecurityGroupEntities().add(securityGroup);
        securityGroupRepository.save(securityGroup);
    }

}
