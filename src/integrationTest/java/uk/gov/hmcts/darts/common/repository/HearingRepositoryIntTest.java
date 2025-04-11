package uk.gov.hmcts.darts.common.repository;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.HearingStub;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.hmcts.darts.test.common.data.CourthouseTestData.someMinimalCourthouse;
import static uk.gov.hmcts.darts.test.common.data.CourtroomTestData.someMinimalCourtRoom;
import static uk.gov.hmcts.darts.test.common.data.DefenceTestData.createListOfDefenceForCase;
import static uk.gov.hmcts.darts.test.common.data.DefendantTestData.createListOfDefendantsForCase;
import static uk.gov.hmcts.darts.test.common.data.JudgeTestData.createListOfJudges;
import static uk.gov.hmcts.darts.test.common.data.ProsecutorTestData.createListOfProsecutor;

@Slf4j
class HearingRepositoryIntTest extends PostgresIntegrationBase {

    // generation count. Should always be an even number
    private static final int GENERATION_COUNT = 10;

    private static final int RESULT_LIMIT = GENERATION_COUNT;
    public static final String COURTHOUSE_NAME = "TEST COURTHOUSE";
    public static final String COURTROOM_NAME = "TEST COURTROOM";

    private List<HearingEntity> generatedHearingEntities;

    @Autowired
    private HearingStub hearingStub;

    @Autowired
    private HearingRepository hearingRepository;

    @BeforeEach
    public void before() {
        generatedHearingEntities = hearingStub.generateHearings(GENERATION_COUNT);
    }

    @Test
    void testGetAllHearingNoSearchCriteria() {
        generatedHearingEntities.sort((he1, he2) -> he2.getHearingDate().compareTo(he1.getHearingDate()));
        List<HearingEntity> hearingEntityList = hearingRepository.findHearingDetails(null, null,
                                                                                     null, null,
                                                                                     null, RESULT_LIMIT);
        assertEquals(generatedHearingEntities.size(), hearingEntityList.size());
        for (int i = 0; i < generatedHearingEntities.size(); i++) {
            assertEquals(generatedHearingEntities.get(i).getId(), hearingEntityList.get(i).getId());
        }
    }

    @Test
    void testGetHearingWithLimit() {
        generatedHearingEntities.sort((he1, he2) -> he2.getHearingDate().compareTo(he1.getHearingDate()));
        int resultLimit = 2;
        List<HearingEntity> hearingEntityList = hearingRepository.findHearingDetails(null, null,
                                                                                     null, null,
                                                                                     null, resultLimit);
        assertEquals(resultLimit, hearingEntityList.size());
        for (int i = 0; i < generatedHearingEntities.size(); i++) {
            if (i < resultLimit) {
                assertEquals(generatedHearingEntities.get(i).getId(), hearingEntityList.get(i).getId());
            } else {
                break;
            }
        }
    }

    @Test
    void testGetHearingUsingAllSearchCriteria() {
        int recordIndexToFind = GENERATION_COUNT / 2;
        List<HearingEntity> hearingEntityList = hearingRepository
            .findHearingDetails(List.of(generatedHearingEntities
                                            .get(recordIndexToFind).getCourtroom().getCourthouse().getId()),
                                generatedHearingEntities
                                    .get(recordIndexToFind).getCourtCase().getCaseNumber(),
                                generatedHearingEntities
                                    .get(recordIndexToFind).getCourtroom().getName(),
                                generatedHearingEntities
                                    .get(recordIndexToFind).getHearingDate(),
                                generatedHearingEntities
                                    .get(recordIndexToFind).getHearingDate(), RESULT_LIMIT);
        assertEquals(1, hearingEntityList.size());
        assertEquals(generatedHearingEntities.get(recordIndexToFind).getId(),
                     hearingEntityList.getFirst().getId());
    }

