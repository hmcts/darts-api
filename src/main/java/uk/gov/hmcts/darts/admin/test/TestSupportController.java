package uk.gov.hmcts.darts.admin.test;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.darts.audit.enums.AuditActivityEnum;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.AuditActivityEntity;
import uk.gov.hmcts.darts.common.entity.AuditEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.AuditActivityRepository;
import uk.gov.hmcts.darts.common.repository.AuditRepository;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.CourthouseRepository;
import uk.gov.hmcts.darts.common.repository.CourtroomRepository;
import uk.gov.hmcts.darts.common.repository.NodeRegistrationRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.common.service.bankholidays.BankHolidaysService;
import uk.gov.hmcts.darts.common.service.bankholidays.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.lang.Integer.parseInt;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

@RestController
@RequestMapping(value = "/functional-tests")
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "darts.testing-support-endpoints", name = "enabled", havingValue = "true")
public class TestSupportController {

    private final SessionFactory sessionFactory;
    private final CourthouseRepository courthouseRepository;
    private final CourtroomRepository courtroomRepository;
    private final UserAccountRepository userAccountRepository;
    private final NodeRegistrationRepository nodeRegistrationRepository;
    private final AuditActivityRepository auditActivityRepository;
    private final AuditRepository auditRepository;
    private final CaseRepository caseRepository;
    private final UserIdentity userIdentity;

    private final List<Integer> courthouseTrash = new ArrayList<>();
    private final List<Integer> courtroomTrash = new ArrayList<>();
    private final BankHolidaysService bankHolidaysService;


    @SuppressWarnings("unchecked")
    @DeleteMapping(value = "/clean")
    public void cleanUpDataAfterFunctionalTests() {
        Session session = sessionFactory.openSession();
        session.beginTransaction();

        auditRepository.deleteAll();
        var caseIds = getCaseIdsToBeDeleted(session);
        var hearingIds = hearingIdsToBeDeleted(session, caseIds);
        var eventIds = eventIdsToBeDeleted(session, hearingIds);

        removeHearingEventJoins(session, hearingIds);
        removeEvents(session, eventIds);
        removeHearings(session, hearingIds);

        removeCaseJudgeJoins(session, caseIds);
        removeCaseDefence(session, caseIds);
        removeCaseDefendant(session, caseIds);
        removeCaseProsecutor(session, caseIds);
        removeCases(session, caseIds);

        List nodeRegisterIds = nodeRegisterIdsToBeDeleted(session, courtroomTrash);
        removeNodeRegisters(nodeRegisterIds);

        removeDailyLists(session);

        removeUserCourthousePermissions(session, courthouseTrash);

        removeCourtHouses(session);

        session.getTransaction().commit();
        session.close();
    }

    private void removeUserCourthousePermissions(Session session,List cthIds) {
        session.createNativeQuery("""
                                             delete from darts.security_group_courthouse_ae where cth_id in (?)
                                             """, Integer.class)
            .setParameter(1, cthIds)
            .executeUpdate();
    }

    private void removeDailyLists(Session session) {
        session.createNativeQuery("""
                                      delete from darts.daily_list where unique_id like 'func-%'
                                      """)
            .executeUpdate();
    }

    private void removeCourtHouses(Session session) {
        session.createNativeQuery("""
                                      delete from darts.courtroom where courtroom_name like 'func-%'
                                      """)
            .executeUpdate();
        session.createNativeQuery("""
                                      delete from darts.courthouse where courthouse_name like 'func-%'
                                      """)
            .executeUpdate();
    }

    @PostMapping(value = "/courthouse/{courthouse_name}/courtroom/{courtroom_name}")
    @Transactional
    public ResponseEntity<String> createCourthouseAndCourtroom(
        @PathVariable(name = "courthouse_name") String courthouseName,
        @PathVariable(name = "courtroom_name") String courtroomName) {

        if (!courthouseName.startsWith("func-")) {
            return new ResponseEntity<>("Courthouse name must start with func-", BAD_REQUEST);
        }

        if (courtroomRepository.findByCourthouseNameAndCourtroomName(courthouseName, courtroomName).isEmpty()) {
            var courthouse = courthouseRepository.findByCourthouseNameIgnoreCase(courthouseName)
                .orElseGet(() -> newCourthouse(courthouseName));

            newUserCourthousePermissions(courthouse);
            newCourtroom(courtroomName, courthouse);

            courthouseTrash.add(courthouse.getId());
        }

        return new ResponseEntity<>(CREATED);
    }

    private void newUserCourthousePermissions(CourthouseEntity courthouse) {
        UserAccountEntity userAccountEntity = userIdentity.getUserAccount();
        for (SecurityGroupEntity securityGroupEntity : userAccountEntity.getSecurityGroupEntities()) {
            courthouse.getSecurityGroups().add(securityGroupEntity);
        }
        courthouseRepository.saveAndFlush(courthouse);
    }

