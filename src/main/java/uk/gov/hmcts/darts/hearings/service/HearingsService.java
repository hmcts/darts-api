package uk.gov.hmcts.darts.hearings.service;

import uk.gov.hmcts.darts.hearings.model.GetHearingResponse;

public interface HearingsService {

    GetHearingResponse getHearings(Integer hearingId);

}