    @Test
    void testGetHearingForCourthouseIds() {
        generatedHearingEntities.sort((he1, he2) -> he2.getHearingDate().compareTo(he1.getHearingDate()));
        int recordIndexToFind = GENERATION_COUNT / 2;
        int recordIndexToFindNext = (GENERATION_COUNT / 2) + 1;

        List<HearingEntity> hearingEntityList = hearingRepository.findHearingDetails(List.of(generatedHearingEntities
                                                                                                 .get(recordIndexToFind)
                                                                                                 .getCourtroom().getCourthouse().getId(),
                                                                                             generatedHearingEntities
                                                                                                 .get(recordIndexToFindNext)
                                                                                                 .getCourtroom().getCourthouse().getId()),
                                                                                     null,
                                                                                     null,
                                                                                     null,
                                                                                     null, RESULT_LIMIT);
        assertEquals(2, hearingEntityList.size());
        assertEquals(generatedHearingEntities.get(recordIndexToFind).getId(), hearingEntityList.getFirst().getId());
        assertEquals(generatedHearingEntities.get(recordIndexToFindNext).getId(), hearingEntityList.get(1).getId());
    }

    @Test
    void testGetHearingsForCaseNumber() {
        int recordIndexToFind = GENERATION_COUNT / 2;

        List<HearingEntity> hearingEntityList = hearingRepository.findHearingDetails(null,
                                                                                     generatedHearingEntities
                                                                                         .get(recordIndexToFind).getCourtCase().getCaseNumber(),
                                                                                     null,
                                                                                     null,
                                                                                     null, RESULT_LIMIT);
        assertEquals(1, hearingEntityList.size());
        assertEquals(generatedHearingEntities.get(recordIndexToFind).getId(), hearingEntityList.getFirst().getId());
    }

    @Test
    void testGetHearingsForPrefixCaseInsensitiveCaseNumber() {
        Integer recordIndexToFind = GENERATION_COUNT / 2;

        List<HearingEntity> hearingEntityList = hearingRepository
            .findHearingDetails(null,
                                HearingSubStringQueryEnum.CASE_NUMBER
                                    .getQueryStringPrefix(recordIndexToFind.toString()),
                                null,
                                null,
                                null, RESULT_LIMIT);
        assertEquals(1, hearingEntityList.size());
        assertEquals(generatedHearingEntities.get(recordIndexToFind).getId(), hearingEntityList.getFirst().getId());
    }

    @Test
    void testGetHearingsForPostfixCaseInsensitiveCaseNumber() {
        Integer recordIndexToFind = GENERATION_COUNT / 2;

        List<HearingEntity> hearingEntityList = hearingRepository.findHearingDetails(null,
                                                                                     HearingSubStringQueryEnum.CASE_NUMBER
                                                                                         .getQueryStringPostfix(recordIndexToFind.toString()),
                                                                                     null,
                                                                                     null,
                                                                                     null, RESULT_LIMIT);
        assertEquals(1, hearingEntityList.size());
        assertEquals(generatedHearingEntities.get(recordIndexToFind).getId(), hearingEntityList.getFirst().getId());
    }

    @Test
    void testGetHearingsForCourtroomName() {
        int recordIndexToFind = GENERATION_COUNT / 2;

        List<HearingEntity> hearingEntityList = hearingRepository.findHearingDetails(null,
                                                                                     null,
                                                                                     generatedHearingEntities
                                                                                         .get(recordIndexToFind).getCourtroom().getName(),
                                                                                     null,
                                                                                     null, RESULT_LIMIT);
        assertEquals(1, hearingEntityList.size());
        assertEquals(generatedHearingEntities.get(recordIndexToFind).getId(), hearingEntityList.getFirst().getId());
    }

    @Test
    void testGetHearingsForPrefixCourtroomCaseInsensitiveCourtroomName() {
        Integer recordIndexToFind = GENERATION_COUNT / 2;

        List<HearingEntity> hearingEntityList = hearingRepository.findHearingDetails(null,
                                                                                     null,
                                                                                     HearingSubStringQueryEnum.COURT_HOUSE
                                                                                         .getQueryStringPrefix(recordIndexToFind.toString()),
                                                                                     null,
                                                                                     null, RESULT_LIMIT);
        assertEquals(1, hearingEntityList.size());
        assertEquals(generatedHearingEntities.get(recordIndexToFind).getId(), hearingEntityList.getFirst().getId());
    }

