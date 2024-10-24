package uk.gov.hmcts.darts.log.service.impl;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.cases.model.AddCaseRequest;
import uk.gov.hmcts.darts.cases.model.GetCasesRequest;
import uk.gov.hmcts.darts.log.service.CasesLoggerService;
import uk.gov.hmcts.darts.util.DataUtil;

@Service
@NoArgsConstructor
@Slf4j
public class CasesLoggerServiceImpl implements CasesLoggerService {

    @Override
    public void casesRequestedByDarPc(GetCasesRequest getCasesRequest) {
        log.info("getCases request received: courthouse={}, courtroom={}",
                 DataUtil.toUpperCase(getCasesRequest.getCourthouse()),
                 DataUtil.toUpperCase(getCasesRequest.getCourtroom()));
    }

    @Override
    public void defendantNameOverflow(AddCaseRequest addCaseRequest) {
        log.warn("Defendant name overflow: case_number={}, courthouse={}",
                 addCaseRequest.getCaseNumber(),
                 DataUtil.toUpperCase(addCaseRequest.getCourthouse()));
    }

    @Override
    public void defendantNotAdded(String defendant, String caseNumber) {
        log.info("Defendant not added to case: defendant={}, case_number={}", defendant, caseNumber);
    }

    @Override
    public void caseDeletedDueToExpiry(Integer caseId, String caseNumber) {
        log.info("Case expired: cas_id={}, case_number={}", caseId, caseNumber);
    }
}
