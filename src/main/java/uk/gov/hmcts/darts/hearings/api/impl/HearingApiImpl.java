package uk.gov.hmcts.darts.hearings.api.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.hearings.api.HearingApi;
import uk.gov.hmcts.darts.hearings.service.HearingsService;

@Service
@RequiredArgsConstructor
public class HearingApiImpl implements HearingApi {

    private final HearingsService hearingsService;

    @Override
    public void removeMediaLinkToHearing(Integer courtCaseId) {
        hearingsService.removeMediaLinkToHearing(courtCaseId);
    }

}
