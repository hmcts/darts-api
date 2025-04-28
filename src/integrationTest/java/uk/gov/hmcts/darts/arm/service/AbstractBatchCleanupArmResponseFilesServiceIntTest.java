package uk.gov.hmcts.darts.arm.service;


import com.azure.core.util.BinaryData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.ARM;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;
import static uk.gov.hmcts.darts.test.common.TestUtils.getContentsFromFile;

@SuppressWarnings({"PMD.NcssCount"})
abstract class AbstractBatchCleanupArmResponseFilesServiceIntTest extends IntegrationBase {

    @MockitoBean
    private ArmDataManagementApi armDataManagementApi;
    @MockitoBean
    private UserIdentity userIdentity;
    @MockitoBean
    private CurrentTimeHelper currentTimeHelper;

    @Autowired
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;

    private MediaEntity savedMedia;

    private final String manifestFilePrefix;

    protected AbstractBatchCleanupArmResponseFilesServiceIntTest(String manifestFilePrefix) {
        super();
        this.manifestFilePrefix = manifestFilePrefix + "_";
    }

    @BeforeEach
    void setupData() {
        savedMedia = PersistableFactory.getMediaTestData()
            .someMinimal();
        dartsPersistence.save(savedMedia);
    }

    @Test
    /*
        IU  - childEod1   - response file1    - success delete
                          - response file2    - success delete
            - childEod2   - response file1    - success delete
                          - response file2    - success delete
     */
    void successProcess1InputUploadWith4AssociatedFilesOver2uuids() throws IOException {
        String inputUploadUuid = "InputUploadUUID";

        OffsetDateTime lastModifiedDateTime = OffsetDateTime.of(2023, 10, 27, 22, 0, 0, 0, ZoneOffset.UTC);
        String manifestFilename = manifestFilePrefix + inputUploadUuid + ".a360";

        ExternalObjectDirectoryEntity eodEntry1 = createEodEntry(manifestFilename, lastModifiedDateTime);
        ExternalObjectDirectoryEntity eodEntry2 = createEodEntry(manifestFilename, lastModifiedDateTime);

        OffsetDateTime testTime = OffsetDateTime.now().plusMinutes(20);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(testTime);

        String inputUploadHash = "6a374f19a9ce7dc9cc480ea8d4eca0fb";
        String inputUploadFilename = manifestFilePrefix + inputUploadUuid + "_" + inputUploadHash + "_1_iu.rsp";
        when(armDataManagementApi.listResponseBlobs(manifestFilePrefix + "InputUploadUUID")).thenReturn(List.of(inputUploadFilename));

        String createRecordFilename1 = inputUploadHash + "_00000001-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp";
        String uploadFileFilename1 = inputUploadHash + "_00000002-952a-79b6-8362-13259aae1895_1_uf.rsp";
        String createRecordFilename2 = inputUploadHash + "_00000003-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp";
        String uploadFileFilename2 = inputUploadHash + "_00000004-952a-79b6-8362-13259aae1895_1_uf.rsp";
        when(armDataManagementApi.listResponseBlobs(inputUploadHash)).thenReturn(
            List.of(createRecordFilename1, uploadFileFilename1, createRecordFilename2, uploadFileFilename2));

        String createRecordFileTemplate = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/CreateRecord.rsp";
        BinaryData createRecordBinaryDataTest1 = convertStringToBinaryData(getCreateRecordFileContents(createRecordFileTemplate, eodEntry1.getId()));
        when(armDataManagementApi.getBlobData(createRecordFilename1)).thenReturn(createRecordBinaryDataTest1);

        String validUploadFileTemplate = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/UploadFile.rsp";
        BinaryData uploadFileBinaryDataTest1 = convertStringToBinaryData(getUploadFileContents(validUploadFileTemplate, eodEntry1.getId(), "123"));
        when(armDataManagementApi.getBlobData(uploadFileFilename1)).thenReturn(uploadFileBinaryDataTest1);


        BinaryData createRecordBinaryDataTest2 = convertStringToBinaryData(getCreateRecordFileContents(createRecordFileTemplate, eodEntry2.getId()));
        when(armDataManagementApi.getBlobData(createRecordFilename2)).thenReturn(createRecordBinaryDataTest2);

        BinaryData uploadFileBinaryDataTest2 = convertStringToBinaryData(getUploadFileContents(validUploadFileTemplate, eodEntry2.getId(), "123"));
        when(armDataManagementApi.getBlobData(uploadFileFilename2)).thenReturn(uploadFileBinaryDataTest2);


        when(armDataManagementApi.deleteBlobData(inputUploadFilename)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(createRecordFilename1)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(uploadFileFilename1)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(createRecordFilename2)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(uploadFileFilename2)).thenReturn(true);

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);


