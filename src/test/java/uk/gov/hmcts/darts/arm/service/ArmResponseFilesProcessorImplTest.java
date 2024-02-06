package uk.gov.hmcts.darts.arm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.component.ArmResponseFilesProcessSingleElement;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.service.impl.ArmResponseFilesProcessorImpl;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArmResponseFilesProcessorImplTest {

    private static final LocalDate HEARING_DATE = LocalDate.of(2023, 6, 10);

    private ArmResponseFilesProcessor armResponseFilesProcessor;

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
    private ExternalObjectDirectoryEntity externalObjectDirectoryArmDropZone;
    @Mock
    private ExternalObjectDirectoryEntity externalObjectDirectoryArmResponseProcessing;

    @Mock
    private ArmResponseFilesProcessSingleElement armResponseFilesProcessSingleElement;

    @Mock
    private MediaEntity mediaEntity;


    @Captor
    private ArgumentCaptor<ExternalObjectDirectoryEntity> externalObjectDirectoryEntityCaptor;


    @BeforeEach
    void setupData() {

        ObjectMapperConfig objectMapperConfig = new ObjectMapperConfig();
        ObjectMapper objectMapper = objectMapperConfig.objectMapper();

        armResponseFilesProcessor = new ArmResponseFilesProcessorImpl(
                externalObjectDirectoryRepository,
                objectRecordStatusRepository,
                externalLocationTypeRepository,
                armDataManagementApi,
                fileOperationService,
                armDataManagementConfiguration,
                objectMapper,
                userIdentity,
                armResponseFilesProcessSingleElement
        );
    }

    @Test
    void processResponseFilesUnableToFindInputUploadFile() {

        when(externalLocationTypeRepository.getReferenceById(3)).thenReturn(externalLocationTypeArm);

        when(objectRecordStatusRepository.findById(2)).thenReturn(Optional.of(objectRecordStatusStored));
        when(objectRecordStatusRepository.findById(13)).thenReturn(Optional.of(objectRecordStatusArmDropZone));
        when(objectRecordStatusRepository.findById(17)).thenReturn(Optional.of(objectRecordStatusArmResponseProcessingFailed));
        when(objectRecordStatusRepository.findById(16)).thenReturn(Optional.of(objectRecordStatusArmProcessingFiles));
        when(objectRecordStatusRepository.findById(18)).thenReturn(Optional.of(objectRecordStatusArmChecksumFailed));

        when(externalObjectDirectoryArmDropZone.getId()).thenReturn(1);
        when(externalObjectDirectoryArmDropZone.getStatus()).thenReturn(objectRecordStatusArmDropZone);

        when(externalObjectDirectoryArmResponseProcessing.getId()).thenReturn(1);
        when(externalObjectDirectoryArmResponseProcessing.getStatus()).thenReturn(objectRecordStatusArmResponseProcessingFailed);
        when(externalObjectDirectoryArmResponseProcessing.getMedia()).thenReturn(mediaEntity);

        List<ExternalObjectDirectoryEntity> inboundList = new ArrayList<>(Collections.singletonList(externalObjectDirectoryArmDropZone));
        when(externalObjectDirectoryRepository.findByExternalLocationTypeAndObjectStatus(
                     externalLocationTypeArm,
                     objectRecordStatusArmDropZone
             )
        ).thenReturn(inboundList);
        when(externalObjectDirectoryRepository.findById(1)).thenReturn(Optional.of(externalObjectDirectoryArmResponseProcessing));

        armResponseFilesProcessor.processResponseFiles();

        verify(externalObjectDirectoryRepository, times(1)).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());

    }

}
