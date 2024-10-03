package uk.gov.hmcts.darts.common.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.ObjectHiddenReasonEntity;
import uk.gov.hmcts.darts.common.mapper.HiddenReasonMapper;
import uk.gov.hmcts.darts.common.model.HiddenReason;
import uk.gov.hmcts.darts.common.repository.ObjectHiddenReasonRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HiddenReasonsServiceTest {

    @Mock
    private ObjectHiddenReasonRepository objectHiddenReasonRepository;
    @Mock
    private HiddenReasonMapper hiddenReasonMapper;
    
    private HiddenReasonsService hiddenReasonsService;


    private void setupHiddenReasonService(boolean manualDeletionEnabled) {
        hiddenReasonsService = new HiddenReasonsService(objectHiddenReasonRepository, hiddenReasonMapper, manualDeletionEnabled);
    }

    private ObjectHiddenReasonEntity createObjectHiddenReasonEntity(boolean isMarkedForDeletion, int displayOrder) {
        ObjectHiddenReasonEntity objectHiddenReasonEntity = new ObjectHiddenReasonEntity();
        objectHiddenReasonEntity.setMarkedForDeletion(isMarkedForDeletion);
        objectHiddenReasonEntity.setDisplayOrder(displayOrder);
        return objectHiddenReasonEntity;
    }

    @Test
    void manualDeletionDisabled() {
        ObjectHiddenReasonEntity objectHiddenReasonEntity1 = createObjectHiddenReasonEntity(false, 1);
        ObjectHiddenReasonEntity objectHiddenReasonEntity2 = createObjectHiddenReasonEntity(true, 2);
        ObjectHiddenReasonEntity objectHiddenReasonEntity3 = createObjectHiddenReasonEntity(false, 3);
        when(objectHiddenReasonRepository.findAll()).thenReturn(List.of(objectHiddenReasonEntity1, objectHiddenReasonEntity2, objectHiddenReasonEntity3));


        List<HiddenReason> expectedReasons = List.of(new HiddenReason(), new HiddenReason());
        when(hiddenReasonMapper.mapToApiModel(any())).thenReturn(expectedReasons);

        setupHiddenReasonService(false);

        assertThat(hiddenReasonsService.getHiddenReasons()).isEqualTo(expectedReasons);
        verify(objectHiddenReasonRepository).findAll();
        verify(hiddenReasonMapper).mapToApiModel(List.of(objectHiddenReasonEntity1, objectHiddenReasonEntity3));
    }

    @Test
    void manualDeletionNotDisabled() {
        ObjectHiddenReasonEntity objectHiddenReasonEntity1 = createObjectHiddenReasonEntity(false, 1);
        ObjectHiddenReasonEntity objectHiddenReasonEntity2 = createObjectHiddenReasonEntity(true, 2);
        ObjectHiddenReasonEntity objectHiddenReasonEntity3 = createObjectHiddenReasonEntity(false, 3);
        when(objectHiddenReasonRepository.findAll()).thenReturn(List.of(objectHiddenReasonEntity1, objectHiddenReasonEntity2, objectHiddenReasonEntity3));


        List<HiddenReason> expectedReasons = List.of(new HiddenReason(), new HiddenReason(), new HiddenReason());
        when(hiddenReasonMapper.mapToApiModel(any())).thenReturn(expectedReasons);

        setupHiddenReasonService(true);

        assertThat(hiddenReasonsService.getHiddenReasons()).isEqualTo(expectedReasons);
        verify(objectHiddenReasonRepository).findAll();
        verify(hiddenReasonMapper).mapToApiModel(List.of(objectHiddenReasonEntity1, objectHiddenReasonEntity2, objectHiddenReasonEntity3));
    }
}
