package uk.gov.hmcts.darts.dets.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.ObjectStateRecordEntity;
import uk.gov.hmcts.darts.common.repository.ObjectStateRecordRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ObjectStateRecordServiceImplTest {
    @Mock
    private ObjectStateRecordRepository objectStateRecordRepository;

    @InjectMocks
    private ObjectStateRecordServiceImpl objectStateRecordService;


    @Test
    void testGetObjectStateRecordEntityById_Success() {
        Long objectStateRecordId = 1L;
        ObjectStateRecordEntity objectStateRecordEntity = new ObjectStateRecordEntity();
        objectStateRecordEntity.setUuid(objectStateRecordId);

        when(objectStateRecordRepository.findById(objectStateRecordId)).thenReturn(Optional.of(objectStateRecordEntity));

        ObjectStateRecordEntity result = objectStateRecordService.getObjectStateRecordEntityById(objectStateRecordId);

        assertNotNull(result);
        assertEquals(objectStateRecordId, result.getUuid());
        verify(objectStateRecordRepository).findById(objectStateRecordId);
    }

    @Test
    void testGetObjectStateRecordEntityById_NotFound() {
        Long objectStateRecordId = 1L;

        when(objectStateRecordRepository.findById(objectStateRecordId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            objectStateRecordService.getObjectStateRecordEntityById(objectStateRecordId);
        });

        assertEquals("ObjectStateRecordEntity with id " + objectStateRecordId + " not found", exception.getMessage());
        verify(objectStateRecordRepository).findById(objectStateRecordId);
    }
}