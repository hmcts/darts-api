package uk.gov.hmcts.darts.arm.component.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArmResponseFilesProcessSingleElementImpl_deleteResponseBlobsTest {

    @Mock
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    @Mock
    private ObjectRecordStatusRepository objectRecordStatusRepository;
    @Mock
    private ArmDataManagementApi armDataManagementApi;
    @Mock
    private FileOperationService fileOperationService;
    @Mock
    private ArmDataManagementConfiguration armDataManagementConfiguration;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private UserIdentity userIdentity;

    private ArmResponseFilesProcessSingleElementImpl armResponseFilesProcessSingleElement;

    @BeforeEach
    void setupData() {
        armResponseFilesProcessSingleElement = new ArmResponseFilesProcessSingleElementImpl(
            externalObjectDirectoryRepository,
            objectRecordStatusRepository,
            armDataManagementApi,
            fileOperationService,
            armDataManagementConfiguration,
            objectMapper,
            userIdentity
        );
    }

    @Test
    void deleteResponseBlobsShouldDeleteInputUploadFileWhenResponseBlobsIsEmpty() {
        // Given
        String armInputUploadFilename = "dropzone/DARTS/response/2760_187_1_2d50a0bbde794e0ea9f4918aafeaccde_0_iu.rsp";
        List<String> responseBlobs = Collections.emptyList();
        ExternalObjectDirectoryEntity externalObjectDirectory = new ExternalObjectDirectoryEntity();

        when(armDataManagementApi.deleteBlobData(armInputUploadFilename)).thenReturn(true);

        // When
        armResponseFilesProcessSingleElement.deleteResponseBlobs(
            armInputUploadFilename,
            responseBlobs,
            externalObjectDirectory
        );

        // Then
        verify(armDataManagementApi).deleteBlobData(armInputUploadFilename);
        verifyNoMoreInteractions(armDataManagementApi);
        assertTrue(externalObjectDirectory.isResponseCleaned());
    }

    @Test
    void deleteResponseBlobsShouldDeleteInputUploadFileWhenResponseBlobsAllDeleted() {
        // Given
        String armInputUploadFilename = "dropzone/DARTS/response/2760_187_1_2d50a0bbde794e0ea9f4918aafeaccde_1_iu.rsp";
        String uploadFileFilename = "dropzone/DARTS/response/2d50a0bbde794e0ea9f4918aafeaccde_4117b202-91de-4530-9fc5-8328f25068ba_1_uf.rsp";
        String createRecordFilename = "dropzone/DARTS/response/2d50a0bbde794e0ea9f4918aafeaccde_a138c9be-eb4b-4a24-b44d-4d490fe3fffb_1_cr.rsp";

        when(armDataManagementApi.deleteBlobData(uploadFileFilename)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(createRecordFilename)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(armInputUploadFilename)).thenReturn(true);

        List<String> responseBlobs = List.of(uploadFileFilename, createRecordFilename);
        ExternalObjectDirectoryEntity externalObjectDirectory = new ExternalObjectDirectoryEntity();

        // When
        armResponseFilesProcessSingleElement.deleteResponseBlobs(
            armInputUploadFilename,
            responseBlobs,
            externalObjectDirectory
        );

        // Then
        verify(armDataManagementApi).deleteBlobData(uploadFileFilename);
        verify(armDataManagementApi).deleteBlobData(createRecordFilename);
        verify(armDataManagementApi).deleteBlobData(armInputUploadFilename);
        verifyNoMoreInteractions(armDataManagementApi);
        assertTrue(externalObjectDirectory.isResponseCleaned());
    }

    @Test
    void deleteResponseBlobsShouldNotDeleteInputUploadFileWhenCreateRecordDeleteFails() {
        // Given
        String armInputUploadFilename = "dropzone/DARTS/response/2760_187_1_2d50a0bbde794e0ea9f4918aafeaccde_1_iu.rsp";
        String uploadFileFilename = "dropzone/DARTS/response/2d50a0bbde794e0ea9f4918aafeaccde_4117b202-91de-4530-9fc5-8328f25068ba_1_uf.rsp";
        String createRecordFilename = "dropzone/DARTS/response/2d50a0bbde794e0ea9f4918aafeaccde_a138c9be-eb4b-4a24-b44d-4d490fe3fffb_1_cr.rsp";

        when(armDataManagementApi.deleteBlobData(uploadFileFilename)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(createRecordFilename)).thenReturn(false);

        List<String> responseBlobs = List.of(uploadFileFilename, createRecordFilename);
        ExternalObjectDirectoryEntity externalObjectDirectory = new ExternalObjectDirectoryEntity();

        // When
        armResponseFilesProcessSingleElement.deleteResponseBlobs(
            armInputUploadFilename,
            responseBlobs,
            externalObjectDirectory
        );

        // Then
        verify(armDataManagementApi).deleteBlobData(uploadFileFilename);
        verify(armDataManagementApi).deleteBlobData(createRecordFilename);
        verifyNoMoreInteractions(armDataManagementApi);
        assertFalse(externalObjectDirectory.isResponseCleaned());
    }

}
