package uk.gov.hmcts.darts.event.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.event.model.AdminEventSearch;
import uk.gov.hmcts.darts.event.service.EventSearchService;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.IntegrationBaseWithWiremock;

import java.util.ArrayList;
import java.util.List;

import static java.time.LocalDate.parse;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.darts.test.common.data.CourtroomTestData.someMinimalCourtRoom;

@TestPropertySource(properties = {"darts.events.admin-search.max-results=5"})
class AdminEventSearchTest extends IntegrationBaseWithWiremock {

    @Autowired
    private AdminEventsSearchGivensBuilder given;

    @Autowired
    private EventSearchService eventSearchService;

    @BeforeEach
    void setUp() {
        openInViewUtil.openEntityManager();
    }

    @AfterEach
    void tearDown() {
        openInViewUtil.closeEntityManager();
    }

    @Test
    void findsEventsByCourtHouseIdOnly() {
        var persistedEvents = given.persistedEvents(3);

        var mutablePersistedEvents = new ArrayList<>(persistedEvents);
        mutablePersistedEvents.sort((event1, event2) -> event1.getCourtroom().getCourthouse().getDisplayName().compareTo(
            event2.getCourtroom().getCourthouse().getDisplayName()));

        var eventSearchResults = eventSearchService.searchForEvents(
            new AdminEventSearch()
                .courthouseIds(courthouseIdsAssociatedWithEvents(persistedEvents)));

        assertThat(eventSearchResults)
            .extracting(event -> event.getCourthouse().getDisplayName())
            .isEqualTo(mutablePersistedEvents.stream()
                           .map(event -> event.getCourtroom().getCourthouse().getDisplayName())
                           .toList());
    }

    @Test
    void findsEventsByCaseNumberOnly() {
        var hearing = PersistableFactory.getHearingTestData().someMinimalHearing();
        var persistedEventsForHearing = given.persistedEventsForHearing(3, hearing, true);
        given.persistedEvents(3);  // Persist some other events for the other hearings

        var mutablePersistedEvents = new ArrayList<>(persistedEventsForHearing);
        mutablePersistedEvents.sort((event1, event2) -> event1.getCourtroom().getCourthouse().getDisplayName().compareTo(
            event2.getCourtroom().getCourthouse().getDisplayName()));

        var eventSearchResults = eventSearchService.searchForEvents(
            new AdminEventSearch()
                .caseNumber(hearing.getCourtCase().getCaseNumber()));

        assertThat(eventSearchResults)
            .extracting(event -> event.getCourthouse().getDisplayName())
            .isEqualTo(mutablePersistedEvents.stream()
                           .map(event -> event.getCourtroom().getCourthouse().getDisplayName())
                           .toList());
    }

    @Test
    void findsEventsByCourtroomName() {
        var courtRoom = someMinimalCourtRoom();
        var persistedEventsForCourtroom = given.persistedEventsForCourtroom(3, courtRoom);
        given.persistedEvents(3);  // Persist some other events for the other courtrooms

        var mutablePersistedEvents = new ArrayList<>(persistedEventsForCourtroom);
        mutablePersistedEvents.sort((event1, event2) -> event1.getCourtroom().getCourthouse().getDisplayName().compareTo(
            event2.getCourtroom().getCourthouse().getDisplayName()));

        var eventSearchResults = eventSearchService.searchForEvents(
            new AdminEventSearch()
                .courtroomName(courtRoom.getName()));

        assertThat(eventSearchResults)
            .extracting(event -> event.getCourthouse().getDisplayName())
            .isEqualTo(mutablePersistedEvents.stream()
                           .map(event -> event.getCourtroom().getCourthouse().getDisplayName())
                           .toList());
    }

    @Test
    void findsEventByEarliestHearingDate() {
        var persistedEvents = given.persistedEventsWithHearingDates(
            3,
            parse("2020-01-01"),
            parse("2020-02-01"),
            parse("2020-03-01"));

        var eventSearchResults = eventSearchService.searchForEvents(
            new AdminEventSearch()
                .hearingStartAt(parse("2020-02-28")));

        assertThat(eventSearchResults)
            .extracting("id")
            .containsExactly(persistedEvents.get(2).getId());
    }

    @Test
    void findsEventByLatestHearingDate() {
        var persistedEvents = given.persistedEventsWithHearingDates(
            3,
            parse("2020-01-01"),
            parse("2020-02-01"),
            parse("2020-03-01"));

        var eventSearchResults = eventSearchService.searchForEvents(
            new AdminEventSearch()
                .hearingEndAt(parse("2020-01-28")));

        assertThat(eventSearchResults)
            .extracting("id")
            .containsExactly(persistedEvents.getFirst().getId());
    }

    @Test
    void treatsEmptyCourthouseIdListAsNull() {
        var persistedEvents = given.persistedEvents(3);

        var mutablePersistedEvents = new ArrayList<>(persistedEvents);
        mutablePersistedEvents.sort((event1, event2) -> event1.getCourtroom().getCourthouse().getDisplayName().compareTo(
            event2.getCourtroom().getCourthouse().getDisplayName()));

        var eventSearchResults = eventSearchService.searchForEvents(
            new AdminEventSearch()
                .courthouseIds(List.of()));

        assertThat(eventSearchResults)
            .extracting(event -> event.getCourthouse().getDisplayName())
            .isEqualTo(mutablePersistedEvents.stream()
                           .map(event -> event.getCourtroom().getCourthouse().getDisplayName())
                           .toList());
    }

    @Test
    void searchForEvent_shouldNotFindEvent_whenIsCurrentFalse() {
        var hearing = PersistableFactory.getHearingTestData().someMinimalHearing();
        given.persistedEventsForHearing(3, hearing, false);
        given.persistedEvents(3);  // Persist some other events for the other hearings

        var eventSearchResults = eventSearchService.searchForEvents(
            new AdminEventSearch()
                .caseNumber(hearing.getCourtCase().getCaseNumber()));

        assertThat(eventSearchResults).isEmpty();
    }

    private List<Integer> courthouseIdsAssociatedWithEvents(List<EventEntity> events) {
        return events.stream()
            .map(eve -> eve.getHearingEntity().getCourtroom().getCourthouse().getId())
            .toList();
    }
}