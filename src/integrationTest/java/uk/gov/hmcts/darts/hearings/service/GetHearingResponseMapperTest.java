package uk.gov.hmcts.darts.hearings.service;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.cases.mapper.CasesMapper;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.hearings.mapper.GetHearingResponseMapper;
import uk.gov.hmcts.darts.hearings.model.GetHearingResponse;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;

import static java.time.OffsetDateTime.now;
import static java.util.Comparator.naturalOrder;
import static java.util.stream.IntStream.rangeClosed;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.gov.hmcts.darts.testutils.data.EventTestData.createEventWithDefaults;
import static uk.gov.hmcts.darts.testutils.data.EventTestData.someReportingRestrictionId;
import static uk.gov.hmcts.darts.testutils.data.HearingTestData.createSomeMinimalHearing;


class GetHearingResponseMapperTest extends IntegrationBase {

    @Autowired
    GetHearingResponseMapper getHearingResponseMapper;

    @Autowired
    CasesMapper casesMapper;

    @Test
    void getHearingWithNoReportingRestrictions() {
        var minimalHearing = createSomeMinimalHearing();
        var hearingEntity = dartsDatabase.save(minimalHearing);

        GetHearingResponse getHearingResponse = getHearingResponseMapper.map(hearingEntity);
        assertEquals(0, getHearingResponse.getReportingRestrictions().size());
    }

    @Test
    void getHearingWithOneReportingRestrictions() {
        var reportingRestrictions = createEventsWithDefaults(1).stream()
                .map(eve -> dartsDatabase.addHandlerToEvent(eve, someReportingRestrictionId()))
                .toList();
        var minimalHearing = createSomeMinimalHearing();
        dartsDatabase.saveEventsForHearing(minimalHearing, reportingRestrictions);

        GetHearingResponse getHearingResponse = getHearingResponseMapper.map(minimalHearing);
        assertEquals(1, getHearingResponse.getReportingRestrictions().size());
        assertEquals("some-event-name-1", getHearingResponse.getReportingRestrictions().get(0).getEventName());
        assertEquals("some-event-text-1", getHearingResponse.getReportingRestrictions().get(0).getEventText());
        assertEquals(minimalHearing.getId(), getHearingResponse.getReportingRestrictions().get(0).getHearingId());
        assertEquals(reportingRestrictions.get(0).getId(), getHearingResponse.getReportingRestrictions().get(0).getEventId());
    }

    @Test
    void getHearingWithThreeReportingRestrictions() {
        var reportingRestrictions = createEventsWithDefaults(3).stream()
                .map(eve -> dartsDatabase.addHandlerToEvent(eve, someReportingRestrictionId()))
                .toList();
        var minimalHearing = createSomeMinimalHearing();
        dartsDatabase.saveEventsForHearing(minimalHearing, reportingRestrictions);

        GetHearingResponse getHearingResponse = getHearingResponseMapper.map(minimalHearing);
        assertEquals(3, getHearingResponse.getReportingRestrictions().size());
        assertEquals("some-event-name-1", getHearingResponse.getReportingRestrictions().get(0).getEventName());
        assertEquals("some-event-name-2", getHearingResponse.getReportingRestrictions().get(1).getEventName());
        assertEquals("some-event-name-3", getHearingResponse.getReportingRestrictions().get(2).getEventName());
    }

    @Test
    void getHearingWithOrderedReportingRestrictions() {
        var reportingRestrictions = createEventsWithDifferentTimestamps(10).stream()
                .map(eve -> dartsDatabase.addHandlerToEvent(eve, someReportingRestrictionId()))
                .toList();
        List<OffsetDateTime> expectedOrderedTs = orderedTsFrom(reportingRestrictions);
        var minimalHearing = createSomeMinimalHearing();
        dartsDatabase.saveEventsForHearing(minimalHearing, reportingRestrictions);

        GetHearingResponse getHearingResponse = getHearingResponseMapper.map(minimalHearing);

        rangeClosed(0, 9).forEach(index -> {
            var mappedTsAtIndex = getHearingResponse.getReportingRestrictions().get(index).getEventTs().truncatedTo(ChronoUnit.SECONDS);
            assertThat(mappedTsAtIndex).isEqualTo(expectedOrderedTs.get(index).truncatedTo(ChronoUnit.SECONDS));
        });
    }





    private List<EventEntity> createEventsWithDefaults(int quantity) {
        return rangeClosed(1, quantity)
                .mapToObj(index -> {
                    var event = createEventWithDefaults();
                    event.setEventName("some-event-name-" + index);
                    event.setEventText("some-event-text-" + index);
                    event.setMessageId("some-message-id-" + index);
                    event.setTimestamp(now());
                    return event;
                }).toList();
    }

    private List<EventEntity> createEventsWithDifferentTimestamps(int quantity) {
        var random = new Random();
        return createEventsWithDefaults(quantity).stream()
                .peek(event -> event.setTimestamp(now().plusDays(random.nextInt(1, 1000))))
                .toList();
    }

    private List<OffsetDateTime> orderedTsFrom(List<EventEntity> reportingRestrictions) {
        return reportingRestrictions.stream()
                .map(EventEntity::getTimestamp).sorted(naturalOrder())
                .toList();
    }
}
