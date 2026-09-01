package uk.gov.hmcts.darts.audio.helper;

import com.azure.core.util.BinaryData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;
import uk.gov.hmcts.darts.common.enums.SystemUsersEnum;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.datamanagement.config.DataManagementConfiguration;
import uk.gov.hmcts.darts.datamanagement.model.BlobClientUploadResponseImpl;
import uk.gov.hmcts.darts.datamanagement.service.DataManagementService;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnstructuredDataHelperTest {

    @Mock
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    @Mock
    private ObjectRecordStatusRepository objectRecordStatusRepository;
    @Mock
    private ExternalLocationTypeRepository externalLocationTypeRepository;
    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private DataManagementService dataManagementService;
    @Mock
    private DataManagementConfiguration dataManagementConfiguration;
    @Mock
    private BlobClientUploadResponseImpl blobClientUploadResponseImpl;

    private UnstructuredDataHelper unstructuredDataHelper;

    @BeforeEach
    void setUp() {
        unstructuredDataHelper = new UnstructuredDataHelper(
            externalObjectDirectoryRepository,
            objectRecordStatusRepository,
            externalLocationTypeRepository,
            userAccountRepository,
            dataManagementService,
            dataManagementConfiguration
        );
    }

    @Test
    void createUnstructuredDataFromEod_shouldCreateAndDeleteRecords() {
        when(dataManagementConfiguration.getUnstructuredContainerName()).thenReturn("unstructured");
        when(dataManagementService.saveBlobData(anyString(), any(InputStream.class))).thenReturn(blobClientUploadResponseImpl);
        when(blobClientUploadResponseImpl.getBlobName()).thenReturn("blob-id");

        ObjectRecordStatusEntity storedStatus = new ObjectRecordStatusEntity();
        storedStatus.setId(ObjectRecordStatusEnum.STORED.getId());
        when(objectRecordStatusRepository.getReferenceById(ObjectRecordStatusEnum.STORED.getId())).thenReturn(storedStatus);

        ExternalLocationTypeEntity unstructuredType = new ExternalLocationTypeEntity();
        unstructuredType.setId(ExternalLocationTypeEnum.UNSTRUCTURED.getId());
        when(externalLocationTypeRepository.getReferenceById(ExternalLocationTypeEnum.UNSTRUCTURED.getId())).thenReturn(unstructuredType);

        UserAccountEntity systemUser = new UserAccountEntity();
        systemUser.setId(SystemUsersEnum.DEFAULT.getId());
        when(userAccountRepository.getReferenceById(SystemUsersEnum.DEFAULT.getId())).thenReturn(systemUser);

        MediaEntity mediaEntity = new MediaEntity();
        ExternalObjectDirectoryEntity eodEntity = new ExternalObjectDirectoryEntity();
        eodEntity.setMedia(mediaEntity);
        ExternalObjectDirectoryEntity eodEntityToDelete = new ExternalObjectDirectoryEntity();
        eodEntityToDelete.setMedia(mediaEntity);

        BinaryData data = BinaryData.fromString("Test String");

        boolean created = unstructuredDataHelper.createUnstructuredDataFromEod(
            eodEntityToDelete,
            eodEntity,
            data.toStream()
        );

        assertTrue(created);
        verify(externalObjectDirectoryRepository).save(any(ExternalObjectDirectoryEntity.class));
        verify(externalObjectDirectoryRepository).delete(eq(eodEntityToDelete));
    }

    @Test
    void createUnstructuredDataFromEod_whenUploadThrows_shouldReturnFalseAndSkipDbUpdates() {
        when(dataManagementConfiguration.getUnstructuredContainerName()).thenReturn("unstructured");
        when(dataManagementService.saveBlobData(anyString(), any(InputStream.class)))
            .thenThrow(new RuntimeException("upload failed"));

        ExternalObjectDirectoryEntity eodEntity = new ExternalObjectDirectoryEntity();
        ExternalObjectDirectoryEntity eodEntityToDelete = new ExternalObjectDirectoryEntity();

        boolean created = unstructuredDataHelper.createUnstructuredDataFromEod(
            eodEntityToDelete,
            eodEntity,
            BinaryData.fromString("Test String").toStream()
        );

        assertFalse(created);
        verify(externalObjectDirectoryRepository, never()).save(any(ExternalObjectDirectoryEntity.class));
        verify(externalObjectDirectoryRepository, never()).delete(any(ExternalObjectDirectoryEntity.class));
    }

    @Test
    void createUnstructuredDataFromEod_whenBlobNameMissing_shouldReturnFalse() {
        when(dataManagementConfiguration.getUnstructuredContainerName()).thenReturn("unstructured");
        when(dataManagementService.saveBlobData(anyString(), any(InputStream.class))).thenReturn(blobClientUploadResponseImpl);
        when(blobClientUploadResponseImpl.getBlobName()).thenReturn(null);

        ExternalObjectDirectoryEntity eodEntity = new ExternalObjectDirectoryEntity();
        ExternalObjectDirectoryEntity eodEntityToDelete = new ExternalObjectDirectoryEntity();

        boolean created = unstructuredDataHelper.createUnstructuredDataFromEod(
            eodEntityToDelete,
            eodEntity,
            BinaryData.fromString("Test String").toStream()
        );

        assertFalse(created);
        verify(externalObjectDirectoryRepository, never()).save(any(ExternalObjectDirectoryEntity.class));
        verify(externalObjectDirectoryRepository, never()).delete(any(ExternalObjectDirectoryEntity.class));
    }
}