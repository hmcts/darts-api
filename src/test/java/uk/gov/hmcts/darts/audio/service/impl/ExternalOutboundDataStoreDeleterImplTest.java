package uk.gov.hmcts.darts.audio.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audio.deleter.impl.outbound.ExternalOutboundDataStoreDeleter;
import uk.gov.hmcts.darts.audio.deleter.impl.outbound.OutboundDataStoreDeleter;
import uk.gov.hmcts.darts.audio.deleter.impl.outbound.OutboundExternalObjectDirectoryDeletedFinder;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;
import uk.gov.hmcts.darts.common.repository.TransformedMediaRepository;
import uk.gov.hmcts.darts.common.repository.TransientObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExternalOutboundDataStoreDeleterImplTest {

    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private TransientObjectDirectoryRepository transientObjectDirectoryRepository;
    private ExternalOutboundDataStoreDeleter deleter;
    private ObjectRecordStatusEntity markedForDeletionStatus;
    @Mock
    private OutboundExternalObjectDirectoryDeletedFinder finder;
    @Mock
    private OutboundDataStoreDeleter outboundDataStoreDeleter;
    @Mock
    private TransformedMediaRepository transformedMediaRepository;

    @BeforeEach
    void setUp() {
        this.deleter = new ExternalOutboundDataStoreDeleter(
            transientObjectDirectoryRepository,
            finder,
            outboundDataStoreDeleter,
            transformedMediaRepository
        );
    }

    private void mockStatus() {
        markedForDeletionStatus = new ObjectRecordStatusEntity();
        markedForDeletionStatus.setId(ObjectRecordStatusEnum.MARKED_FOR_DELETION.getId());
    }

    private List<TransientObjectDirectoryEntity> createOutboundData() {
        TransientObjectDirectoryEntity outboundAudio = new TransientObjectDirectoryEntity();
        outboundAudio.setStatus(markedForDeletionStatus);
        int id1 = 1;
        outboundAudio.setId(id1);
        outboundAudio.setTransformedMedia(createTransformedMedia(id1));

        TransientObjectDirectoryEntity outboundAudio2 = new TransientObjectDirectoryEntity();
        outboundAudio2.setStatus(markedForDeletionStatus);
        int id2 = 21;
        outboundAudio2.setId(id2);
        outboundAudio2.setTransformedMedia(createTransformedMedia(id2));

        List<TransientObjectDirectoryEntity> outboundList = new ArrayList<>();
        outboundList.add(outboundAudio);
        outboundList.add(outboundAudio2);
        return outboundList;
    }

    private TransformedMediaEntity createTransformedMedia(int id) {
        TransformedMediaEntity transformedMediaEntity = new TransformedMediaEntity();
        transformedMediaEntity.setId(id);
        return transformedMediaEntity;
    }

    @Test
    void deleteFromOutboundDataStore() {
        mockStatus();

        List<TransientObjectDirectoryEntity> outboundData = createOutboundData();

        when(finder.findMarkedForDeletion()).thenReturn(outboundData);

        List<TransientObjectDirectoryEntity> deletedItems = deleter.delete();

        assertThat(deletedItems, containsInAnyOrder(
            hasProperty("id", is(1)),
            hasProperty("id", is(21))
        ));
        assertEquals(2, deletedItems.size());

    }

}