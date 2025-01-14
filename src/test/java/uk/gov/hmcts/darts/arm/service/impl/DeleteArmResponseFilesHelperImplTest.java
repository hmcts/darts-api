package uk.gov.hmcts.darts.arm.service.impl;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.model.blobs.ArmResponseBatchData;
import uk.gov.hmcts.darts.arm.service.ExternalObjectDirectoryService;
import uk.gov.hmcts.darts.arm.util.files.BatchInputUploadFileFilenameProcessor;
import uk.gov.hmcts.darts.arm.util.files.CreateRecordFilenameProcessor;
import uk.gov.hmcts.darts.arm.util.files.UploadFileFilenameProcessor;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.service.impl.EodHelperMocks;
import uk.gov.hmcts.darts.common.util.EodHelper;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteArmResponseFilesHelperImplTest {

    private static final EodHelperMocks EOD_HELPER_MOCKS = new EodHelperMocks();

    @Mock
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    @Mock
    private ArmDataManagementApi armDataManagementApi;
    @Mock
    private ExternalObjectDirectoryService externalObjectDirectoryService;
    @Mock
    private BatchInputUploadFileFilenameProcessor batchInputUploadFileFilenameProcessor;
    private CreateRecordFilenameProcessor createRecordFilenameProcessor;
    private UploadFileFilenameProcessor uploadFileFilenameProcessor;

    private ExternalObjectDirectoryEntity eod;


    private DeleteArmResponseFilesHelperImpl deleteArmResponseFilesHelper;

    @BeforeEach
    void setUp() {
        deleteArmResponseFilesHelper = new DeleteArmResponseFilesHelperImpl(
            externalObjectDirectoryRepository,
            armDataManagementApi,
            externalObjectDirectoryService
        );
        eod = createExternalObjectDirectoryEntity();
        createRecordFilenameProcessor = new CreateRecordFilenameProcessor(
            "DARTS/response/6a374f19a9ce7dc9cc480ea8d4eca0fb_a17b9015-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp");
        uploadFileFilenameProcessor = new UploadFileFilenameProcessor(
            "DARTS/response/6a374f19a9ce7dc9cc480ea8d4eca0fb_04e6bc3b-952a-79b6-8362-13259aae1895_1_uf.rsp");
    }

    @AfterAll
    public static void close() {
        EOD_HELPER_MOCKS.close();
    }

    @Test
    void deleteResponseBlobsByManifestName_shouldDeleteBlobsWhenAllResponsesAreCompletedAndCleaned() {
        // given
        String manifestName = "DARTS_6a374f19a9ce7dc9cc480ea8d4eca0fb.a360";
        eod.setStatus(EodHelper.armRpoPendingStatus());
        eod.setManifestFile(manifestName);
        eod.setResponseCleaned(true);

        when(externalObjectDirectoryRepository.findByManifestFile(manifestName)).thenReturn(List.of(eod));
        when(batchInputUploadFileFilenameProcessor.getBatchMetadataFilename()).thenReturn(
            "DARTS_6a374f19a9ce7dc9cc480ea8d4eca0fb_04e6bc3b-952a-79b6-8362-13259aae1895_1_iu.rsp");
        when(batchInputUploadFileFilenameProcessor.getBatchMetadataFilenameAndPath()).thenReturn(
            "dropzone/DARTS/response/DARTS_6a374f19a9ce7dc9cc480ea8d4eca0fb_04e6bc3b-952a-79b6-8362-13259aae1895_1_iu.rsp");

        // when
        deleteArmResponseFilesHelper.deleteResponseBlobsByManifestName(batchInputUploadFileFilenameProcessor, manifestName);

        // then
        verify(armDataManagementApi).deleteBlobData(
            "dropzone/DARTS/response/DARTS_6a374f19a9ce7dc9cc480ea8d4eca0fb_04e6bc3b-952a-79b6-8362-13259aae1895_1_iu.rsp");
    }

    @Test
    void deleteDanglingResponses_shouldDeleteDanglingResponses() {
        // given
        BatchInputUploadFileFilenameProcessor processor = mock(BatchInputUploadFileFilenameProcessor.class);
        when(processor.getHashcode()).thenReturn("testHashcode");
        when(armDataManagementApi.listResponseBlobs("testHashcode")).thenReturn(List.of("responseBlob"));
        when(armDataManagementApi.deleteBlobData("responseBlob")).thenReturn(true);
        when(processor.getBatchMetadataFilename()).thenReturn("testFile");
        when(processor.getBatchMetadataFilenameAndPath()).thenReturn("testFilePath");

        // when
        deleteArmResponseFilesHelper.deleteDanglingResponses(processor);

        // then
        verify(armDataManagementApi).deleteBlobData("responseBlob");
        verify(armDataManagementApi).deleteBlobData("testFilePath");
    }

    @Test
    void deleteResponseBlobs_shouldDeleteAllResponseBlobs() {
        // given
        List<String> responseBlobs = List.of("blob1", "blob2");
        when(armDataManagementApi.deleteBlobData("blob1")).thenReturn(true);
        when(armDataManagementApi.deleteBlobData("blob2")).thenReturn(true);

        // when
        List<Boolean> result = deleteArmResponseFilesHelper.deleteResponseBlobs(responseBlobs);

        // then
        assertTrue(result.stream().allMatch(Boolean::booleanValue));
    }

    @Test
    void deleteResponseBlobs_shouldUpdateEodWhenResponsesAreDeleted() {
        // given
        ArmResponseBatchData batchData = mock(ArmResponseBatchData.class);
        when(batchData.getExternalObjectDirectoryId()).thenReturn(1);
        when(externalObjectDirectoryService.eagerLoadExternalObjectDirectory(1)).thenReturn(Optional.of(eod));

        eod.setStatus(EodHelper.armResponseChecksumVerificationFailedStatus());

        when(armDataManagementApi.deleteBlobData(anyString())).thenReturn(true);
        when(batchData.getCreateRecordFilenameProcessor()).thenReturn(createRecordFilenameProcessor);
        when(batchData.getUploadFileFilenameProcessor()).thenReturn(uploadFileFilenameProcessor);
        when(batchData.getInvalidLineFileFilenameProcessors()).thenReturn(Collections.emptyList());

        // when
        deleteArmResponseFilesHelper.deleteResponseBlobs(batchData);

        // then
        assertTrue(eod.isResponseCleaned());
        verify(externalObjectDirectoryRepository).saveAndFlush(eod);
    }

    private ExternalObjectDirectoryEntity createExternalObjectDirectoryEntity() {
        ExternalObjectDirectoryEntity eod = new ExternalObjectDirectoryEntity();
        eod.setId(1);
        return eod;
    }
}