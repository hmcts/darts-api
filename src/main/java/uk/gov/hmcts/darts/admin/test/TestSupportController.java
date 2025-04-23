package uk.gov.hmcts.darts.admin.test;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.RandomStringGenerator;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
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
import uk.gov.hmcts.darts.common.repository.NodeRegisterRepository;
import uk.gov.hmcts.darts.common.repository.RetentionPolicyTypeRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.common.service.bankholidays.BankHolidaysService;
import uk.gov.hmcts.darts.common.service.bankholidays.Event;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static java.lang.Integer.parseInt;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

@RestController
@RequestMapping(value = "/functional-tests")
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "darts.testing-support-endpoints", name = "enabled", havingValue = "true")
@SuppressWarnings({
    "PMD.UnnecessaryAnnotationValueElement",
    "PMD.TestClassWithoutTestCases",
    "PMD.CouplingBetweenObjects",//TODO - refactor to reduce coupling when this class is next edited
    "PMD.TooManyMethods"//TODO - refactor to reduce methods when this class is next edited
})
public class TestSupportController {

    private static final String FUNCTIONAL_TEST_KEY = "FUNC-";
    private static final String IDS = "ids";
    private final SessionFactory sessionFactory;
    private final CourthouseRepository courthouseRepository;
    private final CourtroomRepository courtroomRepository;
    private final UserAccountRepository userAccountRepository;
    private final NodeRegisterRepository nodeRegisterRepository;
    private final AuditActivityRepository auditActivityRepository;
    private final AuditRepository auditRepository;
    private final CaseRepository caseRepository;
    private final UserIdentity userIdentity;
    private final CaseRetentionRepository caseRetentionRepository;
    private final RetentionPolicyTypeRepository retentionPolicyTypeRepository;

    private final List<Integer> courthouseTrash = new ArrayList<>();
    private final List<Integer> courtroomTrash = new ArrayList<>();
    private final BankHolidaysService bankHolidaysService;

    @SuppressWarnings({"PMD.CloseResource"})
    @DeleteMapping(value = "/clean")
    public void cleanUpDataAfterFunctionalTests() {
        Session session = sessionFactory.openSession();
        session.beginTransaction();

        var caseIds = getCaseIdsToBeDeleted(session);
        var hearingIds = hearingIdsToBeDeleted(session, caseIds);
        var eventIds = eventIdsToBeDeleted(session, hearingIds);

        removeHearingEventJoins(session, hearingIds);
        removeEventLinkedCases(session, caseIds, eventIds);
        removeEvents(session, eventIds);
        removeHearings(session, hearingIds);

        log.info("Cleaned Events and Hearings");

        removeCaseRetentions(session, caseIds);
        removeCaseAudit(session, caseIds);
        removeCaseJudgeJoins(session, caseIds);
        removeCaseDefence(session, caseIds);
        removeCaseDefendant(session, caseIds);
        removeCaseProsecutor(session, caseIds);
        removeCases(session, caseIds);

        log.info("Cleaned case data");

        List<Integer> nodeRegisterIds = nodeRegisterIdsToBeDeleted(session, courtroomTrash);
        removeNodeRegisters(nodeRegisterIds);

        removeDailyLists(session);

        log.info("Cleaned node register and daily lists");

        removeUserCourthousePermissions(session, courthouseTrash);

        removeCourtHouses(session);

        log.info("Cleaned Courthouses");

        removeUserPermission(session);

        removeUsers(session);
        removeSecurityGroups(session);

        log.info("Cleaned users and groups");

        removeRetentionPolicyTypes(session);
        log.info("Cleaned retention policy types");

        session.getTransaction().commit();
        session.close();

        log.info("Cleanup finished");
    }

    private void removeUserPermission(Session session) {
        session.createNativeMutationQuery("""
                                              delete from darts.security_group_user_account_ae where usr_id in
                                              (select usr_id from darts.user_account where description = 'A temporary user created by functional test');
                                              """)
            .executeUpdate();
    }

    @SuppressWarnings({"PMD.UnusedFormalParameter"})
    private void removeUserCourthousePermissions(Session session, List<Integer> cthIds) {
        session.createNativeMutationQuery("""
                                              delete from darts.security_group_courthouse_ae where cth_id in
                                              (select cth_id from darts.courthouse where courthouse_name like 'FUNC-%');
                                              """)
            .executeUpdate();
    }

