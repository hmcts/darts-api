package uk.gov.hmcts.darts.cases.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.common.service.DataAnonymisationService;
import uk.gov.hmcts.darts.hearings.service.HearingsService;
import uk.gov.hmcts.darts.task.config.CaseExpiryDeletionAutomatedTaskConfig;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseExpiryDeleterImplTest {

    @Mock
    private CaseExpiryDeletionAutomatedTaskConfig config;
    @Mock
    private CurrentTimeHelper currentTimeHelper;
    @Mock
    private CaseRepository caseRepository;
    @Mock
    private DataAnonymisationService dataAnonymisationService;
    @Mock
    private UserIdentity userIdentity;
    @Mock
    private HearingsService hearingsService;

    private CaseExpiryDeleterImpl caseExpiryDeleter;

    @BeforeEach
    void setUp() {
        caseExpiryDeleter = new CaseExpiryDeleterImpl(
            currentTimeHelper,
            dataAnonymisationService,
            hearingsService,
            caseRepository,
            userIdentity,
            config
        );
    }

    @Test
    void delete_shouldAnonymiseData() {
        Duration duration = Duration.ofHours(24);
        when(config.getBufferDuration()).thenReturn(duration);
        UserAccountEntity userAccount = mock(UserAccountEntity.class);
        when(userIdentity.getUserAccount()).thenReturn(userAccount);
        OffsetDateTime offsetDateTime = OffsetDateTime.now();
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(offsetDateTime);

        when(caseRepository.findCaseIdsToBeAnonymised(any(), any()))
            .thenReturn(List.of(1, 2, 3));

        caseExpiryDeleter.delete(5);

        verify(currentTimeHelper, times(1)).currentOffsetDateTime();

        verify(dataAnonymisationService).anonymiseCourtCaseById(userAccount, 1, false);
        verify(hearingsService).removeMediaLinkToHearing(1);
        verify(dataAnonymisationService).anonymiseCourtCaseById(userAccount, 2, false);
        verify(hearingsService).removeMediaLinkToHearing(2);
        verify(dataAnonymisationService).anonymiseCourtCaseById(userAccount, 3, false);
        verify(hearingsService).removeMediaLinkToHearing(3);
        verify(caseRepository).findCaseIdsToBeAnonymised(offsetDateTime.minus(duration), Limit.of(5));
        verify(userIdentity).getUserAccount();
    }

    @Test
    void delete_shouldDoNothingWhenNoCasesToAnonymise() {
        Duration duration = Duration.ofHours(48);
        when(config.getBufferDuration()).thenReturn(duration);
        UserAccountEntity userAccount = mock(UserAccountEntity.class);
        when(userIdentity.getUserAccount()).thenReturn(userAccount);
        OffsetDateTime now = OffsetDateTime.now();
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(now);

        when(caseRepository.findCaseIdsToBeAnonymised(any(), any()))
            .thenReturn(List.of());

        caseExpiryDeleter.delete(10);

        verify(currentTimeHelper, times(1)).currentOffsetDateTime();
        verify(caseRepository).findCaseIdsToBeAnonymised(now.minus(duration), Limit.of(10));
        // No anonymisation or hearing updates when list is empty
        verify(dataAnonymisationService, times(0)).anonymiseCourtCaseById(any(UserAccountEntity.class), any(Integer.class), any(Boolean.class));
        verify(hearingsService, times(0)).removeMediaLinkToHearing(any());
    }

    @Test
    void delete_shouldContinueProcessingWhenAnonymisationThrowsException() {
        Duration duration = Duration.ofDays(1);
        when(config.getBufferDuration()).thenReturn(duration);
        UserAccountEntity userAccount = mock(UserAccountEntity.class);
        when(userIdentity.getUserAccount()).thenReturn(userAccount);
        OffsetDateTime now = OffsetDateTime.now();
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(now);

        when(caseRepository.findCaseIdsToBeAnonymised(any(), any()))
            .thenReturn(List.of(10, 20, 30));

        // First case throws, others succeed
        RuntimeException anonymiseFailure = new RuntimeException("failure on first case");
        org.mockito.Mockito.doThrow(anonymiseFailure)
            .when(dataAnonymisationService).anonymiseCourtCaseById(userAccount, 10, false);

        caseExpiryDeleter.delete(3);

        // First case anonymisation attempted but fails, so no media unlink for that case
        verify(dataAnonymisationService).anonymiseCourtCaseById(userAccount, 10, false);
        // Other cases should still be processed fully
        verify(dataAnonymisationService).anonymiseCourtCaseById(userAccount, 20, false);
        verify(dataAnonymisationService).anonymiseCourtCaseById(userAccount, 30, false);

        // removeMediaLinkToHearing should be invoked only for successful anonymisations (20 and 30)
        verify(hearingsService, times(0)).removeMediaLinkToHearing(10);
        verify(hearingsService).removeMediaLinkToHearing(20);
        verify(hearingsService).removeMediaLinkToHearing(30);

        verify(caseRepository).findCaseIdsToBeAnonymised(now.minus(duration), Limit.of(3));
    }

    @Test
    void delete_shouldUseProvidedBatchSizeInLimit() {
        Duration duration = Duration.ofHours(6);
        when(config.getBufferDuration()).thenReturn(duration);
        UserAccountEntity userAccount = mock(UserAccountEntity.class);
        when(userIdentity.getUserAccount()).thenReturn(userAccount);
        OffsetDateTime now = OffsetDateTime.now();
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(now);

        when(caseRepository.findCaseIdsToBeAnonymised(any(), any()))
            .thenReturn(List.of(100));

        int batchSize = 25;
        caseExpiryDeleter.delete(batchSize);

        verify(caseRepository).findCaseIdsToBeAnonymised(now.minus(duration), Limit.of(batchSize));
        verify(dataAnonymisationService).anonymiseCourtCaseById(userAccount, 100, false);
        verify(hearingsService).removeMediaLinkToHearing(100);
    }

    @Test
    void delete_shouldCalculateMaxRetentionDateUsingConfigBuffer() {
        Duration duration = Duration.ofDays(7);
        when(config.getBufferDuration()).thenReturn(duration);
        UserAccountEntity userAccount = mock(UserAccountEntity.class);
        when(userIdentity.getUserAccount()).thenReturn(userAccount);
        OffsetDateTime fixedNow = OffsetDateTime.parse("2026-03-16T10:00:00Z");
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(fixedNow);

        when(caseRepository.findCaseIdsToBeAnonymised(any(), any()))
            .thenReturn(List.of());

        caseExpiryDeleter.delete(1);

        OffsetDateTime expectedMaxRetentionDate = fixedNow.minus(duration);
        verify(caseRepository).findCaseIdsToBeAnonymised(expectedMaxRetentionDate, Limit.of(1));
    }
}

