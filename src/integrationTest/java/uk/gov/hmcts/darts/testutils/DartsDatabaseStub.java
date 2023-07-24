package uk.gov.hmcts.darts.testutils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.audio.repository.MediaRequestRepository;
import uk.gov.hmcts.darts.cases.repository.CaseRepository;
import uk.gov.hmcts.darts.cases.repository.ReportingRestrictionsRepository;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectDirectoryStatusEntity;
import uk.gov.hmcts.darts.common.entity.ObjectDirectoryStatusEnum;
import uk.gov.hmcts.darts.common.repository.CourtroomRepository;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.HearingMediaRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.ObjectDirectoryStatusRepository;
import uk.gov.hmcts.darts.common.repository.TransientObjectDirectoryRepository;
import uk.gov.hmcts.darts.courthouse.CourthouseRepository;
import uk.gov.hmcts.darts.dailylist.repository.DailyListRepository;
import uk.gov.hmcts.darts.notification.entity.NotificationEntity;
import uk.gov.hmcts.darts.notification.repository.NotificationRepository;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static java.time.LocalDate.now;
import static java.util.Arrays.asList;
import static uk.gov.hmcts.darts.common.util.CommonTestDataUtil.createCase;
import static uk.gov.hmcts.darts.common.util.CommonTestDataUtil.createCourtroom;
import static uk.gov.hmcts.darts.common.util.CommonTestDataUtil.createHearing;
import static uk.gov.hmcts.darts.testutils.MinimalEntities.aCase;
import static uk.gov.hmcts.darts.testutils.MinimalEntities.aCourtHouseWithName;
import static uk.gov.hmcts.darts.testutils.MinimalEntities.aMediaEntity;

@Service
@AllArgsConstructor
@SuppressWarnings({"PMD.TooManyMethods", "PMD.ExcessiveImports"})
@Getter
@Slf4j
public class DartsDatabaseStub {

    private final CaseRepository caseRepository;
    private final EventRepository eventRepository;
    private final ReportingRestrictionsRepository reportingRestrictionsRepository;
    private final CourthouseRepository courthouseRepository;
    private final HearingRepository hearingRepository;
    private final CourtroomRepository courtroomRepository;
    private final MediaRepository mediaRepository;
    private final MediaRequestRepository mediaRequestRepository;
    private final HearingMediaRepository hearingMediaRepository;
    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final ExternalLocationTypeRepository externalLocationTypeRepository;
    private final NotificationRepository notificationRepository;
    private final ObjectDirectoryStatusRepository objectDirectoryStatusRepository;
    private final DailyListRepository dailyListRepository;
    private final TransientObjectDirectoryRepository transientObjectDirectoryRepository;

    public void clearDatabase() {
        externalObjectDirectoryRepository.deleteAll();
        hearingMediaRepository.deleteAll();
        mediaRepository.deleteAll();
        transientObjectDirectoryRepository.deleteAll();
        mediaRequestRepository.deleteAll();
        eventRepository.deleteAll();
        hearingRepository.deleteAll();
        notificationRepository.deleteAll();
        courtroomRepository.deleteAll();
        caseRepository.deleteAll();
        dailyListRepository.deleteAll();
        courthouseRepository.deleteAll();
    }

    public Optional<CourtCaseEntity> findByCaseByCaseNumberAndCourtHouseName(String someCaseNumber, String someCourthouse) {
        return caseRepository.findByCaseNumberAndCourthouse_CourthouseName(someCaseNumber, someCourthouse);
    }

    public List<HearingEntity> findByCourthouseCourtroomAndDate(String someCourthouse, String someRoom, LocalDate toLocalDate) {
        return hearingRepository.findByCourthouseCourtroomAndDate(someCourthouse, someRoom, toLocalDate);
    }

    public List<EventEntity> getAllEvents() {
        return eventRepository.findAll();
    }

    @Transactional
    public void givenTheCourtHouseHasRoom(CourthouseEntity courthouse, String roomName) {
        var courtroom = new CourtroomEntity();
        courtroom.setName(roomName);
        courtroom.setCourthouse(courthouseRepository.getReferenceById(courthouse.getId()));
        courtroomRepository.saveAndFlush(courtroom);
    }

