package uk.gov.hmcts.darts.task.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.component.ArchiveRecordFileGenerator;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.service.ArchiveRecordService;
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.ARM;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.UNSTRUCTURED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_MANIFEST_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RAW_DATA_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;
import static uk.gov.hmcts.darts.common.util.EodEntities.armDropZoneStatus;
import static uk.gov.hmcts.darts.common.util.EodEntities.armLocation;
import static uk.gov.hmcts.darts.common.util.EodEntities.failedArmManifestFileStatus;

@SpringBootTest
@ActiveProfiles({"intTest", "h2db"})
class UnstructuredToArmBatchProcessorIntTest extends IntegrationBase {

    ArgumentCaptor<File> manifestFileNameCaptor = ArgumentCaptor.forClass(File.class);

    @MockBean
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
    @Autowired
    private ArchiveRecordService archiveRecordService;
    @Autowired
    private AuthorisationStub authorisationStub;
    @Autowired
    private ExternalObjectDirectoryStub externalObjectDirectoryStub;
    @SpyBean
    private ArchiveRecordFileGenerator archiveRecordFileGenerator;
    @Autowired
    private
    ExternalObjectDirectoryRepository eodRepository;

    @Autowired
    private UnstructuredToArmBatchProcessorImpl unstructuredToArmProcessor;

    @BeforeEach
    void setupData() {
        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);
    }

    @Test
    void testBatchedQuery() {

        //given
        //batch size is 5
        List<MediaEntity> medias = dartsDatabase.getMediaStub().createAndSaveSomeMedias();
        externalObjectDirectoryStub.createAndSaveEod(medias.get(0), STORED, UNSTRUCTURED);
        externalObjectDirectoryStub.createAndSaveEod(medias.get(1), STORED, UNSTRUCTURED);
        externalObjectDirectoryStub.createAndSaveEod(medias.get(2), STORED, UNSTRUCTURED);
        externalObjectDirectoryStub.createAndSaveEod(medias.get(2), ARM_RAW_DATA_FAILED, ARM);
        externalObjectDirectoryStub.createAndSaveEod(medias.get(3), STORED, UNSTRUCTURED);
        externalObjectDirectoryStub.createAndSaveEod(medias.get(3), ARM_MANIFEST_FAILED, ARM);
        externalObjectDirectoryStub.createAndSaveEod(medias.get(4), STORED, UNSTRUCTURED);
        externalObjectDirectoryStub.createAndSaveEod(medias.get(4), ARM_MANIFEST_FAILED, ARM);

        // skipped
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
        assertThat(foundMediaList.size()).isEqualTo(armDataManagementConfiguration.getBatchSize());
        assertThat(
            eodRepository.findMediaIdsByInMediaIdStatusAndType(List.of(medias.get(5).getId()), failedArmManifestFileStatus(), armLocation())
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
        var foundMediaList = eodRepository.findMediaIdsByInMediaIdStatusAndType(
            List.of(medias.get(0).getId(), medias.get(1).getId()), armDropZoneStatus(), armLocation()
        );
        assertEquals(2, foundMediaList.size());

        verify(armDataManagementApi, times(2)).saveBlobDataToArm(matches(".+_.+_1"), any());
        verify(armDataManagementApi, times(1)).saveBlobDataToArm(matches("DARTS_.+\\.a360"), any());

        verify(archiveRecordFileGenerator).generateArchiveRecords(any(), manifestFileNameCaptor.capture());
        Path generatedManifestFilePath = manifestFileNameCaptor.getValue().toPath();
        assertThat(lines(generatedManifestFilePath).count()).isEqualTo(2);
        assertThat(readString(generatedManifestFilePath))
            .contains("\"operation\":\"create_record\"",
                      "\"operation\":\"upload_new_file\"",
                      format("_%d_", medias.get(0).getId()),
                      format("_%d_", medias.get(1).getId()));
    }

    @Test
    void movePreviousArmFailedBelowMaxAttemptFromUnstructuredToArmStorage() {

        //given
        List<MediaEntity> medias = dartsDatabase.getMediaStub().createAndSaveSomeMedias();
        externalObjectDirectoryStub.createAndSaveEod(medias.get(0), STORED, UNSTRUCTURED);
        externalObjectDirectoryStub.createAndSaveEod(medias.get(0), ARM_RAW_DATA_FAILED, ARM, eod -> eod.setTransferAttempts(2));
        externalObjectDirectoryStub.createAndSaveEod(medias.get(1), STORED, UNSTRUCTURED);
        externalObjectDirectoryStub.createAndSaveEod(medias.get(1), ARM_MANIFEST_FAILED, ARM);
        externalObjectDirectoryStub.createAndSaveEod(medias.get(2), STORED, UNSTRUCTURED);
        externalObjectDirectoryStub.createAndSaveEod(medias.get(2), ARM_MANIFEST_FAILED, ARM, eod -> eod.setTransferAttempts(5));

        //when
        unstructuredToArmProcessor.processUnstructuredToArm();

        //then
        verify(armDataManagementApi, times(1)).saveBlobDataToArm(matches(".+_.+_2"), any());
        verify(armDataManagementApi, times(1)).saveBlobDataToArm(matches("DARTS_.+\\.a360"), any());

        var armDropzoneEodsMedia0 = eodRepository.findByMediaStatusAndType(medias.get(0), armDropZoneStatus(), armLocation());
        assertThat(armDropzoneEodsMedia0).hasSize(1);
        var armDropzoneEodsMedia1 = eodRepository.findByMediaStatusAndType(medias.get(1), armDropZoneStatus(), armLocation());
        assertThat(armDropzoneEodsMedia1).hasSize(1);

        verify(archiveRecordFileGenerator).generateArchiveRecords(any(), manifestFileNameCaptor.capture());
        assertThat(armDropzoneEodsMedia0.get(0).getManifestFile()).isEqualTo(manifestFileNameCaptor.getValue().getName());
        assertThat(armDropzoneEodsMedia1.get(0).getManifestFile()).isEqualTo(manifestFileNameCaptor.getValue().getName());
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
        assertThat(lines(generatedManifestFilePath).count()).isEqualTo(1);
        assertThat(readString(generatedManifestFilePath)).contains(format("_%d_", medias.get(1).getId()));
        assertThat(readString(generatedManifestFilePath)).doesNotContain(format("_%d_", medias.get(0).getId()));
    }

}
