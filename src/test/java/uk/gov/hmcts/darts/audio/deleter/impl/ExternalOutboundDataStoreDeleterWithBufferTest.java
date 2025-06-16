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
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.TransformedMediaRepository;
import uk.gov.hmcts.darts.common.repository.TransientObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.common.service.impl.EodHelperMocks;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExternalOutboundDataStoreDeleterWithBufferTest {
    private static final Duration BUFFER_DURATION = Duration.ofDays(90);

    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private TransientObjectDirectoryRepository transientObjectDirectoryRepository;
    private ExternalOutboundDataStoreDeleterWithBuffer deleter;
    @Mock
    private DataManagementApi dataManagementApi;
    @Mock
    private TransformedMediaRepository transformedMediaRepository;
    @Mock
    private CurrentTimeHelper currentTimeHelper;
    private OffsetDateTime currentTime;
    private EodHelperMocks eodHelperMocks;

    @BeforeEach
    void setUp() {
        eodHelperMocks = new EodHelperMocks();
        this.deleter = spy(new ExternalOutboundDataStoreDeleterWithBuffer(
            transientObjectDirectoryRepository,
            transformedMediaRepository,
            dataManagementApi,
            currentTimeHelper,
            BUFFER_DURATION
        ));
        currentTime = OffsetDateTime.now();
        lenient().when(currentTimeHelper.currentOffsetDateTime()).thenReturn(currentTime);
    }

    @AfterEach
    void afterEach() {
        eodHelperMocks.close();
    }

    private List<TransientObjectDirectoryEntity> createTransientObjectDirectoryEntities() {
        TransientObjectDirectoryEntity transientObjectDirectoryEntity1 = new TransientObjectDirectoryEntity();
        transientObjectDirectoryEntity1.setId(1L);//Esures equality checks are unique
        transientObjectDirectoryEntity1.setStatus(eodHelperMocks.getMarkForDeletionStatus());
        TransformedMediaEntity transformedMediaEntity1 = new TransformedMediaEntity();
        transformedMediaEntity1.setId(1);//Esures equality checks are unique
        transientObjectDirectoryEntity1.setTransformedMedia(transformedMediaEntity1);

        TransientObjectDirectoryEntity transientObjectDirectoryEntity2 = new TransientObjectDirectoryEntity();
        transientObjectDirectoryEntity2.setId(3L);//Esures equality checks are unique
        transientObjectDirectoryEntity2.setStatus(eodHelperMocks.getMarkForDeletionStatus());
        TransformedMediaEntity transformedMediaEntity2 = new TransformedMediaEntity();
        transformedMediaEntity2.setId(4);//Esures equality checks are unique
        transientObjectDirectoryEntity2.setTransformedMedia(transformedMediaEntity2);
        return List.of(transientObjectDirectoryEntity1, transientObjectDirectoryEntity2);
    }


    @Test
    void delete_shouldDeleteFromDatastore_andUpdateStatusToDatastoreDeleted() {
        doNothing().when(deleter).deleteExpiredTransientObjectEntities(any());

        //Given

        List<TransientObjectDirectoryEntity> outboundData = createTransientObjectDirectoryEntities();
        when(transientObjectDirectoryRepository.findByStatus(any(), any())).thenReturn(outboundData);

        //Then

        Collection<TransientObjectDirectoryEntity> directoryEntityList = deleter.delete(10);

        //When
        assertThat(directoryEntityList)
            .hasSize(2)
            .containsAll(outboundData);

        assertThat(outboundData.get(0).getStatus())
            .isEqualTo(eodHelperMocks.getDatastoreDeletionStatus());
        assertThat(outboundData.get(1).getStatus())
            .isEqualTo(eodHelperMocks.getDatastoreDeletionStatus());

        verify(deleter).deleteExpiredTransientObjectEntities(10);
        verify(transientObjectDirectoryRepository)
            .findByStatus(eodHelperMocks.getMarkForDeletionStatus(), Limit.of(10));

        //Checl the status update is saved
        verify(transientObjectDirectoryRepository)
            .save(outboundData.get(0));
        verify(transientObjectDirectoryRepository)
            .save(outboundData.get(1));
        verifyNoMoreInteractions(transientObjectDirectoryRepository, transformedMediaRepository);
    }


    @Test
    void deleteExpiredTransientObjectEntities_shouldDeleteBothTransformedMediaAndTransientObjectDirectories() {
        //Given
        List<TransientObjectDirectoryEntity> expiredEntities = new ArrayList<>(createTransientObjectDirectoryEntities());

        //Add an entry with a null transformed media to ensure it does not exception
        TransientObjectDirectoryEntity transientObjectDirectoryEntity3 = new TransientObjectDirectoryEntity();
        transientObjectDirectoryEntity3.setId(5L);//Esures equality checks are unique

        expiredEntities.add(transientObjectDirectoryEntity3);

        doReturn(expiredEntities).when(transientObjectDirectoryRepository)
            .findByTransformedMediaIsNullOrExpirtyBeforeMaxExpiryTime(any(), any(), any());

        //When
        deleter.deleteExpiredTransientObjectEntities(10);

        //Then
        verify(transientObjectDirectoryRepository)
            .findByTransformedMediaIsNullOrExpirtyBeforeMaxExpiryTime(
                currentTime.minus(BUFFER_DURATION),
                ObjectRecordStatusEnum.DATASTORE_DELETED.getId(),
                Limit.of(10)
            );

        //Should only attempt to delete the transformed media that is not null
        verify(transformedMediaRepository).deleteAll(
            List.of(expiredEntities.get(0).getTransformedMedia(),
                    expiredEntities.get(1).getTransformedMedia()));
        //Should delete all transient object directories
        verify(transientObjectDirectoryRepository).deleteAll(expiredEntities);
    }

    @Test
    void datastoreDeletionCallback_shouldUpdateTransientObjectDirectoryStatusToDatastoreDeleted() {
        TransientObjectDirectoryEntity transientObjectDirectoryEntity = new TransientObjectDirectoryEntity();

        assertThat(transientObjectDirectoryEntity.getStatus())
            .isNull();
        deleter.datastoreDeletionCallback(transientObjectDirectoryEntity);

        assertThat(transientObjectDirectoryEntity.getStatus())
            .isEqualTo(eodHelperMocks.getDatastoreDeletionStatus());
        verify(transientObjectDirectoryRepository).save(transientObjectDirectoryEntity);
        verifyNoMoreInteractions(transientObjectDirectoryRepository);
        verifyNoInteractions(transformedMediaRepository);
    }
}