        getCleanupArmResponseFilesService().cleanupResponseFiles(100);

        ExternalObjectDirectoryEntity foundMediaEod1 = externalObjectDirectoryRepository.findById(eodEntry1.getId())
            .orElseThrow();
        assertTrue(foundMediaEod1.isResponseCleaned());

        ExternalObjectDirectoryEntity foundMediaEod2 = externalObjectDirectoryRepository.findById(eodEntry2.getId())
            .orElseThrow();
        assertTrue(foundMediaEod2.isResponseCleaned());

        verify(armDataManagementApi).listResponseBlobs(inputUploadHash);
        verify(armDataManagementApi).listResponseBlobs(manifestFilePrefix + inputUploadUuid);
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

    protected abstract BatchCleanupArmResponseFilesService getCleanupArmResponseFilesService();

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
        String inputUploadUuid = "InputUploadUUID";
        String manifestFilename = manifestFilePrefix + inputUploadUuid + ".a360";

        OffsetDateTime lastModifiedDateTime = OffsetDateTime.of(2023, 10, 27, 22, 0, 0, 0, ZoneOffset.UTC);
        ExternalObjectDirectoryEntity eodEntry1 = createEodEntry(manifestFilename, lastModifiedDateTime);
        ExternalObjectDirectoryEntity eodEntry2 = createEodEntry(manifestFilename, lastModifiedDateTime);
        ExternalObjectDirectoryEntity eodEntry3 = createEodEntry(manifestFilename, lastModifiedDateTime);

        OffsetDateTime testTime = OffsetDateTime.now().plusMinutes(20);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(testTime);

        String inputUploadHash = "6a374f19a9ce7dc9cc480ea8d4eca0fb";
        String inputUploadFilename = manifestFilePrefix + inputUploadUuid + "_" + inputUploadHash + "_1_iu.rsp";
        when(armDataManagementApi.listResponseBlobs(manifestFilePrefix + "InputUploadUUID")).thenReturn(List.of(inputUploadFilename));

        String createRecordFilename1 = inputUploadHash + "_00000001-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp";
        String uploadFileFilename1 = inputUploadHash + "_00000002-952a-79b6-8362-13259aae1895_1_uf.rsp";
        String createRecordFilename2 = inputUploadHash + "_00000003-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp";
        String uploadFileFilename2 = inputUploadHash + "_00000004-952a-79b6-8362-13259aae1895_1_uf.rsp";
        String createRecordFilename3 = inputUploadHash + "_00000005-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp";
        String uploadFileFilename3 = inputUploadHash + "_00000006-952a-79b6-8362-13259aae1895_1_uf.rsp";
        when(armDataManagementApi.listResponseBlobs(inputUploadHash)).thenReturn(
            List.of(createRecordFilename1, uploadFileFilename1, createRecordFilename2, uploadFileFilename2, createRecordFilename3, uploadFileFilename3));

        String createRecordFileTemplate = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/CreateRecord.rsp";
        BinaryData createRecordBinaryDataTest1 = convertStringToBinaryData(getCreateRecordFileContents(createRecordFileTemplate, eodEntry1.getId()));
        when(armDataManagementApi.getBlobData(createRecordFilename1)).thenReturn(createRecordBinaryDataTest1);

