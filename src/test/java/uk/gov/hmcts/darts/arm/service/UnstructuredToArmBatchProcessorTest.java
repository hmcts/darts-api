package uk.gov.hmcts.darts.arm.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.component.ArchiveRecordFileGenerator;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.service.impl.UnstructuredToArmBatchProcessorImpl;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.common.service.impl.EodEntitiesMock;
import uk.gov.hmcts.darts.common.util.EodEntities;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnstructuredToArmBatchProcessorTest {

    private static final String TEST_BINARY_DATA = "test binary data";
    private static final Integer MAX_RETRY_ATTEMPTS = 3;

    @Mock
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    @Mock
    private ObjectRecordStatusRepository objectRecordStatusRepository;
    @Mock
    private ExternalLocationTypeRepository externalLocationTypeRepository;
    @Mock
    private DataManagementApi dataManagementApi;
    @Mock
    private ArmDataManagementApi armDataManagementApi;
    @Mock
    private UserIdentity userIdentity;
    @Mock
    private ArmDataManagementConfiguration armDataManagementConfiguration;
    @Mock
    private MediaEntity mediaEntity;
    @Mock
    private TranscriptionDocumentEntity transcriptionDocumentEntity;
    @Mock
    private AnnotationDocumentEntity annotationDocumentEntity;
    @Mock
    private ExternalObjectDirectoryEntity eodUnstructured;
    @Mock
    private ExternalObjectDirectoryEntity eodArm;
    @Mock
    private ExternalObjectDirectoryEntity eod1;
    @Mock
    private ExternalObjectDirectoryEntity eod2;
    @Mock
    private ExternalLocationTypeEntity externalLocationTypeUnstructured;
    @Mock
    private ExternalLocationTypeEntity externalLocationTypeArm;
    @Mock
    private FileOperationService fileOperationService;
    @Mock
    private ArchiveRecordService archiveRecordService;

    private UnstructuredToArmProcessor unstructuredToArmProcessor;

    @Mock
    private ObjectRecordStatusEntity objectRecordStatusEntityStored;
    @Mock
    private ObjectRecordStatusEntity objectRecordStatusEntityArmIngestion;
    @Mock
    private ObjectRecordStatusEntity objectRecordStatusEntityRawDataFailed;
    @Mock
    private ObjectRecordStatusEntity objectRecordStatusEntityManifestFailed;
    @Mock
    private ObjectRecordStatusEntity objectRecordStatusEntityArmDropZone;
    @Mock
    private ExternalObjectDirectoryService eodService;
    @Mock
    private ArchiveRecordFileGenerator archiveRecordFileGenerator;
    @Captor
    private ArgumentCaptor<ExternalObjectDirectoryEntity> externalObjectDirectoryEntityCaptor;
    @Mock(answer = RETURNS_DEEP_STUBS)
    Path manifestFilePath;
    @Mock
    File manifestFile;

    @TempDir
    private File tempDirectory;


    @BeforeEach
    void setUp() {

        unstructuredToArmProcessor = new UnstructuredToArmBatchProcessorImpl(
            externalObjectDirectoryRepository,
            objectRecordStatusRepository,
            externalLocationTypeRepository,
            dataManagementApi,
            armDataManagementApi,
            userIdentity,
            armDataManagementConfiguration,
            fileOperationService,
            archiveRecordService,
            eodService,
            archiveRecordFileGenerator
        );

        EodEntitiesMock eodEntitiesMock = new EodEntitiesMock();
        eodEntitiesMock.givenEodEntitiesAreMocked();

        when(manifestFilePath.toFile()).thenReturn(manifestFile);
    }

    @Test
    public void testDartsArmClientConfigInBatchQuery() {
        //given
        when(armDataManagementConfiguration.getArmClient()).thenReturn("darts");
        when(armDataManagementConfiguration.getBatchSize()).thenReturn(5);
        when(externalObjectDirectoryRepository.findExternalObjectsNotIn2StorageLocations(any(), any(), any(), any())).thenReturn(emptyList());
        when(eodService.findFailedStillRetriableArmEODs(any())).thenReturn(emptyList());

        //when
        unstructuredToArmProcessor.processUnstructuredToArm();

        //then
        verify(externalObjectDirectoryRepository).findExternalObjectsNotIn2StorageLocations(
            EodEntities.storedStatus(),
            EodEntities.unstructuredLocation(),
            EodEntities.armLocation(),
            Pageable.ofSize(5)
        );
    }

    @Test
    public void testDetsArmClientConfigInBatchQuery() {
        //given
        when(armDataManagementConfiguration.getArmClient()).thenReturn("dets");
        when(armDataManagementConfiguration.getBatchSize()).thenReturn(5);
        when(externalObjectDirectoryRepository.findExternalObjectsNotIn2StorageLocations(any(), any(), any(), any())).thenReturn(emptyList());
        when(eodService.findFailedStillRetriableArmEODs(any())).thenReturn(emptyList());

        //when
        unstructuredToArmProcessor.processUnstructuredToArm();

        //then
        verify(externalObjectDirectoryRepository).findExternalObjectsNotIn2StorageLocations(
            EodEntities.storedStatus(),
            EodEntities.detsLocation(),
            EodEntities.armLocation(),
            Pageable.ofSize(5)
        );
    }

    @Test
    public void testUnknownArmClientConfigInBatchQuery() {
        //given
        when(armDataManagementConfiguration.getArmClient()).thenReturn("unknown");
        when(armDataManagementConfiguration.getBatchSize()).thenReturn(5);

        //when
        unstructuredToArmProcessor.processUnstructuredToArm();

        //then
        verifyNoInteractions(userIdentity);
    }

    @Test
    public void testPaginatedBatchQuery() throws IOException {
        //given
        when(armDataManagementConfiguration.getArmClient()).thenReturn("darts");
        when(armDataManagementConfiguration.getBatchSize()).thenReturn(5);
        when(externalObjectDirectoryRepository.findExternalObjectsNotIn2StorageLocations(any(), any(), any(), any())).thenReturn(List.of(eod1, eod2));
        when(eodService.findFailedStillRetriableArmEODs(any())).thenReturn(emptyList());

        when(fileOperationService.createFile(any(), any(), anyBoolean())).thenReturn(manifestFilePath);

        //when
        unstructuredToArmProcessor.processUnstructuredToArm();

        //then
        verify(externalObjectDirectoryRepository).findExternalObjectsNotIn2StorageLocations(
            EodEntities.storedStatus(),
            EodEntities.unstructuredLocation(),
            EodEntities.armLocation(),
            Pageable.ofSize(5)
        );
        verify(eodService).findFailedStillRetriableArmEODs(Pageable.ofSize(3));
    }

    @Test
    void testManifestFileName() throws IOException {
        //given
        when(armDataManagementConfiguration.getArmClient()).thenReturn("darts");
        when(armDataManagementConfiguration.getBatchSize()).thenReturn(5);
        when(armDataManagementConfiguration.getManifestFilePrefix()).thenReturn("DARTS");
        when(armDataManagementConfiguration.getFileExtension()).thenReturn("a360");
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn("/temp_workspace");
        when(externalObjectDirectoryRepository.findExternalObjectsNotIn2StorageLocations(any(), any(), any(), any())).thenReturn(List.of(eod1));
        when(eodService.findFailedStillRetriableArmEODs(any())).thenReturn(emptyList());

        when(fileOperationService.createFile(any(), any(), anyBoolean())).thenReturn(manifestFilePath);

        //when
        unstructuredToArmProcessor.processUnstructuredToArm();

        //then
        verify(fileOperationService).createFile(matches("DARTS_.+\\.a360"), eq("/temp_workspace"), eq(true));
    }

}
