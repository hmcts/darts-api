package uk.gov.hmcts.darts.arm.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.ArmRpoStatusEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsException;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.ArmRpoExecutionDetailRepository;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArmRpoServiceImplTest {

    @Mock
    private CurrentTimeHelper currentTimeHelper;

    @Mock
    private UserIdentity userIdentity;

    @Mock
    private ArmRpoExecutionDetailRepository armRpoExecutionDetailRepository;

    @InjectMocks
    private ArmRpoServiceImpl armRpoService;


    @Test
    void getArmRpoExecutionDetailEntityReturnsEntityWhenFound() {
        Integer executionId = 1;
        ArmRpoExecutionDetailEntity expectedEntity = new ArmRpoExecutionDetailEntity();
        when(armRpoExecutionDetailRepository.findById(executionId)).thenReturn(Optional.of(expectedEntity));

        ArmRpoExecutionDetailEntity result = armRpoService.getArmRpoExecutionDetailEntity(executionId);

        assertEquals(expectedEntity, result);
    }

    @Test
    void getArmRpoExecutionDetailEntityThrowsExceptionWhenNotFound() {
        Integer executionId = 1;
        when(armRpoExecutionDetailRepository.findById(executionId)).thenReturn(Optional.empty());

        DartsException exception = assertThrows(DartsException.class, () -> armRpoService.getArmRpoExecutionDetailEntity(executionId));

        assertEquals(ArmRpoServiceImpl.ARM_RPO_EXECUTION_DETAIL_NOT_FOUND, exception.getMessage());
    }

    @Test
    void updateArmRpoExecutionDetailsUpdatesEntity() {
        Integer executionId = 1;
        ArmRpoStatusEntity armRpoStatus = new ArmRpoStatusEntity();
        ArmRpoExecutionDetailEntity entity = new ArmRpoExecutionDetailEntity();
        when(armRpoExecutionDetailRepository.findById(executionId)).thenReturn(Optional.of(entity));
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(OffsetDateTime.now());
        when(userIdentity.getUserAccount()).thenReturn(new UserAccountEntity());

        armRpoService.updateArmRpoExecutionDetails(executionId, armRpoStatus);

        assertEquals(armRpoStatus, entity.getArmRpoStatus());
        assertNotNull(entity.getLastModifiedDateTime());
        assertNotNull(entity.getLastModifiedBy());
        verify(armRpoExecutionDetailRepository).save(entity);
    }
}