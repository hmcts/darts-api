package uk.gov.hmcts.darts.audio.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;
import uk.gov.hmcts.darts.audio.deleter.impl.ExternalOutboundDataStoreDeleterWithBuffer;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExternalOutboundDataStoreDeleterWithBufferTest {
    private static final Duration BUFFER_DURATION = Duration.ofDays(90);

    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private TransientObjectDirectoryRepository transientObjectDirectoryRepository;
    private ExternalOutboundDataStoreDeleterWithBuffer deleter;
    private ObjectRecordStatusEntity markedForDeletionStatus;
    @Mock
    private DataManagementApi dataManagementApi;
    @Mock
    private TransformedMediaRepository transformedMediaRepository;
    @Mock
    private CurrentTimeHelper currentTimeHelper;
    private OffsetDateTime currentTime;

    @BeforeEach
    void setUp() {
        this.deleter = new ExternalOutboundDataStoreDeleterWithBuffer(
            transientObjectDirectoryRepository,
            transformedMediaRepository,
            dataManagementApi,
            currentTimeHelper,
            BUFFER_DURATION
        );
        currentTime = OffsetDateTime.now();
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(currentTime);
    }

    private void mockStatus() {
        markedForDeletionStatus = new ObjectRecordStatusEntity();
        markedForDeletionStatus.setId(ObjectRecordStatusEnum.MARKED_FOR_DELETION.getId());
    }

    private List<TransientObjectDirectoryEntity> createOutboundData(OffsetDateTime expirtyTs) {
        TransientObjectDirectoryEntity outboundAudio = new TransientObjectDirectoryEntity();
        outboundAudio.setStatus(markedForDeletionStatus);
        Long id1 = 1L;
        outboundAudio.setId(id1);
        outboundAudio.setTransformedMedia(createTransformedMedia(id1.intValue(), expirtyTs));

        TransientObjectDirectoryEntity outboundAudio2 = new TransientObjectDirectoryEntity();
        outboundAudio2.setStatus(markedForDeletionStatus);
        Long id2 = 21L;
        outboundAudio2.setId(id2);
        outboundAudio2.setTransformedMedia(createTransformedMedia(id2.intValue(), expirtyTs));

        List<TransientObjectDirectoryEntity> outboundList = new ArrayList<>();
        outboundList.add(outboundAudio);
        outboundList.add(outboundAudio2);
        return outboundList;
    }

    private TransformedMediaEntity createTransformedMedia(int id, OffsetDateTime expirtyTs) {
        TransformedMediaEntity transformedMediaEntity = new TransformedMediaEntity();
        transformedMediaEntity.setId(id);
        transformedMediaEntity.setExpiryTime(expirtyTs);
        return transformedMediaEntity;
    }

    @Test
    void deleteFromOutboundDataStore_whenMediaExpirtyIs91DaysAgo_shouldDeleteTransientObject() {
        mockStatus();

        List<TransientObjectDirectoryEntity> outboundData = createOutboundData(currentTime.minus(Duration.ofDays(91)));

        try (EodHelperMocks eodHelperMocks = new EodHelperMocks()) {

            when(transientObjectDirectoryRepository.findByStatus(any(), any())).thenReturn(outboundData);

            Collection<TransientObjectDirectoryEntity> deletedItems = deleter.delete(100);

            assertThat(deletedItems, containsInAnyOrder(
                hasProperty("id", is(1)),
                hasProperty("id", is(21))
            ));
            assertEquals(2, deletedItems.size());
            verify(transientObjectDirectoryRepository)
                .findByStatus(eodHelperMocks.getMarkForDeletionStatus(), Limit.of(100));

            verify(transientObjectDirectoryRepository).delete(outboundData.get(0));
            verify(transientObjectDirectoryRepository).delete(outboundData.get(1));
            verify(transformedMediaRepository).delete(outboundData.get(0).getTransformedMedia());
            verify(transformedMediaRepository).delete(outboundData.get(1).getTransformedMedia());
        }
    }

    @Test
    void deleteFromOutboundDataStore_whenMediaExpirtyIs81DaysAgo_shouldNotDeleteTransientObjectButShouldDeleteEod() {
        mockStatus();

        List<TransientObjectDirectoryEntity> outboundData = createOutboundData(currentTime.minus(Duration.ofDays(81)));

        try (EodHelperMocks eodHelperMocks = new EodHelperMocks()) {

            when(transientObjectDirectoryRepository.findByStatus(any(), any())).thenReturn(outboundData);

            Collection<TransientObjectDirectoryEntity> deletedItems = deleter.delete(100);

            assertThat(deletedItems, containsInAnyOrder(
                hasProperty("id", is(1)),
                hasProperty("id", is(21))
            ));
            assertEquals(2, deletedItems.size());
            verify(transientObjectDirectoryRepository)
                .findByStatus(eodHelperMocks.getMarkForDeletionStatus(), Limit.of(100));

            verify(transientObjectDirectoryRepository, never()).delete(outboundData.get(0));
            verify(transientObjectDirectoryRepository, never()).delete(outboundData.get(1));
            verify(transformedMediaRepository, never()).delete(outboundData.get(0).getTransformedMedia());
            verify(transformedMediaRepository, never()).delete(outboundData.get(1).getTransformedMedia());
        }
    }
}