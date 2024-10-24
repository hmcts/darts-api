package uk.gov.hmcts.darts.common.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.DefenceEntity;
import uk.gov.hmcts.darts.common.entity.DefendantEntity;
import uk.gov.hmcts.darts.common.entity.ProsecutorEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.DefenceRepository;
import uk.gov.hmcts.darts.common.repository.DefendantRepository;
import uk.gov.hmcts.darts.common.repository.ProsecutorRepository;
import uk.gov.hmcts.darts.common.service.CreateCoreObjectService;
import uk.gov.hmcts.darts.util.DataUtil;

@RequiredArgsConstructor
@Service
@Slf4j
public class CreateCoreObjectServiceImpl implements CreateCoreObjectService {


    private final DefenceRepository defenceRepository;
    private final DefendantRepository defendantRepository;
    private final ProsecutorRepository prosecutorRepository;

    @Override
    public DefenceEntity createDefence(String defenceName, CourtCaseEntity courtCase, UserAccountEntity userAccount) {
        DefenceEntity defence = new DefenceEntity();
        defence.setName(DataUtil.trim(defenceName));
        defence.setCourtCase(courtCase);
        defence.setCreatedBy(userAccount);
        defence.setLastModifiedBy(userAccount);
        return defenceRepository.saveAndFlush(defence);
    }

    @Override
    public DefendantEntity createDefendant(String defendantName, CourtCaseEntity courtCase, UserAccountEntity userAccount) {
        DefendantEntity defendant = new DefendantEntity();
        defendant.setName(DataUtil.trim(defendantName));
        defendant.setCourtCase(courtCase);
        defendant.setCreatedBy(userAccount);
        defendant.setLastModifiedBy(userAccount);
        return defendantRepository.saveAndFlush(defendant);
    }

    @Override
    public ProsecutorEntity createProsecutor(String prosecutorName, CourtCaseEntity courtCase, UserAccountEntity userAccount) {
        ProsecutorEntity prosecutor = new ProsecutorEntity();
        prosecutor.setName(DataUtil.trim(prosecutorName));
        prosecutor.setCourtCase(courtCase);
        prosecutor.setCreatedBy(userAccount);
        prosecutor.setLastModifiedBy(userAccount);
        prosecutorRepository.saveAndFlush(prosecutor);
        return prosecutor;
    }
}
