package uk.gov.hmcts.darts.hearings.service;


import uk.gov.hmcts.darts.hearings.model.HearingsSearchRequest;
import uk.gov.hmcts.darts.hearings.model.HearingsSearchResponse;

import java.util.List;

public interface AdminHearingsService {
    List<HearingsSearchResponse> adminHearingSearch(HearingsSearchRequest request);
}