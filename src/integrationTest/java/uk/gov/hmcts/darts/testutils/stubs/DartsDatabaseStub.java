package uk.gov.hmcts.darts.testutils.stubs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.repository.MediaRequestRepository;
import uk.gov.hmcts.darts.audio.util.AudioTestDataUtil;
import uk.gov.hmcts.darts.cases.repository.CaseRepository;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.JudgeEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectDirectoryStatusEntity;
import uk.gov.hmcts.darts.common.entity.ObjectDirectoryStatusEnum;
import uk.gov.hmcts.darts.common.repository.CourtroomRepository;
import uk.gov.hmcts.darts.common.repository.DefenceRepository;
import uk.gov.hmcts.darts.common.repository.DefendantRepository;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.repository.JudgeRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.ObjectDirectoryStatusRepository;
import uk.gov.hmcts.darts.common.repository.ProsecutorRepository;
import uk.gov.hmcts.darts.common.repository.TransientObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
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
import static uk.gov.hmcts.darts.testutils.data.CaseTestData.createCaseAtCourthouse;
import static uk.gov.hmcts.darts.testutils.data.CaseTestData.createCaseWithCaseNumber;
import static uk.gov.hmcts.darts.testutils.data.CourthouseTestData.createCourthouse;
import static uk.gov.hmcts.darts.testutils.data.CourtroomTestData.createCourtRoomWithNameAtCourthouse;
import static uk.gov.hmcts.darts.testutils.data.HearingTestData.createHearingWith;
import static uk.gov.hmcts.darts.testutils.data.MediaTestData.createMediaWith;

@Service
@AllArgsConstructor
@SuppressWarnings({"PMD.TooManyMethods", "PMD.ExcessiveImports"})
@Getter
@Slf4j
public class DartsDatabaseStub {

    private final CaseRepository caseRepository;
    private final EventRepository eventRepository;
    private final CourthouseRepository courthouseRepository;
    private final HearingRepository hearingRepository;
    private final CourtroomRepository courtroomRepository;
    private final MediaRepository mediaRepository;
    private final MediaRequestRepository mediaRequestRepository;
    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final ExternalLocationTypeRepository externalLocationTypeRepository;
    private final NotificationRepository notificationRepository;
    private final ObjectDirectoryStatusRepository objectDirectoryStatusRepository;
    private final DailyListRepository dailyListRepository;
    private final TransientObjectDirectoryRepository transientObjectDirectoryRepository;
    private final JudgeRepository judgeRepository;
    private final ProsecutorRepository prosecutorRepository;
    private final DefenceRepository defenceRepository;
    private final DefendantRepository defendantRepository;
    private final UserAccountRepository userAccountRepository;

