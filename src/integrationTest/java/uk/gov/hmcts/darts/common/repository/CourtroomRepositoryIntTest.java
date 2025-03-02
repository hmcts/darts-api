package uk.gov.hmcts.darts.common.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.darts.test.common.data.CourthouseTestData.someMinimalCourthouse;
import static uk.gov.hmcts.darts.test.common.data.CourtroomTestData.someMinimalCourtRoom;
import static uk.gov.hmcts.darts.test.common.data.DefenceTestData.createListOfDefenceForCase;
import static uk.gov.hmcts.darts.test.common.data.DefendantTestData.createListOfDefendantsForCase;
import static uk.gov.hmcts.darts.test.common.data.JudgeTestData.createListOfJudges;
import static uk.gov.hmcts.darts.test.common.data.ProsecutorTestData.createListOfProsecutor;

class CourtroomRepositoryIntTest extends PostgresIntegrationBase {

    private static final String TEST_COURTHOUSE = "Test Courthouse";
    private static final String COURTHOUSE_NAME = "TEST COURTHOUSE";
    private static final String TEST_COURTROOM = "Test Courtroom";
    private static final String COURTROOM_NAME = "TEST COURTROOM";

    @Autowired
    private CourtroomRepository courtroomRepository;

    private CourthouseEntity courthouse;
    private CourtroomEntity courtroom;

    @BeforeEach
    void setUp() {

        courthouse = someMinimalCourthouse();
        courthouse.setCourthouseName(TEST_COURTHOUSE);
        courthouse.setDisplayName(COURTHOUSE_NAME);

        courtroom = someMinimalCourtRoom();
        courtroom.setName(TEST_COURTROOM);
        courtroom.setCourthouse(courthouse);

        HearingEntity hearingForCase = setupHearingForCase(courthouse, courtroom);
        dartsPersistence.saveAll(hearingForCase);
    }

    @Test
    void findByCourthouseNameAndCourtroomName_ShouldReturnCourtroom() {
        // when
        Optional<CourtroomEntity> foundCourtroom = courtroomRepository.findByCourthouseNameAndCourtroomName(TEST_COURTHOUSE, TEST_COURTROOM);

        // then
        assertTrue(foundCourtroom.isPresent());
        assertEquals(courtroom.getName(), foundCourtroom.get().getName());
        assertEquals(courthouse.getCourthouseName(), foundCourtroom.get().getCourthouse().getCourthouseName());
    }

    @Test
    void findByCourthouseNameAndCourtroomName_ShouldReturnCourtroomWithWhitespace() {
        // when
        Optional<CourtroomEntity> foundCourtroom = courtroomRepository.findByCourthouseNameAndCourtroomName(" Test Courthouse ", " Test Courtroom ");

        // then
        assertTrue(foundCourtroom.isPresent());
        assertEquals(TEST_COURTROOM, foundCourtroom.get().getName());
        assertEquals(COURTHOUSE_NAME, foundCourtroom.get().getCourthouse().getCourthouseName());
    }

    @Test
    void findByCourthouseNameAndCourtroomName_ShouldNotReturnCourtroom_WithInvalidCourthouseName() {
        // when
        Optional<CourtroomEntity> foundCourtroom = courtroomRepository.findByCourthouseNameAndCourtroomName("Test Courthouse2", TEST_COURTROOM);

        // then
        assertTrue(foundCourtroom.isEmpty());
    }

    @Test
    void findByCourthouseNameAndCourtroomName_ShouldNotReturnCourtroom_WithInvalidCourtroom() {
        // when
        Optional<CourtroomEntity> foundCourtroom = courtroomRepository.findByCourthouseNameAndCourtroomName(TEST_COURTHOUSE, "Test Courtroom2");

        // then
        assertTrue(foundCourtroom.isEmpty());
    }

    @Test
    void findByNameAndId_ShouldReturnCourtroom() {
        // when
        Optional<CourtroomEntity> foundCourtroom = courtroomRepository.findByNameAndId(courthouse.getId(), TEST_COURTROOM);

        // then
        assertTrue(foundCourtroom.isPresent());
        assertEquals(COURTROOM_NAME, foundCourtroom.get().getName());
        assertEquals(COURTHOUSE_NAME, foundCourtroom.get().getCourthouse().getCourthouseName());
        assertEquals(courtroom.getId(), foundCourtroom.get().getId());
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
