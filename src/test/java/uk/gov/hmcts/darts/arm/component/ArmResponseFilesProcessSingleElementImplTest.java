package uk.gov.hmcts.darts.arm.component;

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
import uk.gov.hmcts.darts.arm.component.impl.ArmResponseFilesProcessSingleElementImpl;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.config.ObjectMapperConfig;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_PROCESSING_RESPONSE_FILES;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.FAILURE_ARM_RESPONSE_PROCESSING;

@ExtendWith(MockitoExtension.class)
@Slf4j
class ArmResponseFilesProcessSingleElementImplTest {

    @Mock
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    @Mock
    private ObjectRecordStatusRepository objectRecordStatusRepository;
    @Mock
    private ExternalLocationTypeRepository externalLocationTypeRepository;
    @Mock
    private ArmDataManagementApi armDataManagementApi;
    @Mock
    private FileOperationService fileOperationService;
    @Mock
    private ArmDataManagementConfiguration armDataManagementConfiguration;
    @Mock
    private UserIdentity userIdentity;

    @Mock
    private ObjectRecordStatusEntity objectRecordStatusStored;
    @Mock
    private ObjectRecordStatusEntity objectRecordStatusArmDropZone;
    @Mock
    private ObjectRecordStatusEntity objectRecordStatusArmResponseProcessingFailed;
    @Mock
    private ObjectRecordStatusEntity objectRecordStatusArmProcessingFiles;
    @Mock
    private ObjectRecordStatusEntity objectRecordStatusArmChecksumFailed;
    @Mock
    private ObjectRecordStatusEntity objectRecordStatusManifestFiledFailed;
    @Mock
    private UserAccountEntity userAccountEntity;
    @Mock
    private ExternalObjectDirectoryEntity externalObjectDirectoryArmResponseProcessing;

    @Mock
    private MediaEntity mediaEntity;

    @TempDir
    private File tempDirectory;

    @Captor
    private ArgumentCaptor<ExternalObjectDirectoryEntity> externalObjectDirectoryEntityCaptor;

    private ArmResponseFilesProcessSingleElement armResponseFilesProcessSingleElement;


    @BeforeEach
    void setupData() {

        ObjectMapperConfig objectMapperConfig = new ObjectMapperConfig();
        ObjectMapper objectMapper = objectMapperConfig.objectMapper();

        armResponseFilesProcessSingleElement = new ArmResponseFilesProcessSingleElementImpl(
                externalObjectDirectoryRepository,
                objectRecordStatusRepository,
                externalLocationTypeRepository,
                armDataManagementApi,
                fileOperationService,
                armDataManagementConfiguration,
                objectMapper,
                userIdentity
        );
    }

    @Test
    void processResponseFilesFor_WithInvalidInputUploadFilename() {

        when(objectRecordStatusRepository.findById(2)).thenReturn(Optional.of(objectRecordStatusStored));
        when(objectRecordStatusRepository.findById(13)).thenReturn(Optional.of(objectRecordStatusArmDropZone));
        when(objectRecordStatusRepository.findById(17)).thenReturn(Optional.of(objectRecordStatusArmResponseProcessingFailed));
        when(objectRecordStatusRepository.findById(16)).thenReturn(Optional.of(objectRecordStatusArmProcessingFiles));
        when(objectRecordStatusRepository.findById(18)).thenReturn(Optional.of(objectRecordStatusArmChecksumFailed));
        when(objectRecordStatusRepository.findById(15)).thenReturn(Optional.of(objectRecordStatusManifestFiledFailed));

        when(mediaEntity.getId()).thenReturn(1);

        when(externalObjectDirectoryArmResponseProcessing.getId()).thenReturn(1);
        when(externalObjectDirectoryArmResponseProcessing.getStatus()).thenReturn(objectRecordStatusArmProcessingFiles);
        when(externalObjectDirectoryArmResponseProcessing.getMedia()).thenReturn(mediaEntity);
        when(externalObjectDirectoryArmResponseProcessing.getTransferAttempts()).thenReturn(1);

        when(externalObjectDirectoryRepository.findById(1)).thenReturn(Optional.of(externalObjectDirectoryArmResponseProcessing));

        String prefix = "1_1_1";
        String responseBlobFilename = prefix + "_iu.rsp";
        List<String> responseBlobs = new ArrayList<>();
        responseBlobs.add(responseBlobFilename);
        when(armDataManagementApi.listResponseBlobs(prefix)).thenReturn(responseBlobs);

        when(userIdentity.getUserAccount()).thenReturn(userAccountEntity);

        armResponseFilesProcessSingleElement.processResponseFilesFor(1);

        verify(externalObjectDirectoryRepository, times(1)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());

    }

