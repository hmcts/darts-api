package uk.gov.hmcts.darts.cases.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.cases.mapper.CasesMapper;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Comparator.naturalOrder;
import static java.util.stream.IntStream.rangeClosed;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.darts.test.common.data.EventTestData.REPORTING_RESTRICTIONS_LIFTED_DB_ID;
import static uk.gov.hmcts.darts.test.common.data.EventTestData.SECTION_11_1981_DB_ID;
import static uk.gov.hmcts.darts.test.common.data.EventTestData.SECTION_4_1981_DB_ID;
import static uk.gov.hmcts.darts.test.common.data.EventTestData.someReportingRestrictionId;

@SuppressWarnings("VariableDeclarationUsageDistance")
class CaseMapperTest extends IntegrationBase {

    @Autowired
    private CasesMapper casesMapper;

    @Test
    void mapsSingleCaseCorrectlyWhenZeroReportingRestrictionsAssociatedWithCase() {
        var minimalHearing = PersistableFactory.getHearingTestData().someMinimalHearing();
        var hearingEntity = dartsDatabase.save(minimalHearing);

        var singleCase = casesMapper.mapToSingleCase(hearingEntity.getCourtCase());

        assertThat(singleCase.getReportingRestrictions()).size().isEqualTo(0);
    }

    @Test
    void mapsOneReportingRestrictionsCorrectly() {
        List<OffsetDateTime> eventDateTimes = new ArrayList<>();
        eventDateTimes.add(OffsetDateTime.parse("2023-06-26T13:00:00Z"));
        var reportingRestrictions = createEventsWithDifferentTimestamps(eventDateTimes).stream()
            .map(eve -> dartsDatabase.addHandlerToEvent(eve, someReportingRestrictionId()))
            .toList();
        var minimalHearing = PersistableFactory.getHearingTestData().someMinimalHearing();
        dartsDatabase.saveEventsForHearing(minimalHearing, reportingRestrictions);

        var singleCase = casesMapper.mapToSingleCase(minimalHearing.getCourtCase());

        var mappedRestrictions = singleCase.getReportingRestrictions();
        assertThat(mappedRestrictions).extracting("eventName").hasSameElementsAs(eventNamesFrom(reportingRestrictions));
        assertThat(mappedRestrictions).extracting("eventText").hasSameElementsAs(eventTextFrom(reportingRestrictions));
        assertThat(mappedRestrictions).extracting("hearingId").hasSameElementsAs(hearingIdsFrom(reportingRestrictions));
        assertThat(mappedRestrictions).extracting("eventId").hasSameElementsAs(eventIdsFrom(reportingRestrictions));
        assertThat(mappedRestrictions).extracting((rr) -> rr.getEventTs().truncatedTo(MILLIS)).hasSameElementsAs(eventTsFrom(reportingRestrictions));
    }

    @Test
    void mapsCaseCourtroomIdCorrectly() {
        List<OffsetDateTime> eventDateTimes = new ArrayList<>();
        eventDateTimes.add(OffsetDateTime.parse("2023-06-26T13:00:00Z"));
        var reportingRestrictions = createEventsWithDifferentTimestamps(eventDateTimes).stream()
            .map(eve -> dartsDatabase.addHandlerToEvent(eve, someReportingRestrictionId()))
            .toList();
        var minimalHearing = PersistableFactory.getHearingTestData().someMinimalHearing();
        dartsDatabase.saveEventsForHearing(minimalHearing, reportingRestrictions);

        var singleCase = casesMapper.mapToSingleCase(minimalHearing.getCourtCase());

        assertThat(singleCase.getCourthouseId()).isGreaterThan(0);
    }