        String validUploadFileTemplate = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/UploadFile.rsp";
        BinaryData uploadFileBinaryDataTest1 = convertStringToBinaryData(getUploadFileContents(validUploadFileTemplate, eodEntry1.getId(), "123"));
        when(armDataManagementApi.getBlobData(uploadFileFilename1)).thenReturn(uploadFileBinaryDataTest1);


        BinaryData createRecordBinaryDataTest2 = convertStringToBinaryData(getCreateRecordFileContents(createRecordFileTemplate, eodEntry2.getId()));
        when(armDataManagementApi.getBlobData(createRecordFilename2)).thenReturn(createRecordBinaryDataTest2);

        BinaryData uploadFileBinaryDataTest2 = convertStringToBinaryData(getUploadFileContents(validUploadFileTemplate, eodEntry2.getId(), "123"));
        when(armDataManagementApi.getBlobData(uploadFileFilename2)).thenReturn(uploadFileBinaryDataTest2);

        BinaryData createRecordBinaryDataTest3 = convertStringToBinaryData(getCreateRecordFileContents(createRecordFileTemplate, eodEntry3.getId()));
        when(armDataManagementApi.getBlobData(createRecordFilename3)).thenReturn(createRecordBinaryDataTest3);

        BinaryData uploadFileBinaryDataTest3 = convertStringToBinaryData(getUploadFileContents(validUploadFileTemplate, eodEntry3.getId(), "123"));
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


        getCleanupArmResponseFilesService().cleanupResponseFiles(100);

        ExternalObjectDirectoryEntity foundChildEod1 = dartsDatabase.getExternalObjectDirectoryRepository().findById(eodEntry1.getId())
            .orElseThrow();
        assertTrue(foundChildEod1.isResponseCleaned());

        ExternalObjectDirectoryEntity foundChildEod2 = dartsDatabase.getExternalObjectDirectoryRepository().findById(eodEntry2.getId())
            .orElseThrow();
        assertFalse(foundChildEod2.isResponseCleaned());

        ExternalObjectDirectoryEntity foundChildEod3 = dartsDatabase.getExternalObjectDirectoryRepository().findById(eodEntry3.getId())
            .orElseThrow();
        assertTrue(foundChildEod3.isResponseCleaned());

        verify(armDataManagementApi).listResponseBlobs(inputUploadHash);
        verify(armDataManagementApi).listResponseBlobs(manifestFilePrefix + inputUploadUuid);
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

    @Test
    void successNoResults() {
        OffsetDateTime testTime = OffsetDateTime.now().plusMinutes(20);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(testTime);

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);

        getCleanupArmResponseFilesService().cleanupResponseFiles(100);

