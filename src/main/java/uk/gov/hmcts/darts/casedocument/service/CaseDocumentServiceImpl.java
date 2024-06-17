package uk.gov.hmcts.darts.casedocument.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.casedocument.mapper.CourtCaseDocumentMapper;
import uk.gov.hmcts.darts.casedocument.template.CourtCaseDocument;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.repository.CaseRepository;

@RequiredArgsConstructor
@Service
@Slf4j
public class CaseDocumentServiceImpl implements CaseDocumentService {

    private final CaseRepository caseRepository;
    private final CourtCaseDocumentMapper mapper;
    private final EntityManager entityManager;

    @Transactional
    public CourtCaseDocument generateCaseDocument(Integer caseId) {
//        Map<String, Object> properties = Map.of(
//            "javax.persistence.loadgraph",
//            entityManager.getEntityGraph("CourtCase.caseDocument")
//        );

        log.info("retrieving entity");
//        CourtCaseEntity courtCase = entityManager.find(CourtCaseEntity.class, caseId);

        CourtCaseEntity courtCase = caseRepository.findById(caseId).orElseThrow();
        CourtCaseDocument courtCaseDocument = mapper.map(courtCase);
        return courtCaseDocument;
    }
}
