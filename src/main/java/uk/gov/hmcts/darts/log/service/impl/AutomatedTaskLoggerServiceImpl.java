package uk.gov.hmcts.darts.log.service.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.log.service.AutomatedTaskLoggerService;

import java.util.UUID;

@Service
@AllArgsConstructor
@Slf4j
public class AutomatedTaskLoggerServiceImpl implements AutomatedTaskLoggerService {

    @Override
    public void taskStarted(UUID taskExecutionId, String taskName, Integer batchSize) {
        log.info("Task started: run_id={}, task_name={}, batch_size={}", taskExecutionId, taskName, batchSize);
    }

    @Override
    public void taskCompleted(UUID taskExecutionId, String taskName) {
        log.info("Task completed: run_id={}, task_name={}", taskExecutionId, taskName);
    }

    @Override
    public void taskFailed(UUID taskExecutionId, String taskName) {
        log.info("Task failed: run_id={}, task_name={}", taskExecutionId, taskName);
    }
}