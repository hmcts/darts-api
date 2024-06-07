package uk.gov.hmcts.darts.arm.service;


import com.azure.core.util.BinaryData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.test.common.data.MediaTestData;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.ARM;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;
import static uk.gov.hmcts.darts.test.common.TestUtils.getContentsFromFile;

class BatchCleanupArmResponseFilesServiceIntTest extends IntegrationBase {

    private static final LocalDateTime HEARING_DATE = LocalDateTime.of(2023, 9, 26, 10, 0, 0);

    @MockBean
    private ArmDataManagementApi armDataManagementApi;
    @MockBean
    private UserIdentity userIdentity;
    @MockBean
    private ArmDataManagementConfiguration armDataManagementConfiguration;
    @MockBean
    private CurrentTimeHelper currentTimeHelper;

    @Autowired
    private BatchCleanupArmResponseFilesService cleanupArmResponseFilesService;

    private MediaEntity savedMedia;

    @BeforeEach
    void setupData() {
        HearingEntity hearing = dartsDatabase.createHearing(
            "Bristol",
            "Int Test Courtroom 1",
            "1",
            HEARING_DATE
        );

        savedMedia = dartsDatabase.save(
            MediaTestData.createMediaWith(
                hearing.getCourtroom(),
                OffsetDateTime.parse("2023-09-26T13:00:00Z"),
                OffsetDateTime.parse("2023-09-26T13:45:00Z"),
                1
            ));
        dartsDatabase.save(savedMedia);

    }

    @Test
    /*
        IU  - childEod1   - response file1    - success delete
                          - response file2    - success delete
            - childEod2   - response file1    - success delete
                          - response file2    - success delete
     */
    void successProcess1InputUploadWith4AssociatedFilesOver2uuids() throws IOException {
        String manifestFilePrefix = "DARTS_";
        String inputUploadUUID = "InputUploadUUID";

        ExternalObjectDirectoryEntity armIuEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            STORED,
            ARM,
            UUID.randomUUID()
        );
        OffsetDateTime latestDateTime = OffsetDateTime.of(2023, 10, 27, 22, 0, 0, 0, ZoneOffset.UTC);
        String manifestFilename = manifestFilePrefix + inputUploadUUID + ".a360";
        armIuEod.setLastModifiedDateTime(latestDateTime);
        armIuEod.setTransferAttempts(1);
        armIuEod.setResponseCleaned(false);
        armIuEod.setManifestFile(manifestFilename);
        armIuEod = dartsDatabase.save(armIuEod);

        OffsetDateTime testTime = OffsetDateTime.now().plusMinutes(20);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(testTime);


        when(armDataManagementConfiguration.getBatchResponseCleanupBufferMinutes()).thenReturn(15);
        when(armDataManagementConfiguration.getManifestFilePrefix()).thenReturn(manifestFilePrefix);

        String inputUploadHash = "6a374f19a9ce7dc9cc480ea8d4eca0fb";
        String inputUploadFilename = manifestFilePrefix + inputUploadUUID + "_" + inputUploadHash + "_1_iu.rsp";
        when(armDataManagementApi.listResponseBlobs("DARTS_InputUploadUUID")).thenReturn(List.of(inputUploadFilename));

        String createRecordFilename1 = inputUploadHash + "_00000001-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp";
        String uploadFileFilename1 = inputUploadHash + "_00000002-952a-79b6-8362-13259aae1895_1_uf.rsp";
        String createRecordFilename2 = inputUploadHash + "_00000003-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp";
        String uploadFileFilename2 = inputUploadHash + "_00000004-952a-79b6-8362-13259aae1895_1_uf.rsp";
        when(armDataManagementApi.listResponseBlobs(inputUploadHash)).thenReturn(
            List.of(createRecordFilename1, uploadFileFilename1, createRecordFilename2, uploadFileFilename2));

