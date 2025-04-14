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
            userIdentity
        );
    }

    @Test
    void delete_should() {
        Duration duration = Duration.ofHours(24);
        when(config.getBufferDuration()).thenReturn(duration);
        UserAccountEntity userAccount = mock(UserAccountEntity.class);
        when(userIdentity.getUserAccount()).thenReturn(userAccount);
        OffsetDateTime offsetDateTime = OffsetDateTime.now();
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(offsetDateTime);

        when(caseRepository.findCaseIdsToBeAnonymised(any(), any()))
            .thenReturn(List.of(1, 2, 3));

        caseExpiryDeleter.delete(config, 5);

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
}