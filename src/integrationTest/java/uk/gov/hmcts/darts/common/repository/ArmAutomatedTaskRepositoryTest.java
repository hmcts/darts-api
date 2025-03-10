package uk.gov.hmcts.darts.common.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.ArmAutomatedTaskEntity;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArmAutomatedTaskRepositoryTest extends PostgresIntegrationBase {

    @Autowired
    private AutomatedTaskRepository automatedTaskRepository;

    @Autowired
    private ArmAutomatedTaskRepository armAutomatedTaskRepository;

    @Test
    void findByAutomatedTaskTaskName_shouldReturnExpectedArmTask_whenItExists() {
        List<AutomatedTaskEntity> automatedTasks = automatedTaskRepository.findAll();

        // Given we have a couple of arbitrary automated tasks
        var automatedTask1 = automatedTasks.getFirst();
        var automatedTask2 = automatedTasks.get(1);

        // And we relate them to some arm tasks
        var armAutomatedTask1 = new ArmAutomatedTaskEntity();
        armAutomatedTask1.setAutomatedTask(automatedTask1);
        armAutomatedTask1 = armAutomatedTaskRepository.save(armAutomatedTask1);

        var armAutomatedTask2 = new ArmAutomatedTaskEntity();
        armAutomatedTask2.setAutomatedTask(automatedTask2);
        armAutomatedTaskRepository.save(armAutomatedTask2);

        // When we try to find an arm task that's related to a specific automated task
        var foundArmAutomatedTask1 = armAutomatedTaskRepository.findByAutomatedTask_taskName(automatedTask1.getTaskName())
            .orElseThrow();

        // Then
        assertEquals(armAutomatedTask1.getId(), foundArmAutomatedTask1.getId());
    }

    @Test
    void findByAutomatedTaskTaskName_shouldReturnEmptyOptional_whenItDoesNotExist() {
        // When we try to find an arm task that is related to a task that does not exist
        var foundArmAutomatedTask1 = armAutomatedTaskRepository.findByAutomatedTask_taskName("some non existent task name");

        // Then
        assertTrue(foundArmAutomatedTask1.isEmpty());
    }

}