        ExternalObjectDirectoryEntity armChildEod1 = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            STORED,
            ARM,
            UUID.randomUUID()
        );
        String createRecordFileTemplate = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/CreateRecord.rsp";
        BinaryData createRecordBinaryDataTest1 = convertStringToBinaryData(getCreateRecordFileContents(createRecordFileTemplate, armChildEod1.getId()));
        when(armDataManagementApi.getBlobData(createRecordFilename1)).thenReturn(createRecordBinaryDataTest1);

        String validUploadFileTemplate = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/UploadFile.rsp";
        BinaryData uploadFileBinaryDataTest1 = convertStringToBinaryData(getUploadFileContents(validUploadFileTemplate, armChildEod1.getId(), "123"));
        when(armDataManagementApi.getBlobData(uploadFileFilename1)).thenReturn(uploadFileBinaryDataTest1);


        ExternalObjectDirectoryEntity armChildEod2 = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            STORED,
            ARM,
            UUID.randomUUID()
        );
        BinaryData createRecordBinaryDataTest2 = convertStringToBinaryData(getCreateRecordFileContents(createRecordFileTemplate, armChildEod2.getId()));
        when(armDataManagementApi.getBlobData(createRecordFilename2)).thenReturn(createRecordBinaryDataTest2);

        BinaryData uploadFileBinaryDataTest2 = convertStringToBinaryData(getUploadFileContents(validUploadFileTemplate, armChildEod2.getId(), "123"));
        when(armDataManagementApi.getBlobData(uploadFileFilename2)).thenReturn(uploadFileBinaryDataTest2);


        when(armDataManagementApi.deleteBlobData(inputUploadFilename)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(createRecordFilename1)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(uploadFileFilename1)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(createRecordFilename2)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(uploadFileFilename2)).thenReturn(true);

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);


        cleanupArmResponseFilesService.cleanupResponseFiles(100);

        ExternalObjectDirectoryEntity foundMediaEod = dartsDatabase.getExternalObjectDirectoryRepository().findById(armIuEod.getId())
            .orElseThrow();
        assertTrue(foundMediaEod.isResponseCleaned());

        ExternalObjectDirectoryEntity foundMediaEod1 = dartsDatabase.getExternalObjectDirectoryRepository().findById(armChildEod1.getId())
            .orElseThrow();
        assertTrue(foundMediaEod1.isResponseCleaned());

        ExternalObjectDirectoryEntity foundMediaEod2 = dartsDatabase.getExternalObjectDirectoryRepository().findById(armChildEod2.getId())
            .orElseThrow();
        assertTrue(foundMediaEod2.isResponseCleaned());

        verify(armDataManagementApi).listResponseBlobs(inputUploadHash);
        verify(armDataManagementApi).listResponseBlobs(manifestFilePrefix + inputUploadUUID);
        verify(armDataManagementApi).getBlobData(createRecordFilename1);
        verify(armDataManagementApi).getBlobData(uploadFileFilename1);
        verify(armDataManagementApi).getBlobData(createRecordFilename2);
        verify(armDataManagementApi).getBlobData(uploadFileFilename2);

        verify(armDataManagementApi).deleteBlobData(createRecordFilename1);
        verify(armDataManagementApi).deleteBlobData(uploadFileFilename1);
        verify(armDataManagementApi).deleteBlobData(createRecordFilename2);
        verify(armDataManagementApi).deleteBlobData(uploadFileFilename2);
        verify(armDataManagementApi).deleteBlobData(inputUploadFilename);
        verifyNoMoreInteractions(armDataManagementApi);
    }

    @Test
    /*
        IU  - childEod1   - createRecord file    - success delete
                          - uploadFile           - success delete
            - childEod2   - createRecord file    - fail delete
                          - uploadFile           - delete not attempted
            - childEod3   - createRecord file    - success delete
                          - uploadFile           - success delete
     */
    void successProcess1InputUploadWith6AssociatedFilesOver3uuids1ResponseFail() throws IOException {

        String manifestFilePrefix = "DARTS_";
        String inputUploadUUID = "InputUploadUUID";

        ExternalObjectDirectoryEntity armIuEod = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            STORED,
            ARM,
            UUID.randomUUID()
        );
        OffsetDateTime latestDateTime = OffsetDateTime.of(2023, 10, 27, 22, 0, 0, 0, ZoneOffset.UTC);
        String manifestFilename = manifestFilePrefix + inputUploadUUID + ".a360";
        armIuEod.setLastModifiedDateTime(latestDateTime);
        armIuEod.setTransferAttempts(1);
        armIuEod.setResponseCleaned(false);
        armIuEod.setManifestFile(manifestFilename);
        armIuEod = dartsDatabase.save(armIuEod);

        OffsetDateTime testTime = OffsetDateTime.now().plusMinutes(20);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(testTime);


        when(armDataManagementConfiguration.getBatchResponseCleanupBufferMinutes()).thenReturn(15);
        when(armDataManagementConfiguration.getManifestFilePrefix()).thenReturn(manifestFilePrefix);

        String inputUploadHash = "6a374f19a9ce7dc9cc480ea8d4eca0fb";
        String inputUploadFilename = manifestFilePrefix + inputUploadUUID + "_" + inputUploadHash + "_1_iu.rsp";
        when(armDataManagementApi.listResponseBlobs("DARTS_InputUploadUUID")).thenReturn(List.of(inputUploadFilename));

        String createRecordFilename1 = inputUploadHash + "_00000001-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp";
        String uploadFileFilename1 = inputUploadHash + "_00000002-952a-79b6-8362-13259aae1895_1_uf.rsp";
        String createRecordFilename2 = inputUploadHash + "_00000003-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp";
        String uploadFileFilename2 = inputUploadHash + "_00000004-952a-79b6-8362-13259aae1895_1_uf.rsp";
        String createRecordFilename3 = inputUploadHash + "_00000005-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp";
        String uploadFileFilename3 = inputUploadHash + "_00000006-952a-79b6-8362-13259aae1895_1_uf.rsp";
        when(armDataManagementApi.listResponseBlobs(inputUploadHash)).thenReturn(
            List.of(createRecordFilename1, uploadFileFilename1, createRecordFilename2, uploadFileFilename2, createRecordFilename3, uploadFileFilename3));

        ExternalObjectDirectoryEntity armChildEod1 = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            STORED,
            ARM,
            UUID.randomUUID()
        );
        String createRecordFileTemplate = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/CreateRecord.rsp";
        BinaryData createRecordBinaryDataTest1 = convertStringToBinaryData(getCreateRecordFileContents(createRecordFileTemplate, armChildEod1.getId()));
        when(armDataManagementApi.getBlobData(createRecordFilename1)).thenReturn(createRecordBinaryDataTest1);

        String validUploadFileTemplate = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/UploadFile.rsp";
        BinaryData uploadFileBinaryDataTest1 = convertStringToBinaryData(getUploadFileContents(validUploadFileTemplate, armChildEod1.getId(), "123"));
        when(armDataManagementApi.getBlobData(uploadFileFilename1)).thenReturn(uploadFileBinaryDataTest1);


        ExternalObjectDirectoryEntity armChildEod2 = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            STORED,
            ARM,
            UUID.randomUUID()
        );
        BinaryData createRecordBinaryDataTest2 = convertStringToBinaryData(getCreateRecordFileContents(createRecordFileTemplate, armChildEod2.getId()));
        when(armDataManagementApi.getBlobData(createRecordFilename2)).thenReturn(createRecordBinaryDataTest2);

        BinaryData uploadFileBinaryDataTest2 = convertStringToBinaryData(getUploadFileContents(validUploadFileTemplate, armChildEod2.getId(), "123"));
        when(armDataManagementApi.getBlobData(uploadFileFilename2)).thenReturn(uploadFileBinaryDataTest2);

        ExternalObjectDirectoryEntity armChildEod3 = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            STORED,
            ARM,
            UUID.randomUUID()
        );
        BinaryData createRecordBinaryDataTest3 = convertStringToBinaryData(getCreateRecordFileContents(createRecordFileTemplate, armChildEod3.getId()));
        when(armDataManagementApi.getBlobData(createRecordFilename3)).thenReturn(createRecordBinaryDataTest3);

        BinaryData uploadFileBinaryDataTest3 = convertStringToBinaryData(getUploadFileContents(validUploadFileTemplate, armChildEod3.getId(), "123"));
        when(armDataManagementApi.getBlobData(uploadFileFilename3)).thenReturn(uploadFileBinaryDataTest3);


        when(armDataManagementApi.deleteBlobData(inputUploadFilename)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(createRecordFilename1)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(uploadFileFilename1)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(createRecordFilename2)).thenReturn(false);
        when(armDataManagementApi.deleteBlobData(uploadFileFilename2)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(createRecordFilename3)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(uploadFileFilename3)).thenReturn(true);

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);


        cleanupArmResponseFilesService.cleanupResponseFiles(100);

        ExternalObjectDirectoryEntity foundChildEod = dartsDatabase.getExternalObjectDirectoryRepository().findById(armIuEod.getId())
            .orElseThrow();
        assertFalse(foundChildEod.isResponseCleaned());

        ExternalObjectDirectoryEntity foundChildEod1 = dartsDatabase.getExternalObjectDirectoryRepository().findById(armChildEod1.getId())
            .orElseThrow();
        assertTrue(foundChildEod1.isResponseCleaned());

        ExternalObjectDirectoryEntity foundChildEod2 = dartsDatabase.getExternalObjectDirectoryRepository().findById(armChildEod2.getId())
            .orElseThrow();
        assertFalse(foundChildEod2.isResponseCleaned());

        ExternalObjectDirectoryEntity foundChildEod3 = dartsDatabase.getExternalObjectDirectoryRepository().findById(armChildEod3.getId())
            .orElseThrow();
        assertTrue(foundChildEod3.isResponseCleaned());

        verify(armDataManagementApi).listResponseBlobs(inputUploadHash);
        verify(armDataManagementApi).listResponseBlobs(manifestFilePrefix + inputUploadUUID);
        verify(armDataManagementApi).getBlobData(createRecordFilename1);
        verify(armDataManagementApi).getBlobData(uploadFileFilename1);
        verify(armDataManagementApi).getBlobData(createRecordFilename2);
        verify(armDataManagementApi).getBlobData(uploadFileFilename2);
        verify(armDataManagementApi).getBlobData(createRecordFilename3);
        verify(armDataManagementApi).getBlobData(uploadFileFilename3);

        verify(armDataManagementApi).deleteBlobData(createRecordFilename1);
        verify(armDataManagementApi).deleteBlobData(uploadFileFilename1);
        verify(armDataManagementApi).deleteBlobData(createRecordFilename2);
        verify(armDataManagementApi, times(0)).deleteBlobData(uploadFileFilename2);
        verify(armDataManagementApi).deleteBlobData(createRecordFilename3);
        verify(armDataManagementApi).deleteBlobData(uploadFileFilename3);
        verifyNoMoreInteractions(armDataManagementApi);
    }


    private BinaryData convertStringToBinaryData(String contents) {
        return BinaryData.fromString(contents);
    }

    private String getCreateRecordFileContents(String createRecordFilename, Integer externalObjectDirectoryId) throws IOException {
        String expectedResponse = getContentsFromFile(createRecordFilename);
        expectedResponse = expectedResponse.replaceAll("<EODID>", String.valueOf(externalObjectDirectoryId));
        return expectedResponse;
    }

    private String getUploadFileContents(String uploadFilename, int externalObjectDirectoryId, String checksum) throws IOException {
        String expectedResponse = getContentsFromFile(uploadFilename);
        expectedResponse = expectedResponse.replaceAll("<CHECKSUM>", checksum);
        expectedResponse = expectedResponse.replaceAll("<EODID>", String.valueOf(externalObjectDirectoryId));
        return expectedResponse;
    }


}
