package uk.gov.hmcts.darts.common.service.impl;


import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
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
        if (StringUtils.isBlank(judgeName)) {
            throw new IllegalArgumentException("Judge name cannot be null or empty");
        }
        String trimmedJudgeName = judgeName.trim();
        Optional<JudgeEntity> foundJudge = judgeRepository.findByNameIgnoreCase(trimmedJudgeName);
        return foundJudge.orElseGet(() -> createJudge(trimmedJudgeName, userAccount));
    }

    private JudgeEntity createJudge(String judgeName, UserAccountEntity userAccount) {
        JudgeEntity judge = new JudgeEntity();
        String upperCaseJudgeName = judgeName.toUpperCase(Locale.ROOT);
        judge.setName(upperCaseJudgeName);
        judge.setCreatedBy(userAccount);
        judge.setLastModifiedBy(userAccount);
        return judgeRepository.saveAndFlush(judge);
    }
}
