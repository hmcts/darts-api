package uk.gov.hmcts.darts.hearings.service;

import uk.gov.hmcts.darts.hearings.model.EventResponse;
import uk.gov.hmcts.darts.hearings.model.GetHearingResponse;

import java.util.List;

public interface HearingsService {

    GetHearingResponse getHearings(Integer hearingId);

    List<EventResponse> getEvents(Integer hearingId);

}