    @Test
    void mapsMultipleReportingRestrictionsValuesCorrectly() {

        List<OffsetDateTime> eventDateTimes = new ArrayList<>();
        eventDateTimes.add(OffsetDateTime.parse("2023-06-26T13:00:00Z"));
        eventDateTimes.add(OffsetDateTime.parse("2023-06-26T13:45:00Z"));
        eventDateTimes.add(OffsetDateTime.parse("2023-06-26T14:45:12Z"));

        var reportingRestrictions = createEventsWithDifferentTimestamps(eventDateTimes).stream()
            .map(eve -> dartsDatabase.addHandlerToEvent(eve, someReportingRestrictionId()))
            .toList();
        var minimalHearing = PersistableFactory.getHearingTestData().someMinimalHearing();
        dartsDatabase.saveEventsForHearing(minimalHearing, reportingRestrictions);

        var singleCase = casesMapper.mapToSingleCase(minimalHearing.getCourtCase());

        var mappedRestrictions = singleCase.getReportingRestrictions();
        assertThat(mappedRestrictions).extracting("eventName").hasSameElementsAs(eventNamesFrom(reportingRestrictions));
        assertThat(mappedRestrictions).extracting("eventText").hasSameElementsAs(eventTextFrom(reportingRestrictions));
        assertThat(mappedRestrictions).extracting("hearingId").hasSameElementsAs(hearingIdsFrom(reportingRestrictions));
        assertThat(mappedRestrictions).extracting("eventId").hasSameElementsAs(eventIdsFrom(reportingRestrictions));
        assertThat(mappedRestrictions).extracting((rr) -> rr.getEventTs().truncatedTo(MILLIS)).hasSameElementsAs(eventTsFrom(reportingRestrictions));
    }

    @Test
    void ordersMultipleReportingRestrictionsElementCorrectly() {
        List<OffsetDateTime> eventDateTimes = new ArrayList<>();
        eventDateTimes.add(OffsetDateTime.parse("2023-06-26T13:00:00Z"));
        eventDateTimes.add(OffsetDateTime.parse("2023-06-26T13:45:00Z"));
        eventDateTimes.add(OffsetDateTime.parse("2023-06-26T14:45:12Z"));
        eventDateTimes.add(OffsetDateTime.parse("2023-06-26T13:10:00Z"));
        eventDateTimes.add(OffsetDateTime.parse("2023-06-26T13:15:00Z"));
        eventDateTimes.add(OffsetDateTime.parse("2023-06-26T14:25:12Z"));
        eventDateTimes.add(OffsetDateTime.parse("2023-06-26T12:45:12Z"));
        eventDateTimes.add(OffsetDateTime.parse("2023-06-26T12:10:00Z"));
        eventDateTimes.add(OffsetDateTime.parse("2023-06-26T12:15:00Z"));
        eventDateTimes.add(OffsetDateTime.parse("2023-06-26T12:25:12Z"));
        var reportingRestrictions = createEventsWithDifferentTimestamps(eventDateTimes).stream()
            .map(eve -> dartsDatabase.addHandlerToEvent(eve, someReportingRestrictionId()))
            .toList();
        var expectedOrderedTs = orderedTsFrom(reportingRestrictions);
        var minimalHearing = PersistableFactory.getHearingTestData().someMinimalHearing();
        dartsDatabase.saveEventsForHearing(minimalHearing, reportingRestrictions);

        var singleCase = casesMapper.mapToSingleCase(minimalHearing.getCourtCase());

        rangeClosed(0, 9).forEach(index -> {
            var mappedTsAtIndex = singleCase.getReportingRestrictions().get(index).getEventTs().truncatedTo(SECONDS);
            assertThat(mappedTsAtIndex).isEqualTo(expectedOrderedTs.get(index).truncatedTo(SECONDS));
        });
    }

