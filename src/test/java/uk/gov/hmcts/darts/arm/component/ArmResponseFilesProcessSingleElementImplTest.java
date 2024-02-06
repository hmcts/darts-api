package uk.gov.hmcts.darts.arm.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.component.impl.ArmResponseFilesProcessSingleElementImpl;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.config.ObjectMapperConfig;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    private ExternalLocationTypeEntity externalLocationTypeArm;
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
    private ExternalObjectDirectoryEntity externalObjectDirectoryArmResponseProcessing;

    @Mock
    private MediaEntity mediaEntity;

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
    void processResponseFilesFor() {

        when(objectRecordStatusRepository.findById(2)).thenReturn(Optional.of(objectRecordStatusStored));
        when(objectRecordStatusRepository.findById(13)).thenReturn(Optional.of(objectRecordStatusArmDropZone));
        when(objectRecordStatusRepository.findById(17)).thenReturn(Optional.of(objectRecordStatusArmResponseProcessingFailed));
        when(objectRecordStatusRepository.findById(16)).thenReturn(Optional.of(objectRecordStatusArmProcessingFiles));
        when(objectRecordStatusRepository.findById(18)).thenReturn(Optional.of(objectRecordStatusArmChecksumFailed));

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

        armResponseFilesProcessSingleElement.processResponseFilesFor(1);

        verify(externalObjectDirectoryRepository, times(1)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());

    }
}