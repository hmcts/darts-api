package uk.gov.hmcts.darts.arm.component.impl;

import feign.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.client.ArmRpoClient;
import uk.gov.hmcts.darts.arm.exception.ArmRpoException;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelperMocks;
import uk.gov.hmcts.darts.arm.service.ArmRpoService;
import uk.gov.hmcts.darts.common.entity.ArmAutomatedTaskEntity;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.AutomatedTaskEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.repository.ArmAutomatedTaskRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.service.impl.EodHelperMocks;
import uk.gov.hmcts.darts.common.util.EodHelper;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Slf4j
@SuppressWarnings("PMD.CloseResource")
class StubbedArmRpoDownloadProductionImplTest {
    @Mock
    private ArmRpoClient armRpoClient;

    @Mock
    private ArmAutomatedTaskRepository armAutomatedTaskRepository;

    @Mock
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;

    @Mock
    private ArmRpoService armRpoService;

    @InjectMocks
    private StubbedArmRpoDownloadProductionImpl stubbedArmRpoDownloadProduction;

    private static final ArmRpoHelperMocks ARM_RPO_HELPER_MOCKS = new ArmRpoHelperMocks();
    private static final EodHelperMocks EOD_HELPER_MOCKS = new EodHelperMocks();

    @Test
    void downloadProduction_shouldThrowException_whenAutomatedTaskNotFound() {
        // given
        when(armAutomatedTaskRepository.findByAutomatedTask_taskName(anyString()))
            .thenReturn(Optional.empty());

        // when
        ArmRpoException exception = assertThrows(ArmRpoException.class, () ->
            stubbedArmRpoDownloadProduction.downloadProduction("token", 1, "fileId"));

        // then
        assertTrue(exception.getMessage().contains("Unable to find ARM automated task"));
    }

    @Test
    void downloadProduction_shouldThrowException_whenNoEodsFound() {
        // given
        ArmAutomatedTaskEntity taskEntity = createArmAutomatedTaskEntity();
        when(armAutomatedTaskRepository.findByAutomatedTask_taskName(anyString()))
            .thenReturn(Optional.of(taskEntity));

        when(armRpoService.getArmRpoExecutionDetailEntity(anyInt()))
            .thenReturn(createArmRpoExecutionDetailEntity());
        when(externalObjectDirectoryRepository.findAllByStatusAndDataIngestionTsBetweenAndLimit(
            any(), any(), any(), any()))
            .thenReturn(Collections.emptyList());

        // when
        ArmRpoException exception = assertThrows(ArmRpoException.class, () ->
            stubbedArmRpoDownloadProduction.downloadProduction("token", 1, "fileId"));

        // then
        assertTrue(exception.getMessage().contains("No EODS found"));
    }


    @Test
    void downloadProduction_shouldReturnResponse_whenEodsFound() {
        // given
        ArmAutomatedTaskEntity taskEntity = createArmAutomatedTaskEntity();
        when(armAutomatedTaskRepository.findByAutomatedTask_taskName(anyString())).thenReturn(Optional.of(taskEntity));

        ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity = createArmRpoExecutionDetailEntity();
        when(armRpoService.getArmRpoExecutionDetailEntity(anyInt())).thenReturn(armRpoExecutionDetailEntity);

        ExternalObjectDirectoryEntity eod = createExternalObjectDirectoryEntity();
        when(externalObjectDirectoryRepository.findAllByStatusAndDataIngestionTsBetweenAndLimit(
            any(), any(), any(), any()))
            .thenReturn(Collections.singletonList(eod));

        Response response = mock(Response.class);
        when(armRpoClient.downloadProduction(anyString(), anyString(), anyString()))
            .thenReturn(response);

        Response result = stubbedArmRpoDownloadProduction.downloadProduction("token", 1, "fileId");

        assertNotNull(result);
        verify(armRpoClient).downloadProduction(anyString(), anyString(), anyString());
    }

    private ExternalObjectDirectoryEntity createExternalObjectDirectoryEntity() {
        ExternalObjectDirectoryEntity eod = new ExternalObjectDirectoryEntity();
        eod.setId(1);
        eod.setStatus(EodHelper.armRpoPendingStatus());
        return eod;
    }

    private ArmRpoExecutionDetailEntity createArmRpoExecutionDetailEntity() {
        ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoExecutionDetailEntity.setId(1);
        armRpoExecutionDetailEntity.setCreatedDateTime(OffsetDateTime.now().minusHours(26));
        return armRpoExecutionDetailEntity;
    }

    private ArmAutomatedTaskEntity createArmAutomatedTaskEntity() {
        ArmAutomatedTaskEntity taskEntity = mock(ArmAutomatedTaskEntity.class);
        taskEntity.setId(1);
        taskEntity.setAutomatedTask(mock(AutomatedTaskEntity.class));
        taskEntity.setRpoCsvStartHour(25);
        taskEntity.setRpoCsvEndHour(49);
        return taskEntity;
    }

    @AfterAll
    static void close() {
        ARM_RPO_HELPER_MOCKS.close();
        EOD_HELPER_MOCKS.close();
    }
}