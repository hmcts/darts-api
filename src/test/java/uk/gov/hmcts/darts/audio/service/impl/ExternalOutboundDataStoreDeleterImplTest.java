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
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.helper.SystemUserHelper;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.repository.TransientObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExternalOutboundDataStoreDeleterImplTest {

    @Mock
    ObjectRecordStatusRepository objectRecordStatusRepository;
    @Mock
    UserAccountRepository userAccountRepository;
    @Mock
    TransientObjectDirectoryRepository transientObjectDirectoryRepository;

    ExternalOutboundDataStoreDeleter deleter;

    private ObjectRecordStatusEntity markedForDeletionStatus;


    @Mock
    private OutboundExternalObjectDirectoryDeletedFinder finder;

    @Mock
    private OutboundDataStoreDeleter outboundDataStoreDeleter;

    @Mock
    private SystemUserHelper systemUserHelper;


    @BeforeEach
    void setUp() {
        this.deleter = new ExternalOutboundDataStoreDeleter(
            objectRecordStatusRepository,
            userAccountRepository,
            transientObjectDirectoryRepository,
            finder,
            outboundDataStoreDeleter, systemUserHelper
        );
    }

    private void mockStatus() {
        this.markedForDeletionStatus = new ObjectRecordStatusEntity();
        markedForDeletionStatus.setId(ObjectRecordStatusEnum.DELETED.getId());
        when(objectRecordStatusRepository.getReferenceById(ObjectRecordStatusEnum.DELETED.getId())).thenReturn(
            markedForDeletionStatus);
    }

    private void mockSystemUser() {
        when(systemUserHelper.findSystemUserGuid(anyString())).thenReturn("");
        when(userAccountRepository.findSystemUser(anyString())).thenReturn(new UserAccountEntity());
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
    void deleteFromOutboundDataStore() {
        mockStatus();

        mockSystemUser();

        List<TransientObjectDirectoryEntity> outboundData = createOutboundData();

        when(finder.findMarkedForDeletion()).thenReturn(outboundData);

        List<TransientObjectDirectoryEntity> deletedItems = deleter.delete();

        assertThat(deletedItems, containsInAnyOrder(
            hasProperty("id", is(1)),
            hasProperty("id", is(21))
        ));
        assertEquals(2, deletedItems.size());

    }


    @Test
    void testDeleteWhenSystemUserDoesNotExist() {
        when(systemUserHelper.findSystemUserGuid(anyString())).thenReturn("");
        when(userAccountRepository.findSystemUser(anyString())).thenReturn(null);
        assertThrows(DartsApiException.class, () -> deleter.delete());
    }
}
