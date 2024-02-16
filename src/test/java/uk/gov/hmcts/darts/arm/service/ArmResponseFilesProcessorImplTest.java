package uk.gov.hmcts.darts.arm.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.component.ArmResponseFilesProcessSingleElement;
import uk.gov.hmcts.darts.arm.service.impl.ArmResponseFilesProcessorImpl;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;

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
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RESPONSE_PROCESSING_FAILED;

@ExtendWith(MockitoExtension.class)
class ArmResponseFilesProcessorImplTest {

    @Mock
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    @Mock
    private ObjectRecordStatusRepository objectRecordStatusRepository;
    @Mock
    private ExternalLocationTypeRepository externalLocationTypeRepository;

    @Mock
    private UserIdentity userIdentity;

    @Mock
    private ExternalObjectDirectoryEntity externalObjectDirectoryArmDropZone;

    @Mock
    private ArmResponseFilesProcessSingleElement armResponseFilesProcessSingleElement;

    @Captor
    private ArgumentCaptor<ExternalObjectDirectoryEntity> externalObjectDirectoryEntityCaptor;

    private ExternalLocationTypeEntity externalLocationTypeArm;
    private ObjectRecordStatusEntity objectRecordStatusArmDropZone;
    private ObjectRecordStatusEntity objectRecordStatusArmProcessingFiles;
    private ObjectRecordStatusEntity objectRecordStatusArmResponseProcessingFailed;

    private ArmResponseFilesProcessor armResponseFilesProcessor;

    @BeforeEach
    void setupData() {

        externalLocationTypeArm = new ExternalLocationTypeEntity();
        externalLocationTypeArm.setId(ARM.getId());

        objectRecordStatusArmDropZone = new ObjectRecordStatusEntity();
        objectRecordStatusArmDropZone.setId(ARM_DROP_ZONE.getId());
        objectRecordStatusArmDropZone.setDescription("Arm Drop Zone");

        objectRecordStatusArmProcessingFiles = new ObjectRecordStatusEntity();
        objectRecordStatusArmProcessingFiles.setId(ARM_PROCESSING_RESPONSE_FILES.getId());
        objectRecordStatusArmProcessingFiles.setDescription("Arm Processing Response Files");

        objectRecordStatusArmResponseProcessingFailed = new ObjectRecordStatusEntity();
        objectRecordStatusArmResponseProcessingFailed.setId(ARM_RESPONSE_PROCESSING_FAILED.getId());
        objectRecordStatusArmResponseProcessingFailed.setDescription("Arm Response Process Failed");

        armResponseFilesProcessor = new ArmResponseFilesProcessorImpl(
            externalObjectDirectoryRepository,
            objectRecordStatusRepository,
            externalLocationTypeRepository,
            userIdentity,
            armResponseFilesProcessSingleElement
        );
    }

    @Test
    void processResponseFilesUnableToFindInputUploadFile() {

        when(externalLocationTypeRepository.getReferenceById(ARM.getId()))
            .thenReturn(externalLocationTypeArm);

        when(objectRecordStatusRepository.findById(ARM_DROP_ZONE.getId()))
            .thenReturn(Optional.of(objectRecordStatusArmDropZone));
        when(objectRecordStatusRepository.findById(ARM_PROCESSING_RESPONSE_FILES.getId()))
            .thenReturn(Optional.of(objectRecordStatusArmProcessingFiles));
        when(objectRecordStatusRepository.findById(ARM_RESPONSE_PROCESSING_FAILED.getId()))
            .thenReturn(Optional.of(objectRecordStatusArmResponseProcessingFailed));

        when(externalObjectDirectoryArmDropZone.getId())
            .thenReturn(1);
        when(externalObjectDirectoryArmDropZone.getStatus())
            .thenReturn(objectRecordStatusArmDropZone);

        List<ExternalObjectDirectoryEntity> inboundList = new ArrayList<>(Collections.singletonList(externalObjectDirectoryArmDropZone));
        when(externalObjectDirectoryRepository.findByExternalLocationTypeAndObjectStatus(externalLocationTypeArm, objectRecordStatusArmDropZone))
            .thenReturn(inboundList);

        armResponseFilesProcessor.processResponseFiles();

        verify(objectRecordStatusRepository).findById(ARM_DROP_ZONE.getId());
        verify(objectRecordStatusRepository).findById(ARM_PROCESSING_RESPONSE_FILES.getId());
        verify(objectRecordStatusRepository).findById(ARM_RESPONSE_PROCESSING_FAILED.getId());
        verify(externalLocationTypeRepository).getReferenceById(ARM.getId());
        verify(externalObjectDirectoryRepository).findByExternalLocationTypeAndObjectStatus(externalLocationTypeArm, objectRecordStatusArmDropZone);
        verify(externalObjectDirectoryRepository).saveAndFlush(externalObjectDirectoryEntityCaptor.capture());
        verify(armResponseFilesProcessSingleElement).processResponseFilesFor(1);

        verifyNoMoreInteractions(
            objectRecordStatusRepository,
            externalLocationTypeRepository,
            externalObjectDirectoryRepository,
            armResponseFilesProcessSingleElement
        );
    }

}
