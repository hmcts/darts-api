package uk.gov.hmcts.darts.hearings.service;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.cases.mapper.CasesMapper;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.hearings.mapper.GetHearingResponseMapper;
import uk.gov.hmcts.darts.hearings.model.GetHearingResponse;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;

import static java.util.Comparator.naturalOrder;
import static java.util.stream.IntStream.rangeClosed;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.hmcts.darts.test.common.data.EventHandlerTestData.someMinimalEventHandler;
import static uk.gov.hmcts.darts.test.common.data.EventTestData.SECTION_11_1981_DB_ID;
import static uk.gov.hmcts.darts.test.common.data.EventTestData.SECTION_39_1933_DB_ID;
import static uk.gov.hmcts.darts.test.common.data.EventTestData.SECTION_4_1981_DB_ID;
import static uk.gov.hmcts.darts.test.common.data.EventTestData.someReportingRestrictionId;

class GetHearingResponseMapperIntTest extends IntegrationBase {

    @Autowired
    GetHearingResponseMapper getHearingResponseMapper;

    @Autowired
    CasesMapper casesMapper;

    @Test
    void getHearingWithNoReportingRestrictions() {
        var minimalHearing = PersistableFactory.getHearingTestData().someMinimalHearing();
        var hearingEntity = dartsDatabase.save(minimalHearing);

        GetHearingResponse getHearingResponse = getHearingResponseMapper.map(hearingEntity);
        assertEquals(0, getHearingResponse.getCaseReportingRestrictions().size());
    }

    @Test
    void getHearingWithOneReportingRestrictions() {
        var reportingRestrictions = createEventsWithDefaults(1).stream()
            .map(eve -> dartsDatabase.addHandlerToEvent(eve, SECTION_11_1981_DB_ID))
            .toList();
        var minimalHearing = PersistableFactory.getHearingTestData().someMinimalHearing();
        dartsDatabase.saveEventsForHearing(minimalHearing, reportingRestrictions);

        GetHearingResponse getHearingResponse = getHearingResponseMapper.map(minimalHearing);
        assertEquals(1, getHearingResponse.getCaseReportingRestrictions().size());
        assertEquals("Section 11 of the Contempt of Court Act 1981", getHearingResponse.getCaseReportingRestrictions().getFirst().getEventName());
        assertEquals("some-event-text-1", getHearingResponse.getCaseReportingRestrictions().getFirst().getEventText());
        assertEquals(minimalHearing.getId(), getHearingResponse.getCaseReportingRestrictions().getFirst().getHearingId());
        assertEquals(reportingRestrictions.getFirst().getId(), getHearingResponse.getCaseReportingRestrictions().getFirst().getEventId());
    }

    @Test
    void getHearingWithThreeReportingRestrictions() {
        var reportingRestrictions = createEventsWithDefaults(3);
        dartsDatabase.addHandlerToEvent(reportingRestrictions.getFirst(), SECTION_4_1981_DB_ID);
        dartsDatabase.addHandlerToEvent(reportingRestrictions.get(1), SECTION_11_1981_DB_ID);
        dartsDatabase.addHandlerToEvent(reportingRestrictions.get(2), SECTION_39_1933_DB_ID);

        var minimalHearing = PersistableFactory.getHearingTestData().someMinimalHearing();
        dartsDatabase.saveEventsForHearing(minimalHearing, reportingRestrictions);

        GetHearingResponse getHearingResponse = getHearingResponseMapper.map(minimalHearing);
        assertEquals(3, getHearingResponse.getCaseReportingRestrictions().size());
        assertEquals("Section 4(2) of the Contempt of Court Act 1981", getHearingResponse.getCaseReportingRestrictions().getFirst().getEventName());
        assertEquals("Section 11 of the Contempt of Court Act 1981", getHearingResponse.getCaseReportingRestrictions().get(1).getEventName());
        assertEquals("Section 39 of the Children and Young Persons Act 1933", getHearingResponse.getCaseReportingRestrictions().get(2).getEventName());
    }

    @Test
    void getHearingWithOrderedReportingRestrictions() {
        var reportingRestrictions = createEventsWithDifferentTimestamps(10).stream()
            .map(eve -> dartsDatabase.addHandlerToEvent(eve, someReportingRestrictionId()))
            .toList();
        List<OffsetDateTime> expectedOrderedTs = orderedTsFrom(reportingRestrictions);
        var minimalHearing = PersistableFactory.getHearingTestData().someMinimalHearing();
        dartsDatabase.saveEventsForHearing(minimalHearing, reportingRestrictions);

        GetHearingResponse getHearingResponse = getHearingResponseMapper.map(minimalHearing);

        rangeClosed(0, 9).forEach(index -> {
            var mappedTsAtIndex = getHearingResponse.getCaseReportingRestrictions().get(index).getEventTs().truncatedTo(ChronoUnit.SECONDS);
            assertThat(mappedTsAtIndex).isEqualTo(expectedOrderedTs.get(index).truncatedTo(ChronoUnit.SECONDS));
        });
    }

    @Test
    void includesMigratedCaseWithRestrictionPersistedOnCaseTable() {
        var hearing = PersistableFactory.getHearingTestData().someMinimalHearing();
        dartsDatabase.addHandlerToCase(hearing.getCourtCase(), someReportingRestrictionId());

        GetHearingResponse getHearingResponse = getHearingResponseMapper.map(hearing);

        assertEquals(1, getHearingResponse.getCaseReportingRestrictions().size());
    }


    private List<EventEntity> createEventsWithDefaults(int quantity) {
        EventHandlerEntity eventHandler = someMinimalEventHandler();
        return rangeClosed(1, quantity)
            .mapToObj(index -> {
                var event = dartsDatabase.getEventStub().createDefaultEvent();
                event.setEventType(eventHandler);
                event.setEventText("some-event-text-" + index);
                event.setMessageId("some-message-id-" + index);
                return event;
            }).toList();
    }

    private List<EventEntity> createEventsWithDifferentTimestamps(int quantity) {
        var random = new Random();
        return createEventsWithDefaults(quantity).stream()
            .peek(event -> event.setTimestamp(OffsetDateTime.of(2020, 4, 10, 10, 0, 0, 0, ZoneOffset.UTC).plusDays(random.nextInt(1, 1000))))
            .toList();
    }

    private List<OffsetDateTime> orderedTsFrom(List<EventEntity> reportingRestrictions) {
        return reportingRestrictions.stream()
            .map(EventEntity::getTimestamp).sorted(naturalOrder())
            .toList();
    }
}