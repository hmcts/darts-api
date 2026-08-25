package uk.gov.hmcts.darts.usermanagement.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.transcriptions.service.TranscriptionService;
import uk.gov.hmcts.darts.usermanagement.service.DisableInactiveUserAccountsService;

import java.time.OffsetDateTime;
import java.time.Period;
import java.util.List;
import java.util.Set;

import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_ADMIN;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.SUPER_USER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.TRANSCRIBER;

@Service
@Slf4j
@RequiredArgsConstructor
public class DisableInactiveUserAccountsServiceImpl implements DisableInactiveUserAccountsService {

    private static final Period INACTIVITY_PERIOD = Period.ofMonths(6);
    private static final Set<Integer> PRIVILEGED_USER_ROLE_IDS = Set.of(SUPER_USER.getId(), SUPER_ADMIN.getId());
    private static final int MINIMUM_BATCH_SIZE = 1;

    private final UserAccountRepository userAccountRepository;
    private final CurrentTimeHelper currentTimeHelper;
    private final TranscriptionService transcriptionService;

    @Override
    @Transactional
    public void process(int batchSize) {
        int safeBatchSize = Math.max(batchSize, MINIMUM_BATCH_SIZE);
        OffsetDateTime cutoffDateTime = currentTimeHelper.currentOffsetDateTime().minus(INACTIVITY_PERIOD);

        // System users are excluded in the repository query with isSystemUser = false.
        List<UserAccountEntity> inactiveUsers = userAccountRepository.findInactiveUsersExcludingRoles(
            cutoffDateTime,
            PRIVILEGED_USER_ROLE_IDS,
            PageRequest.of(0, safeBatchSize)
        );

        if (inactiveUsers.isEmpty()) {
            log.info("No inactive user accounts found to disable");
            return;
        }

        inactiveUsers.forEach(this::disableAndRemoveFromSecurityGroups);
        userAccountRepository.saveAll(inactiveUsers);

        log.info("Disabled {} inactive user accounts", inactiveUsers.size());
    }

    private void disableAndRemoveFromSecurityGroups(UserAccountEntity userAccount) {
        rollbackAssignedTranscriptionsIfTranscriber(userAccount);
        userAccount.getSecurityGroupEntities().clear();
        userAccount.setActive(false);
    }

    private void rollbackAssignedTranscriptionsIfTranscriber(UserAccountEntity userAccount) {
        boolean isTranscriber = userAccountRepository
            .findByRoleAndUserId(TRANSCRIBER.getId(), userAccount.getId())
            .isPresent();

        if (isTranscriber) {
            transcriptionService.rollbackUserTranscriptions(userAccount);
        }
    }
}
