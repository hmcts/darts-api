package uk.gov.hmcts.darts.common.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.CaseRepository;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CaseService {

    private final CaseRepository caseRepository;
    private final CourthouseService courthouseService;

    @Transactional
    public CourtCaseEntity createCase(String courthouseName, String caseNumber, UserAccountEntity userAccount) {
        CourthouseEntity foundCourthouse = courthouseService.retrieveCourthouse(courthouseName);
        return createCase(foundCourthouse, caseNumber, userAccount);
    }

    @Transactional
    public CourtCaseEntity createCase(CourthouseEntity courthouse, String caseNumber, UserAccountEntity userAccount) {
        CourtCaseEntity courtCase = new CourtCaseEntity();
        courtCase.setCaseNumber(caseNumber);
        courtCase.setCourthouse(courthouse);
        courtCase.setClosed(false);
        courtCase.setInterpreterUsed(false);
        courtCase.setCreatedBy(userAccount);
        courtCase.setLastModifiedBy(userAccount);
        caseRepository.saveAndFlush(courtCase);
        return courtCase;
    }

    @Transactional
    public CourtCaseEntity setCourtCaseLastDateModifiedBy(final CourtCaseEntity courtCaseEntity, final UserAccountEntity userAccountEntity) {
        courtCaseEntity.setLastModifiedBy(userAccountEntity);
        caseRepository.saveAndFlush(courtCaseEntity);
        return courtCaseEntity;
    }

    @Transactional
    public CourtCaseEntity retrieveOrCreateCase(String courthouseName, String caseNumber, UserAccountEntity userAccount) {
        Optional<CourtCaseEntity> foundCase = caseRepository.findByCaseNumberAndCourthouse_CourthouseNameIgnoreCase(
            caseNumber,
            courthouseName
        );

        return foundCase.map(entity -> setCourtCaseLastDateModifiedBy(entity, userAccount))
            .orElseGet(() -> createCase(courthouseName, caseNumber, userAccount));
    }

    @Transactional
    public CourtCaseEntity retrieveOrCreateCase(CourthouseEntity courthouse, String caseNumber, UserAccountEntity userAccount) {
        Optional<CourtCaseEntity> foundCase = caseRepository.findByCaseNumberAndCourthouse(
            caseNumber,
            courthouse
        );

        return foundCase.map(entity -> setCourtCaseLastDateModifiedBy(entity, userAccount))
            .orElseGet(() -> createCase(courthouse, caseNumber, userAccount));
    }
}