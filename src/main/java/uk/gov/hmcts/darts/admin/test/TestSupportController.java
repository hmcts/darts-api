package uk.gov.hmcts.darts.admin.test;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
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
@ConditionalOnExpression("${testing-support-endpoints.enabled:false}")
public class TestSupportController {

    private final SessionFactory sessionFactory;
    private final CourthouseRepository courthouseRepository;
    private final CourtroomRepository courtroomRepository;
    private final UserAccountRepository userAccountRepository;
    private final NodeRegistrationRepository nodeRegistrationRepository;

    private final List<Integer> courthouseTrash = new ArrayList<>();
    private final List<Integer> courtroomTrash = new ArrayList<>();


    @DeleteMapping(value = "/clean")
    public void cleanUpDataAfterFunctionalTests() {

        Session session = sessionFactory.openSession();
        session.beginTransaction();

        var caseIds = getCaseIdsToBeDeleted(session);
        var hearingIds = hearingIdsToBeDeleted(session, caseIds);
        var eventIds = eventIdsToBeDeleted(session, hearingIds);

        removeHearingEventJoins(session, hearingIds);
        removeEvents(session, eventIds);
        removeHearings(session, hearingIds);
        removeCases(session, caseIds);

        @SuppressWarnings("unchecked")
        List<Integer> nodeRegisterIds =  nodeRegisterIdsToBeDeleted(session, courtroomTrash);
        removeNodeRegisters(nodeRegisterIds);

        emptyCourthouseTrash();

        session.getTransaction().commit();
        session.close();
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
        courtroomRepository.deleteAllById(courtroomTrash);
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
