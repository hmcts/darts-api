package uk.gov.hmcts.darts.common.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.JudgeEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.JudgeRepository;

import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class JudgeService {

    private final JudgeRepository judgeRepository;

    @Transactional
    public JudgeEntity retrieveOrCreateJudge(String judgeName, UserAccountEntity userAccount) {
        Optional<JudgeEntity> foundJudge = judgeRepository.findByNameIgnoreCase(judgeName);
        return foundJudge.orElseGet(() -> createJudge(judgeName, userAccount));
    }

    @Transactional
    public JudgeEntity createJudge(String judgeName, UserAccountEntity userAccount) {
        JudgeEntity judge = new JudgeEntity();
        String upperCaseJudgeName = judgeName != null ? judgeName.toUpperCase(Locale.ROOT) : null;
        judge.setName(upperCaseJudgeName);
        judge.setCreatedBy(userAccount);
        judge.setLastModifiedBy(userAccount);
        judgeRepository.saveAndFlush(judge);
        return judge;
    }
}