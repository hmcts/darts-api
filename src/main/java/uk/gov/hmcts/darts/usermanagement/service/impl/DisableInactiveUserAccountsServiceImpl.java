package uk.gov.hmcts.darts.usermanagement.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
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
import static uk.gov.hmcts.darts.transcriptions.enums.TranscriptionStatusEnum.WITH_TRANSCRIBER;

@Service
@Slf4j
@RequiredArgsConstructor
public class DisableInactiveUserAccountsServiceImpl implements DisableInactiveUserAccountsService {

    @Value("${darts.automated.task.disable-inactive-user-accounts.inactivity-period-limit:P6M}")
    private Period inactivityPeriodLimit;

    private static final Set<Integer> PRIVILEGED_USER_ROLE_IDS = Set.of(SUPER_USER.getId(), SUPER_ADMIN.getId());
    private static final int MINIMUM_BATCH_SIZE = 1;

    private final UserAccountRepository userAccountRepository;
    private final UserAccountSecurityGroupService userAccountSecurityGroupService;
    private final CurrentTimeHelper currentTimeHelper;
    private final TranscriptionService transcriptionService;

    @Override
    @Transactional
    public void process(int batchSize) {
        int safeBatchSize = Math.max(batchSize, MINIMUM_BATCH_SIZE);
        OffsetDateTime cutoffDateTime = currentTimeHelper.currentOffsetDateTime().minus(inactivityPeriodLimit);

        // Excludes protected users and returns inactive accounts that are active, grouped, or still own transcriber work.
        List<UserAccountEntity> inactiveUsers = userAccountRepository.findInactiveUsersForCleanupExcludingRoles(
            cutoffDateTime,
            PRIVILEGED_USER_ROLE_IDS,
            WITH_TRANSCRIBER.getId(),
            Limit.of(safeBatchSize)
        );

        if (inactiveUsers.isEmpty()) {
            log.info("No inactive user accounts found to process");
            return;
        }

        inactiveUsers.forEach(this::processInactiveUser);
        userAccountRepository.saveAll(inactiveUsers);

        log.info("Processed {} inactive user accounts", inactiveUsers.size());
    }

    private void processInactiveUser(UserAccountEntity userAccount) {
        transcriptionService.rollbackUserTranscriptions(userAccount);
        userAccountSecurityGroupService.unassignUserFromGroupsTheyArePartOf(userAccount);
        if (Boolean.TRUE.equals(userAccount.isActive())) {
            userAccount.setActive(false);
        }
    }
}