    @Test
    void processResponseFilesFor_WithInvalidUploadFileJson() throws IOException {
        when(objectRecordStatusArmProcessingFiles.getDescription()).thenReturn(ARM_PROCESSING_RESPONSE_FILES.name());
        when(objectRecordStatusArmResponseProcessingFailed.getDescription()).thenReturn(FAILURE_ARM_RESPONSE_PROCESSING.name());

        when(objectRecordStatusRepository.findById(2)).thenReturn(Optional.of(objectRecordStatusStored));
        when(objectRecordStatusRepository.findById(13)).thenReturn(Optional.of(objectRecordStatusArmDropZone));
        when(objectRecordStatusRepository.findById(17)).thenReturn(Optional.of(objectRecordStatusArmResponseProcessingFailed));
        when(objectRecordStatusRepository.findById(16)).thenReturn(Optional.of(objectRecordStatusArmProcessingFiles));
        when(objectRecordStatusRepository.findById(18)).thenReturn(Optional.of(objectRecordStatusArmChecksumFailed));
        when(objectRecordStatusRepository.findById(15)).thenReturn(Optional.of(objectRecordStatusManifestFiledFailed));

        when(mediaEntity.getId()).thenReturn(1);

        when(externalObjectDirectoryArmResponseProcessing.getId()).thenReturn(1);
        when(externalObjectDirectoryArmResponseProcessing.getStatus()).thenReturn(objectRecordStatusArmProcessingFiles);
        when(externalObjectDirectoryArmResponseProcessing.getMedia()).thenReturn(mediaEntity);
        when(externalObjectDirectoryArmResponseProcessing.getTransferAttempts()).thenReturn(1);

        when(externalObjectDirectoryRepository.findById(1)).thenReturn(Optional.of(externalObjectDirectoryArmResponseProcessing));

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

        when(userIdentity.getUserAccount()).thenReturn(userAccountEntity);

        armResponseFilesProcessSingleElement.processResponseFilesFor(1);

        verify(externalObjectDirectoryRepository, times(1)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());

    }

    @Test
    void processResponseFilesFor_WithInvalidInvalidLineFilename() {

        when(objectRecordStatusRepository.findById(2)).thenReturn(Optional.of(objectRecordStatusStored));
        when(objectRecordStatusRepository.findById(13)).thenReturn(Optional.of(objectRecordStatusArmDropZone));
        when(objectRecordStatusRepository.findById(17)).thenReturn(Optional.of(objectRecordStatusArmResponseProcessingFailed));
        when(objectRecordStatusRepository.findById(16)).thenReturn(Optional.of(objectRecordStatusArmProcessingFiles));
        when(objectRecordStatusRepository.findById(18)).thenReturn(Optional.of(objectRecordStatusArmChecksumFailed));
        when(objectRecordStatusRepository.findById(15)).thenReturn(Optional.of(objectRecordStatusManifestFiledFailed));

        when(mediaEntity.getId()).thenReturn(1);

        when(externalObjectDirectoryArmResponseProcessing.getId()).thenReturn(1);
        when(externalObjectDirectoryArmResponseProcessing.getStatus()).thenReturn(objectRecordStatusArmProcessingFiles);
        when(externalObjectDirectoryArmResponseProcessing.getMedia()).thenReturn(mediaEntity);
        when(externalObjectDirectoryArmResponseProcessing.getTransferAttempts()).thenReturn(1);

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

        verify(externalObjectDirectoryRepository, times(1)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());

    }

