package uk.gov.hmcts.darts.cases.service;

import uk.gov.hmcts.darts.common.entity.CaseLinkedCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;

import java.util.List;

public interface CaseLinkedCaseService {

    List<CaseLinkedCaseEntity> getLinkedCases(CourtCaseEntity courtCase);

    List<CaseLinkedCaseEntity> getLinkedCasesByCourtCases(List<CourtCaseEntity> courtCases);
}
