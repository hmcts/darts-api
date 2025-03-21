package uk.gov.hmcts.darts.common.repository;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class CaseRepositoryIntTest extends PostgresIntegrationBase {

    @Autowired
    private CaseRepository caseRepository;

    private CourtCaseEntity courtCaseOpen;
    private CourtCaseEntity courtCaseClosed;

    @BeforeEach
    void setUp() {
        courtCaseOpen = PersistableFactory.getCourtCaseTestData().someMinimalBuilder()
            .closed(false)
            .build().getEntity();
        courtCaseOpen = dartsPersistence.save(courtCaseOpen);
        HearingEntity hearingEntity1 = PersistableFactory.getHearingTestData().createHearingFor(courtCaseOpen);
        HearingEntity hearingEntity2 = PersistableFactory.getHearingTestData().createHearingFor(courtCaseOpen);
        HearingEntity hearingEntity3 = PersistableFactory.getHearingTestData().createHearingFor(courtCaseOpen);

        courtCaseClosed = PersistableFactory.getCourtCaseTestData().someMinimalBuilder()
            .closed(true)
            .build().getEntity();
        courtCaseClosed.setClosed(true);
        courtCaseClosed = dartsPersistence.save(courtCaseClosed);
        HearingEntity hearingEntity4 = PersistableFactory.getHearingTestData().createHearingFor(courtCaseClosed);

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

    @Test
    void findOpenCaseNumbers_shouldSaveAndReturnOpenCaseNumbers_WithOutsideWhitespaceTrimmedButNotInnerWhitespace() {
        // given
        CourtCaseEntity courtCaseOpenEntity = PersistableFactory.getCourtCaseTestData().someMinimalBuilder()
            .closed(false)
            .build().getEntity();
        courtCaseOpenEntity.getCourthouse().setCourthouseName(" Test Courthouse Name With Whitespace ");
        courtCaseOpenEntity = dartsPersistence.save(courtCaseOpenEntity);
        HearingEntity hearing = PersistableFactory.getHearingTestData().createHearingFor(courtCaseOpenEntity);
        dartsPersistence.saveAll(hearing);

        var courthouseNameWithWhitespace = courtCaseOpenEntity.getCourthouse().getCourthouseName();
        log.info("courthouseNameWithWhitespace: '{}'", courthouseNameWithWhitespace);
        // verify the courthouse name has whitespace trimmed and converted to uppercase when saved
        assertEquals("TEST COURTHOUSE NAME WITH WHITESPACE", courthouseNameWithWhitespace);

        // when
        var result = caseRepository.findOpenCaseNumbers(
            courthouseNameWithWhitespace, List.of(courtCaseOpenEntity.getCaseNumber()));

        // then
        assertThat(result).isNotEmpty();
        assertTrue(result.contains(courtCaseOpenEntity.getCaseNumber()));
    }
}