    @Test
    void testGetHearingsForPostfixCourtroomCaseInsensitiveCourtroomName() {
        Integer recordIndexToFind = GENERATION_COUNT / 2;

        List<HearingEntity> hearingEntityList = hearingRepository.findHearingDetails(null,
                                                                                     null,
                                                                                     HearingSubStringQueryEnum.COURT_HOUSE
                                                                                         .getQueryStringPostfix(recordIndexToFind.toString()),
                                                                                     null,
                                                                                     null, RESULT_LIMIT);
        assertEquals(1, hearingEntityList.size());
        assertEquals(generatedHearingEntities.get(recordIndexToFind).getId(), hearingEntityList.getFirst().getId());
    }

    @Test
    void testGetHearingsForHearingStartDate() {
        generatedHearingEntities.sort((he1, he2) -> he2.getHearingDate().compareTo(he1.getHearingDate()));
        Integer recordIndexToFindFrom = GENERATION_COUNT / 2;

        List<HearingEntity> hearingEntityList = hearingRepository.findHearingDetails(null,
                                                                                     null,
                                                                                     null,
                                                                                     generatedHearingEntities.get(recordIndexToFindFrom).getHearingDate(),
                                                                                     null, RESULT_LIMIT);

        List<HearingEntity> expectedHearings = generatedHearingEntities.subList(0, recordIndexToFindFrom + 1);
        assertEquals(expectedHearings.size(), hearingEntityList.size());

        for (int i = 0; i < expectedHearings.size(); i++) {
            assertEquals(expectedHearings.get(i).getId(), hearingEntityList.get(i).getId());
        }
    }

    @Test
    void testGetHearingsForHearingAfterDate() {
        Integer recordIndexToFindTo = GENERATION_COUNT / 2;
        generatedHearingEntities.sort((he1, he2) -> he2.getHearingDate().compareTo(he1.getHearingDate()));

        List<HearingEntity> hearingEntityList = hearingRepository.findHearingDetails(null,
                                                                                     null,
                                                                                     null,
                                                                                     null,
                                                                                     generatedHearingEntities.get(recordIndexToFindTo).getHearingDate(),
                                                                                     RESULT_LIMIT);

        List<HearingEntity> expectedHearings = generatedHearingEntities.subList(recordIndexToFindTo, generatedHearingEntities.size());
        assertEquals(expectedHearings.size(), hearingEntityList.size());

        for (int i = 0; i < expectedHearings.size(); i++) {
            assertEquals(expectedHearings.get(i).getId(), hearingEntityList.get(i).getId());
        }
    }

    @Test
    void testGetHearingsForHearingBetweenBeforeAndAfterDate() {
        generatedHearingEntities.sort((he1, he2) -> he2.getHearingDate().compareTo(he1.getHearingDate()));
        Integer recordToOffset = 2;
        Integer recordIndexToFindFrom = (GENERATION_COUNT / 2) - 1;
        Integer recordIndexToFindTo = (GENERATION_COUNT / 2) + recordToOffset - 1;

        List<HearingEntity> hearingEntityList = hearingRepository.findHearingDetails(null,
                                                                                     null,
                                                                                     null,
                                                                                     generatedHearingEntities.get(recordIndexToFindTo).getHearingDate(),
                                                                                     generatedHearingEntities.get(recordIndexToFindFrom).getHearingDate(),
                                                                                     RESULT_LIMIT);
        List<HearingEntity> expectedHearings = generatedHearingEntities.subList(recordIndexToFindFrom, recordIndexToFindTo + 1);
        assertEquals(expectedHearings.size(), hearingEntityList.size());

        for (int i = 0; i < expectedHearings.size(); i++) {
            assertEquals(expectedHearings.get(i).getId(), hearingEntityList.get(i).getId());
        }
    }

    @Test
    void testFindHearingIdsByEventId() {
        EventEntity event = dartsDatabase.getEventStub().createEvent(generatedHearingEntities.getFirst());

        List<Integer> hearingIdList = hearingRepository.findHearingIdsByEventId(event.getId());
        assertEquals(1, hearingIdList.size());
        assertEquals(1, hearingIdList.getFirst());
    }

