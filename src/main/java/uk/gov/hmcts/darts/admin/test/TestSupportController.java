package uk.gov.hmcts.darts.admin.test;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import uk.gov.hmcts.darts.audit.api.AuditActivity;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.AuditActivityEntity;
import uk.gov.hmcts.darts.common.entity.AuditEntity;
import uk.gov.hmcts.darts.common.entity.CaseRetentionEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.RetentionPolicyTypeEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.AuditActivityRepository;
import uk.gov.hmcts.darts.common.repository.AuditRepository;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.CaseRetentionRepository;
import uk.gov.hmcts.darts.common.repository.CourthouseRepository;
import uk.gov.hmcts.darts.common.repository.CourtroomRepository;
import uk.gov.hmcts.darts.common.repository.NodeRegistrationRepository;
import uk.gov.hmcts.darts.common.repository.RetentionPolicyTypeRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.common.service.bankholidays.BankHolidaysService;
import uk.gov.hmcts.darts.common.service.bankholidays.Event;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.lang.Integer.parseInt;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
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
    private final CaseRetentionRepository caseRetentionRepository;
    private final RetentionPolicyTypeRepository retentionPolicyTypeRepository;

    private final List<Integer> courthouseTrash = new ArrayList<>();
    private final List<Integer> courtroomTrash = new ArrayList<>();
    private final BankHolidaysService bankHolidaysService;

    @Value("${darts.audit.application-server}")
    private String applicationServer;

    @SuppressWarnings("unchecked")
    @DeleteMapping(value = "/clean")
    public void cleanUpDataAfterFunctionalTests() {
        /*
        Session session = sessionFactory.openSession();
        session.beginTransaction();

        var caseIds = getCaseIdsToBeDeleted(session);
        var hearingIds = hearingIdsToBeDeleted(session, caseIds);
        var eventIds = eventIdsToBeDeleted(session, hearingIds);

        removeHearingEventJoins(session, hearingIds);
        removeEvents(session, eventIds);
        removeHearings(session, hearingIds);

        removeCaseRetentions(session, caseIds);
        removeRetentionPolicyType(session);
        removeCaseAudit(session, caseIds);
        removeCaseJudgeJoins(session, caseIds);
        removeCaseDefence(session, caseIds);
        removeCaseDefendant(session, caseIds);
        removeCaseProsecutor(session, caseIds);
        removeCases(session, caseIds);

        List<Integer> nodeRegisterIds = nodeRegisterIdsToBeDeleted(session, courtroomTrash);
        removeNodeRegisters(nodeRegisterIds);

        removeDailyLists(session);

        removeUserCourthousePermissions(session, courthouseTrash);

        removeCourtHouses(session);

        removeUsers(session);
        removeSecurityGroups(session);

        session.getTransaction().commit();
        session.close();

         */
    }

    private void removeUserCourthousePermissions(Session session, List<Integer> cthIds) {
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
        if (userAccountEntity != null) {
            for (SecurityGroupEntity securityGroupEntity : userAccountEntity.getSecurityGroupEntities()) {
                courthouse.getSecurityGroups().add(securityGroupEntity);
            }
        }
        courthouseRepository.saveAndFlush(courthouse);
    }

    @PostMapping(value = "/audit/{audit_activity}/courthouse/{courthouse_name}")
    @Transactional(rollbackOn = DataIntegrityViolationException.class)
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
        try {
            AuditActivityEntity foundAuditActivity = auditActivityRepository.getReferenceById(
                AuditActivity.valueOf(auditActivity).getId()
            );
            audit.setAuditActivity(foundAuditActivity);

            audit.setApplicationServer(applicationServer);

            auditRepository.saveAndFlush(audit);
            return new ResponseEntity<>(CREATED);
        } catch (DataIntegrityViolationException e) {
            throw new ResponseStatusException(BAD_REQUEST);
        }
    }

    private CourthouseEntity newCourthouse(String courthouseName) {
        var courthouse = new CourthouseEntity();
        courthouse.setCourthouseName(courthouseName);
        courthouse.setDisplayName(courthouseName);
        return courthouseRepository.save(courthouse);
    }

    private void newCourtroom(String courtroomName, CourthouseEntity courthouse) {
        var courtroom = new CourtroomEntity();
        courtroom.setCourthouse(courthouse);
        courtroom.setName(courtroomName);
        courtroomRepository.save(courtroom);

        courtroomTrash.add(courtroom.getId());
    }

    private void removeCases(Session session, List<Integer> casIds) {
        session.createNativeQuery("""
                                      delete from darts.court_case where cas_id in (?)
                                      """, Integer.class)
            .setParameter(1, casIds)
            .executeUpdate();
    }

    private void removeHearings(Session session, List<Integer> heaIds) {
        session.createNativeQuery("""
                                      delete from darts.hearing where hea_id in (?)
                                      """, Integer.class)
            .setParameter(1, heaIds)
            .executeUpdate();
    }

    private void removeEvents(Session session, List<Integer> eveIds) {
        session.createNativeQuery("""
                                      delete from darts.event where event.eve_id in (?)
                                      """, Integer.class)
            .setParameter(1, eveIds)
            .executeUpdate();
    }

    private void removeHearingEventJoins(Session session, List<Integer> heaIds) {
        session.createNativeQuery("""
                                      delete from darts.hearing_event_ae where hea_id in (?)
                                      """, Integer.class)
            .setParameter(1, heaIds)
            .executeUpdate();
    }

    private void removeCaseAudit(Session session, List<Integer> caseIds) {
        session.createNativeQuery("""
                                      delete from darts.audit where cas_id in (?)
                                      """, Integer.class)
            .setParameter(1, caseIds)
            .executeUpdate();
    }

    private void removeCaseRetentions(Session session, List<Integer> caseIds) {
        session.createNativeQuery("""
                                      delete from darts.case_retention where cas_id in (?)
                                      """, Integer.class)
            .setParameter(1, caseIds)
            .executeUpdate();
    }

    private void removeRetentionPolicyType(Session session) {
        session.createNativeQuery("""
                                      delete from darts.retention_policy_type where rpt_id = 1
                                      """, Integer.class)
            .executeUpdate();
    }

    private void removeCaseJudgeJoins(Session session, List<Integer> caseIds) {
        session.createNativeQuery("""
                                      delete from darts.case_judge_ae where cas_id in (?)
                                      """, Integer.class)
            .setParameter(1, caseIds)
            .executeUpdate();
    }

    private void removeCaseDefence(Session session, List<Integer> caseIds) {
        session.createNativeQuery("""
                                      delete from darts.defence where cas_id in (?)
                                      """, Integer.class)
            .setParameter(1, caseIds)
            .executeUpdate();
    }

    private void removeCaseDefendant(Session session, List<Integer> caseIds) {
        session.createNativeQuery("""
                                      delete from darts.defendant where cas_id in (?)
                                      """, Integer.class)
            .setParameter(1, caseIds)
            .executeUpdate();
    }

    private void removeCaseProsecutor(Session session, List<Integer> caseIds) {
        session.createNativeQuery("""
                                      delete from darts.prosecutor where cas_id in (?)
                                      """, Integer.class)
            .setParameter(1, caseIds)
            .executeUpdate();
    }

    private void removeNodeRegisters(List<Integer> nodeIds) {
        nodeRegistrationRepository.deleteAllById(nodeIds);
    }

    private static List<Integer> eventIdsToBeDeleted(Session session, List<Integer> heaIds) {
        return session.createNativeQuery("""
                                             select eve_id from darts.hearing_event_ae where hea_id in (?)
                                             """, Integer.class)
            .setParameter(1, heaIds)
            .getResultList();
    }

    private static List<Integer> hearingIdsToBeDeleted(Session session, List<Integer> casIds) {
        return session.createNativeQuery("""
                                             select hea_id from darts.hearing where cas_id in (?)
                                             """, Integer.class)
            .setParameter(1, casIds)
            .getResultList();
    }

    private static List<Integer> nodeRegisterIdsToBeDeleted(Session session, List<Integer> crtIds) {
        return session.createNativeQuery("""
                                             select node_id from darts.node_register where ctr_id in (?)
                                             """, Integer.class)
            .setParameter(1, crtIds)
            .getResultList();
    }

    private static List<Integer> getCaseIdsToBeDeleted(Session session) {
        return session.createNativeQuery("""
                                             select cas_id from darts.court_case where case_number like 'func-%'
                                             """, Integer.class)
            .getResultList();
    }

    private void removeUsers(Session session) {
        session.createNativeQuery("""
                                      delete from darts.user_account where description = 'A temporary user created by functional test'
                                      """, Integer.class)
            .executeUpdate();
    }

    private void removeSecurityGroups(Session session) {
        session.createNativeQuery("""
                                      delete from darts.security_group where description = 'A temporary group created by functional test'
                                      """, Integer.class)
            .executeUpdate();
    }

    @GetMapping(value = "/bank-holidays/{year}")
    public ResponseEntity<List<Event>> getBankHolidaysForYear(@PathVariable(name = "year") String year) {
        var bankHolidays = bankHolidaysService.getBankHolidays(parseInt(year));
        return new ResponseEntity<>(bankHolidays, OK);
    }

    //Controller only runs in test environment
    @SuppressWarnings({"java:S2245"})
    @PostMapping(value = "/case-retentions/caseNumber/{caseNumber}")
    public ResponseEntity<Integer> createCaseRetention(@PathVariable(name = "caseNumber") String caseNumber) {
        CourtCaseEntity courtCase = new CourtCaseEntity();
        courtCase.setCaseNumber(caseNumber);
        courtCase.setClosed(false);
        courtCase.setInterpreterUsed(false);

        String courtrooomNamme = "func-" + randomAlphanumeric(7);
        String courthouseName = "func-" + randomAlphanumeric(7);
        CourthouseEntity courthouse = newCourthouse(courthouseName);
        newCourtroom(courtrooomNamme, courthouse);

        courtCase.setCourthouse(courthouse);
        caseRepository.saveAndFlush(courtCase);

        RetentionPolicyTypeEntity retentionPolicyTypeEntity = new RetentionPolicyTypeEntity();
        retentionPolicyTypeEntity.setId(1);
        retentionPolicyTypeEntity.setFixedPolicyKey(1);
        retentionPolicyTypeEntity.setPolicyName("Standard");
        retentionPolicyTypeEntity.setDuration("7");
        retentionPolicyTypeEntity.setPolicyStart(OffsetDateTime.now().minusYears(1));
        retentionPolicyTypeEntity.setPolicyEnd(OffsetDateTime.now().plusYears(1));
        retentionPolicyTypeEntity.setCreatedDateTime(OffsetDateTime.now());
        retentionPolicyTypeEntity.setCreatedBy(userAccountRepository.getReferenceById(0));
        retentionPolicyTypeEntity.setLastModifiedDateTime(OffsetDateTime.now());
        retentionPolicyTypeEntity.setLastModifiedBy(userAccountRepository.getReferenceById(0));
        retentionPolicyTypeRepository.saveAndFlush(retentionPolicyTypeEntity);

        CaseRetentionEntity caseRetentionEntity = new CaseRetentionEntity();
        caseRetentionEntity.setCourtCase(courtCase);
        caseRetentionEntity.setId(1);
        caseRetentionEntity.setRetentionPolicyType(retentionPolicyTypeEntity);
        caseRetentionEntity.setTotalSentence("10 years?");
        caseRetentionEntity.setSubmittedBy(userAccountRepository.getReferenceById(0));
        caseRetentionEntity.setRetainUntil(OffsetDateTime.now().plusYears(7));
        caseRetentionEntity.setRetainUntilAppliedOn(OffsetDateTime.now().plusYears(1));
        caseRetentionEntity.setCurrentState("a_state");
        caseRetentionEntity.setCreatedDateTime(OffsetDateTime.now());
        caseRetentionEntity.setCreatedBy(userAccountRepository.getReferenceById(0));
        caseRetentionEntity.setLastModifiedDateTime(OffsetDateTime.now());
        caseRetentionEntity.setLastModifiedBy(userAccountRepository.getReferenceById(0));

        caseRetentionRepository.saveAndFlush(caseRetentionEntity);

        return new ResponseEntity<>(courtCase.getId(), OK);
    }
}
