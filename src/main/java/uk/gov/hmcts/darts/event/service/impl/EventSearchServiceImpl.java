package uk.gov.hmcts.darts.event.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.event.mapper.EventSearchMapper;
import uk.gov.hmcts.darts.event.model.AdminEventSearch;
import uk.gov.hmcts.darts.event.model.AdminSearchEventResponseResult;
import uk.gov.hmcts.darts.event.model.EventSearchResult;
import uk.gov.hmcts.darts.event.service.EventSearchService;

import java.util.List;

import static uk.gov.hmcts.darts.event.exception.EventError.TOO_MANY_SEARCH_RESULTS;

@Service
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings({"squid:S1168"})
public class EventSearchServiceImpl implements EventSearchService {

    private final EventRepository eventRepository;
    private final EventSearchMapper eventSearchMapper;

    @Value("${darts.events.admin-search.max-results}")
    private Integer maxResults;

    @Override
    public List<AdminSearchEventResponseResult> searchForEvents(AdminEventSearch adminEventSearch) {
        Page<EventSearchResult> eventSearchResults = eventRepository.searchEventsFilteringOn(
            getNonEmptyOrNull(adminEventSearch.getCourthouseIds()),
            adminEventSearch.getCaseNumber(),
            adminEventSearch.getCourtroomName(),
            adminEventSearch.getHearingStartAt(),
            adminEventSearch.getHearingEndAt(),
            PageRequest.of(0, maxResults)
        );

        if (eventSearchResults.hasNext()) {
            throw new DartsApiException(
                TOO_MANY_SEARCH_RESULTS,
                "Number of results exceeded " + maxResults + " please narrow your search."
            );
        }

        return eventSearchResults.stream()
            .map(eventSearchMapper::adminSearchEventResponseResultFrom)
            .toList();
    }

    private static List<Integer> getNonEmptyOrNull(List<Integer> integerList) {
        if (integerList != null && integerList.isEmpty()) {
           return null;
        }
        return integerList;
    }
}