package uk.gov.hmcts.darts.task.service;

import uk.gov.hmcts.darts.tasks.model.AutomatedTask;

import java.util.List;

public interface AdminAutomatedTaskService {

    List<AutomatedTask> getAllAutomatedTasks();

}