    @Test
    void processResponseFilesFor_WithInvalidInvalidLineFileJson() throws IOException {
        when(objectRecordStatusArmProcessingFiles.getDescription()).thenReturn(ARM_PROCESSING_RESPONSE_FILES.name());
        when(objectRecordStatusArmResponseProcessingFailed.getDescription()).thenReturn(FAILURE_ARM_RESPONSE_PROCESSING.name());

        when(objectRecordStatusRepository.findById(2)).thenReturn(Optional.of(objectRecordStatusStored));
        when(objectRecordStatusRepository.findById(13)).thenReturn(Optional.of(objectRecordStatusArmDropZone));
        when(objectRecordStatusRepository.findById(17)).thenReturn(Optional.of(objectRecordStatusArmResponseProcessingFailed));
        when(objectRecordStatusRepository.findById(16)).thenReturn(Optional.of(objectRecordStatusArmProcessingFiles));
        when(objectRecordStatusRepository.findById(18)).thenReturn(Optional.of(objectRecordStatusArmChecksumFailed));
        when(objectRecordStatusRepository.findById(15)).thenReturn(Optional.of(objectRecordStatusManifestFiledFailed));

        when(mediaEntity.getId()).thenReturn(1);

        when(externalObjectDirectoryArmResponseProcessing.getId()).thenReturn(1);
        when(externalObjectDirectoryArmResponseProcessing.getStatus()).thenReturn(objectRecordStatusArmProcessingFiles);
        when(externalObjectDirectoryArmResponseProcessing.getMedia()).thenReturn(mediaEntity);
        when(externalObjectDirectoryArmResponseProcessing.getTransferAttempts()).thenReturn(1);

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

        String fileLocation = tempDirectory.getAbsolutePath();
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);

        String uploadFileTestFilename = "Tests/arm/component/ArmResponseFilesProcessSingleElement/testInvalidInputFileJson/" +
            "InvalidInputFile.json";
        String uploadFileJson = TestUtils.getContentsFromFile(uploadFileTestFilename);
        BinaryData invalidLineFileBinaryData = BinaryData.fromString(uploadFileJson);
        when(armDataManagementApi.getBlobData(invalidLineFileFilename)).thenReturn(invalidLineFileBinaryData);

        when(userIdentity.getUserAccount()).thenReturn(userAccountEntity);

        armResponseFilesProcessSingleElement.processResponseFilesFor(1);

        verify(externalObjectDirectoryRepository, times(1)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());

    }

    @Test
    void processResponseFilesFor_WithValidInvalidLineFileJson() throws IOException {
        when(objectRecordStatusArmProcessingFiles.getDescription()).thenReturn(ARM_PROCESSING_RESPONSE_FILES.name());
        when(objectRecordStatusArmResponseProcessingFailed.getDescription()).thenReturn(FAILURE_ARM_RESPONSE_PROCESSING.name());

        when(objectRecordStatusRepository.findById(2)).thenReturn(Optional.of(objectRecordStatusStored));
        when(objectRecordStatusRepository.findById(13)).thenReturn(Optional.of(objectRecordStatusArmDropZone));
        when(objectRecordStatusRepository.findById(17)).thenReturn(Optional.of(objectRecordStatusArmResponseProcessingFailed));
        when(objectRecordStatusRepository.findById(16)).thenReturn(Optional.of(objectRecordStatusArmProcessingFiles));
        when(objectRecordStatusRepository.findById(18)).thenReturn(Optional.of(objectRecordStatusArmChecksumFailed));
        when(objectRecordStatusRepository.findById(15)).thenReturn(Optional.of(objectRecordStatusManifestFiledFailed));

        when(mediaEntity.getId()).thenReturn(1);

        when(externalObjectDirectoryArmResponseProcessing.getId()).thenReturn(1);
        when(externalObjectDirectoryArmResponseProcessing.getStatus()).thenReturn(objectRecordStatusArmProcessingFiles);
        when(externalObjectDirectoryArmResponseProcessing.getMedia()).thenReturn(mediaEntity);
        when(externalObjectDirectoryArmResponseProcessing.getTransferAttempts()).thenReturn(1);

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

        String fileLocation = tempDirectory.getAbsolutePath();
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);

        String uploadFileTestFilename = "Tests/arm/component/ArmResponseFilesProcessSingleElement/testInvalidInputFileJson/" +
            "InvalidLineFile.json";
        String uploadFileJson = TestUtils.getContentsFromFile(uploadFileTestFilename);
        BinaryData invalidLineFileBinaryData = BinaryData.fromString(uploadFileJson);
        when(armDataManagementApi.getBlobData(invalidLineFileFilename)).thenReturn(invalidLineFileBinaryData);

        when(userIdentity.getUserAccount()).thenReturn(userAccountEntity);

        Path path = Path.of(uploadFileTestFilename);
        when(fileOperationService.saveBinaryDataToSpecifiedWorkspace(any(BinaryData.class),anyString(), anyString(), anyBoolean())).thenReturn(path);

        armResponseFilesProcessSingleElement.processResponseFilesFor(1);

        verify(externalObjectDirectoryRepository, times(1)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());

    }
}
