package uk.gov.hmcts.darts.event.service;

import uk.gov.hmcts.darts.event.model.AdminGetEventForIdResponseResult;

import java.util.List;

public interface EventService {
    AdminGetEventForIdResponseResult adminGetEventById(Integer eventId);

    void obfuscateEventByIds(List<Integer> eveIds);
}