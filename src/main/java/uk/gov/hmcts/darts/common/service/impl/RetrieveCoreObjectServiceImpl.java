package uk.gov.hmcts.darts.common.service.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.cases.repository.CaseRepository;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.exception.CommonApiError;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.CourtroomRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;
import uk.gov.hmcts.darts.courthouse.CourthouseRepository;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.Optional;

@RequiredArgsConstructor
@Service
@Slf4j
public class RetrieveCoreObjectServiceImpl implements RetrieveCoreObjectService {
    private final HearingRepository hearingRepository;
    private final CaseRepository caseRepository;
    private final CourthouseRepository courthouseRepository;
    private final CourtroomRepository courtroomRepository;

    private final int MAX_RETRIES = 3;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public HearingEntity retrieveOrCreateHearing(String courthouseName, String courtroomName, String caseNumber, LocalDate hearingDate) {
        int retryCount = 0;
        while (true) {
            try {
                Optional<HearingEntity> foundHearing = hearingRepository.findHearing(
                    courthouseName,
                    courtroomName,
                    caseNumber,
                    hearingDate
                );
                if (foundHearing.isPresent()) {
                    log.debug("Found a hearing with id " + foundHearing.get().getId());
                }

                return foundHearing.orElseGet(() -> createHearing(
                    courthouseName,
                    courtroomName,
                    caseNumber,
                    hearingDate
                ));
            } catch (DataIntegrityViolationException e) {
                if (++retryCount >= MAX_RETRIES) {
                    throw e;
                }
                randomSleep();
                Session session = entityManager.unwrap(Session.class);
                session.clear();
            }
        }
    }

    private HearingEntity createHearing(String courthouseName, String courtroomName, String caseNumber, LocalDate hearingDate) {
        CourtCaseEntity courtCase = retrieveOrCreateCase(courthouseName, caseNumber);
        CourtroomEntity courtroom = retrieveOrCreateCourtroom(courtCase.getCourthouse(), courtroomName);
        HearingEntity hearing = new HearingEntity();
        hearing.setCourtCase(courtCase);
        hearing.setCourtroom(courtroom);
        hearing.setHearingDate(hearingDate);
        hearing.setNew(true);
        hearingRepository.saveAndFlush(hearing);
        return hearing;
    }

    @Override
    @Transactional
    public CourtroomEntity retrieveOrCreateCourtroom(CourthouseEntity courthouse, String courtroomName) {
        Optional<CourtroomEntity> foundCourtroom = courtroomRepository.findByNameAndId(
            courthouse.getId(),
            courtroomName
        );
        return foundCourtroom.orElseGet(() -> createCourtroom(courthouse, courtroomName));

    }


    @Override
    @Transactional
    public CourtroomEntity retrieveOrCreateCourtroom(String courthouseName, String courtroomName) {
        Optional<CourtroomEntity> foundCourtroom = courtroomRepository.findByNames(courthouseName, courtroomName);
        if (foundCourtroom.isPresent()) {
            return foundCourtroom.get();
        }

        CourthouseEntity courthouse = retrieveCourthouse(courthouseName);
        return createCourtroom(courthouse, courtroomName);
    }


    private CourtroomEntity createCourtroom(CourthouseEntity courthouse, String courtroomName) {
        CourtroomEntity courtroom = new CourtroomEntity();
        courtroom.setCourthouse(courthouse);
        courtroom.setName(courtroomName);
        try {
            courtroomRepository.saveAndFlush(courtroom);
        } catch (DataIntegrityViolationException e) {
            log.warn(
                "Trying to create a courtroom that already exists. courthouse={}, courtroom={}.",
                courthouse.getCourthouseName(),
                courtroom,
                e
            );
            courtroom = courtroomRepository.findByNameAndId(courthouse.getId(), courtroomName).get();
        }
        return courtroom;
    }

    @Override
    @Transactional
    public CourtCaseEntity retrieveOrCreateCase(String courthouseName, String caseNumber) {
        int retryCount = 0;
        while (true) {
            try {
                log.debug("looking for existing case");
                Optional<CourtCaseEntity> foundCase = caseRepository.findByCaseNumberAndCourthouse_CourthouseName(
                    caseNumber,
                    courthouseName
                );
                if (foundCase.isPresent()) {
                    log.debug("Found a case with id " + foundCase.get().getId());
                } else {
                    log.debug("not found a case");
                }

                return foundCase.orElseGet(() -> createCase(courthouseName, caseNumber));
            } catch (DataIntegrityViolationException e) {
                randomSleep();
                if (++retryCount >= MAX_RETRIES) {
                    log.debug("max retries reached, throwing create case exception");
                    throw e;
                }
                Session session = entityManager.unwrap(Session.class);
                session.clear();
            }
        }
    }

    private static void randomSleep() {
//        try {
//            long millis = (long) (Math.random() * 400) + 100;
//            log.debug("sleeping for " + millis);
//            Thread.currentThread().sleep(millis);
//        } catch (InterruptedException ex) {
//            log.debug("Runtime exception thrown = " + ex.toString());
//            throw new RuntimeException(ex);
//        }
    }

    private CourtCaseEntity createCase(String courthouseName, String caseNumber) {
        CourthouseEntity foundCourthouse = retrieveCourthouse(courthouseName);
        CourtCaseEntity courtCase = new CourtCaseEntity();
        courtCase.setCaseNumber(caseNumber);
        courtCase.setCourthouse(foundCourthouse);
        caseRepository.saveAndFlush(courtCase);
        log.debug("Created case with ID " + courtCase.getId());
        return courtCase;
    }

    @Override
    public CourthouseEntity retrieveCourthouse(String courthouseName) {
        Optional<CourthouseEntity> foundCourthouse = courthouseRepository.findByCourthouseNameIgnoreCase(courthouseName);
        if (foundCourthouse.isEmpty()) {
            //Courthouses need to be created manually in the screens. throw an error.
            String message = MessageFormat.format("Courthouse ''{0}'' not found.", courthouseName);
            log.error(message);
            throw new DartsApiException(CommonApiError.COURTHOUSE_PROVIDED_DOES_NOT_EXIST, message);
        }
        return foundCourthouse.get();
    }

}