    @Transactional
    public HearingEntity givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
        String caseNumber, String courthouseName, String courtroomName, LocalDate hearingDate) {
        var caseEntity = givenTheDatabaseContainsCourtCaseAndCourthouseWithRoom(
            caseNumber,
            courthouseName,
            courtroomName
        );
        var courtroomEntity = courtroomRepository.findByNames(courthouseName, courtroomName);
        var hearingEntity = new HearingEntity();
        hearingEntity.setHearingIsActual(true);
        hearingEntity.setHearingDate(hearingDate);
        hearingEntity.setCourtCase(caseEntity);
        hearingEntity.setCourtroom(courtroomEntity);
        return hearingRepository.saveAndFlush(hearingEntity);
    }

    @Transactional
    public CourtCaseEntity givenTheDatabaseContainsCourtCaseAndCourthouseWithRoom(String caseNumber, String courthouseName, String courtroomName) {
        var courtroom = givenTheDatabaseContainsCourthouseWithRoom(courthouseName, courtroomName);
        var caseEntity = aCase();
        caseEntity.setCaseNumber(caseNumber);
        caseEntity.setCourthouse(courtroom.getCourthouse());
        return caseRepository.saveAndFlush(caseEntity);
    }

    @Transactional
    public CourtroomEntity givenTheDatabaseContainsCourthouseWithRoom(String courthouseName, String courtroomName) {
        var courthouse = new CourthouseEntity();
        courthouse.setCourthouseName(courthouseName);
        var persistedCourthouse = courthouseRepository.saveAndFlush(courthouse);

        var courtroom = new CourtroomEntity();
        courtroom.setName(courtroomName);
        courtroom.setCourthouse(persistedCourthouse);
        courtroomRepository.saveAndFlush(courtroom);
        return courtroom;
    }

    public CourtCaseEntity hasSomeCourtCase() {
        return caseRepository.save(aCase());
    }

    public List<NotificationEntity> getNotificationsForCase(Integer caseId) {
        return notificationRepository.findByCourtCase_Id(caseId);
    }

    @Transactional
    public HearingEntity hasSomeHearing() {
        return hearingRepository.saveAndFlush(createHearing(createCase("c1"), createCourtroom("r1"), now()));
    }

    public void saveAll(HearingEntity... hearingEntities) {
        hearingRepository.saveAll(asList(hearingEntities));
    }

    public CourthouseEntity createCourthouseWithoutCourtrooms(String courthouseName) {
        return courthouseRepository.save(aCourtHouseWithName(courthouseName));
    }

    public CourthouseEntity createCourthouseWithNameAndCode(String name, Integer code) {
        var courthouse = aCourtHouseWithName(name);
        courthouse.setCode(code);
        return courthouseRepository.save(courthouse);
    }

    public MediaEntity createMediaEntity(OffsetDateTime startTime, OffsetDateTime endTime, int channel) {
        return mediaRepository.saveAndFlush(aMediaEntity(startTime, endTime, channel));
    }


    public CourtroomEntity findCourtroomBy(String courthouseName, String courtroomName) {
        return courtroomRepository.findByNames(courthouseName, courtroomName);
    }

    public void save(CourtCaseEntity courtCaseEntity) {
        caseRepository.save(courtCaseEntity);
    }

    public void save(CourthouseEntity courthouseEntity) {
        courthouseRepository.save(courthouseEntity);
    }

    public CourthouseEntity findCourthouseWithName(String name) {
        return courthouseRepository.findByCourthouseName(name).get();
    }

    public ExternalLocationTypeEntity getExternalLocationTypeEntity(ExternalLocationTypeEnum externalLocationTypeEnum) {
        return externalLocationTypeRepository.getReferenceById(externalLocationTypeEnum.getId());
    }

    public ObjectDirectoryStatusEntity getObjectDirectoryStatusEntity(ObjectDirectoryStatusEnum objectDirectoryStatusEnum) {
        return objectDirectoryStatusRepository.getReferenceById(objectDirectoryStatusEnum.getId());
    }

}
