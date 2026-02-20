package uk.gov.hmcts.darts.cases.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.cases.model.AdvancedSearchResult;
import uk.gov.hmcts.darts.cases.model.GetCasesSearchRequest;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.SecurityGroupRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.test.common.data.CourthouseTestData;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.test.common.data.SecurityGroupTestData;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.APPROVER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSLATION_QA;
import static uk.gov.hmcts.darts.test.common.data.CourtroomTestData.createCourtRoomWithNameAtCourthouse;
import static uk.gov.hmcts.darts.testutils.stubs.UserAccountStub.INTEGRATION_TEST_USER_EMAIL;

@Slf4j
class CaseServiceAdvancedSearchUseInterpreterIntTest extends IntegrationBase {

    @Autowired
    private SecurityGroupRepository securityGroupRepository;
    @Autowired
    private UserAccountRepository userAccountRepository;
    @Autowired
    private CaseService service;
    private CourthouseEntity swanseaCourthouse;
    private CourthouseEntity cardiffCourthouse;
    private UserAccountEntity user;

    @BeforeEach
    void setupData() {
        // swansea cases
        swanseaCourthouse = CourthouseTestData.createCourthouseWithName("SWANSEA");

        CourtCaseEntity case1 = PersistableFactory.getCourtCaseTestData()
            .createCaseAt(swanseaCourthouse, "Case1");
        CourtCaseEntity case2 = PersistableFactory.getCourtCaseTestData()
            .createCaseAt(swanseaCourthouse, "Case2");
        case2.setInterpreterUsed(true);

        CourtroomEntity courtroom1 = createCourtRoomWithNameAtCourthouse(swanseaCourthouse, "courtroom1");
        var hearing1 = PersistableFactory.getHearingTestData().createHearingWith(case1, courtroom1);
        var hearing2 = PersistableFactory.getHearingTestData().createHearingWith(case2, courtroom1);

        dartsDatabase.saveAll(hearing1, hearing2);

        // cardiff cases
        cardiffCourthouse = CourthouseTestData.createCourthouseWithName("CARDIFF");

        CourtCaseEntity case3 = PersistableFactory.getCourtCaseTestData()
            .createCaseAt(cardiffCourthouse, "Case3");
        CourtCaseEntity case4 = PersistableFactory.getCourtCaseTestData()
            .createCaseAt(cardiffCourthouse, "Case4");
        case4.setInterpreterUsed(true);

        CourtroomEntity courtroom2 = createCourtRoomWithNameAtCourthouse(cardiffCourthouse, "courtroom2");
        var hearing3 = PersistableFactory.getHearingTestData().createHearingWith(case3, courtroom2);
        var hearing4 = PersistableFactory.getHearingTestData().createHearingWith(case4, courtroom2);

        dartsPersistence.saveAll(hearing3, hearing4);

        // liverpool cases
        CourthouseEntity liverpoolCourthouse = CourthouseTestData.createCourthouseWithName("LIVERPOOL");

        CourtCaseEntity case5 = PersistableFactory.getCourtCaseTestData().createCaseAt(liverpoolCourthouse, "Case5");

        CourtCaseEntity case6 = PersistableFactory.getCourtCaseTestData().createCaseAt(liverpoolCourthouse, "Case6");
        case6.setInterpreterUsed(true);

        CourtroomEntity courtroom3 = createCourtRoomWithNameAtCourthouse(liverpoolCourthouse, "courtroom3");
        var hearing5 = PersistableFactory.getHearingTestData().createHearingWith(case5, courtroom3);
        var hearing6 = PersistableFactory.getHearingTestData().createHearingWith(case6, courtroom3);

        dartsPersistence.saveAll(hearing5, hearing6);

        givenBearerTokenExists(INTEGRATION_TEST_USER_EMAIL);
        user = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        user.getSecurityGroupEntities().clear();
    }

    @Test
    void testSearchCasesWithUseInterpreterFalsePermissionIsIgnoredForTranslationQaAndCasesWithInterpreterAreReturned() {
        // given
        var securityGroup = SecurityGroupTestData.createGroupForRole(TRANSLATION_QA);
        securityGroup.setGlobalAccess(true);
        securityGroup.setUseInterpreter(false);
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
    void testSearchCasesWithPermissionsGlobalTrue() {
        // given
        var securityGroup = SecurityGroupTestData.createGroupForRole(TRANSLATION_QA);
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
    void testSearchCasesWithPermissionsGlobalFalse() {
        // given
        var securityGroup = SecurityGroupTestData.createGroupForRole(TRANSLATION_QA);
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
    void testSearchCasesWithPermissionsGlobalTrueOnSecurityGroupForCourthouse() {
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
    void testSearchCasesWithPermissionsGlobalFalseOnSecurityGroupForCourthouse() {
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
    void testSearchCasesWithPermissionsGlobalTrueOnSecurityGroupForOneOfCourthouses() {
        // given
        var securityGroupSwansea = SecurityGroupTestData.buildGroupForRoleAndCourthouse(TRANSLATION_QA, swanseaCourthouse);
        securityGroupSwansea.setGlobalAccess(true);
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
        assertThat(caseNumbers).containsExactlyInAnyOrder("Case2", "Case4", "Case6");
    }

    @Test
    void testSearchCasesWithPermissionsGlobalFalseOnSecurityGroupsForAllCourthouses() {
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
    void testSearchCasesWithMultipleSecurityGroupsOneBeingTranslationQaThenOnlyCasesWithInterpreterAreReturned() {
        // given
        var securityGroupSwansea = SecurityGroupTestData.buildGroupForRoleAndCourthouse(APPROVER, swanseaCourthouse);
        securityGroupSwansea.setGlobalAccess(false);
        securityGroupSwansea.setUseInterpreter(false);
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

    private void assignSecurityGroupToUser(UserAccountEntity user, SecurityGroupEntity securityGroup) {
        securityGroup.getUsers().add(user);
        user.getSecurityGroupEntities().add(securityGroup);
        securityGroupRepository.save(securityGroup);
    }

}