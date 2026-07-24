package uk.gov.hmcts.darts.audio.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;
import uk.gov.hmcts.darts.common.exception.DartsException;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.datamanagement.config.DataManagementConfiguration;
import uk.gov.hmcts.darts.datamanagement.service.DataManagementService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InboundAudioFailureCorrectionServiceImplTest {

    private static final String INBOUND_CONTAINER_NAME = "darts-inbound-container";
    private static final int BATCH_SIZE = 25;
    private static final int USER_ID = 987;

    private ObjectRecordStatusEntity failureStatus;

    @Mock
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    @Mock
    private DataManagementService dataManagementService;
    @Mock
    private DataManagementConfiguration dataManagementConfiguration;
    @Mock
    private UserIdentity userIdentity;

    @InjectMocks
    private InboundAudioFailureCorrectionServiceImpl service;

    @BeforeEach
    void beforeEach() {
        ExternalLocationTypeEntity inboundLocation = new ExternalLocationTypeEntity();
        inboundLocation.setId(ExternalLocationTypeEnum.INBOUND.getId());
        inboundLocation.setDescription(ExternalLocationTypeEnum.INBOUND.name());

        failureStatus = new ObjectRecordStatusEntity();
        failureStatus.setId(ObjectRecordStatusEnum.FAILURE.getId());
        failureStatus.setDescription(ObjectRecordStatusEnum.FAILURE.name());

        ReflectionTestUtils.setField(EodHelper.class, "inboundLocation", inboundLocation);
        ReflectionTestUtils.setField(EodHelper.class, "failureStatus", failureStatus);

        UserAccountEntity userAccount = new UserAccountEntity();
        userAccount.setId(USER_ID);
        when(userIdentity.getUserAccount()).thenReturn(userAccount);
    }

    @Test
    void correctAudioFailure_restoresBlobVersionAndUpdatesEodStatus_whenFailedEodHasExternalLocation() {
        ExternalObjectDirectoryEntity failedEod = createExternalObjectDirectory(123L, "audio-blob-id");
        when(externalObjectDirectoryRepository.findFailedAudiosWithMaxAttempts(any(ExternalLocationTypeEntity.class),
                                                                               any(ObjectRecordStatusEntity.class),
                                                                               eq(InboundAudioFailureCorrectionServiceImpl.MAX_ATTEMPTS),
                                                                               eq(BATCH_SIZE)))
            .thenReturn(List.of(failedEod));
        when(dataManagementConfiguration.getInboundContainerName()).thenReturn(INBOUND_CONTAINER_NAME);

        service.correctAudioFailure(BATCH_SIZE);

        verify(userIdentity).getUserAccount();
        verify(dataManagementService).restoreBlobVersion(INBOUND_CONTAINER_NAME, "audio-blob-id");
        verify(externalObjectDirectoryRepository).updateEodStatusAndTransferAttemptsWhereIdIn(
            failureStatus,
            0,
            USER_ID,
            List.of(123L)
        );
    }

    @Test
    void correctAudioFailure_doesNotRestoreBlobVersion_whenFailedEodHasNoExternalLocation() {
        ExternalObjectDirectoryEntity failedEod = createExternalObjectDirectory(123L, null);
        when(externalObjectDirectoryRepository.findFailedAudiosWithMaxAttempts(any(ExternalLocationTypeEntity.class),
                                                                               any(ObjectRecordStatusEntity.class),
                                                                               eq(InboundAudioFailureCorrectionServiceImpl.MAX_ATTEMPTS),
                                                                               eq(BATCH_SIZE)))
            .thenReturn(List.of(failedEod));

        service.correctAudioFailure(BATCH_SIZE);

        verify(userIdentity).getUserAccount();
        verify(dataManagementService, never()).restoreBlobVersion(anyString(), anyString());
        verify(externalObjectDirectoryRepository, never()).updateEodStatusAndTransferAttemptsWhereIdIn(
            failureStatus,
            0,
            USER_ID,
            List.of(123L)
        );
    }

    @Test
    void correctAudioFailure_continuesProcessing_whenRestoreBlobVersionThrowsException() {
        ExternalObjectDirectoryEntity failedEod = createExternalObjectDirectory(123L, "audio-blob-id");
        ExternalObjectDirectoryEntity recoverableEod = createExternalObjectDirectory(456L, "next-audio-blob-id");
        when(externalObjectDirectoryRepository.findFailedAudiosWithMaxAttempts(any(ExternalLocationTypeEntity.class),
                                                                               any(ObjectRecordStatusEntity.class),
                                                                               eq(InboundAudioFailureCorrectionServiceImpl.MAX_ATTEMPTS),
                                                                               eq(BATCH_SIZE)))
            .thenReturn(List.of(failedEod, recoverableEod));
        when(dataManagementConfiguration.getInboundContainerName()).thenReturn(INBOUND_CONTAINER_NAME);
        doThrow(new DartsException("Restore failed")).when(dataManagementService).restoreBlobVersion(INBOUND_CONTAINER_NAME, "audio-blob-id");

        service.correctAudioFailure(BATCH_SIZE);

        verify(dataManagementService).restoreBlobVersion(INBOUND_CONTAINER_NAME, "audio-blob-id");
        verify(dataManagementService).restoreBlobVersion(INBOUND_CONTAINER_NAME, "next-audio-blob-id");
        verify(externalObjectDirectoryRepository, never()).updateEodStatusAndTransferAttemptsWhereIdIn(
            failureStatus,
            0,
            USER_ID,
            List.of(123L)
        );
        verify(externalObjectDirectoryRepository).updateEodStatusAndTransferAttemptsWhereIdIn(
            failureStatus,
            0,
            USER_ID,
            List.of(456L)
        );
    }

    private ExternalObjectDirectoryEntity createExternalObjectDirectory(Long id, String externalLocation) {
        ExternalObjectDirectoryEntity externalObjectDirectory = new ExternalObjectDirectoryEntity();
        externalObjectDirectory.setId(id);
        externalObjectDirectory.setExternalLocation(externalLocation);
        return externalObjectDirectory;
    }
}
