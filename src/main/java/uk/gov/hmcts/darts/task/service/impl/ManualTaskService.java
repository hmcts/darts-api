package uk.gov.hmcts.darts.task.service.impl;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.task.runner.AutoloadingManualTask;
import uk.gov.hmcts.darts.task.runner.impl.AbstractLockableAutomatedTask;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ManualTaskService {
    private final List<AutoloadingManualTask> autoloadingManualTasks;

    @Getter
    private final List<AbstractLockableAutomatedTask> automatedTasks = new ArrayList<>();

    @PostConstruct
    public void configureAndLoadAutomatedTasks() {
        autoloadingManualTasks.forEach(autoloadingManualTask -> {
            AbstractLockableAutomatedTask task = autoloadingManualTask.getAbstractLockableAutomatedTask();
            task.setManualTask();
            automatedTasks.add(task);
        });
    }
}