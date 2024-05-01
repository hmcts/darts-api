package uk.gov.hmcts.darts.task.service;

import uk.gov.hmcts.darts.tasks.model.AutomatedTaskSummary;
import uk.gov.hmcts.darts.tasks.model.DetailedAutomatedTask;

import java.util.List;

public interface AdminAutomatedTaskService {

    List<AutomatedTaskSummary> getAllAutomatedTasks();

    DetailedAutomatedTask getAutomatedTaskById(Integer taskId);
}
