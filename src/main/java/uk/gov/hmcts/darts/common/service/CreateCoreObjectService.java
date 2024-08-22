package uk.gov.hmcts.darts.common.service;


import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.DefenceEntity;
import uk.gov.hmcts.darts.common.entity.DefendantEntity;
import uk.gov.hmcts.darts.common.entity.ProsecutorEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

public interface CreateCoreObjectService {

    @Transactional
    DefenceEntity createDefence(String defence, CourtCaseEntity courtCase, UserAccountEntity userAccount);

    @Transactional
    DefendantEntity createDefendant(String defendant, CourtCaseEntity courtCase, UserAccountEntity userAccount);

    @Transactional
    ProsecutorEntity createProsecutor(String prosecution, CourtCaseEntity courtCase, UserAccountEntity userAccount);
}