    @Test
    void findByIsActualCaseIds_shouldReturnHearingEntities() {
        HearingEntity hearing1 = generatedHearingEntities.getFirst();
        HearingEntity hearing2 = generatedHearingEntities.get(1);
        HearingEntity hearing3 = generatedHearingEntities.get(2);
        HearingEntity hearing4 = generatedHearingEntities.get(3);

        CourtCaseEntity courtCase = hearing1.getCourtCase();

        hearing1.setHearingIsActual(true);
        hearing2.setHearingIsActual(false);
        hearing2.setCourtCase(courtCase);
        hearing3.setHearingIsActual(true);
        hearing3.setCourtCase(courtCase);
        hearing4.setHearingIsActual(true);

        dartsDatabase.save(hearing1);
        dartsDatabase.save(hearing2);
        dartsDatabase.save(hearing3);
        dartsDatabase.save(hearing4);

        List<HearingEntity> hearingEntities = hearingRepository.findByIsActualCaseIds(List.of(
            hearing1.getCourtCase().getId(),
            hearing4.getCourtCase().getId()
        ));
        assertThat(hearingEntities).hasSize(3);
        assertThat(hearingEntities.stream().map(HearingEntity::getId).toList())
            .containsExactlyInAnyOrder(hearing1.getId(), hearing3.getId(), hearing4.getId());
    }

    @Test
    void findByCourthouseCourtroomAndDate_ReturnsHearing() {
        // given
        CourthouseEntity courthouse = someMinimalCourthouse();
        courthouse.setCourthouseName("Test Courthouse");

        CourtroomEntity courtroom = someMinimalCourtRoom();
        courtroom.setName("Test Courtroom");
        courtroom.setCourthouse(courthouse);

        HearingEntity hearingForCase = setupHearingForCase(courthouse, courtroom);
        dartsPersistence.saveAll(hearingForCase);

        String courthouseName = hearingForCase.getCourtroom().getCourthouse().getCourthouseName();
        String courtroomName = hearingForCase.getCourtroom().getName();

        List<HearingEntity> hearingEntities = hearingRepository.findByCourthouseCourtroomAndDate(
            courthouseName,
            courtroomName,
            hearingForCase.getHearingDate()
        );

        // then
        assertThat(hearingEntities).hasSize(1);
        assertThat(hearingEntities.getFirst().getId()).isEqualTo(hearingForCase.getId());
        assertEquals(COURTHOUSE_NAME, hearingEntities.getFirst().getCourtroom().getCourthouse().getCourthouseName());
        assertEquals(COURTROOM_NAME, hearingEntities.getFirst().getCourtroom().getName());
    }

    @Test
    void findByCourthouseCourtroomAndDate_ReturnsHearing_UsingWhitespacesFindBy() {
        // given
        CourthouseEntity courthouse = someMinimalCourthouse();
        courthouse.setCourthouseName("Test Courthouse");

        CourtroomEntity courtroom = someMinimalCourtRoom();
        courtroom.setName("Test Courtroom");
        courtroom.setCourthouse(courthouse);

        HearingEntity hearingForCase = setupHearingForCase(courthouse, courtroom);
        dartsPersistence.saveAll(hearingForCase);

        String courthouseName = " Test Courthouse ";
        String courtroomName = " Test Courtroom ";

        log.info("Courthouse name: {}", courthouseName);
        log.info("Courtroom name: {}", courtroomName);

        // when
        List<HearingEntity> hearingEntities = hearingRepository.findByCourthouseCourtroomAndDate(
            courthouseName,
            courtroomName,
            hearingForCase.getHearingDate()
        );

        // then
        assertThat(hearingEntities).hasSize(1);
        assertThat(hearingEntities.getFirst().getId()).isEqualTo(hearingForCase.getId());
        assertEquals(COURTHOUSE_NAME, hearingEntities.getFirst().getCourtroom().getCourthouse().getCourthouseName());
        assertEquals(COURTROOM_NAME, hearingEntities.getFirst().getCourtroom().getName());
    }

