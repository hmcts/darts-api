package uk.gov.hmcts.darts.task.service.impl;

import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.AssertionErrors.assertNotNull;

@ExtendWith(MockitoExtension.class)
class LockServiceImplTest {

    private LockServiceImpl lockService;

    @Mock
    private AutomatedTaskRepository automatedTaskRepository;
    @Mock
    private CurrentTimeHelper currentTimeHelper;
    @Mock
    private LockProvider lockProvider;

    @BeforeEach
    void setUp() {
        lockService = new LockServiceImpl(automatedTaskRepository, currentTimeHelper, lockProvider);
    }

    @Test
    void getLockingTaskExecutor() {
        assertNotNull("TaskExecutor", lockService.getLockingTaskExecutor());
    }

    @Test
    void getLockAtMostFor() {
        assertEquals(Duration.ofMinutes(300), lockService.getLockAtMostFor());
    }

    @Test
    void getLockAtLeastFor() {
        assertEquals(Duration.ofSeconds(20), lockService.getLockAtLeastFor());
    }

    @Test
    void isLockedTrue() {
        OffsetDateTime someHoursAgo = OffsetDateTime.parse("2026-11-16T07:00:00Z");
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(someHoursAgo);

        LocalDateTime timestamp = LocalDateTime.parse("2026-11-16T12:00:00");
        when(automatedTaskRepository.findLockedUntilForTask(anyString())).thenReturn(List.of(timestamp));

        var taskName = "TestTask";
        var automatedTaskEntity = new AutomatedTaskEntity();
        automatedTaskEntity.setTaskName(taskName);

        assertTrue(lockService.isLocked(automatedTaskEntity));
    }

    @Test
    void isLockedFalse() {
        OffsetDateTime someHoursAhead = OffsetDateTime.parse("2026-11-16T17:00:00Z");
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(someHoursAhead);

        LocalDateTime timestamp = LocalDateTime.parse("2026-11-16T12:00:00");
        when(automatedTaskRepository.findLockedUntilForTask(anyString())).thenReturn(List.of(timestamp));

        var taskName = "TestTask";
        var automatedTaskEntity = new AutomatedTaskEntity();
        automatedTaskEntity.setTaskName(taskName);

        assertFalse(lockService.isLocked(automatedTaskEntity));
    }

    @Test
    void given_lockExpiresAtCurrentInstant_when_clockUsesDifferentOffset_then_treatLockAsExpired() {
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(OffsetDateTime.parse("2026-11-16T14:00:00+02:00"));
        when(automatedTaskRepository.findLockedUntilForTask(anyString()))
            .thenReturn(List.of(LocalDateTime.parse("2026-11-16T12:00:00")));
        var automatedTaskEntity = new AutomatedTaskEntity();
        automatedTaskEntity.setTaskName("TestTask");

        assertFalse(lockService.isLocked(automatedTaskEntity));
    }

    @Test
    void isLockedWhenNotSet() {
        var taskName = "TestTask";
        var automatedTaskEntity = new AutomatedTaskEntity();
        automatedTaskEntity.setTaskName(taskName);
        when(automatedTaskRepository.findLockedUntilForTask(anyString())).thenReturn(Collections.emptyList());
        assertFalse(lockService.isLocked(automatedTaskEntity));
    }
}
