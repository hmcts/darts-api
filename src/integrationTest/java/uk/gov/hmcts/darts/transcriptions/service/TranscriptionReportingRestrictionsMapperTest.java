package uk.gov.hmcts.darts.transcriptions.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;

import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Comparator.naturalOrder;
import static java.util.stream.IntStream.rangeClosed;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.darts.test.common.data.EventTestData.REPORTING_RESTRICTIONS_LIFTED_DB_ID;
import static uk.gov.hmcts.darts.test.common.data.EventTestData.SECTION_11_1981_DB_ID;
import static uk.gov.hmcts.darts.test.common.data.EventTestData.someReportingRestrictionId;

@SuppressWarnings("VariableDeclarationUsageDistance")
class TranscriptionReportingRestrictionsMapperTest extends IntegrationBase {

    @Autowired
    private TranscriptionService transcriptionService;

    @Test
    void mapsTranscriptionsCorrectlyWhenZeroReportingRestrictionsAssociatedWithCase() {
        var transcriptionEntity = dartsDatabase.getTranscriptionStub().createMinimalTranscription();

        var transcriptionResponse = transcriptionService.getTranscription(transcriptionEntity.getId());

        assertThat(transcriptionResponse.getCaseReportingRestrictions()).size().isEqualTo(0);
    }

    @Test
    void mapsOneReportingRestrictionsCorrectly() {
        var reportingRestrictions = dartsDatabase.getTransactionalUtil()
            .executeInTransaction(() -> createEventsWithDefaults(1).stream()
                .map(eve -> dartsDatabase.addHandlerToEvent(eve, someReportingRestrictionId()))
                .peek(eventEntity -> eventEntity.getEventType().getEventName())//Load the event type
                .toList());

        var transcriptionEntity = transactionalUtil.executeInTransaction(() -> {
            var hearingEntity = dartsDatabase.saveEventsForHearing(dartsDatabase.getHearingStub().createMinimalHearing(), reportingRestrictions);
            return dartsDatabase.getTranscriptionStub().createTranscription(hearingEntity);
        });
        var transcriptionResponse = transcriptionService.getTranscription(transcriptionEntity.getId());

        var mappedRestrictions = transcriptionResponse.getCaseReportingRestrictions();
        assertThat(mappedRestrictions).extracting("eventName").hasSameElementsAs(eventNamesFrom(reportingRestrictions));
        assertThat(mappedRestrictions).extracting("eventText").hasSameElementsAs(eventTextFrom(reportingRestrictions));
        assertThat(mappedRestrictions).extracting("hearingId").hasSameElementsAs(hearingIdsFrom(reportingRestrictions));
        assertThat(mappedRestrictions).extracting("eventId").hasSameElementsAs(eventIdsFrom(reportingRestrictions));
        assertThat(mappedRestrictions).extracting((rr) -> rr.getEventTs().truncatedTo(SECONDS)).hasSameElementsAs(eventTsFrom(reportingRestrictions));
    }

    @Test
    void mapsMultipleReportingRestrictionsValuesCorrectly() {
        var reportingRestrictions = dartsDatabase.getTransactionalUtil()
            .executeInTransaction(() -> createEventsWithDefaults(3).stream()
                .map(eve -> dartsDatabase.addHandlerToEvent(eve, someReportingRestrictionId()))
                .peek(eventEntity -> eventEntity.getEventType().getEventName())//Load the event type
                .toList());

        var transcriptionEntity = transactionalUtil.executeInTransaction(() -> {
            var hearingEntity = dartsDatabase.saveEventsForHearing(dartsDatabase.getHearingStub().createMinimalHearing(), reportingRestrictions);
            return dartsDatabase.getTranscriptionStub().createTranscription(hearingEntity);
        });
        var transcriptionResponse = transcriptionService.getTranscription(transcriptionEntity.getId());

        var mappedRestrictions = transcriptionResponse.getCaseReportingRestrictions();
        assertThat(mappedRestrictions).extracting("eventName").hasSameElementsAs(eventNamesFrom(reportingRestrictions));
        assertThat(mappedRestrictions).extracting("eventText").hasSameElementsAs(eventTextFrom(reportingRestrictions));
        assertThat(mappedRestrictions).extracting("hearingId").hasSameElementsAs(hearingIdsFrom(reportingRestrictions));
        assertThat(mappedRestrictions).extracting("eventId").hasSameElementsAs(eventIdsFrom(reportingRestrictions));
        assertThat(mappedRestrictions).extracting((rr) -> rr.getEventTs().truncatedTo(SECONDS)).hasSameElementsAs(eventTsFrom(reportingRestrictions));
    }

    @Test
    void ordersMultipleReportingRestrictionsElementCorrectly() {
        var reportingRestrictions = createEventsWithDifferentTimestamps(10).stream()
            .map(eve -> dartsDatabase.addHandlerToEvent(eve, someReportingRestrictionId()))
            .toList();
        var expectedOrderedTs = orderedTsFrom(reportingRestrictions);

        var transcriptionEntity = transactionalUtil.executeInTransaction(() -> {
            var hearingEntity = dartsDatabase.saveEventsForHearing(dartsDatabase.getHearingStub().createMinimalHearing(), reportingRestrictions);
            return dartsDatabase.getTranscriptionStub().createTranscription(hearingEntity);
        });
        var transcriptionResponse = transcriptionService.getTranscription(transcriptionEntity.getId());

        rangeClosed(0, 9).forEach(index -> {
            var mappedTsAtIndex = transcriptionResponse.getCaseReportingRestrictions().get(index).getEventTs().truncatedTo(SECONDS);
            assertThat(mappedTsAtIndex).isEqualTo(expectedOrderedTs.get(index).truncatedTo(SECONDS));
        });
    }