    @Test
    void findHearing_ReturnsHearing() {
        CourthouseEntity courthouse = someMinimalCourthouse();
        courthouse.setCourthouseName("Test Courthouse");

        CourtroomEntity courtroom = someMinimalCourtRoom();
        courtroom.setName("Test Courtroom");
        courtroom.setCourthouse(courthouse);

        HearingEntity hearingForCase = setupHearingForCase(courthouse, courtroom);
        dartsPersistence.saveAll(hearingForCase);

        String courthouseName = hearingForCase.getCourtroom().getCourthouse().getCourthouseName();
        String courtroomName = hearingForCase.getCourtroom().getName();

        log.info("Courthouse name: {}", courthouseName);
        log.info("Courtroom name: {}", courtroomName);

        // when
        Optional<HearingEntity> hearingEntities = hearingRepository.findHearing(
            courthouseName,
            courtroomName,
            "Case0000001",
            hearingForCase.getHearingDate()
        );

        // then
        assertThat(hearingEntities).isPresent();
        assertThat(hearingEntities.get().getId()).isEqualTo(hearingForCase.getId());
        assertEquals(COURTHOUSE_NAME, hearingEntities.get().getCourtroom().getCourthouse().getCourthouseName());
        assertEquals(COURTROOM_NAME, hearingEntities.get().getCourtroom().getName());
    }

    @Test
    void findHearing_ReturnsHearing_UsingWhitespaceFindBy() {
        CourthouseEntity courthouse = someMinimalCourthouse();
        courthouse.setCourthouseName("Test Courthouse");

        CourtroomEntity courtroom = someMinimalCourtRoom();
        courtroom.setName("Test Courtroom");
        courtroom.setCourthouse(courthouse);

        HearingEntity hearingForCase = setupHearingForCase(courthouse, courtroom);
        dartsPersistence.saveAll(hearingForCase);

        String courthouseName = " Test Courthouse ";
        String courtroomName = " Test Courtroom ";

        log.info("Courthouse name: {}", courthouseName);
        log.info("Courtroom name: {}", courtroomName);

        // when
        Optional<HearingEntity> hearingEntities = hearingRepository.findHearing(
            courthouseName,
            courtroomName,
            "Case0000001",
            hearingForCase.getHearingDate()
        );

        // then
        assertThat(hearingEntities).isPresent();
        assertThat(hearingEntities.get().getId()).isEqualTo(hearingForCase.getId());
        assertEquals(COURTHOUSE_NAME, hearingEntities.get().getCourtroom().getCourthouse().getCourthouseName());
        assertEquals(COURTROOM_NAME, hearingEntities.get().getCourtroom().getName());
    }

    @Test
    void findHearing_usingCourtCaseAndCourtRoomObjects_ReturnsHearing() {
        CourthouseEntity courthouse = someMinimalCourthouse();
        courthouse.setCourthouseName("Test Courthouse");

        CourtroomEntity courtroom = someMinimalCourtRoom();
        courtroom.setName("Test Courtroom");
        courtroom.setCourthouse(courthouse);

        HearingEntity hearingForCase = setupHearingForCase(courthouse, courtroom);
        dartsPersistence.saveAll(hearingForCase);

        String courthouseName = hearingForCase.getCourtroom().getCourthouse().getCourthouseName();
        String courtroomName = hearingForCase.getCourtroom().getName();

        log.info("Courthouse name: {}", courthouseName);
        log.info("Courtroom name: {}", courtroomName);

        // when
        Optional<HearingEntity> hearingEntities = hearingRepository.findHearing(
            hearingForCase.getCourtCase(),
            hearingForCase.getCourtroom(),
            hearingForCase.getHearingDate()
        );

        // then
        assertThat(hearingEntities).isPresent();
        assertThat(hearingEntities.get().getId()).isEqualTo(hearingForCase.getId());
    }

    private HearingEntity setupHearingForCase(CourthouseEntity courthouseEntity, CourtroomEntity courtroomEntity) {
        var case1 = PersistableFactory.getCourtCaseTestData().createCaseAt(courthouseEntity);
        case1.setCaseNumber("Case0000001");
        case1.setDefendantList(createListOfDefendantsForCase(2, case1));
        case1.setDefenceList(createListOfDefenceForCase(2, case1));
        case1.setProsecutorList(createListOfProsecutor(2, case1));

        var hearingForCase1 = PersistableFactory.getHearingTestData().createHearingWith(case1, courtroomEntity);
        hearingForCase1.addJudges(createListOfJudges(1, case1));
        hearingForCase1.setHearingDate(LocalDate.parse("2023-06-20"));
        hearingForCase1.setScheduledStartTime(LocalTime.parse("09:00"));
        return hearingForCase1;
    }
}