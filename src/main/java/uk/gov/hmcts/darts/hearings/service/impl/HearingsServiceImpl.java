package uk.gov.hmcts.darts.hearings.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.hearings.model.GetHearingResponse;
import uk.gov.hmcts.darts.hearings.service.HearingsService;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class HearingsServiceImpl implements HearingsService {

    @Override
    public GetHearingResponse getHearings(Integer hearingId) {
        GetHearingResponse getHearingResponse = new GetHearingResponse();
        getHearingResponse.setId(1);
        getHearingResponse.setCourthouse("Stub courthouse");
        getHearingResponse.setCourtroom("Stub courtroom");
        getHearingResponse.setCaseId(1);
        getHearingResponse.setCaseNumber("Stub case number");
        getHearingResponse.setHearingDate(LocalDate.now());
        getHearingResponse.setJudges(List.of("stub judge"));
        getHearingResponse.setTranscriptionCount(1);
        return getHearingResponse;
    }
}
