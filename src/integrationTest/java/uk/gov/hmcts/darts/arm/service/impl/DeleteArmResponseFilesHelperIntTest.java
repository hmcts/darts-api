package uk.gov.hmcts.darts.arm.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.model.blobs.ArmResponseBatchData;
import uk.gov.hmcts.darts.arm.util.files.BatchInputUploadFileFilenameProcessor;
import uk.gov.hmcts.darts.arm.util.files.CreateRecordFilenameProcessor;
import uk.gov.hmcts.darts.arm.util.files.UploadFileFilenameProcessor;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.ARM;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RESPONSE_CHECKSUM_VERIFICATION_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RPO_PENDING;

class DeleteArmResponseFilesHelperIntTest extends PostgresIntegrationBase {

    private static final String DARTS_INPUT_UPLOAD_FILE =
        "dropzone/DARTS/response/DARTS_6a374f19a9ce7dc9cc480ea8d4eca0fb_04e6bc3b-952a-79b6-8362-13259aae1895_1_iu.rsp";
    private static final String MANIFEST_PREFIX = "DARTS_6a374f19a9ce7dc9cc480ea8d4eca0fb";
    private static final String RESPONSE_FILE_PREFIX = "04e6bc3b-952a-79b6-8362-13259aae1895";

    @MockitoBean
    private ArmDataManagementApi armDataManagementApi;

    @MockitoBean
    private UserIdentity userIdentity;

    @Autowired
    private DeleteArmResponseFilesHelperImpl deleteArmResponseFilesHelper;

    private ExternalObjectDirectoryEntity eodRpoPending;
    private ExternalObjectDirectoryEntity eodFailed;

    @BeforeEach
    void setUp() {
        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);
        List<MediaEntity> medias = dartsDatabase.getMediaStub().createAndSaveSomeMedias();
        eodRpoPending = dartsDatabase.getExternalObjectDirectoryStub().createAndSaveEod(medias.getFirst(), ARM_RPO_PENDING, ARM);
        eodFailed = dartsDatabase.getExternalObjectDirectoryStub().createAndSaveEod(medias.get(1), ARM_RESPONSE_CHECKSUM_VERIFICATION_FAILED, ARM);
        dartsDatabase.save(eodRpoPending);
        dartsDatabase.save(eodFailed);
    }

    @Test
    void deleteResponseBlobsByManifestName_shouldDeleteBlobsIndividuallyWhenAllResponsesAreCompletedAndCleaned() {
        // given
        String manifestName = MANIFEST_PREFIX + ".a360";
        eodRpoPending.setManifestFile(manifestName);
        eodRpoPending.setResponseCleaned(true);
        dartsDatabase.save(eodRpoPending);
        eodFailed.setManifestFile(manifestName);
        eodFailed.setResponseCleaned(true);
        dartsDatabase.save(eodFailed);

        BatchInputUploadFileFilenameProcessor batchInputUploadFileFilenameProcessor = new BatchInputUploadFileFilenameProcessor(DARTS_INPUT_UPLOAD_FILE);

        when(armDataManagementApi.deleteBlobData(anyString())).thenReturn(true);

        // when
        deleteArmResponseFilesHelper.deleteResponseBlobsByManifestName(batchInputUploadFileFilenameProcessor, manifestName);

        // then
        verify(armDataManagementApi).deleteBlobData(DARTS_INPUT_UPLOAD_FILE);
        verifyNoMoreInteractions(armDataManagementApi);
    }

    @Test
    void deleteDanglingResponses_shouldDeleteDanglingResponses() {
        // given
        BatchInputUploadFileFilenameProcessor batchInputUploadFileFilenameProcessor = new BatchInputUploadFileFilenameProcessor(DARTS_INPUT_UPLOAD_FILE);
        String otherResponseFile = "dropzone/DARTS/response/" + RESPONSE_FILE_PREFIX + "_ABC_1_rsp";
        String crResponseFile = "dropzone/DARTS/response/" + RESPONSE_FILE_PREFIX + "_b17b9015-e6ad-77c5-8d1e-13259aae1896_0_cr.rsp";
        String ilResponseFile = "dropzone/DARTS/response/" + RESPONSE_FILE_PREFIX + "_c17b9015-e6ad-77c5-8d1e-13259aae1896_1_il.rsp";

        when(armDataManagementApi.listResponseBlobs(any())).thenReturn(List.of(otherResponseFile, crResponseFile, ilResponseFile));
        when(armDataManagementApi.deleteMultipleBlobs(any())).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(anyString())).thenReturn(true);

        // when
        deleteArmResponseFilesHelper.deleteDanglingResponses(batchInputUploadFileFilenameProcessor);

        // then
        verify(armDataManagementApi).deleteMultipleBlobs(List.of(otherResponseFile, crResponseFile, ilResponseFile));
        verify(armDataManagementApi).deleteBlobData(DARTS_INPUT_UPLOAD_FILE);
        verify(armDataManagementApi).listResponseBlobs(batchInputUploadFileFilenameProcessor.getHashcode());
        verifyNoMoreInteractions(armDataManagementApi);
    }

    @Test
    void deleteResponseBlobsIndividually_shouldDeleteAllResponseBlobIndividually() {
        // given
        String responseBlob = "blob1";
        when(armDataManagementApi.deleteBlobData(anyString())).thenReturn(true);

        // when
        Boolean result = deleteArmResponseFilesHelper.deleteResponseBlobIndividually(responseBlob);

        // then
        assertTrue(result);
        verify(armDataManagementApi).deleteBlobData("blob1");
        verifyNoMoreInteractions(armDataManagementApi);
    }

    @Test
    void deleteResponseBlobs_shouldUpdateEodWhenResponsesAreDeleted() {
        // given
        String createRecordFilename1 = String.format("%s_a17b9015-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp", RESPONSE_FILE_PREFIX);
        String uploadFileFilename1 = String.format("%s_04e6bc3b-952a-79b6-8362-13259aae1895_1_uf.rsp", RESPONSE_FILE_PREFIX);

        ArmResponseBatchData batchData = ArmResponseBatchData.builder()
            .externalObjectDirectoryId(eodRpoPending.getId())
            .createRecordFilenameProcessor(new CreateRecordFilenameProcessor(createRecordFilename1))
            .uploadFileFilenameProcessor(new UploadFileFilenameProcessor(uploadFileFilename1))
            .build();

        when(armDataManagementApi.deleteMultipleBlobs(any())).thenReturn(true);

        // when
        deleteArmResponseFilesHelper.deleteResponseBlobs(batchData);

        // then
        verify(armDataManagementApi).deleteMultipleBlobs(List.of(createRecordFilename1, uploadFileFilename1));
        verifyNoMoreInteractions(armDataManagementApi);
    }

}
