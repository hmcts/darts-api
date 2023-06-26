package uk.gov.hmcts.darts.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.cases.repository.CaseRepository;
import uk.gov.hmcts.darts.cases.repository.ReportingRestrictionsRepository;
import uk.gov.hmcts.darts.common.entity.Case;
import uk.gov.hmcts.darts.common.entity.Courthouse;
import uk.gov.hmcts.darts.common.entity.Courtroom;
import uk.gov.hmcts.darts.common.entity.Hearing;
import uk.gov.hmcts.darts.common.entity.ReportingRestrictions;
import uk.gov.hmcts.darts.courthouse.CourthouseRepository;
import uk.gov.hmcts.darts.courthouse.CourtroomRepository;

import java.time.OffsetDateTime;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;


// These tests maybe ok to remove once service level tests are in place
@SpringBootTest
@ActiveProfiles({"intTest", "h2db"})
@Transactional
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
        caseRepository.saveAndFlush(new Case());

        var cases = caseRepository.findAll();

        assertThat(cases.size()).isEqualTo(1);
        assertThat(cases.get(0).getId()).isInstanceOf(Integer.class);
    }

    @Test
    void canAddHearingsToCase() {
        var someCase = new Case();
        var hearing1 = new Hearing();
        var hearing2 = new Hearing();
        var hearings = new ArrayList<Hearing>();
        hearings.add(hearing1);
        hearings.add(hearing2);
        someCase.setHearings(hearings);
        caseRepository.saveAndFlush(someCase);

        var cases = caseRepository.findAll();
        var persistedHearings = cases.get(0).getHearings();

        assertThat(persistedHearings.size()).isEqualTo(2);
        persistedHearings.forEach((hearing) -> assertThat(hearing).isNotNull());
    }

    @Test
    void canAddReportingRestrictionsToCase() {
        var reportingRestrictions = new ReportingRestrictions();
        var persistedRestrictions = reportingRestrictionsRepository.saveAndFlush(reportingRestrictions);

        var someCase = new Case();
        someCase.setReportingRestrictions(persistedRestrictions);
        caseRepository.saveAndFlush(someCase);

        var cases = caseRepository.findAll();
        var attachedRestrictions = cases.get(0).getReportingRestrictions();

        assertThat(attachedRestrictions).isInstanceOf(ReportingRestrictions.class);
    }

    @Test
    void canAddCourtroomToHearing() {
        var persistedCourthouse = setupCourthouse();
        var hearing = new Hearing();
        hearing.setCourtroom(persistedCourthouse.getCourtrooms().get(0));

        var someCase = new Case();
        var hearings = new ArrayList<Hearing>();
        hearings.add(hearing);
        someCase.setHearings(hearings);
        caseRepository.saveAndFlush(someCase);

        var cases = caseRepository.findAll();
        var attachedCourtroom = cases.get(0).getHearings().get(0).getCourtroom();

        assertThat(attachedCourtroom).isInstanceOf(Courtroom.class);
    }

    private Courthouse setupCourthouse() {
        var courthouse = new Courthouse();
        courthouse.setCreatedDateTime(OffsetDateTime.now());
        courthouse.setLastModifiedDateTime(OffsetDateTime.now());
        courthouse.setCourthouseName("some-courthouse");
        var persistedCourthouse = courthouseRepository.save(courthouse);

        var courtroom = new Courtroom();
        courtroom.setName("some-courtroom");
        courtroom.setCourthouse(persistedCourthouse);

        var courtrooms = new ArrayList<Courtroom>();
        courtrooms.add(courtroom);
        courthouse.setCourtrooms(courtrooms);
        persistedCourthouse = courthouseRepository.saveAndFlush(courthouse);
        return persistedCourthouse;
    }
}
