package uk.gov.hmcts.darts.common.service.impl;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.service.CaseCommonService;
import uk.gov.hmcts.darts.common.service.CourthouseCommonService;

import java.util.Optional;

@RequiredArgsConstructor
@Service
public class CaseCommonServiceImpl implements CaseCommonService {

    private final CaseRepository caseRepository;
    private final CourthouseCommonService courthouseCommonService;

    @Override
    @Transactional
    public CourtCaseEntity retrieveOrCreateCase(String courthouseName, String caseNumber, UserAccountEntity userAccount) {
        String courthouseNameUpperTrimmed = StringUtils.toRootUpperCase(StringUtils.trimToEmpty(courthouseName));
        Optional<CourtCaseEntity> foundCase = caseRepository.findByCaseNumberAndCourthouse_CourthouseName(caseNumber, courthouseNameUpperTrimmed);
        return foundCase
            .map(entity -> setCourtCaseLastDateModifiedBy(entity, userAccount))
            .orElseGet(() -> createCase(courthouseName, caseNumber, userAccount));
    }

    @Override
    @Transactional
    public CourtCaseEntity retrieveOrCreateCase(CourthouseEntity courthouse, String caseNumber, UserAccountEntity userAccount) {
        Optional<CourtCaseEntity> foundCase = caseRepository.findByCaseNumberAndCourthouse(caseNumber, courthouse);
        return foundCase
            .map(entity -> setCourtCaseLastDateModifiedBy(entity, userAccount))
            .orElseGet(() -> createCase(courthouse, caseNumber, userAccount));
    }

    private CourtCaseEntity createCase(String courthouseName, String caseNumber, UserAccountEntity userAccount) {
        CourthouseEntity foundCourthouse = courthouseCommonService.retrieveCourthouse(courthouseName);
        return createCase(foundCourthouse, caseNumber, userAccount);
    }

    private CourtCaseEntity createCase(CourthouseEntity courthouse, String caseNumber, UserAccountEntity userAccount) {
        CourtCaseEntity courtCase = new CourtCaseEntity();
        courtCase.setCaseNumber(caseNumber);
        courtCase.setCourthouse(courthouse);
        courtCase.setClosed(false);
        courtCase.setInterpreterUsed(false);
        courtCase.setCreatedBy(userAccount);
        courtCase.setLastModifiedBy(userAccount);
        return caseRepository.saveAndFlush(courtCase);
    }

    private CourtCaseEntity setCourtCaseLastDateModifiedBy(final CourtCaseEntity courtCaseEntity, final UserAccountEntity userAccountEntity) {
        courtCaseEntity.setLastModifiedBy(userAccountEntity);
        return caseRepository.saveAndFlush(courtCaseEntity);
    }
}
