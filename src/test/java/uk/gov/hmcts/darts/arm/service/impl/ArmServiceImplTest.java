package uk.gov.hmcts.darts.arm.service.impl;

import com.azure.core.http.rest.PagedIterable;
import com.azure.core.http.rest.Response;
import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.batch.BlobBatchClient;
import com.azure.storage.blob.batch.BlobBatchClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.DeleteSnapshotsOptionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.dao.ArmDataManagementDao;
import uk.gov.hmcts.darts.arm.model.blobs.ContinuationTokenBlobs;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class ArmServiceImplTest {

    private static final String ARM_BLOB_CONTAINER_NAME = "arm_dummy_container";
    private static final String BLOB_FILENAME = "test_filename";
    private static final String TEST_BINARY_STRING = "Test String to be converted to binary!";
    private static final BinaryData BINARY_DATA = BinaryData.fromBytes(TEST_BINARY_STRING.getBytes());
    private static final String TEST_DROP_ZONE = "dummy/dropzone/";

    @Mock
    private ArmDataManagementDao armDataManagementDao;
    @Mock
    private ArmDataManagementConfiguration armDataManagementConfiguration;
    @InjectMocks
    private ArmServiceImpl armService;

    private BlobContainerClient blobContainerClient;
    private BlobClient blobClient;

    @BeforeEach
    void beforeEach() {
        blobContainerClient = mock(BlobContainerClient.class);
        blobClient = mock(BlobClient.class);
    }

    @Test
    void saveBlobData_shouldSaveBlobUsingFilename() {
        var foldersConfig = new ArmDataManagementConfiguration.Folders();
        foldersConfig.setSubmission(TEST_BINARY_STRING);
        when(armDataManagementConfiguration.getFolders()).thenReturn(foldersConfig);

        when(armDataManagementDao.getBlobContainerClient(ARM_BLOB_CONTAINER_NAME)).thenReturn(blobContainerClient);
        when(armDataManagementDao.getBlobClient(any(), any())).thenReturn(blobClient);

        String blobName = armService.saveBlobData(ARM_BLOB_CONTAINER_NAME, BLOB_FILENAME, BINARY_DATA);
        assertNotNull(blobName);
        assertEquals(BLOB_FILENAME, blobName);
    }

    @Test
    void saveBlobData_shouldSaveBlobUsingBlobPathAndFilename() {
        String blobPathAndFilename = TEST_DROP_ZONE + BLOB_FILENAME;
        when(armDataManagementDao.getBlobContainerClient(ARM_BLOB_CONTAINER_NAME)).thenReturn(blobContainerClient);
        when(armDataManagementDao.getBlobClient(any(), any())).thenReturn(blobClient);
        String blobName = armService.saveBlobData(ARM_BLOB_CONTAINER_NAME, BINARY_DATA, blobPathAndFilename);
        assertNotNull(blobName);
        assertEquals(blobPathAndFilename, blobName);
    }

    @Test
    void listSubmissionBlobs_shouldListSubmissionBlobs() {
        PagedIterable<BlobItem> pagedIterable = (PagedIterable<BlobItem>) mock(PagedIterable.class);
        when(blobContainerClient.listBlobsByHierarchy(any(), any(), any())).thenReturn(pagedIterable);
        when(armDataManagementDao.getBlobContainerClient(ARM_BLOB_CONTAINER_NAME)).thenReturn(blobContainerClient);

        var foldersConfig = new ArmDataManagementConfiguration.Folders();
        foldersConfig.setSubmission(TEST_DROP_ZONE);
        foldersConfig.setCollected(TEST_DROP_ZONE);
        foldersConfig.setResponse(TEST_DROP_ZONE);
        when(armDataManagementConfiguration.getFolders()).thenReturn(foldersConfig);

        String prefix = "1_1_1";
        List<String> blobs = armService.listSubmissionBlobs(ARM_BLOB_CONTAINER_NAME, prefix);
        assertNotNull(blobs);
    }

    @Test
    void listResponseBlobs_shouldListResponseBlobs() {
        PagedIterable<BlobItem> pagedIterable = (PagedIterable<BlobItem>) mock(PagedIterable.class);
        when(blobContainerClient.listBlobsByHierarchy(any(), any(), any())).thenReturn(pagedIterable);
        when(armDataManagementDao.getBlobContainerClient(ARM_BLOB_CONTAINER_NAME)).thenReturn(blobContainerClient);

        var foldersConfig = new ArmDataManagementConfiguration.Folders();
        foldersConfig.setSubmission(TEST_DROP_ZONE);
        foldersConfig.setCollected(TEST_DROP_ZONE);
        foldersConfig.setResponse(TEST_DROP_ZONE);
        when(armDataManagementConfiguration.getFolders()).thenReturn(foldersConfig);

        String prefix = "1_1_1";
        List<String> blobs = armService.listResponseBlobs(ARM_BLOB_CONTAINER_NAME, prefix);
        assertNotNull(blobs);
    }

    @Test
    void getBlobData_shouldReturnBlob() {
        when(armDataManagementDao.getBlobContainerClient(ARM_BLOB_CONTAINER_NAME)).thenReturn(blobContainerClient);
        when(armDataManagementDao.getBlobClient(any(), any())).thenReturn(blobClient);
        when(blobClient.exists()).thenReturn(Boolean.TRUE);
        when(blobClient.downloadContent()).thenReturn(BINARY_DATA);

        BinaryData binaryData = armService.getBlobData(ARM_BLOB_CONTAINER_NAME, "blobname");
        assertEquals(BINARY_DATA, binaryData);
    }

    @Test
    void deleteResponseBlob_shouldReturnIsSuccessful() {
        when(armDataManagementDao.getBlobContainerClient(ARM_BLOB_CONTAINER_NAME)).thenReturn(blobContainerClient);
        when(armDataManagementDao.getBlobClient(any(), any())).thenReturn(blobClient);

        Response<Boolean> response = mock(Response.class);
        when(response.getStatusCode()).thenReturn(202);

        when(blobClient.deleteIfExistsWithResponse(DeleteSnapshotsOptionType.INCLUDE, null, Duration.of(60, ChronoUnit.SECONDS), null)).thenReturn(response);

        boolean result = armService.deleteBlobData(ARM_BLOB_CONTAINER_NAME, "blobPathAndName");
        assertTrue(result);
    }

    @Test
    void deleteResponseBlob_shouldReturnIsNotSuccessful_when500Error() {
        when(armDataManagementDao.getBlobContainerClient(ARM_BLOB_CONTAINER_NAME)).thenReturn(blobContainerClient);
        when(armDataManagementDao.getBlobClient(any(), any())).thenReturn(blobClient);

        Response<Boolean> response = mock(Response.class);
        when(response.getStatusCode()).thenReturn(500);

        when(blobClient.deleteIfExistsWithResponse(DeleteSnapshotsOptionType.INCLUDE, null, Duration.of(60, ChronoUnit.SECONDS), null)).thenReturn(response);

        boolean result = armService.deleteBlobData(ARM_BLOB_CONTAINER_NAME, "blobPathAndName");
        assertFalse(result);
    }

    @Test
    void listSubmissionBlobsUsingBatch_shouldListSubmissionBlobsUsingBatch() {
        PagedIterable<BlobItem> pagedIterable = (PagedIterable<BlobItem>) mock(PagedIterable.class);
        when(blobContainerClient.listBlobs(any(), any())).thenReturn(pagedIterable);
        when(armDataManagementDao.getBlobContainerClient(ARM_BLOB_CONTAINER_NAME)).thenReturn(blobContainerClient);

        var foldersConfig = new ArmDataManagementConfiguration.Folders();
        foldersConfig.setSubmission(TEST_DROP_ZONE);
        foldersConfig.setCollected(TEST_DROP_ZONE);
        foldersConfig.setResponse(TEST_DROP_ZONE);
        when(armDataManagementConfiguration.getFolders()).thenReturn(foldersConfig);

        String prefix = "functional";
        Integer batchSize = 5;
        List<String> blobs = armService.listSubmissionBlobsUsingBatch(ARM_BLOB_CONTAINER_NAME, prefix, batchSize);
        assertNotNull(blobs);
    }

    @Test
    void listResponseBlobsUsingBatch_shouldListResponseBlobsUsingBatch() {
        PagedIterable<BlobItem> pagedIterable = (PagedIterable<BlobItem>) mock(PagedIterable.class);
        when(blobContainerClient.listBlobs(any(), any())).thenReturn(pagedIterable);
        when(armDataManagementDao.getBlobContainerClient(ARM_BLOB_CONTAINER_NAME)).thenReturn(blobContainerClient);

        var foldersConfig = new ArmDataManagementConfiguration.Folders();
        foldersConfig.setSubmission(TEST_DROP_ZONE);
        foldersConfig.setCollected(TEST_DROP_ZONE);
        foldersConfig.setResponse(TEST_DROP_ZONE);
        when(armDataManagementConfiguration.getFolders()).thenReturn(foldersConfig);

        String prefix = "functional";
        Integer batchSize = 5;
        List<String> blobs = armService.listResponseBlobsUsingBatch(ARM_BLOB_CONTAINER_NAME, prefix, batchSize);
        assertNotNull(blobs);
    }

    @Test
    void listSubmissionBlobsWithMarker_ShouldSucceed() {

        PagedIterable<BlobItem> mockPagedIterable = (PagedIterable<BlobItem>) mock(PagedIterable.class);

        when(blobContainerClient.listBlobs(any(), any(), any())).thenReturn(mockPagedIterable);
        when(armDataManagementDao.getBlobContainerClient(ARM_BLOB_CONTAINER_NAME)).thenReturn(blobContainerClient);

        var foldersConfig = new ArmDataManagementConfiguration.Folders();
        foldersConfig.setSubmission(TEST_DROP_ZONE);
        foldersConfig.setCollected(TEST_DROP_ZONE);
        foldersConfig.setResponse(TEST_DROP_ZONE);
        when(armDataManagementConfiguration.getFolders()).thenReturn(foldersConfig);

        String prefix = "functional";
        ContinuationTokenBlobs continuationTokenBlobs = armService.listSubmissionBlobsWithMarker(
            ARM_BLOB_CONTAINER_NAME, prefix, 10, null);
        assertNotNull(continuationTokenBlobs);
    }

    @Test
    void listResponseBlobsWithMarker_ShouldReturnToken() {
        PagedIterable<BlobItem> mockPagedTableEntities = (PagedIterable<BlobItem>) mock(PagedIterable.class);
        when(blobContainerClient.listBlobs(any(), any(), any())).thenReturn(mockPagedTableEntities);
        when(armDataManagementDao.getBlobContainerClient(ARM_BLOB_CONTAINER_NAME)).thenReturn(blobContainerClient);

        var foldersConfig = new ArmDataManagementConfiguration.Folders();
        foldersConfig.setSubmission(TEST_DROP_ZONE);
        foldersConfig.setCollected(TEST_DROP_ZONE);
        foldersConfig.setResponse(TEST_DROP_ZONE);
        when(armDataManagementConfiguration.getFolders()).thenReturn(foldersConfig);

        String prefix = "functional";
        ContinuationTokenBlobs continuationTokenBlobs = armService.listResponseBlobsWithMarker(
            ARM_BLOB_CONTAINER_NAME, prefix, 10, null);
        assertNotNull(continuationTokenBlobs);
    }

    @Test
    void deleteMultipleBlobs_shouldReturnFalseAndNotCallAzure_whenNoBlobsProvided() {
        assertFalse(armService.deleteMultipleBlobs(ARM_BLOB_CONTAINER_NAME, null));
        assertFalse(armService.deleteMultipleBlobs(ARM_BLOB_CONTAINER_NAME, List.of()));
    }

    @Test
    void deleteMultipleBlobs_shouldReturnTrue_whenAllDeletesSucceed() {
        when(armDataManagementDao.getBlobContainerClient(ARM_BLOB_CONTAINER_NAME)).thenReturn(blobContainerClient);
        BlobServiceClient blobServiceClient = mock(BlobServiceClient.class);
        when(blobContainerClient.getServiceClient()).thenReturn(blobServiceClient);

        BlobClient blobClient1 = mock(BlobClient.class);
        BlobClient blobClient2 = mock(BlobClient.class);
        when(blobContainerClient.getBlobClient("blob1")).thenReturn(blobClient1);
        when(blobContainerClient.getBlobClient("blob2")).thenReturn(blobClient2);
        when(blobClient1.getBlobUrl()).thenReturn("https://example/blob1");
        when(blobClient2.getBlobUrl()).thenReturn("https://example/blob2");


        PagedIterable<Response<Void>> pagedResponses = (PagedIterable<Response<Void>>) mock(PagedIterable.class);
        Response<Void> r1 = (Response<Void>) mock(Response.class);
        Response<Void> r2 = (Response<Void>) mock(Response.class);
        when(r1.getStatusCode()).thenReturn(202);
        when(r2.getStatusCode()).thenReturn(404);
        when(pagedResponses.iterator()).thenReturn(List.of(r1, r2).iterator());

        BlobBatchClient batchClient = mock(BlobBatchClient.class);
        when(batchClient.deleteBlobs(anyList(), eq(DeleteSnapshotsOptionType.INCLUDE), any(Duration.class), eq(null)))
            .thenReturn(pagedResponses);

        try (var ignored = mockConstruction(BlobBatchClientBuilder.class,
                                            (builder, context) -> when(builder.buildClient()).thenReturn(batchClient))) {
            boolean result = armService.deleteMultipleBlobs(ARM_BLOB_CONTAINER_NAME, List.of("blob1", "blob2"));
            assertTrue(result);
        }
    }

    @Test
    void deleteMultipleBlobs_shouldReturnFalse_whenAnyDeleteFails() {
        when(armDataManagementDao.getBlobContainerClient(ARM_BLOB_CONTAINER_NAME)).thenReturn(blobContainerClient);
        BlobServiceClient blobServiceClient = mock(BlobServiceClient.class);
        when(blobContainerClient.getServiceClient()).thenReturn(blobServiceClient);

        BlobClient blobClient1 = mock(BlobClient.class);
        BlobClient blobClient2 = mock(BlobClient.class);
        when(blobContainerClient.getBlobClient("blob1")).thenReturn(blobClient1);
        when(blobContainerClient.getBlobClient("blob2")).thenReturn(blobClient2);
        when(blobClient1.getBlobUrl()).thenReturn("https://example/blob1");
        when(blobClient2.getBlobUrl()).thenReturn("https://example/blob2");

        PagedIterable<Response<Void>> pagedResponses = (PagedIterable<Response<Void>>) mock(PagedIterable.class);
        Response<Void> r1 = (Response<Void>) mock(Response.class);
        Response<Void> r2 = (Response<Void>) mock(Response.class);
        when(r1.getStatusCode()).thenReturn(202);
        when(r2.getStatusCode()).thenReturn(500);
        when(pagedResponses.iterator()).thenReturn(List.of(r1, r2).iterator());

        BlobBatchClient batchClient = mock(BlobBatchClient.class);
        when(batchClient.deleteBlobs(anyList(), eq(DeleteSnapshotsOptionType.INCLUDE), any(Duration.class), eq(null)))
            .thenReturn(pagedResponses);

        try (var ignored = mockConstruction(BlobBatchClientBuilder.class,
                                            (builder, context) -> when(builder.buildClient()).thenReturn(batchClient))) {
            boolean result = armService.deleteMultipleBlobs(ARM_BLOB_CONTAINER_NAME, List.of("blob1", "blob2"));
            assertFalse(result);
        }
    }

    @Test
    void deleteMultipleBlobs_shouldReturnFalse_whenAzureThrowsException() {
        when(armDataManagementDao.getBlobContainerClient(ARM_BLOB_CONTAINER_NAME)).thenReturn(blobContainerClient);
        BlobServiceClient blobServiceClient = mock(BlobServiceClient.class);
        when(blobContainerClient.getServiceClient()).thenReturn(blobServiceClient);

        BlobClient blobClient1 = mock(BlobClient.class);
        BlobClient blobClient2 = mock(BlobClient.class);
        when(blobContainerClient.getBlobClient("blob1")).thenReturn(blobClient1);
        when(blobContainerClient.getBlobClient("blob2")).thenReturn(blobClient2);
        when(blobClient1.getBlobUrl()).thenReturn("https://example/blob1");
        when(blobClient2.getBlobUrl()).thenReturn("https://example/blob2");

        BlobBatchClient batchClient = mock(BlobBatchClient.class);
        when(batchClient.deleteBlobs(anyList(), eq(DeleteSnapshotsOptionType.INCLUDE), any(Duration.class), eq(null)))
            .thenThrow(new RuntimeException("Azure error"));

        try (var ignored = mockConstruction(BlobBatchClientBuilder.class,
                                            (builder, context) -> when(builder.buildClient()).thenReturn(batchClient))) {
            boolean result = armService.deleteMultipleBlobs(ARM_BLOB_CONTAINER_NAME, List.of("blob1", "blob2"));
            assertFalse(result);
        }
    }

    @Test
    void deleteBlobsIndividually_shouldReturnTrueAndCallDeleteForEachBlob_whenAllDeletesSucceed() {
        ArmServiceImpl serviceSpy = spy(armService);
        doReturn(true).when(serviceSpy).deleteBlobData(eq(ARM_BLOB_CONTAINER_NAME), any());

        boolean result = invokeDeleteBlobsIndividually(serviceSpy, ARM_BLOB_CONTAINER_NAME, List.of("b1", "b2", "b3"));

        assertTrue(result);
        verify(serviceSpy, times(3)).deleteBlobData(eq(ARM_BLOB_CONTAINER_NAME), any());
        verify(serviceSpy).deleteBlobData(ARM_BLOB_CONTAINER_NAME, "b1");
        verify(serviceSpy).deleteBlobData(ARM_BLOB_CONTAINER_NAME, "b2");
        verify(serviceSpy).deleteBlobData(ARM_BLOB_CONTAINER_NAME, "b3");
    }

    @Test
    void deleteBlobsIndividually_shouldReturnFalseAndStillAttemptAllDeletes_whenAnyDeleteFails() {
        ArmServiceImpl serviceSpy = spy(armService);
        doReturn(true).when(serviceSpy).deleteBlobData(ARM_BLOB_CONTAINER_NAME, "b1");
        doReturn(false).when(serviceSpy).deleteBlobData(ARM_BLOB_CONTAINER_NAME, "b2");
        doReturn(true).when(serviceSpy).deleteBlobData(ARM_BLOB_CONTAINER_NAME, "b3");

        boolean result = invokeDeleteBlobsIndividually(serviceSpy, ARM_BLOB_CONTAINER_NAME, List.of("b1", "b2", "b3"));

        assertFalse(result);
        verify(serviceSpy).deleteBlobData(ARM_BLOB_CONTAINER_NAME, "b1");
        verify(serviceSpy).deleteBlobData(ARM_BLOB_CONTAINER_NAME, "b2");
        verify(serviceSpy).deleteBlobData(ARM_BLOB_CONTAINER_NAME, "b3");
        verify(serviceSpy, times(3)).deleteBlobData(eq(ARM_BLOB_CONTAINER_NAME), any());
    }

    @Test
    void deleteBlobsIndividually_shouldReturnTrueAndNotCallDelete_whenBlobListIsEmpty() {
        ArmServiceImpl serviceSpy = spy(armService);

        boolean result = invokeDeleteBlobsIndividually(serviceSpy, ARM_BLOB_CONTAINER_NAME, List.of());

        assertTrue(result);
        verify(serviceSpy, times(0)).deleteBlobData(eq(ARM_BLOB_CONTAINER_NAME), any());
    }

    private boolean invokeDeleteBlobsIndividually(ArmServiceImpl service, String containerName, List<String> blobs) {
        try {
            Method method = ArmServiceImpl.class.getDeclaredMethod("deleteBlobsIndividually", String.class, List.class);
            method.setAccessible(true);
            return (boolean) method.invoke(service, containerName, blobs);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
