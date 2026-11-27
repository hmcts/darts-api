package uk.gov.hmcts.darts.task.runner.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.audio.service.AudioDiscoverySpikeService;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

class AudioSpikeTasksIntTest extends PostgresIntegrationBase {
    @Autowired
    private AutomatedTaskRepository tasks;
    @Autowired
    private UserAccountRepository users;
    @Autowired
    private AudioSpikeCvpAutomatedTask cvp;
    @Autowired
    private AudioSpikeCvpExternalAutomatedTask external;
    @Autowired
    private AudioSpikeCarAutomatedTask car;
    @Autowired
    private AudioSpikeCombinedAutomatedTask combined;
    @Autowired
    private AudioSpikeCombinedFaultyAutomatedTask faulty;
    @MockitoBean
    private AudioDiscoverySpikeService service;

    @ParameterizedTest
    @CsvSource({
        "AudioSpikeCvp,-58", "AudioSpikeCvpExternal,-55", "AudioSpikeCar,-56",
        "AudioSpikeCombined,-57", "AudioSpikeCombinedFaulty,-59"
    })
    @Transactional
    void findByTaskName_returnsDisabledTaskAndSystemUser_whenMigrationApplied(String taskName, int userId) {
        var task = tasks.findByTaskName(taskName).orElseThrow();

        assertThat(task.getTaskEnabled()).isFalse();
        assertThat(task.getCronExpression()).isEqualTo("0 */10 * * * *");
        assertThat(users.findFirstByEmailAddressIgnoreCase("system_" + taskName + "AutomatedTask@hmcts.net").orElseThrow().getId())
            .isEqualTo(userId);
    }

    @Test
    void runTask_delegatesToMatchingSource_whenEachVariantRuns() {
        cvp.runTask();
        external.runTask();
        car.runTask();
        combined.runTask();
        faulty.runTask();

        verify(service).scanCvp(1000);
        verify(service).scanCvpExternal(1000);
        verify(service).scanCar(1000);
        verify(service).scan(true, true, 1000);
        verify(service).scanCombinedFaulty(1000);
    }
}
