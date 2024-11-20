package uk.gov.hmcts.darts.arm.service.impl;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelper;
import uk.gov.hmcts.darts.arm.helper.ArmRpoHelperMocks;
import uk.gov.hmcts.darts.common.entity.ArmRpoExecutionDetailEntity;
import uk.gov.hmcts.darts.common.entity.ArmRpoStateEntity;
import uk.gov.hmcts.darts.common.entity.ArmRpoStatusEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsException;
import uk.gov.hmcts.darts.common.repository.ArmRpoExecutionDetailRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArmRpoServiceImplTest {

    @Mock
    private ArmRpoExecutionDetailRepository armRpoExecutionDetailRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private ArmRpoServiceImpl armRpoService;

    private static final ArmRpoHelperMocks ARM_RPO_HELPER_MOCKS = new ArmRpoHelperMocks();

    private ArmRpoExecutionDetailEntity armRpoExecutionDetailEntity;
    private UserAccountEntity userAccountEntity;

    @BeforeEach
    void setUp() {
        armRpoExecutionDetailEntity = new ArmRpoExecutionDetailEntity();
        userAccountEntity = new UserAccountEntity();
    }

    @Test
    void createArmRpoExecutionDetailEntity_shouldCreateAndSaveExpectedEntityState() {
        // Given
        when(entityManager.merge(userAccountEntity))
            .thenReturn(userAccountEntity);

        var detailEntityCaptor = ArgumentCaptor.forClass(ArmRpoExecutionDetailEntity.class);

        // When
        armRpoService.createArmRpoExecutionDetailEntity(userAccountEntity);

        // Then
        verify(entityManager).merge(userAccountEntity);

        verify(armRpoExecutionDetailRepository).save(detailEntityCaptor.capture());

        var armRpoExecutionDetail = detailEntityCaptor.getValue();
        assertEquals(userAccountEntity, armRpoExecutionDetail.getCreatedBy());
        assertEquals(userAccountEntity, armRpoExecutionDetail.getLastModifiedBy());

        verifyNoMoreInteractions(entityManager, armRpoExecutionDetailRepository);
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
        // given
        ArmRpoStateEntity armRpoStateEntity = ArmRpoHelper.addAsyncSearchRpoState();
        ArmRpoStatusEntity armRpoStatusEntity = ArmRpoHelper.completedRpoStatus();
        armRpoExecutionDetailEntity.setArmRpoStatus(ArmRpoHelper.inProgressRpoStatus());

        // when
        armRpoService.updateArmRpoStateAndStatus(armRpoExecutionDetailEntity,
                                                 armRpoStateEntity,
                                                 ArmRpoHelper.completedRpoStatus(), userAccountEntity);

        // then
        assertEquals(armRpoStateEntity, armRpoExecutionDetailEntity.getArmRpoState());
        assertEquals(armRpoStatusEntity, armRpoExecutionDetailEntity.getArmRpoStatus());
        verify(armRpoExecutionDetailRepository, times(1)).save(armRpoExecutionDetailEntity);
    }

    @Test
    void updateArmRpoStatus_ShouldUpdateStatus() {
        // given
        armRpoExecutionDetailEntity.setArmRpoStatus(ArmRpoHelper.inProgressRpoStatus());

        // when
        armRpoService.updateArmRpoStatus(armRpoExecutionDetailEntity, ArmRpoHelper.failedRpoStatus(), userAccountEntity);

        // then
        assertEquals(ARM_RPO_HELPER_MOCKS.getFailedRpoStatus(), armRpoExecutionDetailEntity.getArmRpoStatus());
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

    @AfterAll
    static void close() {
        ARM_RPO_HELPER_MOCKS.close();
    }
}