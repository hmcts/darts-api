package uk.gov.hmcts.darts.cases.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.cases.service.CaseLinkedCaseService;
import uk.gov.hmcts.darts.common.entity.CaseLinkedCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.repository.CaseLinkedCaseRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CaseLinkedCaseServiceImpl implements CaseLinkedCaseService {

    private final CaseLinkedCaseRepository caseLinkedCaseRepository;

    @Override
    public List<CaseLinkedCaseEntity> getLinkedCases(CourtCaseEntity courtCase) {
        return caseLinkedCaseRepository.findByCourtCase(courtCase);
    }

    @Override
    public List<CaseLinkedCaseEntity> getLinkedCasesByCourtCases(List<CourtCaseEntity> courtCases) {
        if (courtCases.isEmpty()) {
            return List.of();
        }

        return caseLinkedCaseRepository.findByCourtCaseIn(courtCases);
    }
}
