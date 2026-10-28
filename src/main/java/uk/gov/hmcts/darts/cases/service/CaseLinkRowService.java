package uk.gov.hmcts.darts.cases.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.cases.enums.CaseLinkResultStatus;
import uk.gov.hmcts.darts.cases.exception.CaseLinkRowException;
import uk.gov.hmcts.darts.common.entity.CaseLinkImportResultEntity;
import uk.gov.hmcts.darts.common.entity.CaseLinkImportResultStatusEntity;
import uk.gov.hmcts.darts.common.entity.CaseLinkedCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.CaseLinkImportResultRepository;
import uk.gov.hmcts.darts.common.repository.CaseLinkedCaseRepository;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.repository.CourthouseRepository;
import uk.gov.hmcts.darts.common.service.CaseCommonService;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CaseLinkRowService {

    private final CourthouseRepository courthouseRepository;
    private final CaseRepository caseRepository;
    private final CaseCommonService caseCommonService;
    private final CaseLinkedCaseRepository linkedCaseRepository;
    private final CaseLinkImportResultRepository resultRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(CaseLinkImportResultEntity row, UserAccountEntity user) {
        String courthouseName = StringUtils.toRootUpperCase(row.getCase1Courthouse().trim());
        var courthouse = courthouseRepository.findForCaseLinkImport(courthouseName)
            .orElseThrow(() -> new CaseLinkRowException("COURTHOUSE_NOT_FOUND", "The supplied courthouse does not exist"));
        var case1 = caseRepository.findByCaseNumberAndCourthouse(row.getCase1CaseNumber().trim(), courthouse)
            .orElseThrow(() -> new CaseLinkRowException("CASE_1_NOT_FOUND", "Case 1 does not exist and must already exist"));
        row.setCase1Id(case1.getId());
        String case2Number = row.getCase2CaseNumber().trim();
        var existingCase2 = caseRepository.findByCaseNumberAndCourthouse(case2Number, courthouse);
        row.setCase2Created(existingCase2.isEmpty());
        var case2 = existingCase2.orElseGet(() -> caseCommonService.retrieveOrCreateCase(courthouse, case2Number, user));
        row.setCase2Id(case2.getId());
        recordWarnings(row, case1, case2);
        if (case1.getId().equals(case2.getId())) {
            throw new CaseLinkRowException("SELF_LINK", "A case cannot be linked to itself");
        }
        var existingLinks = linkedCaseRepository.findByCaseIds(case1.getId(), case2.getId());
        var link = existingLinks.isEmpty() ? createLink(case1, case2, user) : existingLinks.getFirst();
        row.setLinkedCaseId(link.getId());
        row.setStatus(new CaseLinkImportResultStatusEntity(CaseLinkResultStatus.SUCCESS.getId()));
        resultRepository.saveAndFlush(row);
    }

    private CaseLinkedCaseEntity createLink(CourtCaseEntity case1, CourtCaseEntity case2, UserAccountEntity user) {
        var link = new CaseLinkedCaseEntity();
        link.setCourtCase1(case1);
        link.setCourtCase2(case2);
        link.setCreatedBy(user);
        link.setLastModifiedBy(user);
        return linkedCaseRepository.saveAndFlush(link);
    }

    private void recordWarnings(CaseLinkImportResultEntity row, CourtCaseEntity case1, CourtCaseEntity case2) {
        row.setCase1Deleted(case1.isDeleted());
        row.setCase2Deleted(case2.isDeleted());
        row.setCase1Anonymised(case1.isDataAnonymised());
        row.setCase2Anonymised(case2.isDataAnonymised());
        List<String> warnings = new ArrayList<>();
        addWarning(warnings, row.isCase1Deleted(), "CASE_1_DELETED");
        addWarning(warnings, row.isCase2Deleted(), "CASE_2_DELETED");
        addWarning(warnings, row.isCase1Anonymised(), "CASE_1_ANONYMISED");
        addWarning(warnings, row.isCase2Anonymised(), "CASE_2_ANONYMISED");
        row.setWarningCodes(warnings.isEmpty() ? null : String.join(",", warnings));
    }

    private void addWarning(List<String> warnings, boolean condition, String code) {
        if (condition) {
            warnings.add(code);
        }
    }
}
