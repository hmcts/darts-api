package uk.gov.hmcts.darts.common.service.impl;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.JudgeEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.JudgeRepository;
import uk.gov.hmcts.darts.common.service.JudgeCommonService;

import java.util.Locale;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class JudgeCommonServiceImpl implements JudgeCommonService {

    private final JudgeRepository judgeRepository;

    @Override
    @Transactional
    public JudgeEntity retrieveOrCreateJudge(String judgeName, UserAccountEntity userAccount) {
        Optional<JudgeEntity> foundJudge = judgeRepository.findByNameIgnoreCase(judgeName);
        return foundJudge.orElseGet(() -> createJudge(judgeName, userAccount));
    }

    private JudgeEntity createJudge(String judgeName, UserAccountEntity userAccount) {
        JudgeEntity judge = new JudgeEntity();
        String upperCaseJudgeName = judgeName != null ? judgeName.toUpperCase(Locale.ROOT) : null;
        judge.setName(upperCaseJudgeName);
        judge.setCreatedBy(userAccount);
        judge.setLastModifiedBy(userAccount);
        return judgeRepository.saveAndFlush(judge);
    }
}
