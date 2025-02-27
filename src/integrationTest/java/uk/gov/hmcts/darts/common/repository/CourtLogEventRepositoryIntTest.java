package uk.gov.hmcts.darts.common.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.util.DateConverterUtil;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CourtLogEventRepositoryIntTest extends IntegrationBase {

    @Autowired
    private CourtLogEventRepository courtLogEventRepository;

    private static final OffsetDateTime SOME_DATE_TIME = OffsetDateTime.parse("2023-01-01T12:00Z");
    private static final String SOME_COURTHOUSE = "SOME-COURTHOUSE";
    private static final String SOME_COURTROOM = "some-courtroom";
    private static final String SOME_CASE_ID = "1";

    private EventEntity eventEntity1;
    private EventEntity eventEntity2;
    private EventEntity eventEntity3;
    private EventEntity eventEntity4;
    private EventEntity eventEntity5;

    @BeforeEach
    void setUp() {
        HearingEntity hearingEntity = dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            SOME_CASE_ID,
            SOME_COURTHOUSE,
            SOME_COURTROOM,
            DateConverterUtil.toLocalDateTime(SOME_DATE_TIME)
        );
        
        eventEntity1 = dartsDatabase.createEvent(hearingEntity, 54);
        eventEntity2 = dartsDatabase.createEvent(hearingEntity, 54);
        eventEntity3 = dartsDatabase.createEvent(hearingEntity, 32);
        eventEntity4 = dartsDatabase.createEvent(hearingEntity, 68);
        eventEntity5 = dartsDatabase.createEvent(hearingEntity, 188);
    }

    @Test
    void findByCourthouseAndCaseNumberBetweenStartAndEnd_shouldFindEventEntities() {
        // when
        List<EventEntity> events = courtLogEventRepository.findByCourthouseAndCaseNumberBetweenStartAndEnd(
            SOME_COURTHOUSE,
            SOME_CASE_ID,
            SOME_DATE_TIME.minusHours(1),
            SOME_DATE_TIME.plusHours(1)
        );

        // then
        assertFalse(events.isEmpty());
        assertEquals(5, events.size());
    }

    @Test
    void findByCourthouseAndCourtroomBetweenStartAndEnd_shouldFindEventEntities() {
        // when
        List<EventEntity> page = courtLogEventRepository.findByCourthouseAndCourtroomBetweenStartAndEnd(
            SOME_COURTHOUSE,
            SOME_COURTROOM,
            SOME_DATE_TIME.minusHours(1),
            SOME_DATE_TIME.plusHours(1)
        );

        // then
        assertFalse(page.isEmpty());
        assertEquals(page.size(), 5);
    }
}
