package uk.gov.hmcts.darts.event.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.event.mapper.EventSearchMapper;
import uk.gov.hmcts.darts.event.model.AdminEventSearch;
import uk.gov.hmcts.darts.event.model.AdminSearchEventResponse;
import uk.gov.hmcts.darts.event.service.EventSearchService;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventSearchServiceImpl implements EventSearchService {

    private final EventRepository eventRepository;
    private final EventSearchMapper eventSearchMapper;

    @Value("${darts.events.admin-search.max-results}")
    private Integer maxResults;

    @Override
    public AdminSearchEventResponse searchForEvents(AdminEventSearch adminEventSearch) {
        var eventSearchResults = eventRepository.searchEventsFilteringOn(
            adminEventSearch.getCourthouseIds(),
            adminEventSearch.getCaseNumber(),
            adminEventSearch.getCourtroomName(),
            adminEventSearch.getHearingStartAt(),
            adminEventSearch.getHearingEndAt(),
            PageRequest.of(0, maxResults)
        );

        var adminSearchEventResponseResults = eventSearchResults.stream()
            .map(evr -> eventSearchMapper.adminSearchEventResponseResultFrom(evr))
            .toList();

        return new AdminSearchEventResponse().events(adminSearchEventResponseResults);
    }
}