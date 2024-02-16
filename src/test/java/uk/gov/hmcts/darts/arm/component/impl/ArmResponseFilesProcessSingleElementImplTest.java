package uk.gov.hmcts.darts.arm.component.impl;

import com.azure.core.exception.AzureException;
import com.azure.core.util.BinaryData;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.component.ArmResponseFilesProcessSingleElement;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.config.ObjectMapperConfig;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.common.util.TestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_DROP_ZONE;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_PROCESSING_RESPONSE_FILES;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RESPONSE_CHECKSUM_VERIFICATION_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RESPONSE_MANIFEST_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RESPONSE_PROCESSING_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;

@ExtendWith(MockitoExtension.class)
@Slf4j
class ArmResponseFilesProcessSingleElementImplTest {

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
    private UserIdentity userIdentity;

    @Mock
    private UserAccountEntity userAccountEntity;

    @Mock
    private MediaEntity mediaEntity;

    @TempDir
    private File tempDirectory;

    @Captor
    private ArgumentCaptor<ExternalObjectDirectoryEntity> externalObjectDirectoryEntityCaptor;

    private ObjectRecordStatusEntity objectRecordStatusArmDropZone;
    private ObjectRecordStatusEntity objectRecordStatusArmResponseProcessingFailed;
    private ExternalObjectDirectoryEntity externalObjectDirectoryArmResponseProcessing;

    private ExternalObjectDirectoryEntity externalObjectDirectoryArmDropZone;

    private ArmResponseFilesProcessSingleElement armResponseFilesProcessSingleElement;


    @BeforeEach
    void setupData() {

        ObjectRecordStatusEntity objectRecordStatusStored = new ObjectRecordStatusEntity();
        objectRecordStatusStored.setId(STORED.getId());
        objectRecordStatusStored.setDescription("Stored");

        objectRecordStatusArmDropZone = new ObjectRecordStatusEntity();
        objectRecordStatusArmDropZone.setId(ARM_DROP_ZONE.getId());
        objectRecordStatusArmDropZone.setDescription("Arm Drop Zone");

        ObjectRecordStatusEntity objectRecordStatusArmProcessingFiles = new ObjectRecordStatusEntity();
        objectRecordStatusArmProcessingFiles.setId(ARM_PROCESSING_RESPONSE_FILES.getId());
        objectRecordStatusArmProcessingFiles.setDescription("Arm Processing Response Files");

        objectRecordStatusArmResponseProcessingFailed = new ObjectRecordStatusEntity();
        objectRecordStatusArmResponseProcessingFailed.setId(ARM_RESPONSE_PROCESSING_FAILED.getId());
        objectRecordStatusArmResponseProcessingFailed.setDescription("Arm Response Process Failed");

        ObjectRecordStatusEntity objectRecordStatusArmChecksumFailed = new ObjectRecordStatusEntity();
        objectRecordStatusArmChecksumFailed.setId(ARM_RESPONSE_CHECKSUM_VERIFICATION_FAILED.getId());
        objectRecordStatusArmChecksumFailed.setDescription("Arm Response Checksum Verification Failed");

        ObjectRecordStatusEntity objectRecordStatusArmResponseManifestFailed = new ObjectRecordStatusEntity();
        objectRecordStatusArmResponseManifestFailed.setId(ARM_RESPONSE_MANIFEST_FAILED.getId());
        objectRecordStatusArmResponseManifestFailed.setDescription("Arm Response Manifest Failed");

        when(objectRecordStatusRepository.findById(STORED.getId()))
            .thenReturn(Optional.of(objectRecordStatusStored));
        when(objectRecordStatusRepository.findById(ARM_DROP_ZONE.getId()))
            .thenReturn(Optional.of(objectRecordStatusArmDropZone));
        when(objectRecordStatusRepository.findById(ARM_PROCESSING_RESPONSE_FILES.getId()))
            .thenReturn(Optional.of(objectRecordStatusArmProcessingFiles));
        when(objectRecordStatusRepository.findById(ARM_RESPONSE_PROCESSING_FAILED.getId()))
            .thenReturn(Optional.of(objectRecordStatusArmResponseProcessingFailed));
        when(objectRecordStatusRepository.findById(ARM_RESPONSE_CHECKSUM_VERIFICATION_FAILED.getId()))
            .thenReturn(Optional.of(objectRecordStatusArmChecksumFailed));
        when(objectRecordStatusRepository.findById(ARM_RESPONSE_MANIFEST_FAILED.getId()))
            .thenReturn(Optional.of(objectRecordStatusArmResponseManifestFailed));

        externalObjectDirectoryArmResponseProcessing = new ExternalObjectDirectoryEntity();
        externalObjectDirectoryArmResponseProcessing.setId(1);
        externalObjectDirectoryArmResponseProcessing.setStatus(objectRecordStatusArmProcessingFiles);
        externalObjectDirectoryArmResponseProcessing.setMedia(mediaEntity);
        externalObjectDirectoryArmResponseProcessing.setTransferAttempts(1);
        externalObjectDirectoryArmResponseProcessing.setVerificationAttempts(1);

        externalObjectDirectoryArmDropZone = new ExternalObjectDirectoryEntity();
        externalObjectDirectoryArmDropZone.setId(1);
        externalObjectDirectoryArmDropZone.setStatus(objectRecordStatusArmDropZone);

        ObjectMapperConfig objectMapperConfig = new ObjectMapperConfig();
        ObjectMapper objectMapper = objectMapperConfig.objectMapper();
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
    void processResponseFilesFor_WithInvalidInputUploadFilename() {

        when(mediaEntity.getId()).thenReturn(1);
        when(externalObjectDirectoryRepository.findById(1)).thenReturn(Optional.of(externalObjectDirectoryArmResponseProcessing));

        String prefix = "1_1_1";
        String responseBlobFilename = prefix + "_iu.rsp";
        List<String> responseBlobs = new ArrayList<>();
        responseBlobs.add(responseBlobFilename);
        when(armDataManagementApi.listResponseBlobs(prefix)).thenReturn(responseBlobs);

        when(userIdentity.getUserAccount()).thenReturn(userAccountEntity);

        armResponseFilesProcessSingleElement.processResponseFilesFor(1);

        assertEquals(objectRecordStatusArmResponseProcessingFailed, externalObjectDirectoryArmResponseProcessing.getStatus());
        assertFalse(externalObjectDirectoryArmResponseProcessing.isResponseCleaned());

        verify(armDataManagementApi).listResponseBlobs(prefix);
        verifyNoMoreInteractions(armDataManagementApi);
        verify(externalObjectDirectoryRepository).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());
    }

