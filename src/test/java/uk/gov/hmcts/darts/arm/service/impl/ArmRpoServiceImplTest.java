package uk.gov.hmcts.darts.arm.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.ArmRpoStateEntity;
import uk.gov.hmcts.darts.common.entity.ArmRpoStatusEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsException;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.ArmRpoExecutionDetailRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArmRpoServiceImplTest {

    @Mock
    private CurrentTimeHelper currentTimeHelper;

    @Mock
    private ArmRpoExecutionDetailRepository armRpoExecutionDetailRepository;

    @InjectMocks
    private ArmRpoServiceImpl armRpoService;

    private ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity;
    private ArmRpoStateEntity armRpoStateEntity;
    private ArmRpoStatusEntity armRpoStatusEntity;
    private UserAccountEntity userAccountEntity;

    @BeforeEach
    void setUp() {
        armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        armRpoStateEntity = new ArmRpoStateEntity();
        armRpoStatusEntity = new ArmRpoStatusEntity();
        userAccountEntity = new UserAccountEntity();
    }

    @Test
    void getArmRpoExecutionDetailEntity_ShouldReturnEntity_WhenFound() {
        // given
        when(armRpoExecutionDetailRepository.findById(anyInt())).thenReturn(Optional.of(armRpoExecutionDetailEntity));

        // when
        ArmRpoExecutionDetailEntity result = armRpoService.getArmRpoExecutionDetailEntity(1);

        // then
        assertNotNull(result);
        assertEquals(armRpoExecutionDetailEntity, result);
    }

    @Test
    void getArmRpoExecutionDetailEntity_ShouldThrowException_WhenNotFound() {
        // when
        when(armRpoExecutionDetailRepository.findById(anyInt())).thenReturn(Optional.empty());

        // then
        assertThrows(DartsException.class, () -> armRpoService.getArmRpoExecutionDetailEntity(1));
    }

    @Test
    void updateArmRpoStateAndStatus_ShouldUpdateStateAndStatus() {
        // when
        armRpoService.updateArmRpoStateAndStatus(armRpoExecutionDetailEntity, armRpoStateEntity, armRpoStatusEntity, userAccountEntity);

        // then
        assertEquals(armRpoStateEntity, armRpoExecutionDetailEntity.getArmRpoState());
        assertEquals(armRpoStatusEntity, armRpoExecutionDetailEntity.getArmRpoStatus());
        verify(armRpoExecutionDetailRepository, times(1)).save(armRpoExecutionDetailEntity);
    }

    @Test
    void updateArmRpoStatus_ShouldUpdateStatus() {
        // given
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(null);

        // when
        armRpoService.updateArmRpoStatus(armRpoExecutionDetailEntity, armRpoStatusEntity, userAccountEntity);

        // then
        assertEquals(armRpoStatusEntity, armRpoExecutionDetailEntity.getArmRpoStatus());
        assertEquals(userAccountEntity, armRpoExecutionDetailEntity.getLastModifiedBy());
        verify(armRpoExecutionDetailRepository, times(1)).save(armRpoExecutionDetailEntity);
    }

    @Test
    void saveArmRpoExecutionDetailEntity_ShouldSaveEntity() {
        // given
        when(armRpoExecutionDetailRepository.save(any(ArmRpoExecutionDetailEntity.class))).thenReturn(armRpoExecutionDetailEntity);

        // when
        ArmRpoExecutionDetailEntity result = armRpoService.saveArmRpoExecutionDetailEntity(armRpoExecutionDetailEntity);

        // then
        assertNotNull(result);
        assertEquals(armRpoExecutionDetailEntity, result);
        verify(armRpoExecutionDetailRepository, times(1)).save(armRpoExecutionDetailEntity);
    }
}