package uk.gov.hmcts.darts.task.runner.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.api.AutomatedTaskName;
import uk.gov.hmcts.darts.task.config.DisableInactiveUserAccountsAutomatedTaskConfig;
import uk.gov.hmcts.darts.task.service.LockService;
import uk.gov.hmcts.darts.usermanagement.service.DisableInactiveUserAccountsService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DisableInactiveUserAccountsAutomatedTaskTest {

    @Mock
    private AutomatedTaskRepository automatedTaskRepository;
    @Mock
    private DisableInactiveUserAccountsAutomatedTaskConfig automatedTaskConfig;
    @Mock
    private LogApi logApi;
    @Mock
    private LockService lockService;
    @Mock
    private DisableInactiveUserAccountsService disableInactiveUserAccountsService;

    private DisableInactiveUserAccountsAutomatedTask automatedTask;

    @BeforeEach
    void setUp() {
        automatedTask = new DisableInactiveUserAccountsAutomatedTask(
            automatedTaskRepository,
            automatedTaskConfig,
            logApi,
            lockService,
            disableInactiveUserAccountsService
        );
    }

    @Test
    void runTask_shouldProcessInactiveUsersWithAutomatedTaskBatchSize() {
        AutomatedTaskEntity automatedTaskEntity = new AutomatedTaskEntity();
        automatedTaskEntity.setBatchSize(1000);
        when(automatedTaskRepository.findByTaskName("DisableInactiveUserAccounts")).thenReturn(Optional.of(automatedTaskEntity));

        automatedTask.runTask();

        verify(disableInactiveUserAccountsService).process(1000);
    }

    @Test
    void getAutomatedTaskName_shouldReturnCorrectName() {
        assertThat(automatedTask.getAutomatedTaskName())
            .isEqualTo(AutomatedTaskName.DISABLE_INACTIVE_USER_ACCOUNTS);
    }
}
