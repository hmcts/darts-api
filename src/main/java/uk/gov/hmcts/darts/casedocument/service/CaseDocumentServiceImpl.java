package uk.gov.hmcts.darts.casedocument.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.casedocument.mapper.CourtCaseDocumentMapper;
import uk.gov.hmcts.darts.casedocument.model.CourtCaseDocument;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.exception.DartsException;
import uk.gov.hmcts.darts.common.repository.CaseRepository;

@RequiredArgsConstructor
@Service
@Slf4j
public class CaseDocumentServiceImpl implements CaseDocumentService {

    private final CaseRepository caseRepository;
    private final CourtCaseDocumentMapper caseDocumentMapper;

    @Override
    public CourtCaseDocument generateCaseDocument(Integer caseId) {
        CourtCaseEntity courtCase = caseRepository.findById(caseId).orElseThrow(
            () -> new DartsException(String.format("court case not found: %s", caseId))
        );

        return caseDocumentMapper.mapToCaseDocument(courtCase);
    }
}
