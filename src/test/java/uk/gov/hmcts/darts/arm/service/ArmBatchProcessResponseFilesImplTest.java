package uk.gov.hmcts.darts.arm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.service.impl.ArmBatchProcessResponseFilesImpl;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.config.ObjectMapperConfig;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.ARM;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_DROP_ZONE;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_PROCESSING_RESPONSE_FILES;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("VariableDeclarationUsageDistance")
class ArmBatchProcessResponseFilesImplTest {

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
    private CurrentTimeHelper currentTimeHelper;
    @Mock
    private ArmResponseFilesProcessor armResponseFilesProcessor;
    @Mock
    private ExternalObjectDirectoryService externalObjectDirectoryService;


    @Captor
    private ArgumentCaptor<ExternalObjectDirectoryEntity> externalObjectDirectoryEntityCaptor;

    private ExternalLocationTypeEntity externalLocationTypeArm;
    private ObjectRecordStatusEntity objectRecordStatusArmDropZone;
    private ObjectRecordStatusEntity objectRecordStatusArmProcessingFiles;

    @Mock
    private ExternalObjectDirectoryEntity externalObjectDirectoryArmDropZone;


    private ArmBatchProcessResponseFiles armBatchProcessResponseFiles;

    @BeforeEach
    void setupData() {

        ObjectMapperConfig objectMapperConfig = new ObjectMapperConfig();
        ObjectMapper objectMapper = objectMapperConfig.objectMapper();

        externalLocationTypeArm = new ExternalLocationTypeEntity();
        externalLocationTypeArm.setId(ARM.getId());

        objectRecordStatusArmDropZone = new ObjectRecordStatusEntity();
        objectRecordStatusArmDropZone.setId(ARM_DROP_ZONE.getId());
        objectRecordStatusArmDropZone.setDescription("Arm Drop Zone");

        objectRecordStatusArmProcessingFiles = new ObjectRecordStatusEntity();
        objectRecordStatusArmProcessingFiles.setId(ARM_PROCESSING_RESPONSE_FILES.getId());
        objectRecordStatusArmProcessingFiles.setDescription("Arm Processing Response Files");

        armBatchProcessResponseFiles = new ArmBatchProcessResponseFilesImpl(
            externalObjectDirectoryRepository,
            armDataManagementApi,
            fileOperationService,
            armDataManagementConfiguration,
            objectMapper,
            userIdentity,
            currentTimeHelper,
            armResponseFilesProcessor,
            externalObjectDirectoryService
        );

    }

    @Test
    void batchProcessResponseFilesWithBatchSizeZero() {
        // given
        when(armDataManagementConfiguration.getBatchSize()).thenReturn(0);

        // when
        armBatchProcessResponseFiles.batchProcessResponseFiles();

        // then
        verify(armResponseFilesProcessor).processResponseFiles();

        verifyNoMoreInteractions(
            objectRecordStatusRepository,
            externalLocationTypeRepository,
            externalObjectDirectoryRepository,
            armResponseFilesProcessor
        );
    }

    @Test
    @Disabled("Broken")
    void batchProcessResponseFilesWithBatchSizeTwo() {

        // given
        when(armDataManagementConfiguration.getBatchSize()).thenReturn(0);

        when(externalLocationTypeRepository.getReferenceById(ARM.getId())).thenReturn(externalLocationTypeArm);

        when(objectRecordStatusRepository.findById(ARM_DROP_ZONE.getId())).thenReturn(Optional.of(objectRecordStatusArmDropZone));
        when(objectRecordStatusRepository.findById(ARM_PROCESSING_RESPONSE_FILES.getId())).thenReturn(Optional.of(objectRecordStatusArmProcessingFiles));

        when(externalObjectDirectoryArmDropZone.getId()).thenReturn(1);
        when(externalObjectDirectoryArmDropZone.getStatus()).thenReturn(objectRecordStatusArmDropZone);

        List<ExternalObjectDirectoryEntity> inboundList = new ArrayList<>(Collections.singletonList(externalObjectDirectoryArmDropZone));
        when(externalObjectDirectoryRepository.findByExternalLocationTypeAndObjectStatus(externalLocationTypeArm, objectRecordStatusArmDropZone))
            .thenReturn(inboundList);

        // when
        armBatchProcessResponseFiles.batchProcessResponseFiles();

        // then
        verify(objectRecordStatusRepository).findById(ARM_DROP_ZONE.getId());
        verify(objectRecordStatusRepository).findById(ARM_PROCESSING_RESPONSE_FILES.getId());
        verify(externalLocationTypeRepository).getReferenceById(ARM.getId());
        verify(externalObjectDirectoryRepository).findByExternalLocationTypeAndObjectStatus(externalLocationTypeArm, objectRecordStatusArmDropZone);
        verify(externalObjectDirectoryRepository).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());

        verifyNoMoreInteractions(
            objectRecordStatusRepository,
            externalLocationTypeRepository,
            externalObjectDirectoryRepository,
            armResponseFilesProcessor
        );
    }
}