    @Test
    void includesReportingRestrictionsLifted() {

        var event1 = dartsDatabase.getEventStub().createDefaultEvent();
        event1.setTimestamp(OffsetDateTime.of(2020, 10, 1, 10, 0, 0, 0, ZoneOffset.UTC));
        dartsDatabase.save(event1);
        var reportingRestriction = dartsDatabase.addHandlerToEvent(event1, someReportingRestrictionId());

        var event2 = dartsDatabase.getEventStub().createDefaultEvent();
        event2.setTimestamp(OffsetDateTime.of(2020, 11, 1, 10, 0, 0, 0, ZoneOffset.UTC));
        dartsDatabase.save(event2);
        var reportingRestrictionLifted = dartsDatabase.addHandlerToEvent(event2, REPORTING_RESTRICTIONS_LIFTED_DB_ID);

        var minimalHearing = PersistableFactory.getHearingTestData().someMinimalHearing();
        dartsDatabase.saveEventsForHearing(minimalHearing, reportingRestriction, reportingRestrictionLifted);

        var singleCase = casesMapper.mapToSingleCase(minimalHearing.getCourtCase());

        assertThat(singleCase.getReportingRestrictions()).hasSize(2);
        assertThat(singleCase.getReportingRestrictions()).extracting("eventName").contains("Restrictions lifted");
    }

    @Test
    void includesReportingRestrictionsLiftedWhenReapplied() {
        var event1 = dartsDatabase.getEventStub().createDefaultEvent();
        event1.setTimestamp(now().minusDays(2));
        var reportingRestriction = dartsDatabase.addHandlerToEvent(event1, SECTION_4_1981_DB_ID);

        var event2 = dartsDatabase.getEventStub().createDefaultEvent();
        event2.setTimestamp(now().minusDays(1));
        var reportingRestrictionLifted = dartsDatabase.addHandlerToEvent(event2, REPORTING_RESTRICTIONS_LIFTED_DB_ID);

        var event3 = dartsDatabase.getEventStub().createDefaultEvent();
        event3.setTimestamp(now());
        var reappliedReportingRestriction = dartsDatabase.addHandlerToEvent(event3, SECTION_11_1981_DB_ID);

        var minimalHearing = PersistableFactory.getHearingTestData().someMinimalHearing();
        dartsDatabase.saveEventsForHearing(minimalHearing, reportingRestriction, reportingRestrictionLifted, reappliedReportingRestriction);

        var singleCase = casesMapper.mapToSingleCase(minimalHearing.getCourtCase());

        assertThat(singleCase.getReportingRestrictions()).hasSize(3);
        assertThat(singleCase.getReportingRestrictions()).extracting("eventName")
            .contains("Section 4(2) of the Contempt of Court Act 1981", "Section 11 of the Contempt of Court Act 1981");
    }

    @Test
    void includesMigratedCaseWithRestrictionPersistedOnCaseTable() {
        var caseWithReportingRestrictions =
            dartsDatabase.addHandlerToCase(PersistableFactory.getCourtCaseTestData().createSomeMinimalCase(), someReportingRestrictionId());

        var singleCase = casesMapper.mapToSingleCase(caseWithReportingRestrictions);

        assertThat(singleCase.getReportingRestrictions()).hasSize(1);
        assertThat(singleCase.getReportingRestrictions()).extracting("eventName")
            .hasSameElementsAs(
                List.of(caseWithReportingRestrictions.getReportingRestrictions().getEventName()));
    }

    private List<OffsetDateTime> orderedTsFrom(List<EventEntity> reportingRestrictions) {
        return reportingRestrictions.stream()
            .map(EventEntity::getTimestamp).sorted(naturalOrder())
            .toList();
    }

    private List<EventEntity> createEventsWithDifferentTimestamps(List<OffsetDateTime> eventDateTimes) {
        return rangeClosed(1, eventDateTimes.size())
            .mapToObj(index -> {
                var event = dartsDatabase.getEventStub().createDefaultEvent();
                event.setEventText("some-event-text-" + index);
                event.setMessageId("some-message-id-" + index);
                event.setTimestamp(eventDateTimes.get(index - 1));
                return event;
            }).toList();
    }

    private List<Integer> hearingIdsFrom(List<EventEntity> reportingRestrictions) {
        return reportingRestrictions.stream()
            .flatMap(eventEntity -> eventEntity.getHearingEntities().stream())
            .map(HearingEntity::getId)
            .toList();
    }

    private List<OffsetDateTime> eventTsFrom(List<EventEntity> reportingRestrictions) {
        return reportingRestrictions.stream()
            .map(eventEntity -> eventEntity.getTimestamp().truncatedTo(MILLIS))
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

}