    @Test
    void includesReportingRestrictionsLifted() {
        TranscriptionEntity transcription = transactionalUtil.executeInTransaction(() -> {
            var event1 = dartsDatabase.getEventStub().createDefaultEvent();
            event1.setTimestamp(now().minusDays(1));
            var reportingRestriction = dartsDatabase.addHandlerToEvent(event1, someReportingRestrictionId());

            var event2 = dartsDatabase.getEventStub().createDefaultEvent();
            event2.setTimestamp(now());
            var reportingRestrictionLifted = dartsDatabase.addHandlerToEvent(event2, REPORTING_RESTRICTIONS_LIFTED_DB_ID);

            var hearingEntity = dartsDatabase.saveEventsForHearing(
                dartsDatabase.getHearingStub().createMinimalHearing(),
                reportingRestriction,
                reportingRestrictionLifted
            );
            return dartsDatabase.getTranscriptionStub().createTranscription(hearingEntity);
        });
        var transcriptionResponse = transcriptionService.getTranscription(transcription.getId());

        assertThat(transcriptionResponse.getCaseReportingRestrictions()).hasSize(2);
        assertThat(transcriptionResponse.getCaseReportingRestrictions()).extracting("eventName").contains("Restrictions lifted");
    }

    @Test
    void includesReportingRestrictionsLiftedWhenReapplied() {
        var event1 = dartsDatabase.getEventStub().createDefaultEvent();
        event1.setTimestamp(now().minusDays(2));
        var reportingRestriction = dartsDatabase.addHandlerToEvent(event1, someReportingRestrictionId());

        var event2 = dartsDatabase.getEventStub().createDefaultEvent();
        event2.setTimestamp(now().minusDays(1));
        var reportingRestrictionLifted = dartsDatabase.addHandlerToEvent(event2, REPORTING_RESTRICTIONS_LIFTED_DB_ID);

        var event3 = dartsDatabase.getEventStub().createDefaultEvent();
        event3.setTimestamp(now());
        var reappliedReportingRestriction = dartsDatabase.addHandlerToEvent(event3, SECTION_11_1981_DB_ID);

        HearingEntity hearingEntity = dartsDatabase.saveEventsForHearing(
            PersistableFactory.getHearingTestData().someMinimalHearing(),
            reportingRestriction,
            reportingRestrictionLifted,
            reappliedReportingRestriction
        );

        var transcriptionEntity = dartsDatabase.getTranscriptionStub().createTranscription(hearingEntity);

        var transcriptionResponse = transcriptionService.getTranscription(transcriptionEntity.getId());

        assertThat(transcriptionResponse.getCaseReportingRestrictions()).hasSize(3);
        assertThat(transcriptionResponse.getCaseReportingRestrictions()).extracting("eventName")
            .contains("Restrictions lifted", "Section 11 of the Contempt of Court Act 1981");
    }

    @Test
    void includesMigratedCaseWithRestrictionPersistedOnCaseTable() {
        var caseWithReportingRestrictions = dartsDatabase
            .addHandlerToCase(PersistableFactory.getCourtCaseTestData().createSomeMinimalCase(), someReportingRestrictionId());
        var transcriptionEntity = dartsDatabase
            .getTranscriptionStub().createTranscription(caseWithReportingRestrictions);

        var transcriptionResponse = transcriptionService.getTranscription(transcriptionEntity.getId());

        assertThat(transcriptionResponse.getCaseReportingRestrictions()).hasSize(1);
        assertThat(transcriptionResponse.getCaseReportingRestrictions()).extracting("eventName")
            .hasSameElementsAs(
                List.of(caseWithReportingRestrictions.getReportingRestrictions().getEventName()));
    }


    private List<OffsetDateTime> orderedTsFrom(List<EventEntity> reportingRestrictions) {
        return reportingRestrictions.stream()
            .map(EventEntity::getTimestamp).sorted(naturalOrder())
            .toList();
    }

    private List<EventEntity> createEventsWithDifferentTimestamps(int quantity) {
        var random = new Random();
        return createEventsWithDefaults(quantity).stream()
            .peek(event -> event.setTimestamp(now().plusDays(random.nextInt(1, 1000))))
            .toList();
    }

    private List<Integer> hearingIdsFrom(List<EventEntity> reportingRestrictions) {
        return reportingRestrictions.stream()
            .flatMap(eventEntity -> eventEntity.getHearingEntities().stream())
            .map(HearingEntity::getId)
            .toList();
    }

    private List<OffsetDateTime> eventTsFrom(List<EventEntity> reportingRestrictions) {
        return reportingRestrictions.stream()
            .map(eventEntity -> eventEntity.getTimestamp().truncatedTo(SECONDS))
            .toList();
    }

    private List<Long> eventIdsFrom(List<EventEntity> reportingRestrictions) {
        return reportingRestrictions.stream()
            .map(EventEntity::getId)
            .toList();
    }

    private List<String> eventTextFrom(List<EventEntity> reportingRestrictions) {
        return reportingRestrictions.stream()
            .map(EventEntity::getEventText)
            .toList();
    }

    private List<String> eventNamesFrom(List<EventEntity> reportingRestrictions) {
        return reportingRestrictions.stream()
            .map((e) -> e.getEventType().getEventName())
            .toList();
    }

    private List<EventEntity> createEventsWithDefaults(int quantity) {
        return rangeClosed(1, quantity)
            .mapToObj(index -> {
                var event = dartsDatabase.getEventStub().createDefaultEvent();
                event.setEventText("some-event-text-" + index);
                event.setMessageId("some-message-id-" + index);
                event.setTimestamp(now());
                return event;
            }).toList();
    }

}