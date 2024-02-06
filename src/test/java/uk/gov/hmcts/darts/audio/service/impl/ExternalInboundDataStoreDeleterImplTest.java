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
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.helper.SystemUserHelper;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExternalInboundDataStoreDeleterImplTest {

    private ExternalInboundDataStoreDeleter deleter;

    @Mock
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;

    @Mock
    private ObjectRecordStatusRepository objectRecordStatusRepository;

    private ObjectRecordStatusEntity deletedStatus;

    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private InboundExternalObjectDirectoryDeletedFinder finder;
    @Mock
    private InboundDataStoreDeleter inboundDataStoreDeleter;

    @Mock
    private SystemUserHelper systemUserHelper;

    @BeforeEach
    public void setUp() {
        this.deleter = new ExternalInboundDataStoreDeleter(
              objectRecordStatusRepository,
              userAccountRepository,
              externalObjectDirectoryRepository,
              finder,
              inboundDataStoreDeleter, systemUserHelper
        );


    }

    private void mockSystemUser() {
        when(systemUserHelper.findSystemUserGuid(anyString())).thenReturn("");
        when(userAccountRepository.findSystemUser(anyString())).thenReturn(new UserAccountEntity());
    }


    private void mockStatus() {
        this.deletedStatus = new ObjectRecordStatusEntity();
        deletedStatus.setId(ObjectRecordStatusEnum.DELETED.getId());
        when(objectRecordStatusRepository.getReferenceById(ObjectRecordStatusEnum.DELETED.getId())).thenReturn(
              deletedStatus);
    }

    @Test
    void deleteFromInboundAndUnstructuredDatastore() {
        mockStatus();

        mockSystemUser();

        List<ExternalObjectDirectoryEntity> inboundData = createInboundData();

        when(finder.findMarkedForDeletion()).thenReturn(inboundData);

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
    void testDeleteWhenSystemUserDoesNotExist() {
        when(systemUserHelper.findSystemUserGuid(anyString())).thenReturn("");
        when(userAccountRepository.findSystemUser(anyString())).thenReturn(null);
        assertThrows(DartsApiException.class, () -> deleter.delete());
    }


    private List<ExternalObjectDirectoryEntity> createInboundData() {
        ExternalObjectDirectoryEntity inboundData1 = new ExternalObjectDirectoryEntity();
        inboundData1.setStatus(deletedStatus);
        inboundData1.setExternalLocationType(new ExternalLocationTypeEntity());
        inboundData1.setId(1);
        inboundData1.setVerificationAttempts(1);

        ExternalObjectDirectoryEntity inboundData2 = new ExternalObjectDirectoryEntity();
        inboundData2.setStatus(deletedStatus);
        inboundData2.setExternalLocationType(new ExternalLocationTypeEntity());
        inboundData2.setId(2);
        inboundData2.setVerificationAttempts(2);

        ArrayList<ExternalObjectDirectoryEntity> inboundDataList = new ArrayList<>();
        inboundDataList.add(inboundData1);
        inboundDataList.add(inboundData2);
        return inboundDataList;
    }
}
