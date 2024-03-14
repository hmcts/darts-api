package uk.gov.hmcts.darts.audio.service.impl;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audio.deleter.impl.inbound.ExternalInboundDataStoreDeleter;
import uk.gov.hmcts.darts.audio.deleter.impl.inbound.InboundDataStoreDeleter;
import uk.gov.hmcts.darts.audio.deleter.impl.inbound.InboundExternalObjectDirectoryDeletedFinder;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;
import uk.gov.hmcts.darts.common.exception.AzureDeleteBlobException;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.TransformedMediaRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExternalInboundDataStoreDeleterImplTest {
    private ExternalInboundDataStoreDeleter deleter;

    @Mock
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;

    private ObjectRecordStatusEntity markedForDeletionStatus;

    @Mock
    private InboundExternalObjectDirectoryDeletedFinder finder;
    @Mock
    private InboundDataStoreDeleter inboundDataStoreDeleter;
    @Mock
    private TransformedMediaRepository transformedMediaRepository;

    @BeforeEach
    public void setUp() {
        mockStatus();
        List<ExternalObjectDirectoryEntity> inboundData = createInboundData();
        when(finder.findMarkedForDeletion()).thenReturn(inboundData);

        this.deleter = new ExternalInboundDataStoreDeleter(
            externalObjectDirectoryRepository,
            finder,
            inboundDataStoreDeleter,
            transformedMediaRepository
        );

    }

    private void mockStatus() {
        markedForDeletionStatus = new ObjectRecordStatusEntity();
        markedForDeletionStatus.setId(ObjectRecordStatusEnum.MARKED_FOR_DELETION.getId());
    }

    @Test
    void deleteFromInboundDatastore() {
        List<ExternalObjectDirectoryEntity> deletedItems = deleter.delete();

        assertThat(
            deletedItems,
            containsInAnyOrder(
                allOf(
                    Matchers.hasProperty("id", is(1))
                ),
                allOf(
                    Matchers.hasProperty("id", is(2))
                )
            )
        );
        assertEquals(2, deletedItems.size());
    }

    @Test
    void deleteFromInboundDatastoreShouldNotThrowAzureDeleteBlobException() throws AzureDeleteBlobException {
        doThrow(AzureDeleteBlobException.class).when(inboundDataStoreDeleter).delete(any(UUID.class));

        assertDoesNotThrow(() -> deleter.delete());
    }

    private List<ExternalObjectDirectoryEntity> createInboundData() {
        ExternalObjectDirectoryEntity inboundData1 = new ExternalObjectDirectoryEntity();
        inboundData1.setStatus(markedForDeletionStatus);
        inboundData1.setExternalLocationType(new ExternalLocationTypeEntity());
        inboundData1.setExternalLocation(UUID.randomUUID());
        inboundData1.setId(1);
        inboundData1.setVerificationAttempts(1);

        ExternalObjectDirectoryEntity inboundData2 = new ExternalObjectDirectoryEntity();
        inboundData2.setStatus(markedForDeletionStatus);
        inboundData2.setExternalLocationType(new ExternalLocationTypeEntity());
        inboundData2.setExternalLocation(UUID.randomUUID());
        inboundData2.setId(2);
        inboundData2.setVerificationAttempts(2);

        ArrayList<ExternalObjectDirectoryEntity> inboundDataList = new ArrayList<>();
        inboundDataList.add(inboundData1);
        inboundDataList.add(inboundData2);
        return inboundDataList;
    }

}
