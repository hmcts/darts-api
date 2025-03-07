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
    private EventEntity eventEntity2;
    private EventEntity eventEntity3;
    private EventEntity eventEntity4;
    private EventEntity eventEntity5;
    private HearingEntity hearingEntity;

    @BeforeEach
    void setUp() {
        hearingEntity = PersistableFactory.getHearingTestData().someMinimal();
        dartsPersistence.saveAll(hearingEntity);

        eventEntityWithLogEntryTrue = dartsDatabase.createEvent(hearingEntity, 54);
        eventEntityWithLogEntryTrue.setLogEntry(true);
        eventEntity2 = dartsDatabase.createEvent(hearingEntity, 54);
        eventEntity3 = dartsDatabase.createEvent(hearingEntity, 32);
        eventEntity4 = dartsDatabase.createEvent(hearingEntity, 68);
        eventEntity5 = dartsDatabase.createEvent(hearingEntity, 188);

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

    @Test
    void findByCourthouseAndCourtroomBetweenStartAndEnd_shouldFindEventEntities() {
        // when
        List<EventEntity> resultEvents = courtLogEventRepository.findByCourthouseAndCourtroomBetweenStartAndEnd(
            hearingEntity.getCourtroom().getCourthouse().getCourthouseName(),
            hearingEntity.getCourtroom().getName(),
            SOME_DATE_TIME.minusHours(1),
            SOME_DATE_TIME.plusHours(1)
        );

        // then
        assertFalse(resultEvents.isEmpty());
        assertEquals(resultEvents.size(), 5);
        List.of(eventEntityWithLogEntryTrue, eventEntity2, eventEntity3, eventEntity4, eventEntity5).forEach(event -> {
            event.equals(resultEvents.stream().filter(
                resultEvent -> resultEvent.getId().equals(event.getId())).findFirst().get()
            );
        });
    }
}
