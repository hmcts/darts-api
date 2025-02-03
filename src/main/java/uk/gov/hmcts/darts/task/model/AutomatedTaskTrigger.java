package uk.gov.hmcts.darts.task.model;

import lombok.Getter;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.config.TriggerTask;
import uk.gov.hmcts.darts.task.runner.AutomatedTask;

@Getter
public class AutomatedTaskTrigger extends TriggerTask {
    private final AutomatedTask automatedTask;

    public AutomatedTaskTrigger(AutomatedTask automatedTask, Trigger trigger) {
        super(automatedTask, trigger);
        this.automatedTask = automatedTask;
    }
}
