package uk.gov.hmcts.darts.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.darts.cases.repository.CaseRepository;
import uk.gov.hmcts.darts.cases.repository.ReportingRestrictionsRepository;
import uk.gov.hmcts.darts.common.entity.CaseEntity;
import uk.gov.hmcts.darts.common.entity.ReportingRestrictionsEntity;
import uk.gov.hmcts.darts.common.repository.CourtroomRepository;
import uk.gov.hmcts.darts.common.util.ClearDatabase;
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;
import uk.gov.hmcts.darts.courthouse.CourthouseRepository;

import static org.assertj.core.api.Assertions.assertThat;


// These tests maybe ok to remove once service level tests are in place
@SpringBootTest
@ActiveProfiles({"intTest", "h2db"})
@ClearDatabase
class RepositoryTest {

    @Autowired
    CaseRepository caseRepository;

    @Autowired
    ReportingRestrictionsRepository reportingRestrictionsRepository;

    @Autowired
    CourtroomRepository courtroomRepository;

    @Autowired
    CourthouseRepository courthouseRepository;

    @Test
    void canCreateNewCase() {
        CaseEntity courtCase = CommonTestDataUtil.createCase("2");
        caseRepository.saveAndFlush(courtCase);

        var newCourtCase = caseRepository.findByCaseNumber("2");

        assertThat(newCourtCase.getId()).isInstanceOf(Integer.class);
    }

    @Test
    void canAddReportingRestrictionsToCase() {
        var reportingRestrictions = new ReportingRestrictionsEntity();
        var persistedRestrictions = reportingRestrictionsRepository.saveAndFlush(reportingRestrictions);

        var someCase = CommonTestDataUtil.createCase("1");
        someCase.setReportingRestrictions(persistedRestrictions);
        caseRepository.saveAndFlush(someCase);

        var courtCase = caseRepository.findByCaseNumber("1");
        var attachedRestrictions = courtCase.getReportingRestrictions();

        assertThat(attachedRestrictions).isInstanceOf(ReportingRestrictionsEntity.class);
    }
}
