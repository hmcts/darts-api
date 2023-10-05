package uk.gov.hmcts.darts.admin.test;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.repository.CourtroomRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.courthouse.CourthouseRepository;
import uk.gov.hmcts.darts.noderegistration.repository.NodeRegistrationRepository;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;

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

    private final List<Integer> courthouseTrash = new ArrayList<>();
    private final List<Integer> courtroomTrash = new ArrayList<>();


    @SuppressWarnings("unchecked")
    @DeleteMapping(value = "/clean")
    public void cleanUpDataAfterFunctionalTests() {

        log.info("Cleaning");
        Session session = sessionFactory.openSession();
        session.beginTransaction();

        var caseIds = getCaseIdsToBeDeleted(session);
        caseIds.forEach(c -> log.info("case id " + c));
        log.info("Cleaning get caseids");
        var hearingIds = hearingIdsToBeDeleted(session, caseIds);
        hearingIds.forEach(c -> log.info("hearing id " + c));
        log.info("Cleaning get hearings");
        var eventIds = eventIdsToBeDeleted(session, hearingIds);
        eventIds.forEach(c -> log.info("event id " + c));

        log.info("1 courtroomTrash.size()" + courtroomTrash.size());
        log.info("1 courthouseTrash.size()" + courthouseTrash.size());


        removeHearingEventJoins(session, hearingIds);
        log.info("Cleaning hearing joins");

        log.info("Cleaning get events");
        removeEvents(session, eventIds);
        log.info("Cleaning events");
        removeHearings(session, hearingIds);
        log.info("Cleaning hearings");

        removeCases(session, caseIds);
        log.info("Cleaning cases");

        log.info("2 courtroomTrash.size()" + courtroomTrash.size());
        log.info("2 courthouseTrash.size()" + courthouseTrash.size());

        List nodeRegisterIds =  nodeRegisterIdsToBeDeleted(session, courtroomTrash);
        removeNodeRegisters(nodeRegisterIds);
        log.info("Cleaning node register");

        removeCourtHouses(session);
        //emptyCourthouseTrash();
        log.info("Cleaning court house");

        session.getTransaction().commit();
        session.close();
    }

    private void removeCourtHouses(Session session) {
        log.info("removeCourtHouses");
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

            newCourtroom(courtroomName, courthouse);

            courthouseTrash.add(courthouse.getId());
        }

        return new ResponseEntity<>(CREATED);
    }

    private void emptyCourthouseTrash() {
        log.info("emptyCourthouseTrash");
        log.info("courtroomTrash.size()" + courtroomTrash.size());
        courtroomTrash.forEach(c -> log.info("courtroom id " + c));
        courtroomRepository.deleteAllById(courtroomTrash);
        log.info("courthouseTrash.size()" + courthouseTrash.size());
        courthouseTrash.forEach(c -> log.info("courthouse id " + c));
        courthouseRepository.deleteAllById(courthouseTrash);
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
        courtroom.setCreatedBy(userAccountRepository.getReferenceById(0));
        courtroomRepository.save(courtroom);

        courtroomTrash.add(courtroom.getId());
    }

    private void removeCases(Session session, List casIds) {
        session.createNativeQuery("""
                                      delete from darts.court_case where cas_id in (?)
                                      """)
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
                                      """)
            .setParameter(1, eveIds)
            .executeUpdate();
    }

    private void removeHearingEventJoins(Session session, List heaIds) {
        session.createNativeQuery("""
                                      delete from darts.hearing_event_ae where hea_id in (?)
                                      """)
            .setParameter(1, heaIds)
            .executeUpdate();
    }

    private void removeNodeRegisters(List<Integer> nodeIds) {
        nodeRegistrationRepository.deleteAllById(nodeIds);
    }

    private static List eventIdsToBeDeleted(Session session, List heaIds) {
        return session.createNativeQuery("""
                                             select eve_id from darts.hearing_event_ae where hea_id in (?)
                                             """)
            .setParameter(1, heaIds)
            .getResultList();
    }

    private static List hearingIdsToBeDeleted(Session session, List casIds) {
        return session.createNativeQuery("""
                                             select hea_id from darts.hearing where cas_id in (?)
                                             """)
            .setParameter(1, casIds)
            .getResultList();
    }

    private static List nodeRegisterIdsToBeDeleted(Session session, List crtIds) {
        return session.createNativeQuery("""
                                             select node_id from darts.node_register where ctr_id in (?)
                                             """)
            .setParameter(1, crtIds)
            .getResultList();
    }

    private static List getCaseIdsToBeDeleted(Session session) {
        return session.createNativeQuery("""
                                             select cas_id from darts.court_case where case_number like 'func-%'
                                             """)
            .getResultList();
    }
}
