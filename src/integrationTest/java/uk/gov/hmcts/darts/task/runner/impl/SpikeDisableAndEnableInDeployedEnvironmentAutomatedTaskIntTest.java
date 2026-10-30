package uk.gov.hmcts.darts.task.runner.impl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.datamanagement.service.SpikeDisableAndEnableInDeployedEnvironmentService;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.hmcts.darts.task.status.AutomatedTaskStatus.COMPLETED;
import static uk.gov.hmcts.darts.task.status.AutomatedTaskStatus.SKIPPED;

class SpikeDisableAndEnableInDeployedEnvironmentAutomatedTaskIntTest extends PostgresIntegrationBase {

    @Autowired
    private SpikeDisableAndEnableInDeployedEnvironmentAutomatedTask task;
    @Autowired
    private AutomatedTaskRepository automatedTaskRepository;
    @MockitoBean
    private SpikeDisableAndEnableInDeployedEnvironmentService prototypeService;

    @Test
    void given_disabledTask_when_enabledThenDisabled_then_onlyEnabledRunExecutes() {
        var entity = automatedTaskRepository.findByTaskName(task.getTaskName()).orElseThrow();
        assertThat(entity.getTaskEnabled()).isFalse();
        assertThat(entity.getCronExpression()).isEqualTo("0 */5 * * * *");
        task.setLastCronExpression(entity.getCronExpression());

        task.run(false);
        assertThat(task.getAutomatedTaskStatus()).isEqualTo(SKIPPED);
        verifyNoInteractions(prototypeService);

        try {
            entity.setTaskEnabled(true);
            automatedTaskRepository.saveAndFlush(entity);
            task.run(false);
            assertThat(task.getAutomatedTaskStatus()).isEqualTo(COMPLETED);
            verify(prototypeService).process(1000);
        } finally {
            entity.setTaskEnabled(false);
            automatedTaskRepository.saveAndFlush(entity);
        }

        clearInvocations(prototypeService);
        task.run(false);
        assertThat(task.getAutomatedTaskStatus()).isEqualTo(SKIPPED);
        verifyNoInteractions(prototypeService);
    }
}
