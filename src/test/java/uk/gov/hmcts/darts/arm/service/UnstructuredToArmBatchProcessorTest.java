package uk.gov.hmcts.darts.arm.service;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.component.ArchiveRecordFileGenerator;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.service.impl.UnstructuredToArmBatchProcessorImpl;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.exception.DartsException;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.common.service.impl.EodHelperMocks;
import uk.gov.hmcts.darts.common.util.EodHelper;
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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnstructuredToArmBatchProcessorTest {

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
    private ExternalObjectDirectoryEntity eod1;
    @Mock
    private ExternalObjectDirectoryEntity eod2;
    @Mock
    private FileOperationService fileOperationService;
    @Mock
    private ArchiveRecordService archiveRecordService;

    private UnstructuredToArmProcessor unstructuredToArmProcessor;
    @Mock
    private ExternalObjectDirectoryService eodService;
    @Mock
    private ArchiveRecordFileGenerator archiveRecordFileGenerator;

    @Mock(answer = RETURNS_DEEP_STUBS)
    Path manifestFilePath;
    @Mock
    File manifestFile;

    private static final EodHelperMocks EOD_HELPER_MOCKS = new EodHelperMocks();

    @AfterAll
    static void close() {
        EOD_HELPER_MOCKS.close();
    }

    @BeforeEach
    void setUp() throws IOException {

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

        lenient().when(fileOperationService.createFile(any(), any(), anyBoolean())).thenReturn(manifestFilePath);
        when(manifestFilePath.toFile()).thenReturn(manifestFile);

    }

    @Test
    void testDartsArmClientConfigInBatchQuery() {
        //given
        when(armDataManagementConfiguration.getArmClient()).thenReturn("darts");
        when(armDataManagementConfiguration.getBatchSize()).thenReturn(5);
        when(externalObjectDirectoryRepository.findExternalObjectsNotIn2StorageLocations(any(), any(), any(), any())).thenReturn(emptyList());
        when(eodService.findFailedStillRetriableArmEods(any())).thenReturn(List.of(eod1));
        doReturn(EodHelper.armLocation()).when(eod1).getExternalLocationType();
        EOD_HELPER_MOCKS.givenIsEqualLocationReturns(true);

        //when
        unstructuredToArmProcessor.processUnstructuredToArm();

        //then
        verify(externalObjectDirectoryRepository).findExternalObjectsNotIn2StorageLocations(
            EodHelper.storedStatus(),
            EodHelper.unstructuredLocation(),
            EodHelper.armLocation(),
            Pageable.ofSize(5)
        );
        verify(externalObjectDirectoryRepository).findMatchingExternalObjectDirectoryEntityByLocation(
            EodHelper.storedStatus(),
            EodHelper.unstructuredLocation(),
            null,
            null,
            null,
            null
        );
    }

    @Test
    void testDetsArmClientConfigInBatchQuery() {
        //given
        when(armDataManagementConfiguration.getArmClient()).thenReturn("dets");
        when(armDataManagementConfiguration.getBatchSize()).thenReturn(5);
        when(externalObjectDirectoryRepository.findExternalObjectsNotIn2StorageLocations(any(), any(), any(), any())).thenReturn(emptyList());
        when(eodService.findFailedStillRetriableArmEods(any())).thenReturn(List.of(eod1));
        doReturn(EodHelper.armLocation()).when(eod1).getExternalLocationType();
        EOD_HELPER_MOCKS.givenIsEqualLocationReturns(true);

        //when
        unstructuredToArmProcessor.processUnstructuredToArm();

        //then
        verify(externalObjectDirectoryRepository).findExternalObjectsNotIn2StorageLocations(
            EodHelper.storedStatus(),
            EodHelper.detsLocation(),
            EodHelper.armLocation(),
            Pageable.ofSize(5)
        );
        verify(externalObjectDirectoryRepository).findMatchingExternalObjectDirectoryEntityByLocation(
            EodHelper.storedStatus(),
            EodHelper.detsLocation(),
            null,
            null,
            null,
            null
        );
    }

    @Test
    void testUnknownArmClientConfigInBatchQuery() {
        //given
        when(armDataManagementConfiguration.getArmClient()).thenReturn("unknown");
        when(armDataManagementConfiguration.getBatchSize()).thenReturn(5);

        //then
        Assertions.assertThrows(DartsException.class, () -> unstructuredToArmProcessor.processUnstructuredToArm());
    }

    @Test
    void testPaginatedBatchQuery() throws IOException {
        //given
        when(armDataManagementConfiguration.getArmClient()).thenReturn("darts");
        when(armDataManagementConfiguration.getBatchSize()).thenReturn(5);
        when(externalObjectDirectoryRepository.findExternalObjectsNotIn2StorageLocations(any(), any(), any(), any())).thenReturn(List.of(eod1, eod2));
        when(eodService.findFailedStillRetriableArmEods(any())).thenReturn(emptyList());

        when(fileOperationService.createFile(any(), any(), anyBoolean())).thenReturn(manifestFilePath);

        //when
        unstructuredToArmProcessor.processUnstructuredToArm();

        //then
        verify(externalObjectDirectoryRepository).findExternalObjectsNotIn2StorageLocations(
            EodHelper.storedStatus(),
            EodHelper.unstructuredLocation(),
            EodHelper.armLocation(),
            Pageable.ofSize(5)
        );
        verify(eodService).findFailedStillRetriableArmEods(Pageable.ofSize(3));
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
        when(eodService.findFailedStillRetriableArmEods(any())).thenReturn(emptyList());

        //when
        unstructuredToArmProcessor.processUnstructuredToArm();

        //then
        verify(fileOperationService).createFile(matches("DARTS_.+\\.a360"), eq("/temp_workspace"), eq(true));
    }

}
