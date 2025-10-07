package uk.gov.hmcts.darts.audio.deleter.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.repository.TransformedMediaRepository;
import uk.gov.hmcts.darts.common.repository.TransientObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.service.impl.EodHelperMocks;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExternalOutboundDataStoreDeleterTest {

    @Mock
    private TransientObjectDirectoryRepository transientObjectDirectoryRepository;
    @Mock
    private TransformedMediaRepository transformedMediaRepository;
    @Mock
    private DataManagementApi dataManagementApi;

    private EodHelperMocks eodHelperMocks;
    private ExternalOutboundDataStoreDeleter deleter;

    @BeforeEach
    void setUp() {
        eodHelperMocks = new EodHelperMocks();
        deleter = new ExternalOutboundDataStoreDeleter(
            transientObjectDirectoryRepository,
            transformedMediaRepository,
            dataManagementApi
        );
    }

    @AfterEach
    void tearDown() {
        eodHelperMocks.close();
    }

    @Test
    void deleteFromDataStore_ShouldCallDeleteBlobDataFromOutboundContainer() throws Exception {
        String location = "OutboundLocation";
        doNothing().when(dataManagementApi).deleteBlobDataFromOutboundContainer(location);

        deleter.deleteFromDataStore(location);

        verify(dataManagementApi, times(1)).deleteBlobDataFromOutboundContainer(location);
    }

    @Test
    void findItemsToDelete_ShouldReturnItemsFromRepository() {
        TransientObjectDirectoryEntity entity = mock(TransientObjectDirectoryEntity.class);
        List<TransientObjectDirectoryEntity> expected = List.of(entity);

        when(transientObjectDirectoryRepository.findByStatus(
            any(),
            eq(Limit.of(5))
        )).thenReturn(expected);

        var result = deleter.findItemsToDelete(5);

        verify(transientObjectDirectoryRepository, times(1)).findByStatus(any(), eq(Limit.of(5)));
        assertEquals(expected, result);
    }

    @Test
    void datastoreDeletionCallback_ShouldDeleteTransformedMediaIfPresent() {
        TransientObjectDirectoryEntity entity = mock(TransientObjectDirectoryEntity.class);
        TransformedMediaEntity transformedMedia = mock(TransformedMediaEntity.class);

        when(entity.getTransformedMedia()).thenReturn(transformedMedia);

        deleter.datastoreDeletionCallback(entity);

        verify(transformedMediaRepository, times(1)).delete(transformedMedia);
    }

    @Test
    void datastoreDeletionCallback_ShouldNotDeleteTransformedMediaIfAbsent() {
        TransientObjectDirectoryEntity entity = mock(TransientObjectDirectoryEntity.class);
        when(entity.getTransformedMedia()).thenReturn(null);

        deleter.datastoreDeletionCallback(entity);

        verify(transformedMediaRepository, never()).delete(any());
    }
}