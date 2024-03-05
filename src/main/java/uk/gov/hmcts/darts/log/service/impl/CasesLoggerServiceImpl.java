package uk.gov.hmcts.darts.log.service.impl;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.cases.model.AddCaseRequest;
import uk.gov.hmcts.darts.cases.model.GetCasesRequest;
import uk.gov.hmcts.darts.log.service.CasesLoggerService;

@Service
@NoArgsConstructor
@Slf4j
public class CasesLoggerServiceImpl implements CasesLoggerService {

    @Override
    public void casesRequestedByDarPc(GetCasesRequest getCasesRequest) {
        log.info("getCases request received: courthouse={}, courtroom={}", getCasesRequest.getCourthouse(), getCasesRequest.getCourtroom());
    }

    @Override
    public void defendantNameOverflow(AddCaseRequest addCaseRequest) {
        log.warn("Defendant name overflow: case_number={}, courthouse={}",
                 addCaseRequest.getCaseNumber(),
                 addCaseRequest.getCourthouse());
    }
}
