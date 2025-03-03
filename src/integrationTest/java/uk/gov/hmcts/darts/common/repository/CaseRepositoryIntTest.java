package uk.gov.hmcts.darts.common.repository;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.CourtCaseStub;
import uk.gov.hmcts.darts.testutils.stubs.UserAccountStub;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class CaseRepositoryIntTest extends PostgresIntegrationBase {

    @Autowired
    private UserAccountStub userAccountStub;

    @Autowired
    private CourtCaseStub caseStub;

    @Autowired
    private CaseRepository caseRepository;

    private CourtCaseEntity courtCaseOpen;
    private HearingEntity hearingEntity1;
    private HearingEntity hearingEntity2;
    private HearingEntity hearingEntity3;
    private CourtCaseEntity courtCaseClosed;
    private HearingEntity hearingEntity4;


    @BeforeEach
    void setUp() {
        courtCaseOpen = PersistableFactory.getCourtCaseTestData().someMinimalBuilder()
            .closed(false)
            .build().getEntity();
        hearingEntity1 = PersistableFactory.getHearingTestData().createHearingFor(courtCaseOpen);
        hearingEntity2 = PersistableFactory.getHearingTestData().createHearingFor(courtCaseOpen);
        hearingEntity3 = PersistableFactory.getHearingTestData().createHearingFor(courtCaseOpen);

        courtCaseClosed = PersistableFactory.getCourtCaseTestData().someMinimalBuilder()
            .closed(true)
            .build().getEntity();
        courtCaseClosed.setClosed(true);
        hearingEntity4 = PersistableFactory.getHearingTestData().createHearingFor(courtCaseClosed);

        dartsPersistence.saveAll(hearingEntity1, hearingEntity2, hearingEntity3, hearingEntity4);
    }

    @Test
    void findOpenCaseNumbers_shouldReturnOpenCaseNumbers() {
        // when
        var result = caseRepository.findOpenCaseNumbers(
            courtCaseOpen.getCourthouse().getCourthouseName(), List.of(courtCaseOpen.getCaseNumber(), courtCaseClosed.getCaseNumber()));

        // then
        assertThat(result).isNotEmpty();
        assertTrue(result.contains(courtCaseOpen.getCaseNumber()));
    }

    @Test
    void findOpenCaseNumbers_shouldNotReturnClosedCaseNumbers() {
        // when
        var result = caseRepository.findOpenCaseNumbers(
            courtCaseOpen.getCourthouse().getCourthouseName(), List.of(courtCaseClosed.getCaseNumber()));

        // then
        assertTrue(result.isEmpty());
    }

    @Test
    void findOpenCaseNumbers_shouldReturnOpenCaseNumbersWithWhitespaceTrimmed() {
        // given
        var courthouseNameWithWhitespace = " " + courtCaseOpen.getCourthouse().getCourthouseName() + " ";
        log.info("courthouseNameWithWhitespace: '{}'", courthouseNameWithWhitespace);

        // when
        var result = caseRepository.findOpenCaseNumbers(
            courthouseNameWithWhitespace, List.of(courtCaseOpen.getCaseNumber()));

        // then
        assertThat(result).isNotEmpty();
        assertTrue(result.contains(courtCaseOpen.getCaseNumber()));
    }
}