        verifyNoMoreInteractions(armDataManagementApi);
    }


    @Test
    /*
        process 3 InputUpload files out of 5 in the database.
        each has 3 groups of createRecord/Upload response files.
     */
    void successLargeNumber() throws IOException {

        OffsetDateTime lastModifiedDateTime = OffsetDateTime.of(2023, 10, 27, 22, 0, 0, 0, ZoneOffset.UTC);
        createTestData(5, 3, lastModifiedDateTime);

        OffsetDateTime testTime = OffsetDateTime.now().plusMinutes(20);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(testTime);

        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);

        getCleanupArmResponseFilesService().cleanupResponseFiles(3);

        List<ExternalObjectDirectoryEntity> allEodEntities = dartsDatabase.getExternalObjectDirectoryRepository().findAll();
        assertEquals(15, allEodEntities.size());
        List<ExternalObjectDirectoryEntity> cleanedEodEntities = allEodEntities.stream().filter(ExternalObjectDirectoryEntity::isResponseCleaned).toList();
        assertEquals(9, cleanedEodEntities.size());
    }

    private BinaryData convertStringToBinaryData(String contents) {
        return BinaryData.fromString(contents);
    }

    private String getCreateRecordFileContents(String createRecordFilename, Long externalObjectDirectoryId) throws IOException {
        String expectedResponse = getContentsFromFile(createRecordFilename);
        expectedResponse = expectedResponse.replaceAll("<EODID>", String.valueOf(externalObjectDirectoryId));
        return expectedResponse;
    }

    private String getUploadFileContents(String uploadFilename, long externalObjectDirectoryId, String checksum) throws IOException {
        String expectedResponse = getContentsFromFile(uploadFilename);
        expectedResponse = expectedResponse.replaceAll("<CHECKSUM>", checksum);
        expectedResponse = expectedResponse.replaceAll("<EODID>", String.valueOf(externalObjectDirectoryId));
        return expectedResponse;
    }

    private ExternalObjectDirectoryEntity createEodEntry(String manifestFilename, OffsetDateTime latestModifiedDateTime) {
        ExternalObjectDirectoryEntity eodEntity = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            savedMedia,
            STORED,
            ARM,
            UUID.randomUUID().toString()
        );
        eodEntity.setLastModifiedDateTime(latestModifiedDateTime);
        eodEntity.setResponseCleaned(false);
        eodEntity.setManifestFile(manifestFilename);
        dartsDatabase.save(eodEntity);
        return eodEntity;
    }


    @SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops"})
    private void createTestData(int numOfManifestFiles, int eodPerManifest, OffsetDateTime lastModifiedDateTime) throws IOException {
        for (int manifestFileCounter = 1; manifestFileCounter <= numOfManifestFiles; manifestFileCounter++) {
            String inputUploadUuid = "InputUploadUUID" + manifestFileCounter;
            String manifestFilename = manifestFilePrefix + inputUploadUuid + ".a360";
            String inputUploadHash = "6a374f19a9ce7dc9cc480ea8d4eca0fb-" + manifestFileCounter;
            String inputUploadFilename = manifestFilePrefix + inputUploadUuid + "_" + inputUploadHash + "_1_iu.rsp";
            when(armDataManagementApi.listResponseBlobs(manifestFilePrefix + inputUploadUuid)).thenReturn(
                List.of(inputUploadFilename));

            List<String> responseFilenames = new ArrayList<>();

            for (int eodPerManifestCounter = 1; eodPerManifestCounter <= eodPerManifest; eodPerManifestCounter++) {
                ExternalObjectDirectoryEntity eodEntry = createEodEntry(manifestFilename, lastModifiedDateTime);

                String createRecordFilename = inputUploadHash + "_" + String.format("%08d", eodPerManifestCounter) + "-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp";

                String createRecordFileTemplate = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/CreateRecord.rsp";
                BinaryData createRecordBinaryDataTest1 = convertStringToBinaryData(getCreateRecordFileContents(createRecordFileTemplate, eodEntry.getId()));
                when(armDataManagementApi.getBlobData(createRecordFilename)).thenReturn(createRecordBinaryDataTest1);
                responseFilenames.add(createRecordFilename);
                when(armDataManagementApi.deleteBlobData(createRecordFilename)).thenReturn(true);

                String uploadFileFilename = inputUploadHash + "_" + String.format("%08d", eodPerManifestCounter) + "-952a-79b6-8362-13259aae1895_1_uf.rsp";
                String validUploadFileTemplate = "tests/arm/service/ArmBatchResponseFilesProcessorTest/ValidResponses/UploadFile.rsp";
                BinaryData uploadFileBinaryDataTest1 = convertStringToBinaryData(getUploadFileContents(validUploadFileTemplate, eodEntry.getId(), "123"));
                when(armDataManagementApi.getBlobData(uploadFileFilename)).thenReturn(uploadFileBinaryDataTest1);
                responseFilenames.add(uploadFileFilename);

                when(armDataManagementApi.deleteBlobData(uploadFileFilename)).thenReturn(true);

            }

            when(armDataManagementApi.listResponseBlobs(inputUploadHash)).thenReturn(
                responseFilenames);
        }

    }
}