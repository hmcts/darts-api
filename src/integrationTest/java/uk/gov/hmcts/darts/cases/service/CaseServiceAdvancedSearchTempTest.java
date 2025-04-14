package uk.gov.hmcts.darts.cases.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.cases.model.AdvancedSearchResult;
import uk.gov.hmcts.darts.cases.model.GetCasesSearchRequest;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.repository.SecurityGroupRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.test.common.data.SecurityGroupTestData;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.DartsPersistence;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.APPROVER;
import static uk.gov.hmcts.darts.testutils.stubs.UserAccountStub.INTEGRATION_TEST_USER_EMAIL;

@Slf4j
class CaseServiceAdvancedSearchTempTest extends IntegrationBase {
    @Autowired
    SecurityGroupRepository securityGroupRepository;
    @Autowired
    UserAccountRepository userAccountRepository;
    @Autowired
    CaseService service;
    @Autowired
    DartsPersistence dartsPersistence;
    UserAccountEntity user;

    @BeforeEach
    void setupData() {
        givenBearerTokenExists(INTEGRATION_TEST_USER_EMAIL);
        user = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
    }

    // TODO: Move into CaseServiceAdvancedSearchTest class when it is re-enabled
    @Test
    void getWithDateRangeFromToSameDateWithNotActualHearings() throws IOException {
        var actualHearingDate = LocalDate.of(2023, 10, 22);
        var notActualHearingDate = LocalDate.of(2023, 10, 21);

        HearingEntity actualHearing = PersistableFactory.getHearingTestData().someMinimalHearing();
        actualHearing.setHearingDate(actualHearingDate);

        HearingEntity otherHearing = PersistableFactory.getHearingTestData().createHearingFor(actualHearing.getCourtCase());
        otherHearing.setHearingIsActual(false);
        otherHearing.setHearingDate(notActualHearingDate);

        dartsPersistence.saveAll(actualHearing, otherHearing);

        setupUserAccountSecurityGroup(APPROVER, actualHearing.getCourtCase().getCourthouse());
        userAccountRepository.save(user);

        GetCasesSearchRequest requestActualHearingDate = GetCasesSearchRequest.builder()
            .dateFrom(actualHearingDate)
            .dateTo(actualHearingDate)
            .build();
        List<AdvancedSearchResult> resultListForActualDate = service.advancedSearch(requestActualHearingDate);
        assertEquals(1, resultListForActualDate.size());
        assertEquals(actualHearingDate, resultListForActualDate.getFirst().getHearings().getFirst().getDate());

        GetCasesSearchRequest requestNotActualHearingDate = GetCasesSearchRequest.builder()
            .dateFrom(notActualHearingDate)
            .dateTo(notActualHearingDate)
            .build();
        List<AdvancedSearchResult> resultListForNotActualDate = service.advancedSearch(requestNotActualHearingDate);
        assertEquals(0, resultListForNotActualDate.size());
    }

    @Test
    void getWithCaseNumberCourthouseCourtroomWithNotActualHearings() throws IOException {
        var actualHearingDate = LocalDate.of(2023, 10, 22);
        var notActualHearingDate = LocalDate.of(2023, 10, 21);

        HearingEntity actualHearing = PersistableFactory.getHearingTestData().someMinimalHearing();
        actualHearing.setHearingDate(actualHearingDate);

        HearingEntity otherHearing = PersistableFactory.getHearingTestData().createHearingFor(actualHearing.getCourtCase());
        otherHearing.setHearingIsActual(false);
        otherHearing.setHearingDate(notActualHearingDate);

        dartsPersistence.saveAll(actualHearing, otherHearing);

        setupUserAccountSecurityGroup(APPROVER, actualHearing.getCourtCase().getCourthouse());
        userAccountRepository.save(user);

        GetCasesSearchRequest requestActualHearingDate = GetCasesSearchRequest.builder()
            .caseNumber(actualHearing.getCourtCase().getCaseNumber().substring(0, 5))
            .courthouseIds(List.of(actualHearing.getCourtCase().getCourthouse().getId()))
            .courtroom(actualHearing.getCourtroom().getName())
            .build();
        List<AdvancedSearchResult> resultListForActualDate = service.advancedSearch(requestActualHearingDate);
        assertEquals(1, resultListForActualDate.size());
        assertEquals(actualHearingDate, resultListForActualDate.getFirst().getHearings().getFirst().getDate());

        GetCasesSearchRequest requestNotActualHearingDate = GetCasesSearchRequest.builder()
            .caseNumber(otherHearing.getCourtCase().getCaseNumber().substring(0, 5))
            .courthouseIds(List.of(otherHearing.getCourtCase().getCourthouse().getId()))
            .courtroom(otherHearing.getCourtroom().getName())
            .build();
        List<AdvancedSearchResult> resultListForNotActualDate = service.advancedSearch(requestNotActualHearingDate);
        assertEquals(0, resultListForNotActualDate.size());
    }

    private void setupUserAccountSecurityGroup(SecurityRoleEnum securityRole, CourthouseEntity courthouse) {
        var securityGroup = SecurityGroupTestData.buildGroupForRoleAndCourthouse(securityRole, courthouse);
        securityGroup.setGlobalAccess(false);
        securityGroup.setUseInterpreter(false);
        assignSecurityGroupToUser(user, securityGroup);
    }

    private void assignSecurityGroupToUser(UserAccountEntity user, SecurityGroupEntity securityGroup) {
        securityGroup.getUsers().add(user);
        user.getSecurityGroupEntities().add(securityGroup);
        securityGroupRepository.save(securityGroup);
    }
}