package uk.gov.hmcts.darts.task.runner.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.AutomatedTaskRepository;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.service.DataAnonymisationService;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.task.config.AutomatedTaskConfigurationProperties;
import uk.gov.hmcts.darts.task.service.LockService;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseExpiryDeletionAutomatedTaskTest {

    @Mock
    private AutomatedTaskRepository automatedTaskRepository;
    @Mock
    private AutomatedTaskConfigurationProperties automatedTaskConfigurationProperties;
    @Mock
    private CurrentTimeHelper currentTimeHelper;
    @Mock
    private CaseRepository caseRepository;
    @Mock
    private LogApi logApi;
    @Mock
    private LockService lockService;
    @Mock
    private DataAnonymisationService dataAnonymisationService;
    @Mock
    private UserIdentity userIdentity;


    @InjectMocks
    @Spy
    private CaseExpiryDeletionAutomatedTask caseExpiryDeletionAutomatedTask;

    @Test
    void runTask() {
        UserAccountEntity userAccount = mock(UserAccountEntity.class);
        when(userIdentity.getUserAccount()).thenReturn(userAccount);
        OffsetDateTime offsetDateTime = OffsetDateTime.now();
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(offsetDateTime);

        when(caseRepository.findCaseIdsToBeAnonymised(any(), any()))
            .thenReturn(List.of(1, 2, 3));

        doReturn(5).when(caseExpiryDeletionAutomatedTask)
            .getAutomatedTaskBatchSize();

        caseExpiryDeletionAutomatedTask.runTask();

        verify(currentTimeHelper, times(1))
            .currentOffsetDateTime();

        verify(dataAnonymisationService, times(1))
            .anonymiseCourtCaseById(userAccount, 1);
        verify(dataAnonymisationService, times(1))
            .anonymiseCourtCaseById(userAccount, 2);
        verify(dataAnonymisationService, times(1))
            .anonymiseCourtCaseById(userAccount, 3);


        verify(caseRepository, times(1))
            .findCaseIdsToBeAnonymised(offsetDateTime, Limit.of(5));

        verify(caseExpiryDeletionAutomatedTask, times(1))
            .getAutomatedTaskBatchSize();
        verify(userIdentity, times(1))
            .getUserAccount();
    }
}
