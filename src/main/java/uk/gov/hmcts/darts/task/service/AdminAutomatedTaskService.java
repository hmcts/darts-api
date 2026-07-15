package uk.gov.hmcts.darts.task.service;

import uk.gov.hmcts.darts.tasks.model.AutomatedTaskCronExpressionPatch;
import uk.gov.hmcts.darts.tasks.model.AutomatedTaskCronExpressionPost;
import uk.gov.hmcts.darts.tasks.model.AutomatedTaskCronExpressionSchedule;
import uk.gov.hmcts.darts.tasks.model.AutomatedTaskPatch;
import uk.gov.hmcts.darts.tasks.model.AutomatedTaskSummary;
import uk.gov.hmcts.darts.tasks.model.DetailedAutomatedTask;

import java.util.List;

public interface AdminAutomatedTaskService {

    List<AutomatedTaskSummary> getAllAutomatedTasksSummaries();

    DetailedAutomatedTask getAutomatedTaskById(Integer taskId);

    void runAutomatedTask(Integer taskId);

    DetailedAutomatedTask updateAutomatedTask(Integer taskId, AutomatedTaskPatch automatedTaskPatch);

    List<AutomatedTaskCronExpressionSchedule> getAutomatedTaskCronExpressionSchedule(
        Integer taskId, AutomatedTaskCronExpressionPost automatedTaskCronExpressionPost);

    void updateAutomatedTaskCronExpressionSchedule(
        Integer taskId, AutomatedTaskCronExpressionPatch automatedTaskCronExpressionPatch);
}
