package uk.gov.hmcts.darts.task.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.scheduling.Trigger;
import uk.gov.hmcts.darts.task.runner.AutomatedTask;

@Data
@Builder
public class TriggerAndAutomatedTask {
    private Trigger trigger;
    private AutomatedTask automatedTask;
}