    private void removeDailyLists(Session session) {
        session.createNativeMutationQuery("""
                                              delete from darts.daily_list where unique_id like 'FUNC-%'
                                              """)
            .executeUpdate();
    }

    private void removeCourtHouses(Session session) {
        List<Integer> courthouseIds = session.createNativeQuery(
                """
                    select cth_id from darts.courthouse where  courthouse_name ilike 'FUNC-%'
                    """, Integer.class)
            .getResultList();
        if (courthouseIds.isEmpty()) {
            return;
        }

        session.createNativeMutationQuery("""
                                              delete from darts.courtroom where cth_id in ( :ids ) 
                                              """).setParameter(IDS, courthouseIds).executeUpdate();
        session.createNativeMutationQuery("""
                                              delete from darts.security_group_courthouse_ae where cth_id in ( :ids ) 
                                              """).setParameter(IDS, courthouseIds).executeUpdate();
        session.createNativeMutationQuery("""
                                              delete from darts.courthouse where cth_id in ( :ids ) 
                                              """).setParameter(IDS, courthouseIds).executeUpdate();
    }

    @PostMapping(value = "/courthouse/{courthouse_name}/courtroom/{courtroom_name}")
    @Transactional
    public ResponseEntity<String> createCourthouseAndCourtroom(
        @PathVariable(name = "courthouse_name") String courthouseName,
        @PathVariable(name = "courtroom_name") String courtroomName) {

        if (!courthouseName.startsWith(FUNCTIONAL_TEST_KEY)) {
            return new ResponseEntity<>("Courthouse name must start with FUNC-", BAD_REQUEST);
        }
        String courthouseNameUpperTrimmed = StringUtils.toRootUpperCase(StringUtils.trimToEmpty(courthouseName));

        if (courtroomRepository.findByCourthouseNameAndCourtroomName(courthouseNameUpperTrimmed, courtroomName).isEmpty()) {
            var courthouse = courthouseRepository.findByCourthouseName(courthouseNameUpperTrimmed)
                .orElseGet(() -> newCourthouse(courthouseNameUpperTrimmed));

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
    @Transactional(rollbackFor = DataIntegrityViolationException.class)
    public ResponseEntity<String> createAudit(@PathVariable(name = "audit_activity") String auditActivity,
                                              @PathVariable(name = "courthouse_name") String courthouseName) {

        CourtCaseEntity courtCase = new CourtCaseEntity();
        courtCase.setCaseNumber("FUNC-case1");
        courtCase.setClosed(false);
        courtCase.setInterpreterUsed(false);

        courtCase.setCreatedBy(userAccountRepository.getReferenceById(0));
        courtCase.setCreatedDateTime(OffsetDateTime.now());
        courtCase.setLastModifiedBy(userAccountRepository.getReferenceById(0));
        courtCase.setLastModifiedDateTime(OffsetDateTime.now());

        Optional<CourthouseEntity> foundCourthouse =
            courthouseRepository.findByCourthouseName(StringUtils.toRootUpperCase(StringUtils.trimToEmpty(courthouseName)));
        if (foundCourthouse.isPresent()) {
            courtCase.setCourthouse(foundCourthouse.get());
        } else {
            return new ResponseEntity<>("Court house not found", BAD_REQUEST);
        }

        CourtCaseEntity savedCase = caseRepository.saveAndFlush(courtCase);

        AuditEntity audit = new AuditEntity();
        audit.setCourtCase(savedCase);
        audit.setUser(userAccountRepository.getReferenceById(0));
        audit.setCreatedBy(userAccountRepository.getReferenceById(0));
        audit.setLastModifiedBy(userAccountRepository.getReferenceById(0));

        try {
            AuditActivityEntity foundAuditActivity = auditActivityRepository.getReferenceById(
                AuditActivity.valueOf(auditActivity).getId()
            );
            audit.setAuditActivity(foundAuditActivity);
            auditRepository.saveAndFlush(audit);
            return new ResponseEntity<>(CREATED);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(BAD_REQUEST, "Audit request failed", ex);
        }
    }

    private CourthouseEntity newCourthouse(String courthouseName) {
        var courthouse = new CourthouseEntity();
        courthouse.setCourthouseName(courthouseName);
        courthouse.setDisplayName(StringUtils.toRootLowerCase(courthouseName));
        UserAccountEntity defaultUser = new UserAccountEntity();
        defaultUser.setId(0);
        courthouse.setCreatedBy(defaultUser);
        courthouse.setLastModifiedBy(defaultUser);
        return courthouseRepository.save(courthouse);
    }

    private void newCourtroom(String courtroomName, CourthouseEntity courthouse) {
        var courtroom = new CourtroomEntity();
        courtroom.setCourthouse(courthouse);
        courtroom.setName(courtroomName);

        UserAccountEntity defaultUser = new UserAccountEntity();
        defaultUser.setId(0);
        courtroom.setCreatedBy(defaultUser);
        courtroomRepository.save(courtroom);

        courtroomTrash.add(courtroom.getId());
    }

    private void removeCases(Session session, List<Integer> casIds) {
        session.createNativeMutationQuery("""
                                              delete from darts.court_case where cas_id in (?)
                                              """)
            .setParameter(1, casIds)
            .executeUpdate();
    }

    private void removeHearings(Session session, List<Integer> heaIds) {
        session.createNativeMutationQuery("""
                                              delete from darts.hearing where hea_id in (?)
                                              """)
            .setParameter(1, heaIds)
            .executeUpdate();
    }

    private void removeEvents(Session session, List<Integer> eveIds) {
        session.createNativeMutationQuery("""
                                              delete from darts.event where event.eve_id in (?)
                                              """)
            .setParameter(1, eveIds)
            .executeUpdate();
    }

    private void removeHearingEventJoins(Session session, List<Integer> heaIds) {
        session.createNativeMutationQuery("""
                                              delete from darts.hearing_event_ae where hea_id in (?)
                                              """)
            .setParameter(1, heaIds)
            .executeUpdate();
    }

    private void removeCaseAudit(Session session, List<Integer> caseIds) {
        session.createNativeMutationQuery("""
                                              delete from darts.audit where cas_id in (?)
                                              """)
            .setParameter(1, caseIds)
            .executeUpdate();
    }

    private void removeCaseRetentions(Session session, List<Integer> caseIds) {
        session.createNativeMutationQuery("""
                                              delete from darts.case_retention where cas_id in (?)
                                              """)
            .setParameter(1, caseIds)
            .executeUpdate();
        session.createNativeMutationQuery("""
                                              delete from darts.case_management_retention where cas_id in (?)
                                              """)
            .setParameter(1, caseIds)
            .executeUpdate();
    }

    private void removeCaseJudgeJoins(Session session, List<Integer> caseIds) {
        session.createNativeMutationQuery("""
                                              delete from darts.case_judge_ae where cas_id in (?)
                                              """)
            .setParameter(1, caseIds)
            .executeUpdate();
    }

    private void removeCaseDefence(Session session, List<Integer> caseIds) {
        session.createNativeMutationQuery("""
                                              delete from darts.defence where cas_id in (?)
                                              """)
            .setParameter(1, caseIds)
            .executeUpdate();
    }

    private void removeCaseDefendant(Session session, List<Integer> caseIds) {
        session.createNativeMutationQuery("""
                                              delete from darts.defendant where cas_id in (?)
                                              """)
            .setParameter(1, caseIds)
            .executeUpdate();
    }

    private void removeCaseProsecutor(Session session, List<Integer> caseIds) {
        session.createNativeMutationQuery("""
                                              delete from darts.prosecutor where cas_id in (?)
                                              """)
            .setParameter(1, caseIds)
            .executeUpdate();
    }

    private void removeEventLinkedCases(Session session, List<Integer> caseIds, List<Integer> eventIds) {
        session.createNativeMutationQuery("""
                                              delete from darts.event_linked_case where cas_id in (?)
                                              """)
            .setParameter(1, caseIds)
            .executeUpdate();

        session.createNativeQuery("""
                                      delete from darts.event_linked_case where eve_id in (?)
                                      """, Integer.class)
            .setParameter(1, eventIds)
            .executeUpdate();
    }

    private void removeNodeRegisters(List<Integer> nodeIds) {
        nodeRegisterRepository.deleteAllById(nodeIds);
    }

    private static List<Integer> eventIdsToBeDeleted(Session session, List<Integer> heaIds) {
        List<Integer> eventsByHearing = session.createNativeQuery("""
                                                                      select eve_id from darts.hearing_event_ae where hea_id in (?)
                                                                      """, Integer.class)
            .setParameter(1, heaIds)
            .getResultList();

        List<Integer> eventsByMarker = session.createNativeQuery("""
                                                                     select event.eve_id from darts.event
                                                                     where event_text = 'A temporary event created by functional test'
                                                                     """, Integer.class)
            .getResultList();

        return Stream.concat(eventsByHearing.stream(), eventsByMarker.stream())
            .distinct()
            .toList();
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
                                             select cas_id from darts.court_case where case_number like 'FUNC-%'
                                             """, Integer.class)
            .getResultList();
    }

    private void removeUsers(Session session) {
        session.createNativeMutationQuery("""
                                              delete from darts.user_account where description = 'A temporary user created by functional test'
                                              """)
            .executeUpdate();
    }

    private void removeSecurityGroups(Session session) {
        List<Integer> securityGroupIds = session.createNativeQuery(
                """
                    select grp_id from darts.security_group where description = 'A temporary group created by functional test'
                    or description like '%FUNC-%'
                    """, Integer.class)
            .getResultList();
        if (securityGroupIds.isEmpty()) {
            return;
        }
        session.createNativeMutationQuery("""
                                              delete from darts.security_group_courthouse_ae where grp_id in ( :ids )
                                              """).setParameter("ids", securityGroupIds).executeUpdate();
        session.createNativeMutationQuery("""
                                              delete from darts.security_group where grp_id in ( :ids )
                                              """).setParameter("ids", securityGroupIds).executeUpdate();
    }

    private void removeRetentionPolicyTypes(Session session) {
        session.createNativeMutationQuery("""
                                              delete from darts.retention_policy_type where description like '%FUNC-%'
                                              """)
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
        UserAccountEntity userAccount = userAccountRepository.getReferenceById(0);

        courtCase.setCreatedBy(userAccount);
        courtCase.setCreatedDateTime(OffsetDateTime.now());
        courtCase.setLastModifiedBy(userAccount);
        courtCase.setLastModifiedDateTime(OffsetDateTime.now());

        char[][] allowedCharacterRanges = {{'A', 'Z'}, {'0', '9'}};
        String courtroomName = FUNCTIONAL_TEST_KEY + RandomStringGenerator.builder()
            .withinRange(allowedCharacterRanges).withinRange(7, 7);
        String courthouseName = FUNCTIONAL_TEST_KEY + RandomStringGenerator.builder()
            .withinRange(allowedCharacterRanges).withinRange(7, 7);
        CourthouseEntity courthouse = newCourthouse(courthouseName);
        newCourtroom(courtroomName, courthouse);

        courtCase.setCourthouse(courthouse);
        caseRepository.saveAndFlush(courtCase);

        Optional<RetentionPolicyTypeEntity> retentionPolicyTypeEntity = retentionPolicyTypeRepository.findById(2);

        if (retentionPolicyTypeEntity.isPresent()) {
            CaseRetentionEntity caseRetentionEntity = new CaseRetentionEntity();
            caseRetentionEntity.setCourtCase(courtCase);
            caseRetentionEntity.setRetentionPolicyType(retentionPolicyTypeEntity.get());
            caseRetentionEntity.setTotalSentence("10y0m0d");
            caseRetentionEntity.setSubmittedBy(userAccount);
            caseRetentionEntity.setRetainUntil(OffsetDateTime.now().plusYears(7));
            caseRetentionEntity.setRetainUntilAppliedOn(OffsetDateTime.now().plusYears(1));
            caseRetentionEntity.setCurrentState("a_state");
            caseRetentionEntity.setCreatedDateTime(OffsetDateTime.now());
            caseRetentionEntity.setCreatedBy(userAccount);
            caseRetentionEntity.setLastModifiedDateTime(OffsetDateTime.now());
            caseRetentionEntity.setLastModifiedBy(userAccount);

            caseRetentionRepository.saveAndFlush(caseRetentionEntity);
        }

        return new ResponseEntity<>(courtCase.getId(), OK);
    }
}
