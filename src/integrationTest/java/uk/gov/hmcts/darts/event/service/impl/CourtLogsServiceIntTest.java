package uk.gov.hmcts.darts.event.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.event.service.CourtLogsService;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.hmcts.darts.test.common.data.CourthouseTestData.someMinimalCourthouse;
import static uk.gov.hmcts.darts.test.common.data.CourtroomTestData.someMinimalCourtRoom;
import static uk.gov.hmcts.darts.test.common.data.DefenceTestData.createListOfDefenceForCase;
import static uk.gov.hmcts.darts.test.common.data.DefendantTestData.createListOfDefendantsForCase;
import static uk.gov.hmcts.darts.test.common.data.JudgeTestData.createListOfJudges;
import static uk.gov.hmcts.darts.test.common.data.ProsecutorTestData.createListOfProsecutor;


class CourtLogsServiceIntTest extends PostgresIntegrationBase {

    private static final String TEST_COURTHOUSE = "Test Courthouse";
    private static final String COURTHOUSE_NAME = "TEST COURTHOUSE";
    private static final String TEST_COURTROOM = "Test Courtroom";
    private static final String CASE_NUMBER = "Case0000001";

    private static final OffsetDateTime START = OffsetDateTime.of(2020, 6, 20, 10, 0, 0, 0, ZoneOffset.UTC);
    private static final OffsetDateTime END = OffsetDateTime.of(2020, 6, 20, 14, 0, 0, 0, ZoneOffset.UTC);

    @Autowired
    private CourtLogsService courtLogsService;

    private EventEntity eventEntityWithLogEntryTrue;
    private EventEntity eventEntityWithLogEntryFalse;

    private HearingEntity hearingEntity;
    private CourthouseEntity courthouse;
    private CourtroomEntity courtroom;

    @BeforeEach
    void beforeEach() {
        courthouse = someMinimalCourthouse();
        courthouse.setCourthouseName(TEST_COURTHOUSE);
        courthouse.setDisplayName(COURTHOUSE_NAME);
        courthouse = dartsPersistence.save(courthouse);

        courtroom = someMinimalCourtRoom();
        courtroom.setName(TEST_COURTROOM);
        courtroom.setCourthouse(courthouse);
        courtroom = dartsPersistence.save(courtroom);

        hearingEntity = setupHearingForCase(courthouse, courtroom);
        dartsPersistence.saveAll(hearingEntity);

        eventEntityWithLogEntryTrue = dartsDatabase.createEvent(hearingEntity, 10);
        eventEntityWithLogEntryTrue.setLogEntry(true);
        eventEntityWithLogEntryFalse = dartsDatabase.createEvent(hearingEntity, 10);
        eventEntityWithLogEntryFalse.setLogEntry(false);

        dartsDatabase.saveAll(eventEntityWithLogEntryTrue, eventEntityWithLogEntryFalse);
    }

    @Transactional
    @Test
    void getCourtLogs_ReturnsCourtLogs() {
        // when
        var courtLogs = courtLogsService.getCourtLogs(COURTHOUSE_NAME, CASE_NUMBER, START, END);

        // then
        assertEquals(1, courtLogs.size());
    }

    @Transactional
    @Test
    void getCourtLogs_ReturnsNoCourtLogs_withEmptyCourthouseName() {
        // when
        var courtLogs = courtLogsService.getCourtLogs("", CASE_NUMBER, START, END);

        // then
        assertEquals(0, courtLogs.size());
    }

    @Transactional
    @Test
    void getCourtLogs_ReturnsNoCourtLogs_withNullCourthouseName() {
        // when
        var courtLogs = courtLogsService.getCourtLogs(null, CASE_NUMBER, START, END);

        // then
        assertEquals(0, courtLogs.size());
    }

    private HearingEntity setupHearingForCase(CourthouseEntity courthouseEntity, CourtroomEntity courtroomEntity) {
        var case1 = PersistableFactory.getCourtCaseTestData().createCaseAt(courthouseEntity);
        case1.setCaseNumber(CASE_NUMBER);
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
