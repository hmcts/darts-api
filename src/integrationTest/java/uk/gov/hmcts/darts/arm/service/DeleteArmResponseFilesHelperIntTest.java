package uk.gov.hmcts.darts.arm.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.model.blobs.ArmResponseBatchData;
import uk.gov.hmcts.darts.arm.service.impl.DeleteArmResponseFilesHelperImpl;
import uk.gov.hmcts.darts.arm.util.files.BatchInputUploadFileFilenameProcessor;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.ARM;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RAW_DATA_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RPO_PENDING;

public class DeleteArmResponseFilesHelperIntTest extends PostgresIntegrationBase {

    @Autowired
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;

    @MockBean
    private ArmDataManagementApi armDataManagementApi;

    @MockBean
    private UserIdentity userIdentity;

    @Autowired
    private ExternalObjectDirectoryService externalObjectDirectoryService;

    @Autowired
    private DeleteArmResponseFilesHelperImpl deleteArmResponseFilesHelper;

    private ExternalObjectDirectoryEntity eodRpoPending;
    private ExternalObjectDirectoryEntity eodFailed;

    private UserAccountEntity testUser;


    @BeforeEach
    void setUp() {
        testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);
        List<MediaEntity> medias = dartsDatabase.getMediaStub().createAndSaveSomeMedias();
        eodRpoPending = dartsDatabase.getExternalObjectDirectoryStub().createAndSaveEod(medias.get(0), ARM_RPO_PENDING, ARM);
        eodFailed = dartsDatabase.getExternalObjectDirectoryStub().createAndSaveEod(medias.get(1), ARM_RAW_DATA_FAILED, ARM);
        dartsDatabase.save(eodRpoPending);
        dartsDatabase.save(eodFailed);
    }

    @Test
    void deleteResponseBlobsByManifestName_shouldDeleteBlobsWhenAllResponsesAreCompletedAndCleaned() {
        // given
        String manifestName = "DARTS_6a374f19a9ce7dc9cc480ea8d4eca0fb.a360";
        eodRpoPending.setManifestFile(manifestName);
        eodRpoPending.setResponseCleaned(true);
        dartsDatabase.save(eodRpoPending);
        eodFailed.setManifestFile(manifestName);
        eodFailed.setResponseCleaned(false);
        dartsDatabase.save(eodFailed);

        BatchInputUploadFileFilenameProcessor batchInputUploadFileFilenameProcessor = new BatchInputUploadFileFilenameProcessor(
            "dropzone/DARTS/response/DARTS_6a374f19a9ce7dc9cc480ea8d4eca0fb_04e6bc3b-952a-79b6-8362-13259aae1895_1_iu.rsp");

        when(armDataManagementApi.deleteBlobData(anyString())).thenReturn(true);


        // when
        deleteArmResponseFilesHelper.deleteResponseBlobsByManifestName(batchInputUploadFileFilenameProcessor, manifestName);

        // then
        assertTrue(armDataManagementApi.deleteBlobData(
            "dropzone/DARTS/response/DARTS_6a374f19a9ce7dc9cc480ea8d4eca0fb_04e6bc3b-952a-79b6-8362-13259aae1895_1_iu.rsp"));
    }

    @Test
    void deleteDanglingResponses_shouldDeleteDanglingResponses() {
        // given
        BatchInputUploadFileFilenameProcessor processor = new BatchInputUploadFileFilenameProcessor("testHashcode");
        armDataManagementApi.listResponseBlobs("testHashcode").add("responseBlob");

        // when
        deleteArmResponseFilesHelper.deleteDanglingResponses(processor);

        // then
        assertTrue(armDataManagementApi.deleteBlobData("responseBlob"));
        assertTrue(armDataManagementApi.deleteBlobData("testFilePath"));
    }

    @Test
    void deleteResponseBlobs_shouldDeleteAllResponseBlobs() {
        // given
        List<String> responseBlobs = List.of("blob1", "blob2");

        // when
        List<Boolean> result = deleteArmResponseFilesHelper.deleteResponseBlobs(responseBlobs);

        // then
        assertTrue(result.stream().allMatch(Boolean::booleanValue));
    }

    @Test
    void deleteResponseBlobs_shouldUpdateEodWhenResponsesAreDeleted() {
        // given
        ArmResponseBatchData batchData = ArmResponseBatchData.builder().build();
        batchData.setExternalObjectDirectoryId(1);
        eodRpoPending.setStatus(EodHelper.armResponseChecksumVerificationFailedStatus());
        dartsDatabase.save(eodRpoPending);

        // when
        deleteArmResponseFilesHelper.deleteResponseBlobs(batchData);

        // then
        assertTrue(eodRpoPending.isResponseCleaned());
        externalObjectDirectoryRepository.saveAndFlush(eodRpoPending);
    }

}