    public void clearDatabase() {
        externalObjectDirectoryRepository.deleteAll();
        transientObjectDirectoryRepository.deleteAll();
        userAccountRepository.deleteAll();
        mediaRequestRepository.deleteAll();
        eventRepository.deleteAll();
        judgeRepository.deleteAll();
        hearingRepository.deleteAll();
        mediaRepository.deleteAll();
        notificationRepository.deleteAll();
        courtroomRepository.deleteAll();
        defenceRepository.deleteAll();
        defendantRepository.deleteAll();
        prosecutorRepository.deleteAll();
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

    public JudgeEntity createSimpleJudges(HearingEntity hearing) {

        var judgesEntity = new JudgeEntity();
        judgesEntity.setId(1);
        judgesEntity.setName("judge1");
        judgesEntity.setHearing(hearing);

        return judgeRepository.saveAndFlush(judgesEntity);
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

        var courtroomEntity = courtroomRepository.findByCourthouseNameAndCourtroomName(courthouseName, courtroomName);
        var hearingEntity = new HearingEntity();
        hearingEntity.setHearingIsActual(true);
        hearingEntity.setHearingDate(hearingDate);
        hearingEntity.setCourtCase(caseEntity);
        hearingEntity.setCourtroom(courtroomEntity.get());
        return hearingRepository.saveAndFlush(hearingEntity);
    }

    @Transactional
    public CourtCaseEntity givenTheDatabaseContainsCourtCaseAndCourthouseWithRoom(String caseNumber, String courthouseName, String courtroomName) {
        givenTheDatabaseContainsCourthouseWithRoom(courthouseName, courtroomName);
        var caseEntity = createCaseUnlessExists(caseNumber, courthouseName);

        return caseRepository.saveAndFlush(caseEntity);
    }

    @Transactional
    public CourtCaseEntity createCaseUnlessExists(String caseNumber, String courthouseName) {

        Optional<CourtCaseEntity> caseEntity = caseRepository.findByCaseNumberAndCourthouse_CourthouseName(
            caseNumber,
            courthouseName
        );

        if (caseEntity.isPresent()) {
            return caseEntity.get();
        }

        return createCase(caseNumber, courthouseName);

    }

    @Transactional
    public CourtCaseEntity createCase(String caseNumber, String courthouseName) {

        CourthouseEntity courthouse = createCourthouseUnlessExists(courthouseName);

        return caseRepository.save(createCaseAtCourthouse(caseNumber, courthouse));

    }

    @Transactional
    public CourthouseEntity createCourthouseUnlessExists(String courthouseName) {

        Optional<CourthouseEntity> courthouseEntityOptional = courthouseRepository.findByCourthouseNameIgnoreCase(
            courthouseName);

        CourthouseEntity courthouseEntity;

        if (courthouseEntityOptional.isEmpty()) {
            courthouseEntity = createCourthouseWithoutCourtrooms(courthouseName);
        } else {
            courthouseEntity = courthouseEntityOptional.get();
        }

        return courthouseEntity;
    }

    @Transactional
    public CourtroomEntity givenTheDatabaseContainsCourthouseWithRoom(String courthouseName, String courtroomName) {

        var persistedCourthouse = createCourthouseUnlessExists(courthouseName);

        var courtroom = new CourtroomEntity();
        courtroom.setName(courtroomName);
        courtroom.setCourthouse(persistedCourthouse);
        courtroomRepository.saveAndFlush(courtroom);
        return courtroom;
    }

    public List<NotificationEntity> getNotificationsForCase(Integer caseId) {
        return notificationRepository.findByCourtCase_Id(caseId);
    }

    @Transactional
    public HearingEntity hasSomeHearing() {
        return hearingRepository.saveAndFlush(
            createHearingWith(
                createCaseWithCaseNumber("c1"),
                createCourtRoomWithNameAtCourthouse(
                    createCourthouse("NEWCASTLE"), "r1"), now()
            ));
    }

    public CourthouseEntity createCourthouseWithoutCourtrooms(String courthouseName) {
        return courthouseRepository.save(createCourthouse(courthouseName));
    }

    public CourthouseEntity createCourthouseWithNameAndCode(String name, Integer code) {
        var courthouse = createCourthouse(name);
        courthouse.setCode(code);
        return courthouseRepository.save(courthouse);
    }

    public MediaEntity createMediaEntity(OffsetDateTime startTime, OffsetDateTime endTime, int channel) {
        return mediaRepository.saveAndFlush(createMediaWith(startTime, endTime, channel));
    }


    public CourtroomEntity findCourtroomBy(String courthouseName, String courtroomName) {
        return courtroomRepository.findByCourthouseNameAndCourtroomName(courthouseName, courtroomName).get();
    }

    public CourthouseEntity findCourthouseWithName(String name) {
        return courthouseRepository.findByCourthouseNameIgnoreCase(name).get();
    }

    public ExternalLocationTypeEntity getExternalLocationTypeEntity(ExternalLocationTypeEnum externalLocationTypeEnum) {
        return externalLocationTypeRepository.getReferenceById(externalLocationTypeEnum.getId());
    }

    public ObjectDirectoryStatusEntity getObjectDirectoryStatusEntity(ObjectDirectoryStatusEnum objectDirectoryStatusEnum) {
        return objectDirectoryStatusRepository.getReferenceById(objectDirectoryStatusEnum.getId());
    }

    @Transactional
    public MediaRequestEntity createAndLoadMediaRequestEntity() {

        var caseEntity = save(createCaseWithCaseNumber("2"));
        var courtroomEntity = save(
            createCourtRoomWithNameAtCourthouse(createCourthouse("NEWCASTLE"), "Int Test Courtroom 2"));
        var hearingEntityWithMediaRequest1 = save(createHearingWith(caseEntity, courtroomEntity));

        return save(
            AudioTestDataUtil.createMediaRequest(
                hearingEntityWithMediaRequest1,
                -2,
                OffsetDateTime.parse("2023-06-26T13:00:00Z"),
                OffsetDateTime.parse("2023-06-26T13:45:00Z")
            ));
    }

    public MediaEntity addMediaToHearing(HearingEntity hearing, MediaEntity mediaEntity) {
        mediaRepository.save(mediaEntity);
        hearing.addMedia(mediaEntity);
        hearingRepository.save(hearing);
        return mediaEntity;
    }

    public ExternalObjectDirectoryEntity save(ExternalObjectDirectoryEntity externalObjectDirectoryEntity) {
        return externalObjectDirectoryRepository.save(externalObjectDirectoryEntity);
    }

    public CourtCaseEntity save(CourtCaseEntity courtCaseEntity) {
        return caseRepository.save(courtCaseEntity);
    }

    public CourthouseEntity save(CourthouseEntity courthouseEntity) {
        return courthouseRepository.save(courthouseEntity);
    }

    public CourtroomEntity save(CourtroomEntity courtroom) {
        return courtroomRepository.save(courtroom);
    }

    public MediaRequestEntity save(MediaRequestEntity mediaRequestEntity) {
        return mediaRequestRepository.save(mediaRequestEntity);
    }

    public MediaEntity save(MediaEntity media) {
        return mediaRepository.save(media);
    }

    @Transactional
    public HearingEntity save(HearingEntity hearingEntity) {
        CourtroomEntity referenceById = courtroomRepository.getReferenceById(hearingEntity.getCourtroom().getId());
        hearingEntity.setCourtroom(referenceById);
        return hearingRepository.saveAndFlush(hearingEntity);
    }

    public void saveAll(HearingEntity... hearingEntities) {
        hearingRepository.saveAllAndFlush(asList(hearingEntities));
    }

    public void saveAll(EventEntity... eventEntities) {
        eventRepository.saveAll(asList(eventEntities));
    }

}
