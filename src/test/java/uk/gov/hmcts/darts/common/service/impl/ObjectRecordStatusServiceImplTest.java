package uk.gov.hmcts.darts.common.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;
import uk.gov.hmcts.darts.common.exception.CommonApiError;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ObjectRecordStatusServiceImplTest {

    @Mock
    private ObjectRecordStatusRepository objectRecordStatusRepository;

    @InjectMocks
    private ObjectRecordStatusServiceImpl objectRecordStatusService;


    @Test
    void positiveGetObjectRecordStatusEntity() {
        ObjectRecordStatusEntity objectRecordStatusEntity = new ObjectRecordStatusEntity();
        when(objectRecordStatusRepository.findById(1)).thenReturn(Optional.of(objectRecordStatusEntity));
        assertThat(objectRecordStatusService.getObjectRecordStatusEntity(ObjectRecordStatusEnum.NEW))
            .isEqualTo(objectRecordStatusEntity);
        verify(objectRecordStatusRepository).findById(1);
    }

    @Test
    void negativeGetObjectRecordStatusEntityNotFound() {
        when(objectRecordStatusRepository.findById(1)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> objectRecordStatusService.getObjectRecordStatusEntity(ObjectRecordStatusEnum.NEW))
            .isInstanceOf(DartsApiException.class)
            .hasMessage("Resource not found. Failed to find Object record with status 'NEW'")
            .hasFieldOrPropertyWithValue("error", CommonApiError.NOT_FOUND);
    }
}