    @Test
    void processResponseFilesFor_WithInputUploadFilenameListResponsesThrowsAzureException() {

        externalObjectDirectoryArmResponseProcessing.setTransferAttempts(1);
        externalObjectDirectoryArmResponseProcessing.setVerificationAttempts(1);

        when(mediaEntity.getId()).thenReturn(1);
        when(externalObjectDirectoryRepository.findById(1)).thenReturn(Optional.of(externalObjectDirectoryArmResponseProcessing));
        when(externalObjectDirectoryRepository.saveAndFlush(externalObjectDirectoryArmResponseProcessing))
            .thenReturn(externalObjectDirectoryArmDropZone);

        String prefix = "1_1_1";
        String responseBlobFilename = prefix + "_iu.rsp";
        List<String> responseBlobs = new ArrayList<>();
        responseBlobs.add(responseBlobFilename);
        when(armDataManagementApi.listResponseBlobs(prefix)).thenThrow(new AzureException());

        when(userIdentity.getUserAccount()).thenReturn(userAccountEntity);

        when(armDataManagementConfiguration.getMaxRetryAttempts()).thenReturn(3);

        armResponseFilesProcessSingleElement.processResponseFilesFor(1);

        assertEquals(objectRecordStatusArmDropZone, externalObjectDirectoryArmResponseProcessing.getStatus());
        assertFalse(externalObjectDirectoryArmResponseProcessing.isResponseCleaned());

        verify(armDataManagementApi).listResponseBlobs(prefix);
        verifyNoMoreInteractions(armDataManagementApi);
        verify(externalObjectDirectoryRepository).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());
    }

    @Test
    void processResponseFilesFor_WithInvalidInputUploadFilenameExtention() {

        when(mediaEntity.getId()).thenReturn(1);
        when(externalObjectDirectoryRepository.findById(1)).thenReturn(Optional.of(externalObjectDirectoryArmResponseProcessing));

        String prefix = "1_1_1";
        String responseBlobFilename = prefix + "_abc.rsp";
        List<String> responseBlobs = new ArrayList<>();
        responseBlobs.add(responseBlobFilename);
        when(armDataManagementApi.listResponseBlobs(prefix)).thenReturn(responseBlobs);

        when(userIdentity.getUserAccount()).thenReturn(userAccountEntity);

        armResponseFilesProcessSingleElement.processResponseFilesFor(1);

        assertEquals(objectRecordStatusArmDropZone, externalObjectDirectoryArmResponseProcessing.getStatus());
        assertFalse(externalObjectDirectoryArmResponseProcessing.isResponseCleaned());

        verify(armDataManagementApi).listResponseBlobs(prefix);
        verifyNoMoreInteractions(armDataManagementApi);
        verify(externalObjectDirectoryRepository).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());
    }

    @Test
    void processResponseFilesFor_WithInvalidUploadFilename() {

        when(mediaEntity.getId()).thenReturn(1);
        when(externalObjectDirectoryRepository.findById(1)).thenReturn(Optional.of(externalObjectDirectoryArmResponseProcessing));
        when(externalObjectDirectoryRepository.saveAndFlush(externalObjectDirectoryArmResponseProcessing))
            .thenReturn(externalObjectDirectoryArmResponseProcessing);

        String prefix = "1_1_1";
        String responseBlobFilename = prefix + "_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp";
        List<String> responseBlobs = new ArrayList<>();
        responseBlobs.add(responseBlobFilename);
        when(armDataManagementApi.listResponseBlobs(prefix)).thenReturn(responseBlobs);

        List<String> hashcodeResponseBlobs = new ArrayList<>();
        String hashcode = "6a374f19a9ce7dc9cc480ea8d4eca0fb";
        String createRecordFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_a17b9015-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp";
        String uploadFileFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_1_uf.rsp";
        hashcodeResponseBlobs.add(createRecordFilename);
        hashcodeResponseBlobs.add(uploadFileFilename);
        when(armDataManagementApi.listResponseBlobs(hashcode)).thenReturn(hashcodeResponseBlobs);

        when(userIdentity.getUserAccount()).thenReturn(userAccountEntity);

        armResponseFilesProcessSingleElement.processResponseFilesFor(1);

        verify(externalObjectDirectoryRepository).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());
        assertEquals(objectRecordStatusArmResponseProcessingFailed, externalObjectDirectoryArmResponseProcessing.getStatus());
        assertFalse(externalObjectDirectoryArmResponseProcessing.isResponseCleaned());

    }

    @Test
    void processResponseFilesFor_WithInvalidLineFilename() {

        when(mediaEntity.getId()).thenReturn(1);
        when(externalObjectDirectoryRepository.findById(1)).thenReturn(Optional.of(externalObjectDirectoryArmResponseProcessing));
        when(externalObjectDirectoryRepository.saveAndFlush(externalObjectDirectoryArmResponseProcessing))
            .thenReturn(externalObjectDirectoryArmResponseProcessing);

        String prefix = "1_1_1";
        String responseBlobFilename = prefix + "_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp";
        List<String> responseBlobs = new ArrayList<>();
        responseBlobs.add(responseBlobFilename);
        when(armDataManagementApi.listResponseBlobs(prefix)).thenReturn(responseBlobs);

        List<String> hashcodeResponseBlobs = new ArrayList<>();
        String hashcode = "6a374f19a9ce7dc9cc480ea8d4eca0fb";
        String createRecordFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_a17b9015-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp";
        String invalidLineFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_0_il.rsp";
        hashcodeResponseBlobs.add(createRecordFilename);
        hashcodeResponseBlobs.add(invalidLineFilename);
        when(armDataManagementApi.listResponseBlobs(hashcode)).thenReturn(hashcodeResponseBlobs);

        when(userIdentity.getUserAccount()).thenReturn(userAccountEntity);

        armResponseFilesProcessSingleElement.processResponseFilesFor(1);

        verify(externalObjectDirectoryRepository).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());
        assertEquals(objectRecordStatusArmResponseProcessingFailed, externalObjectDirectoryArmResponseProcessing.getStatus());
        assertFalse(externalObjectDirectoryArmResponseProcessing.isResponseCleaned());

    }

    @Test
    void processResponseFilesFor_WithInvalidUploadFileThrowsException() {

        when(mediaEntity.getId()).thenReturn(1);
        when(externalObjectDirectoryRepository.findById(1)).thenReturn(Optional.of(externalObjectDirectoryArmResponseProcessing));
        when(externalObjectDirectoryRepository.saveAndFlush(externalObjectDirectoryArmResponseProcessing))
            .thenReturn(externalObjectDirectoryArmResponseProcessing);

        String prefix = "1_1_1";
        String responseBlobFilename = prefix + "_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp";
        List<String> responseBlobs = new ArrayList<>();
        responseBlobs.add(responseBlobFilename);
        when(armDataManagementApi.listResponseBlobs(prefix)).thenReturn(responseBlobs);

        List<String> hashcodeResponseBlobs = new ArrayList<>();
        String hashcode = "6a374f19a9ce7dc9cc480ea8d4eca0fb";
        String createRecordFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_a17b9015-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp";
        String uploadFileFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_04e6bc3b-952a-79b6-8362-13259aae1895_1_uf.rsp";
        hashcodeResponseBlobs.add(createRecordFilename);
        hashcodeResponseBlobs.add(uploadFileFilename);
        when(armDataManagementApi.listResponseBlobs(hashcode)).thenReturn(hashcodeResponseBlobs).thenThrow(new AzureException());

        when(userIdentity.getUserAccount()).thenReturn(userAccountEntity);

        armResponseFilesProcessSingleElement.processResponseFilesFor(1);

        verify(externalObjectDirectoryRepository).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());
        assertEquals(objectRecordStatusArmResponseProcessingFailed, externalObjectDirectoryArmResponseProcessing.getStatus());
        assertFalse(externalObjectDirectoryArmResponseProcessing.isResponseCleaned());

    }

    @Test
    void processResponseFilesFor_WithInvalidUploadFileJson() throws IOException {

        when(mediaEntity.getId()).thenReturn(1);
        when(externalObjectDirectoryRepository.findById(1)).thenReturn(Optional.of(externalObjectDirectoryArmResponseProcessing));
        when(externalObjectDirectoryRepository.saveAndFlush(externalObjectDirectoryArmResponseProcessing))
            .thenReturn(externalObjectDirectoryArmResponseProcessing);

        String prefix = "1_1_1";
        String responseBlobFilename = prefix + "_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp";
        List<String> responseBlobs = new ArrayList<>();
        responseBlobs.add(responseBlobFilename);
        when(armDataManagementApi.listResponseBlobs(prefix)).thenReturn(responseBlobs);

        List<String> hashcodeResponseBlobs = new ArrayList<>();
        String hashcode = "6a374f19a9ce7dc9cc480ea8d4eca0fb";
        String createRecordFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_a17b9015-e6ad-77c5-8d1e-13259aae1895_1_cr.rsp";
        String uploadFileFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_04e6bc3b-952a-79b6-8362-13259aae1895_1_uf.rsp";
        hashcodeResponseBlobs.add(createRecordFilename);
        hashcodeResponseBlobs.add(uploadFileFilename);
        when(armDataManagementApi.listResponseBlobs(hashcode)).thenReturn(hashcodeResponseBlobs);

        String fileLocation = tempDirectory.getAbsolutePath();
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);

        String uploadFileTestFilename = "Tests/arm/component/ArmResponseFilesProcessSingleElement/testInvalidInputFileJson/" +
            "InvalidInputFile.json";
        String uploadFileJson = TestUtils.getContentsFromFile(uploadFileTestFilename);
        BinaryData uploadFileBinaryData = BinaryData.fromString(uploadFileJson);
        when(armDataManagementApi.getBlobData(uploadFileFilename)).thenReturn(uploadFileBinaryData);
        when(fileOperationService.saveBinaryDataToSpecifiedWorkspace(any(BinaryData.class), anyString(), anyString(), anyBoolean()))
            .thenReturn(Path.of(uploadFileTestFilename));

        when(armDataManagementApi.deleteBlobData(uploadFileFilename)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(createRecordFilename)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(responseBlobFilename)).thenReturn(true);

        when(userIdentity.getUserAccount()).thenReturn(userAccountEntity);

        armResponseFilesProcessSingleElement.processResponseFilesFor(1);

        verify(externalObjectDirectoryRepository).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());
        assertEquals(objectRecordStatusArmResponseProcessingFailed, externalObjectDirectoryArmResponseProcessing.getStatus());
        assertTrue(externalObjectDirectoryArmResponseProcessing.isResponseCleaned());

        verify(armDataManagementApi).deleteBlobData(uploadFileFilename);
        verify(armDataManagementApi).deleteBlobData(createRecordFilename);
        verify(armDataManagementApi).deleteBlobData(responseBlobFilename);
        verifyNoMoreInteractions(armDataManagementApi);
    }

    @Test
    void processResponseFilesFor_WithInvalidInvalidLineFilename() {

        when(mediaEntity.getId()).thenReturn(1);

        when(externalObjectDirectoryRepository.findById(1)).thenReturn(Optional.of(externalObjectDirectoryArmResponseProcessing));

        String prefix = "1_1_1";
        String responseBlobFilename = prefix + "_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp";
        List<String> responseBlobs = new ArrayList<>();
        responseBlobs.add(responseBlobFilename);
        when(armDataManagementApi.listResponseBlobs(prefix)).thenReturn(responseBlobs);

        List<String> hashcodeResponseBlobs = new ArrayList<>();
        String hashcode = "6a374f19a9ce7dc9cc480ea8d4eca0fb";
        String invalidLineFileFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_04e6bc3b-952a-79b6-8362-13259aae1895_0_il.rsp";
        hashcodeResponseBlobs.add(invalidLineFileFilename);
        when(armDataManagementApi.listResponseBlobs(hashcode)).thenReturn(hashcodeResponseBlobs);

        when(userIdentity.getUserAccount()).thenReturn(userAccountEntity);

        armResponseFilesProcessSingleElement.processResponseFilesFor(1);

        assertEquals(objectRecordStatusArmResponseProcessingFailed, externalObjectDirectoryArmResponseProcessing.getStatus());
    }

    @Test
    void processResponseFilesFor_WithInvalidPathForInvalidFile() throws IOException {

        when(mediaEntity.getId()).thenReturn(1);

        when(externalObjectDirectoryRepository.findById(1)).thenReturn(Optional.of(externalObjectDirectoryArmResponseProcessing));
        when(externalObjectDirectoryRepository.saveAndFlush(externalObjectDirectoryArmResponseProcessing))
            .thenReturn(externalObjectDirectoryArmResponseProcessing);

        String prefix = "1_1_1";
        String responseBlobFilename = prefix + "_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp";
        List<String> responseBlobs = new ArrayList<>();
        responseBlobs.add(responseBlobFilename);
        when(armDataManagementApi.listResponseBlobs(prefix)).thenReturn(responseBlobs);

        List<String> hashcodeResponseBlobs = new ArrayList<>();
        String hashcode = "6a374f19a9ce7dc9cc480ea8d4eca0fb";
        String invalidLineFileFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_04e6bc3b-952a-79b6-8362-13259aae1895_0_il.rsp";
        hashcodeResponseBlobs.add(invalidLineFileFilename);
        when(armDataManagementApi.listResponseBlobs(hashcode)).thenReturn(hashcodeResponseBlobs);

        String fileLocation = tempDirectory.getAbsolutePath();
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);

        String invalidLineJsonFile = "Tests/arm/component/ArmResponseFilesProcessSingleElement/testInvalidLineFileJson/" +
            "InvalidLineFile.json";
        String uploadFileJson = TestUtils.getContentsFromFile(invalidLineJsonFile);
        BinaryData invalidLineFileBinaryData = BinaryData.fromString(uploadFileJson);
        when(armDataManagementApi.getBlobData(invalidLineFileFilename)).thenReturn(invalidLineFileBinaryData);
        when(fileOperationService.saveBinaryDataToSpecifiedWorkspace(any(BinaryData.class), anyString(), anyString(), anyBoolean()))
            .thenReturn(Path.of("Tests/arm/component/ArmResponseFilesProcessSingleElement/nosuchpath"));

        when(userIdentity.getUserAccount()).thenReturn(userAccountEntity);

        armResponseFilesProcessSingleElement.processResponseFilesFor(1);

        assertEquals(objectRecordStatusArmResponseProcessingFailed, externalObjectDirectoryArmResponseProcessing.getStatus());
        assertFalse(externalObjectDirectoryArmResponseProcessing.isResponseCleaned());
    }

    @Test
    void processResponseFilesFor_WithInvalidInvalidLineFileJson() throws IOException {

        when(mediaEntity.getId()).thenReturn(1);

        when(externalObjectDirectoryRepository.findById(1)).thenReturn(Optional.of(externalObjectDirectoryArmResponseProcessing));
        when(externalObjectDirectoryRepository.saveAndFlush(externalObjectDirectoryArmResponseProcessing))
            .thenReturn(externalObjectDirectoryArmResponseProcessing);

        String prefix = "1_1_1";
        String responseBlobFilename = prefix + "_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp";
        List<String> responseBlobs = new ArrayList<>();
        responseBlobs.add(responseBlobFilename);
        when(armDataManagementApi.listResponseBlobs(prefix)).thenReturn(responseBlobs);

        List<String> hashcodeResponseBlobs = new ArrayList<>();
        String hashcode = "6a374f19a9ce7dc9cc480ea8d4eca0fb";
        String invalidLineFileFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_04e6bc3b-952a-79b6-8362-13259aae1895_0_il.rsp";
        hashcodeResponseBlobs.add(invalidLineFileFilename);
        when(armDataManagementApi.listResponseBlobs(hashcode)).thenReturn(hashcodeResponseBlobs);

        String fileLocation = tempDirectory.getAbsolutePath();
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);

        String invalidLineJsonFile = "Tests/arm/component/ArmResponseFilesProcessSingleElement/testInvalidLineFileJson/" +
            "InvalidLineFile.json";
        String uploadFileJson = TestUtils.getContentsFromFile(invalidLineJsonFile);
        BinaryData invalidLineFileBinaryData = BinaryData.fromString(uploadFileJson);
        when(armDataManagementApi.getBlobData(invalidLineFileFilename)).thenReturn(invalidLineFileBinaryData);
        when(fileOperationService.saveBinaryDataToSpecifiedWorkspace(any(BinaryData.class), anyString(), anyString(), anyBoolean()))
            .thenReturn(Path.of(invalidLineJsonFile));

        when(armDataManagementApi.deleteBlobData(invalidLineFileFilename)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(responseBlobFilename)).thenReturn(true);

        when(userIdentity.getUserAccount()).thenReturn(userAccountEntity);

        armResponseFilesProcessSingleElement.processResponseFilesFor(1);

        assertEquals(objectRecordStatusArmResponseProcessingFailed, externalObjectDirectoryArmResponseProcessing.getStatus());
        assertTrue(externalObjectDirectoryArmResponseProcessing.isResponseCleaned());

        verify(armDataManagementApi).deleteBlobData(invalidLineFileFilename);
        verify(armDataManagementApi).deleteBlobData(responseBlobFilename);
        verifyNoMoreInteractions(armDataManagementApi);
    }

    @Test
    void processResponseFilesFor_WithInvalidInvalidLineFile() throws IOException {

        when(mediaEntity.getId()).thenReturn(1);

        when(externalObjectDirectoryRepository.findById(1)).thenReturn(Optional.of(externalObjectDirectoryArmResponseProcessing));
        when(externalObjectDirectoryRepository.saveAndFlush(externalObjectDirectoryArmResponseProcessing))
            .thenReturn(externalObjectDirectoryArmResponseProcessing);

        String prefix = "1_1_1";
        String responseBlobFilename = prefix + "_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp";
        List<String> responseBlobs = new ArrayList<>();
        responseBlobs.add(responseBlobFilename);
        when(armDataManagementApi.listResponseBlobs(prefix)).thenReturn(responseBlobs);

        List<String> hashcodeResponseBlobs = new ArrayList<>();
        String hashcode = "6a374f19a9ce7dc9cc480ea8d4eca0fb";
        String invalidLineFileFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_04e6bc3b-952a-79b6-8362-13259aae1895_0_il.rsp";
        hashcodeResponseBlobs.add(invalidLineFileFilename);
        when(armDataManagementApi.listResponseBlobs(hashcode)).thenReturn(hashcodeResponseBlobs);

        String fileLocation = tempDirectory.getAbsolutePath();
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);

        String uploadFileTestFilename = "Tests/arm/component/ArmResponseFilesProcessSingleElement/testInvalidLineFile/" +
            "InvalidLineFile.json";
        String uploadFileJson = TestUtils.getContentsFromFile(uploadFileTestFilename);
        BinaryData invalidLineFileBinaryData = BinaryData.fromString(uploadFileJson);
        when(armDataManagementApi.getBlobData(invalidLineFileFilename)).thenReturn(invalidLineFileBinaryData);
        when(fileOperationService.saveBinaryDataToSpecifiedWorkspace(any(BinaryData.class), anyString(), anyString(), anyBoolean()))
            .thenReturn(Path.of(uploadFileTestFilename));

        when(armDataManagementApi.deleteBlobData(invalidLineFileFilename)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(responseBlobFilename)).thenReturn(true);

        when(userIdentity.getUserAccount()).thenReturn(userAccountEntity);

        armResponseFilesProcessSingleElement.processResponseFilesFor(1);

        assertEquals(objectRecordStatusArmResponseProcessingFailed.getId(), externalObjectDirectoryArmResponseProcessing.getStatus().getId());
        assertEquals(objectRecordStatusArmResponseProcessingFailed.getDescription(), externalObjectDirectoryArmResponseProcessing.getStatus().getDescription());
        assertTrue(externalObjectDirectoryArmResponseProcessing.isResponseCleaned());

        verify(armDataManagementApi).deleteBlobData(invalidLineFileFilename);
        verify(armDataManagementApi).deleteBlobData(responseBlobFilename);
        verifyNoMoreInteractions(armDataManagementApi);
    }

    @Test
    void processResponseFilesFor_WithValidInvalidLineFileJson() throws IOException {

        when(mediaEntity.getId()).thenReturn(1);

        when(externalObjectDirectoryRepository.findById(1)).thenReturn(Optional.of(externalObjectDirectoryArmResponseProcessing));
        when(externalObjectDirectoryRepository.saveAndFlush(externalObjectDirectoryArmResponseProcessing))
            .thenReturn(externalObjectDirectoryArmResponseProcessing);

        String prefix = "1_1_1";
        String responseBlobFilename = prefix + "_6a374f19a9ce7dc9cc480ea8d4eca0fb_1_iu.rsp";
        List<String> responseBlobs = new ArrayList<>();
        responseBlobs.add(responseBlobFilename);
        when(armDataManagementApi.listResponseBlobs(prefix)).thenReturn(responseBlobs);

        List<String> hashcodeResponseBlobs = new ArrayList<>();
        String hashcode = "6a374f19a9ce7dc9cc480ea8d4eca0fb";
        String invalidLineFileFilename = "6a374f19a9ce7dc9cc480ea8d4eca0fb_04e6bc3b-952a-79b6-8362-13259aae1895_0_il.rsp";
        hashcodeResponseBlobs.add(invalidLineFileFilename);
        when(armDataManagementApi.listResponseBlobs(hashcode)).thenReturn(hashcodeResponseBlobs);

        String fileLocation = tempDirectory.getAbsolutePath();
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);

        String uploadFileTestFilename = "Tests/arm/component/ArmResponseFilesProcessSingleElement/testInvalidLineFile/" +
            "InvalidLineFile.json";
        String uploadFileJson = TestUtils.getContentsFromFile(uploadFileTestFilename);
        BinaryData invalidLineFileBinaryData = BinaryData.fromString(uploadFileJson);
        when(armDataManagementApi.getBlobData(invalidLineFileFilename)).thenReturn(invalidLineFileBinaryData);
        when(fileOperationService.saveBinaryDataToSpecifiedWorkspace(any(BinaryData.class), anyString(), anyString(), anyBoolean()))
            .thenReturn(Path.of(uploadFileTestFilename));

        when(armDataManagementApi.deleteBlobData(invalidLineFileFilename)).thenReturn(true);
        when(armDataManagementApi.deleteBlobData(responseBlobFilename)).thenReturn(true);

        when(userIdentity.getUserAccount()).thenReturn(userAccountEntity);

        armResponseFilesProcessSingleElement.processResponseFilesFor(1);

        verify(externalObjectDirectoryRepository).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());
        assertEquals(objectRecordStatusArmResponseProcessingFailed, externalObjectDirectoryArmResponseProcessing.getStatus());
        assertTrue(externalObjectDirectoryArmResponseProcessing.isResponseCleaned());

        verify(armDataManagementApi).deleteBlobData(invalidLineFileFilename);
        verify(armDataManagementApi).deleteBlobData(responseBlobFilename);
        verifyNoMoreInteractions(armDataManagementApi);
    }

}
