package uk.gov.hmcts.darts.common.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CourtLogEventRepositoryIntTest extends PostgresIntegrationBase {

    @Autowired
    private CourtLogEventRepository courtLogEventRepository;

    private static final OffsetDateTime SOME_DATE_TIME = OffsetDateTime.parse("2020-06-20T10:00Z");

    private EventEntity eventEntityWithLogEntryTrue;
    private HearingEntity hearingEntity;

    @BeforeEach
    void setUp() {
        hearingEntity = PersistableFactory.getHearingTestData().someMinimal();
        dartsPersistence.saveAll(hearingEntity);

        eventEntityWithLogEntryTrue = dartsDatabase.createEvent(hearingEntity, 54);
        eventEntityWithLogEntryTrue.setLogEntry(true);
        EventEntity eventEntity2 = dartsDatabase.createEvent(hearingEntity, 54);
        EventEntity eventEntity3 = dartsDatabase.createEvent(hearingEntity, 32);
        EventEntity eventEntity4 = dartsDatabase.createEvent(hearingEntity, 68);
        EventEntity eventEntity5 = dartsDatabase.createEvent(hearingEntity, 188);
        eventEntity5.setIsCurrent(false);

        dartsDatabase.saveAll(eventEntityWithLogEntryTrue, eventEntity2, eventEntity3, eventEntity4, eventEntity5);
    }

    @Test
    void findByCourthouseAndCaseNumberBetweenStartAndEnd_shouldFindEventEntities() {
        // when
        List<EventEntity> events = courtLogEventRepository.findByCourthouseAndCaseNumberBetweenStartAndEnd(
            hearingEntity.getCourtroom().getCourthouse().getCourthouseName(),
            hearingEntity.getCourtCase().getCaseNumber(),
            SOME_DATE_TIME.minusHours(1),
            SOME_DATE_TIME.plusHours(1)
        );

        // then
        assertFalse(events.isEmpty());
        assertEquals(1, events.size());
        List.of(eventEntityWithLogEntryTrue);
        eventEntityWithLogEntryTrue.equals(events.getFirst());
    }
}
