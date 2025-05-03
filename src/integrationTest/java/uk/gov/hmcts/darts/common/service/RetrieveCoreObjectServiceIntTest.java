package uk.gov.hmcts.darts.common.service;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.JudgeEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.testutils.GivenBuilder;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class RetrieveCoreObjectServiceIntTest extends PostgresIntegrationBase {

    @Autowired
    @MockitoSpyBean
    private JudgeCommonService judgeCommonService;

    @Test
    /*
    Tests that the service can create a hearing with CaseNumber `casenumber` when `CaseNumber` already exists with a hearing.
     */
    void useExistingCaseDifferentCaseNumberCase() throws Exception {
        dartsDatabase.createCourthouseUnlessExists("swansea");
        LocalDateTime hearingDate = LocalDateTime.of(2020, 10, 10, 10, 0, 0, 0);
        UserAccountEntity userAccount = new UserAccountEntity();
        userAccount.setId(0);
        HearingEntity existingHearing = dartsDatabase.getRetrieveCoreObjectService()
            .retrieveOrCreateHearing("swansea",
                                     "1", "CaseNumber",
                                     hearingDate,
                                     userAccount);

        HearingEntity newHearing = dartsDatabase.getRetrieveCoreObjectService()
            .retrieveOrCreateHearing("swansea",
                                     "1", "casenumber",
                                     hearingDate,
                                     userAccount);

        //Should be a different case  as case numbers should be case-sensitive.
        assertNotEquals(existingHearing.getId(), newHearing.getId());
    }

    @Test
    void verifyRetryOccures_onErrorsAndCommitsAfterErrors() {

        UserAccountEntity userAccount = dartsDatabase.createTestUserAccount();
        GivenBuilder.anAuthenticatedUserFor(userAccount);

        AtomicInteger counter = new AtomicInteger(0);
        int timesToFail = 2;
        Mockito.doAnswer(invocation -> {
            if (counter.incrementAndGet() <= timesToFail) {
                throw new DataIntegrityViolationException("Simulated error");
            }
            return invocation.callRealMethod();
        }).when(judgeCommonService).retrieveOrCreateJudge(any(), any());


        String judgeName = "JUDGE" + UUID.randomUUID().toString();

        transactionalUtil.executeInTransaction(() -> {
            JudgeEntity judge = dartsDatabase.getRetrieveCoreObjectService().retrieveOrCreateJudge(judgeName);
            EntityManager entityManager = dartsDatabase.getDartsPersistence().getEntityManager();
            assertTrue(entityManager.contains(judge));

        });

        Optional<JudgeEntity> judge = dartsDatabase.getJudgeRepository().findByNameIgnoreCase(judgeName);

        if (judge.isEmpty()) {
            fail("Judge not created when it should have");
        }
        ArgumentCaptor<UserAccountEntity> userAccountCaptor = ArgumentCaptor.captor();
        verify(judgeCommonService, times(3))
            .retrieveOrCreateJudge(eq(judgeName), userAccountCaptor.capture());

        assertThat(userAccountCaptor.getValue().getId())
            .isEqualTo(userAccount.getId());
    }
}
