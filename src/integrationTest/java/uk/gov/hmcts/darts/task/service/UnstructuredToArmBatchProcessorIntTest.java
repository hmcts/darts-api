package uk.gov.hmcts.darts.task.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.component.ArchiveRecordFileGenerator;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.mapper.MediaArchiveRecordMapper;
import uk.gov.hmcts.darts.arm.service.ArchiveRecordService;
import uk.gov.hmcts.darts.arm.service.ExternalObjectDirectoryService;
import uk.gov.hmcts.darts.arm.service.impl.UnstructuredToArmBatchProcessorImpl;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.AuthorisationStub;
import uk.gov.hmcts.darts.testutils.stubs.ExternalObjectDirectoryStub;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static java.lang.String.format;
import static java.nio.file.Files.lines;
import static java.nio.file.Files.readString;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.ARM;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.UNSTRUCTURED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_MANIFEST_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RAW_DATA_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RESPONSE_MANIFEST_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;
import static uk.gov.hmcts.darts.common.util.EodHelper.armDropZoneStatus;
import static uk.gov.hmcts.darts.common.util.EodHelper.armLocation;
import static uk.gov.hmcts.darts.common.util.EodHelper.failedArmManifestFileStatus;
import static uk.gov.hmcts.darts.common.util.EodHelper.failedArmRawDataStatus;
import static uk.gov.hmcts.darts.common.util.EodHelper.storedStatus;
import static uk.gov.hmcts.darts.common.util.EodHelper.unstructuredLocation;

class UnstructuredToArmBatchProcessorIntTest extends IntegrationBase {

    ArgumentCaptor<File> manifestFileNameCaptor = ArgumentCaptor.forClass(File.class);

    @SpyBean
    private ArmDataManagementApi armDataManagementApi;

    @Autowired
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    @Autowired
    private ObjectRecordStatusRepository objectRecordStatusRepository;
    @Autowired
    private ExternalLocationTypeRepository externalLocationTypeRepository;
    @SpyBean
    private DataManagementApi dataManagementApi;
    @MockBean
    private UserIdentity userIdentity;
    @Autowired
    private ArmDataManagementConfiguration armDataManagementConfiguration;
    @Autowired
    private FileOperationService fileOperationService;
    @SpyBean
    private ArchiveRecordService archiveRecordService;
    @Autowired
    private AuthorisationStub authorisationStub;
    @Autowired
    private ExternalObjectDirectoryStub externalObjectDirectoryStub;
    @SpyBean
    private ArchiveRecordFileGenerator archiveRecordFileGenerator;
    @Autowired
    private ExternalObjectDirectoryRepository eodRepository;
    @SpyBean
    private MediaArchiveRecordMapper mediaArchiveRecordMapper;
    private UserAccountEntity testUser;
    private static final Integer BATCH_SIZE = 5;

    @Autowired
    private LogApi logApi;

    @Autowired
    private ExternalObjectDirectoryService eodService;

    private UnstructuredToArmBatchProcessorImpl unstructuredToArmProcessor;

