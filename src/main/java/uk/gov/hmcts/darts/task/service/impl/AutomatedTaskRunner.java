package uk.gov.hmcts.darts.task.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.task.runner.impl.AbstractLockableAutomatedTask;

@Component
@Slf4j
public class AutomatedTaskRunner {
    @Async
    public void run(AbstractLockableAutomatedTask task, boolean isManualRun) {
        log.info("Attempting {} run of {}", isManualRun ? " manual " : " automated", task.getTaskName());
        task.run(isManualRun);
    }
}
