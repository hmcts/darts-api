package uk.gov.hmcts.darts.hearings.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.darts.hearings.api.HearingsApi;
import uk.gov.hmcts.darts.hearings.model.EventResponse;
import uk.gov.hmcts.darts.hearings.model.GetHearingResponse;
import uk.gov.hmcts.darts.hearings.service.HearingsService;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class HearingsController implements HearingsApi {

    private final HearingsService hearingsService;

    @Override
    public ResponseEntity<GetHearingResponse> getHearing(Integer hearingId) {
        return new ResponseEntity<>(hearingsService.getHearings(hearingId), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<EventResponse>> getEvents(Integer hearingId) {
        return new ResponseEntity<>(hearingsService.getEvents(hearingId), HttpStatus.OK);
    }
}
