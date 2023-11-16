package uk.gov.hmcts.darts.audio.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.ObjectDirectoryStatusEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ObjectDirectoryStatusEnum;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.ObjectDirectoryStatusRepository;
import uk.gov.hmcts.darts.common.repository.TransientObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboundDataStoreDeleterImplImplTest {
    @Mock
    DataManagementApi dataManagementApi;
    @Mock
    ObjectDirectoryStatusRepository objectDirectoryStatusRepository;
    @Mock
    UserAccountRepository userAccountRepository;
    @Mock
    TransientObjectDirectoryRepository transientObjectDirectoryRepository;

    OutboundDataStoreDeleterImpl deleter;

    private ObjectDirectoryStatusEntity markedForDeletionStatus;


    @BeforeEach
    void setUp() {
        this.deleter = new OutboundDataStoreDeleterImpl(
            dataManagementApi,
            objectDirectoryStatusRepository,
            userAccountRepository,
            transientObjectDirectoryRepository
        );
    }

    private void mockStatus() {
        this.markedForDeletionStatus = new ObjectDirectoryStatusEntity();
        markedForDeletionStatus.setId(ObjectDirectoryStatusEnum.MARKED_FOR_DELETION.getId());
        when(objectDirectoryStatusRepository.getReferenceById(ObjectDirectoryStatusEnum.MARKED_FOR_DELETION.getId())).thenReturn(
            markedForDeletionStatus);
    }

    private void mockSystemUser() {
        Optional<UserAccountEntity> userEntity = Optional.of(new UserAccountEntity());
        when(userAccountRepository.findById(0)).thenReturn(userEntity);
    }


    private List<TransientObjectDirectoryEntity> createOutboundData() {
        TransientObjectDirectoryEntity outboundAudio = new TransientObjectDirectoryEntity();
        outboundAudio.setStatus(markedForDeletionStatus);
        outboundAudio.setId(1);

        TransientObjectDirectoryEntity outboundAudio2 = new TransientObjectDirectoryEntity();
        outboundAudio2.setStatus(markedForDeletionStatus);
        outboundAudio2.setId(21);

        List<TransientObjectDirectoryEntity> outboundList = new ArrayList<>();
        outboundList.add(outboundAudio);
        outboundList.add(outboundAudio2);
        return outboundList;
    }

    @Test
    void deleteFromInboundAndUnstructuredDatastore() {
        mockStatus();

        mockSystemUser();

        List<TransientObjectDirectoryEntity> outboundData = createOutboundData();


        when(transientObjectDirectoryRepository.findByStatus(markedForDeletionStatus)).thenReturn(
            outboundData);


        doNothing().when(dataManagementApi).deleteBlobDataFromOutboundContainer(any());

        List<TransientObjectDirectoryEntity> deletedItems = deleter.delete();

        assertThat(deletedItems, containsInAnyOrder(
            hasProperty("id", is(1)),
            hasProperty("id", is(21))
        ));
        assertEquals(2, deletedItems.size());

    }


    @Test
    void testDeleteWhenSystemUserDoesNotExist() {
        when(userAccountRepository.findById(0)).thenReturn(Optional.empty());
        assertThrows(DartsApiException.class, () ->
            deleter.delete());
    }
}
