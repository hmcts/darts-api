package uk.gov.hmcts.darts.arm.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.model.blobs.ArmResponseBatchData;
import uk.gov.hmcts.darts.arm.service.ExternalObjectDirectoryService;
import uk.gov.hmcts.darts.arm.util.files.BatchInputUploadFileFilenameProcessor;
import uk.gov.hmcts.darts.arm.util.files.CreateRecordFilenameProcessor;
import uk.gov.hmcts.darts.arm.util.files.UploadFileFilenameProcessor;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
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

    @Autowired
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;

    @MockitoBean
    private ArmDataManagementApi armDataManagementApi;

    @MockitoBean
    private UserIdentity userIdentity;

    @Autowired
    private ExternalObjectDirectoryService externalObjectDirectoryService;

    @Autowired
    private DeleteArmResponseFilesHelperImpl deleteArmResponseFilesHelper;

    private ExternalObjectDirectoryEntity eodRpoPending;
    private ExternalObjectDirectoryEntity eodFailed;

    @BeforeEach
    void setUp() {
        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);
        List<MediaEntity> medias = dartsDatabase.getMediaStub().createAndSaveSomeMedias();
        eodRpoPending = dartsDatabase.getExternalObjectDirectoryStub().createAndSaveEod(medias.get(0), ARM_RPO_PENDING, ARM);
        eodFailed = dartsDatabase.getExternalObjectDirectoryStub().createAndSaveEod(medias.get(1), ARM_RESPONSE_CHECKSUM_VERIFICATION_FAILED, ARM);
        dartsDatabase.save(eodRpoPending);
        dartsDatabase.save(eodFailed);
    }

    @Test
    void deleteResponseBlobsByManifestName_shouldDeleteBlobsWhenAllResponsesAreCompletedAndCleaned() {
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
        String responseFile = "dropzone/DARTS/response/" + RESPONSE_FILE_PREFIX + "_ABC_1_rsp";
        when(armDataManagementApi.listResponseBlobs(any())).thenReturn(List.of(responseFile));
        when(armDataManagementApi.deleteBlobData(anyString())).thenReturn(true);

        // when
        deleteArmResponseFilesHelper.deleteDanglingResponses(batchInputUploadFileFilenameProcessor);

        // then
        verify(armDataManagementApi).deleteBlobData(responseFile);
        verify(armDataManagementApi).deleteBlobData(DARTS_INPUT_UPLOAD_FILE);
        verify(armDataManagementApi).listResponseBlobs(batchInputUploadFileFilenameProcessor.getHashcode());
        verifyNoMoreInteractions(armDataManagementApi);
    }

    @Test
    void deleteResponseBlobs_shouldDeleteAllResponseBlobs() {
        // given
        List<String> responseBlobs = List.of("blob1", "blob2");
        when(armDataManagementApi.deleteBlobData(anyString())).thenReturn(true);

        // when
        List<Boolean> result = deleteArmResponseFilesHelper.deleteResponseBlobs(responseBlobs);

        // then
        assertTrue(result.stream().allMatch(Boolean::booleanValue));
        verify(armDataManagementApi).deleteBlobData("blob1");
        verify(armDataManagementApi).deleteBlobData("blob2");
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

        when(armDataManagementApi.deleteBlobData(anyString())).thenReturn(true);

        // when
        deleteArmResponseFilesHelper.deleteResponseBlobs(batchData);

        // then
        verify(armDataManagementApi).deleteBlobData(createRecordFilename1);
        verify(armDataManagementApi).deleteBlobData(uploadFileFilename1);
        verifyNoMoreInteractions(armDataManagementApi);
    }

}