    @PostMapping(value = "/audit/{audit_activity}/courthouse/{courthouse_name}")
    @Transactional
    public ResponseEntity<String> createAudit(@PathVariable(name = "audit_activity") String auditActivity,
                                              @PathVariable(name = "courthouse_name") String courthouseName) {

        CourtCaseEntity courtCase = new CourtCaseEntity();
        courtCase.setCaseNumber("func-case1");
        courtCase.setClosed(false);
        courtCase.setInterpreterUsed(false);

        Optional<CourthouseEntity> foundCourthouse = courthouseRepository.findByCourthouseNameIgnoreCase(
            courthouseName);
        if (foundCourthouse.isPresent()) {
            courtCase.setCourthouse(foundCourthouse.get());
        } else {
            return new ResponseEntity<>(BAD_REQUEST);
        }

        CourtCaseEntity savedCase = caseRepository.saveAndFlush(courtCase);

        AuditEntity audit = new AuditEntity();
        audit.setCourtCase(savedCase);
        audit.setUser(userAccountRepository.getReferenceById(0));
        Optional<AuditActivityEntity> foundAuditActivity = auditActivityRepository.findById(AuditActivityEnum.valueOf(
            auditActivity).getId());

        if (foundAuditActivity.isPresent()) {
            audit.setAuditActivity(foundAuditActivity.get());
        } else {
            return new ResponseEntity<>(BAD_REQUEST);
        }

        audit.setApplicationServer("not available");
        auditRepository.saveAndFlush(audit);

        return new ResponseEntity<>(CREATED);
    }

    private CourthouseEntity newCourthouse(String courthouseName) {
        var courthouse = new CourthouseEntity();
        courthouse.setCourthouseName(courthouseName);
        return courthouseRepository.save(courthouse);
    }

    private void newCourtroom(String courtroomName, CourthouseEntity courthouse) {
        var courtroom = new CourtroomEntity();
        courtroom.setCourthouse(courthouse);
        courtroom.setName(courtroomName);
        courtroomRepository.save(courtroom);

        courtroomTrash.add(courtroom.getId());
    }

    private void removeCases(Session session, List casIds) {
        session.createNativeQuery("""
                                      delete from darts.court_case where cas_id in (?)
                                      """, Integer.class)
            .setParameter(1, casIds)
            .executeUpdate();
    }

    private void removeHearings(Session session, List heaIds) {
        session.createNativeQuery("""
                                      delete from darts.hearing where hea_id in (?)
                                      """)
            .setParameter(1, heaIds)
            .executeUpdate();
    }

    private void removeEvents(Session session, List eveIds) {
        session.createNativeQuery("""
                                      delete from darts.event where event.eve_id in (?)
                                      """, Integer.class)
            .setParameter(1, eveIds)
            .executeUpdate();
    }

    private void removeHearingEventJoins(Session session, List heaIds) {
        session.createNativeQuery("""
                                      delete from darts.hearing_event_ae where hea_id in (?)
                                      """, Integer.class)
            .setParameter(1, heaIds)
            .executeUpdate();
    }

    private void removeCaseJudgeJoins(Session session, List caseIds) {
        session.createNativeQuery("""
                                      delete from darts.case_judge_ae where cas_id in (?)
                                      """, Integer.class)
            .setParameter(1, caseIds)
            .executeUpdate();
    }

    private void removeCaseDefence(Session session, List caseIds) {
        session.createNativeQuery("""
                                      delete from darts.defence where cas_id in (?)
                                      """, Integer.class)
            .setParameter(1, caseIds)
            .executeUpdate();
    }

    private void removeCaseDefendant(Session session, List caseIds) {
        session.createNativeQuery("""
                                      delete from darts.defendant where cas_id in (?)
                                      """, Integer.class)
            .setParameter(1, caseIds)
            .executeUpdate();
    }

    private void removeCaseProsecutor(Session session, List caseIds) {
        session.createNativeQuery("""
                                      delete from darts.prosecutor where cas_id in (?)
                                      """, Integer.class)
            .setParameter(1, caseIds)
            .executeUpdate();
    }

    private void removeNodeRegisters(List<Integer> nodeIds) {
        nodeRegistrationRepository.deleteAllById(nodeIds);
    }

    private static List eventIdsToBeDeleted(Session session, List heaIds) {
        return session.createNativeQuery("""
                                             select eve_id from darts.hearing_event_ae where hea_id in (?)
                                             """, Integer.class)
            .setParameter(1, heaIds)
            .getResultList();
    }

    private static List hearingIdsToBeDeleted(Session session, List casIds) {
        return session.createNativeQuery("""
                                             select hea_id from darts.hearing where cas_id in (?)
                                             """, Integer.class)
            .setParameter(1, casIds)
            .getResultList();
    }

    private static List nodeRegisterIdsToBeDeleted(Session session, List crtIds) {
        return session.createNativeQuery("""
                                             select node_id from darts.node_register where ctr_id in (?)
                                             """, Integer.class)
            .setParameter(1, crtIds)
            .getResultList();
    }

    private static List getCaseIdsToBeDeleted(Session session) {
        return session.createNativeQuery("""
                                             select cas_id from darts.court_case where case_number like 'func-%'
                                             """, Integer.class)
            .getResultList();
    }

    @GetMapping(value = "/bank-holidays/{year}")
    public ResponseEntity<List<Event>> getBankHolidaysForYear(@PathVariable(name = "year") String year) {
        var bankHolidays = bankHolidaysService.getBankHolidaysFor(parseInt(year));
        return new ResponseEntity<>(bankHolidays, OK);
    }
}
