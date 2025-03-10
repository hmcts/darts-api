package uk.gov.hmcts.darts.hearings.service;


import uk.gov.hmcts.darts.hearings.model.HearingsAudiosResponseInner;
import uk.gov.hmcts.darts.hearings.model.HearingsResponse;
import uk.gov.hmcts.darts.hearings.model.HearingsSearchRequest;
import uk.gov.hmcts.darts.hearings.model.HearingsSearchResponse;

import java.util.List;

public interface AdminHearingsService {
    List<HearingsSearchResponse> adminHearingSearch(HearingsSearchRequest request);

    HearingsResponse getAdminHearings(Integer hearingId);

    List<HearingsAudiosResponseInner> getHearingAudios(Integer hearingId);
}