    @BeforeEach
    void setupData() {
        testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);

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
            archiveRecordFileGenerator,
            BATCH_SIZE,
            logApi
        );
    }

    @Test
    void testBatchedQuery() {

        //given
        //batch size is 5
        List<MediaEntity> medias = dartsDatabase.getMediaStub().createAndSaveSomeMedias();

        // skipped
        externalObjectDirectoryStub.createAndSaveEod(medias.get(0), STORED, UNSTRUCTURED);

        // processed
        externalObjectDirectoryStub.createAndSaveEod(medias.get(1), STORED, UNSTRUCTURED);
        externalObjectDirectoryStub.createAndSaveEod(medias.get(2), STORED, UNSTRUCTURED);
        externalObjectDirectoryStub.createAndSaveEod(medias.get(2), ARM_RAW_DATA_FAILED, ARM);
        externalObjectDirectoryStub.createAndSaveEod(medias.get(3), STORED, UNSTRUCTURED);
        externalObjectDirectoryStub.createAndSaveEod(medias.get(3), ARM_MANIFEST_FAILED, ARM);
        externalObjectDirectoryStub.createAndSaveEod(medias.get(4), STORED, UNSTRUCTURED);
        externalObjectDirectoryStub.createAndSaveEod(medias.get(4), ARM_MANIFEST_FAILED, ARM);

        externalObjectDirectoryStub.createAndSaveEod(medias.get(5), STORED, UNSTRUCTURED);
        externalObjectDirectoryStub.createAndSaveEod(medias.get(5), ARM_MANIFEST_FAILED, ARM);

        //when
        unstructuredToArmProcessor.processUnstructuredToArm();

        //then
        var foundMediaList = eodRepository.findMediaIdsByInMediaIdStatusAndType(
            List.of(medias.get(0).getId(),
                    medias.get(1).getId(),
                    medias.get(2).getId(),
                    medias.get(3).getId(),
                    medias.get(4).getId(),
                    medias.get(5).getId()),
            armDropZoneStatus(),
            armLocation()
        );
        assertThat(foundMediaList.size()).isEqualTo(BATCH_SIZE);
        assertThat(
            eodRepository.findMediaIdsByInMediaIdStatusAndType(List.of(medias.get(0).getId()), storedStatus(), unstructuredLocation())
        )
            .hasSize(1);
    }

    @Test
    void movePendingMediaDataFromUnstructuredToArmStorage() throws IOException {

        //given
        List<MediaEntity> medias = dartsDatabase.getMediaStub().createAndSaveSomeMedias();
        externalObjectDirectoryStub.createAndSaveEod(medias.get(0), STORED, UNSTRUCTURED);
        externalObjectDirectoryStub.createAndSaveEod(medias.get(1), STORED, UNSTRUCTURED);

        //when
        unstructuredToArmProcessor.processUnstructuredToArm();

        //then
        List<ExternalObjectDirectoryEntity> armDropZoneEodsMedia0 = eodRepository.findByMediaStatusAndType(medias.get(0), armDropZoneStatus(), armLocation());
        assertThat(armDropZoneEodsMedia0).hasSize(1);
        List<ExternalObjectDirectoryEntity> armDropZoneEodsMedia1 = eodRepository.findByMediaStatusAndType(medias.get(1), armDropZoneStatus(), armLocation());
        assertThat(armDropZoneEodsMedia1).hasSize(1);

        var rawFile0Name = format("%d_%d_1", armDropZoneEodsMedia0.get(0).getId(), medias.get(0).getId());
        var rawFile1Name = format("%d_%d_1", armDropZoneEodsMedia1.get(0).getId(), medias.get(1).getId());

        verify(armDataManagementApi, times(1)).copyBlobDataToArm(any(), eq(rawFile0Name));
        verify(armDataManagementApi, times(1)).copyBlobDataToArm(any(), eq(rawFile0Name));
        verify(armDataManagementApi, times(1)).saveBlobDataToArm(matches("DARTS_.+\\.a360"), any());

        verify(archiveRecordFileGenerator).generateArchiveRecords(any(), manifestFileNameCaptor.capture());
        File manifestFile = manifestFileNameCaptor.getValue();
        Path manifestFilePath = manifestFile.toPath();
        assertThat(lines(manifestFilePath).count()).isEqualTo(4);
        assertThat(readString(manifestFilePath))
            .contains("\"operation\":\"create_record\"",
                      "\"operation\":\"upload_new_file\"",
                      "\"dz_file_name\":\"" + rawFile0Name,
                      "\"dz_file_name\":\"" + rawFile1Name);

        assertThat(armDropZoneEodsMedia0.get(0).getManifestFile()).isEqualTo(manifestFile.getName());
        assertThat(armDropZoneEodsMedia0.get(0).getLastModifiedBy().getId()).isEqualTo(testUser.getId());
        assertThat(armDropZoneEodsMedia0.get(0).getLastModifiedDateTime()).isCloseToUtcNow(within(1, SECONDS));
        assertThat(armDropZoneEodsMedia1.get(0).getManifestFile()).isEqualTo(manifestFile.getName());
    }

    @Test
    void movePreviousArmFailedFromUnstructuredToArmStorage() throws IOException {

        //given
        List<MediaEntity> medias = dartsDatabase.getMediaStub().createAndSaveSomeMedias();
        externalObjectDirectoryStub.createAndSaveEod(medias.get(0), STORED, UNSTRUCTURED);
        externalObjectDirectoryStub.createAndSaveEod(medias.get(0), ARM_RAW_DATA_FAILED, ARM, eod -> eod.setTransferAttempts(2));
        externalObjectDirectoryStub.createAndSaveEod(medias.get(1), STORED, UNSTRUCTURED);
        externalObjectDirectoryStub.createAndSaveEod(medias.get(1), ARM_MANIFEST_FAILED, ARM);
        externalObjectDirectoryStub.createAndSaveEod(medias.get(2), STORED, UNSTRUCTURED);
        externalObjectDirectoryStub.createAndSaveEod(medias.get(2), ARM_MANIFEST_FAILED, ARM, eod -> eod.setTransferAttempts(5));
        externalObjectDirectoryStub.createAndSaveEod(medias.get(3), STORED, UNSTRUCTURED);
        externalObjectDirectoryStub.createAndSaveEod(medias.get(3), ARM_RESPONSE_MANIFEST_FAILED, ARM);

        //when
        unstructuredToArmProcessor.processUnstructuredToArm();

        //then
        verify(armDataManagementApi, times(1)).copyBlobDataToArm(any(), matches(".+_.+_2"));
        verify(armDataManagementApi, times(1)).saveBlobDataToArm(matches("DARTS_.+\\.a360"), any());
        verify(armDataManagementApi, times(1)).saveBlobDataToArm(any(), any());

        var armDropzoneEodsMedia0 = eodRepository.findByMediaStatusAndType(medias.get(0), armDropZoneStatus(), armLocation());
        assertThat(armDropzoneEodsMedia0).hasSize(1);
        var armDropzoneEodsMedia1 = eodRepository.findByMediaStatusAndType(medias.get(1), armDropZoneStatus(), armLocation());
        assertThat(armDropzoneEodsMedia1).hasSize(1);
        var armDropzoneEodsMedia3 = eodRepository.findByMediaStatusAndType(medias.get(3), armDropZoneStatus(), armLocation());
        assertThat(armDropzoneEodsMedia3).hasSize(1);

        verify(archiveRecordFileGenerator).generateArchiveRecords(any(), manifestFileNameCaptor.capture());
        File manifestFile = manifestFileNameCaptor.getValue();
        assertThat(armDropzoneEodsMedia0.get(0).getManifestFile()).isEqualTo(manifestFile.getName());
        assertThat(armDropzoneEodsMedia0.get(0).getLastModifiedBy().getId()).isEqualTo(testUser.getId());
        assertThat(armDropzoneEodsMedia0.get(0).getLastModifiedDateTime()).isCloseToUtcNow(within(1, SECONDS));
        assertThat(armDropzoneEodsMedia1.get(0).getManifestFile()).isEqualTo(manifestFile.getName());
        assertThat(armDropzoneEodsMedia3.get(0).getManifestFile()).isEqualTo(manifestFile.getName());

        Path generatedManifestFilePath = manifestFile.toPath();
        assertThat(lines(generatedManifestFilePath).count()).isEqualTo(6);
        assertThat(readString(generatedManifestFilePath)).contains(
            format("_%d_", medias.get(0).getId()),
            format("_%d_", medias.get(1).getId()),
            format("_%d_", medias.get(3).getId())
        );
        assertThat(readString(generatedManifestFilePath)).doesNotContain(format("_%d_", medias.get(2).getId()));
    }

    @Test
    void movePreviousArmFailedWithNoCorrespondingUnstructuredFailsAndProcessingContinues() throws IOException {

        //given
        List<MediaEntity> medias = dartsDatabase.getMediaStub().createAndSaveSomeMedias();
        externalObjectDirectoryStub.createAndSaveEod(medias.get(0), ARM_MANIFEST_FAILED, ARM, eod -> eod.setManifestFile("existingManifestFile"));
        externalObjectDirectoryStub.createAndSaveEod(medias.get(1), STORED, UNSTRUCTURED);
        externalObjectDirectoryStub.createAndSaveEod(medias.get(1), ARM_RAW_DATA_FAILED, ARM);

        //when
        unstructuredToArmProcessor.processUnstructuredToArm();

        //then
        List<ExternalObjectDirectoryEntity> failedArmEods = eodRepository.findByMediaStatusAndType(
            medias.get(0), failedArmManifestFileStatus(), armLocation());
        assertThat(failedArmEods).hasSize(1);
        assertThat(failedArmEods.get(0).getManifestFile()).isEqualTo("existingManifestFile");
        assertThat(failedArmEods.get(0).getTransferAttempts()).isEqualTo(2);
        assertThat(eodRepository.findByMediaStatusAndType(medias.get(1), armDropZoneStatus(), armLocation())).hasSize(1);

        verify(archiveRecordFileGenerator).generateArchiveRecords(any(), manifestFileNameCaptor.capture());
        Path generatedManifestFilePath = manifestFileNameCaptor.getValue().toPath();
        assertThat(lines(generatedManifestFilePath).count()).isEqualTo(2);
        assertThat(readString(generatedManifestFilePath)).contains(format("_%d_", medias.get(1).getId()));
        assertThat(readString(generatedManifestFilePath)).doesNotContain(format("_%d_", medias.get(0).getId()));
    }

    @Test
    void pushRawDataFails() {

        //given
        List<MediaEntity> medias = dartsDatabase.getMediaStub().createAndSaveSomeMedias();
        externalObjectDirectoryStub.createAndSaveEod(medias.get(0), STORED, UNSTRUCTURED);
        externalObjectDirectoryStub.createAndSaveEod(medias.get(0), ARM_RAW_DATA_FAILED, ARM, eod -> {
            eod.setManifestFile("existingManifestFile");
            eod.setTransferAttempts(2);
        });
        externalObjectDirectoryStub.createAndSaveEod(medias.get(1), STORED, UNSTRUCTURED);
        externalObjectDirectoryStub.createAndSaveEod(medias.get(1), ARM_RAW_DATA_FAILED, ARM);

        doThrow(RuntimeException.class).when(armDataManagementApi).copyBlobDataToArm(any(), matches(".+_.+_2"));

        //when
        unstructuredToArmProcessor.processUnstructuredToArm();

        //then
        List<ExternalObjectDirectoryEntity> failedArmEods = eodRepository.findByMediaStatusAndType(medias.get(0), failedArmRawDataStatus(), armLocation());
        assertThat(failedArmEods).hasSize(1);
        List<ExternalObjectDirectoryEntity> ingestedArmEods = eodRepository.findByMediaStatusAndType(medias.get(1), armDropZoneStatus(), armLocation());
        assertThat(ingestedArmEods).hasSize(1);
        var failedEod = failedArmEods.get(0);
        assertThat(failedEod.getTransferAttempts()).isEqualTo(3);
        assertThat(failedEod.getManifestFile()).isEqualTo("existingManifestFile");
    }

    @Test
    void generationOfManifestFileEntryFails() {

        //given
        List<MediaEntity> medias = dartsDatabase.getMediaStub().createAndSaveSomeMedias();
        externalObjectDirectoryStub.createAndSaveEod(medias.get(0), STORED, UNSTRUCTURED);
        externalObjectDirectoryStub.createAndSaveEod(medias.get(0), ARM_RAW_DATA_FAILED, ARM, eod -> eod.setManifestFile("existingManifestFile"));
        externalObjectDirectoryStub.createAndSaveEod(medias.get(1), STORED, UNSTRUCTURED);
        externalObjectDirectoryStub.createAndSaveEod(medias.get(1), ARM_MANIFEST_FAILED, ARM);

        doReturn(null).when(mediaArchiveRecordMapper).mapToMediaArchiveRecord(any(), any());

        //when
        unstructuredToArmProcessor.processUnstructuredToArm();

        //then
        List<ExternalObjectDirectoryEntity> failedArmEodsMedia0 = eodRepository.findByMediaStatusAndType(
            medias.get(0), failedArmManifestFileStatus(), armLocation());
        assertThat(failedArmEodsMedia0).hasSize(1);
        List<ExternalObjectDirectoryEntity> failedArmEodsMedia1 = eodRepository.findByMediaStatusAndType(
            medias.get(1), failedArmManifestFileStatus(), armLocation());
        assertThat(failedArmEodsMedia1).hasSize(1);
        var failedEod = failedArmEodsMedia0.get(0);
        assertThat(failedEod.getTransferAttempts()).isEqualTo(2);
        assertThat(failedEod.getManifestFile()).isEqualTo("existingManifestFile");

        verify(armDataManagementApi, never()).saveBlobDataToArm(matches("DARTS_.+\\.a360"), any());
    }

    @Test
    void writingManifestFileFails() {

        //given
        List<MediaEntity> medias = dartsDatabase.getMediaStub().createAndSaveSomeMedias();
        externalObjectDirectoryStub.createAndSaveEod(medias.get(0), STORED, UNSTRUCTURED);
        externalObjectDirectoryStub.createAndSaveEod(medias.get(0), ARM_RAW_DATA_FAILED, ARM, eod -> eod.setManifestFile("existingManifestFile"));
        externalObjectDirectoryStub.createAndSaveEod(medias.get(1), STORED, UNSTRUCTURED);
        externalObjectDirectoryStub.createAndSaveEod(medias.get(1), ARM_MANIFEST_FAILED, ARM);

        doThrow(RuntimeException.class).when(archiveRecordFileGenerator).generateArchiveRecords(any(), any());

        //when
        unstructuredToArmProcessor.processUnstructuredToArm();

        //then
        List<ExternalObjectDirectoryEntity> failedArmEodsMedia0 = eodRepository.findByMediaStatusAndType(
            medias.get(0), failedArmManifestFileStatus(), armLocation());
        assertThat(failedArmEodsMedia0).hasSize(1);
        List<ExternalObjectDirectoryEntity> failedArmEodsMedia1 = eodRepository.findByMediaStatusAndType(
            medias.get(1), failedArmManifestFileStatus(), armLocation());
        assertThat(failedArmEodsMedia1).hasSize(1);
        var failedEod = failedArmEodsMedia0.get(0);
        assertThat(failedEod.getTransferAttempts()).isEqualTo(2);
        assertThat(failedEod.getManifestFile()).isEqualTo("existingManifestFile");

        verify(armDataManagementApi, never()).saveBlobDataToArm(matches("DARTS_.+\\.a360"), any());
    }

    @Test
    void pushingManifestFileFails() {

        //given
        List<MediaEntity> medias = dartsDatabase.getMediaStub().createAndSaveSomeMedias();
        externalObjectDirectoryStub.createAndSaveEod(medias.get(0), STORED, UNSTRUCTURED);
        externalObjectDirectoryStub.createAndSaveEod(medias.get(0), ARM_RAW_DATA_FAILED, ARM, eod -> eod.setManifestFile("existingManifestFile"));
        externalObjectDirectoryStub.createAndSaveEod(medias.get(1), STORED, UNSTRUCTURED);
        externalObjectDirectoryStub.createAndSaveEod(medias.get(1), ARM_MANIFEST_FAILED, ARM);

        doThrow(RuntimeException.class).when(armDataManagementApi).saveBlobDataToArm(matches("DARTS_.+\\.a360"), any());

        //when
        unstructuredToArmProcessor.processUnstructuredToArm();

        //then
        List<ExternalObjectDirectoryEntity> failedArmEodsMedia0 = eodRepository.findByMediaStatusAndType(
            medias.get(0), failedArmManifestFileStatus(), armLocation());
        assertThat(failedArmEodsMedia0).hasSize(1);
        List<ExternalObjectDirectoryEntity> failedArmEodsMedia1 = eodRepository.findByMediaStatusAndType(
            medias.get(1), failedArmManifestFileStatus(), armLocation());
        assertThat(failedArmEodsMedia1).hasSize(1);
        var failedEod = failedArmEodsMedia0.get(0);
        assertThat(failedEod.getTransferAttempts()).isEqualTo(2);
        assertThat(failedEod.getManifestFile()).isEqualTo("existingManifestFile");
    }

}