package uk.gov.hmcts.darts.task.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.component.ArchiveRecordFileGenerator;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.config.UnstructuredToArmProcessorConfiguration;
import uk.gov.hmcts.darts.arm.helper.DataStoreToArmHelper;
import uk.gov.hmcts.darts.arm.mapper.MediaArchiveRecordMapper;
import uk.gov.hmcts.darts.arm.service.ArchiveRecordService;
import uk.gov.hmcts.darts.arm.service.ExternalObjectDirectoryService;
import uk.gov.hmcts.darts.arm.service.UnstructuredToArmBatchProcessor;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsException;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectRecordStatusRepository;
import uk.gov.hmcts.darts.common.service.FileOperationService;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.AuthorisationStub;
import uk.gov.hmcts.darts.testutils.stubs.ExternalObjectDirectoryStub;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

@Slf4j
class UnstructuredToArmBatchProcessorIntTest extends IntegrationBase {

    ArgumentCaptor<String> manifestFileNameCaptor = ArgumentCaptor.forClass(String.class);

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
    @SpyBean
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
    @SpyBean
    private DataStoreToArmHelper dataStoreToArmHelper;

    @MockBean
    private UnstructuredToArmProcessorConfiguration unstructuredToArmProcessorConfiguration;
    private UserAccountEntity testUser;
    private static final Integer BATCH_SIZE = 5;

    @Autowired
    private LogApi logApi;

    @Autowired
    private ExternalObjectDirectoryService eodService;

    @Autowired
    private UnstructuredToArmBatchProcessor unstructuredToArmProcessor;

    @BeforeEach
    void setupData() {
        testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);
        when(unstructuredToArmProcessorConfiguration.getMaxArmManifestItems()).thenReturn(5);
        when(unstructuredToArmProcessorConfiguration.getThreads()).thenReturn(20);
    }

    @Test
    void testBatchedQueryEqualsBatchSizeSuccess() {

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
        unstructuredToArmProcessor.processUnstructuredToArm(5);

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
    void testBatchedQueryWithBatchSizeGreaterThanManifestConfigSuccess() {

        //given
        //batch size is 5 but manifest size is 3
        when(unstructuredToArmProcessorConfiguration.getMaxArmManifestItems()).thenReturn(3);

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
        unstructuredToArmProcessor.processUnstructuredToArm(5);

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
        assertThat(foundMediaList.size()).isEqualTo(5);
        assertThat(
            eodRepository.findMediaIdsByInMediaIdStatusAndType(List.of(medias.get(0).getId()), storedStatus(), unstructuredLocation())
        )
            .hasSize(1);
    }

    @Test
    void testBatchedQueryWithBatchSizeLessThanManifestConfigSuccess() {

        //given
        //batch size is 3 but manifest size is 5
        when(unstructuredToArmProcessorConfiguration.getMaxArmManifestItems()).thenReturn(5);

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
        unstructuredToArmProcessor.processUnstructuredToArm(3);

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
        assertThat(foundMediaList.size()).isEqualTo(3);
        assertThat(
            eodRepository.findMediaIdsByInMediaIdStatusAndType(List.of(medias.get(0).getId()), storedStatus(), unstructuredLocation())
        )
            .hasSize(1);
    }

    @Test
    void testBatchedQueryWhereSomeFailedToPush() throws IOException {

        //given
        //batch size is 5
        List<MediaEntity> medias = dartsDatabase.getMediaStub().createAndSaveSomeMedias();

        var eod1 = externalObjectDirectoryStub.createAndSaveEod(medias.get(0), STORED, UNSTRUCTURED);
        externalObjectDirectoryStub.createAndSaveEod(medias.get(1), STORED, UNSTRUCTURED);
        var eod3 = externalObjectDirectoryStub.createAndSaveEod(medias.get(2), STORED, UNSTRUCTURED);
        externalObjectDirectoryStub.createAndSaveEod(medias.get(3), STORED, UNSTRUCTURED);
        externalObjectDirectoryStub.createAndSaveEod(medias.get(4), STORED, UNSTRUCTURED);
        externalObjectDirectoryStub.createAndSaveEod(medias.get(5), STORED, UNSTRUCTURED);


        DartsException dartsException = new DartsException("Exception copying file");
        String failedFilename1 = format("^%s.*", 7, eod1.getMedia().getId(), eod1.getTransferAttempts());
        doThrow(dartsException).when(armDataManagementApi).copyBlobDataToArm(any(), matches(failedFilename1));

        String failedFilename3 = format("^%s.*", 9, eod3.getMedia().getId(), eod3.getTransferAttempts());
        doThrow(dartsException).when(armDataManagementApi).copyBlobDataToArm(any(), matches(failedFilename3));

        //when
        unstructuredToArmProcessor.processUnstructuredToArm(5);

        //then
        List<Integer> foundMediaList = eodRepository.findMediaIdsByInMediaIdStatusAndType(
            List.of(medias.get(0).getId(),
                    medias.get(1).getId(),
                    medias.get(2).getId(),
                    medias.get(3).getId(),
                    medias.get(4).getId(),
                    medias.get(5).getId()),
            armDropZoneStatus(),
            armLocation()
        );
        assertThat(foundMediaList.size()).isEqualTo(3);
        assertThat(
            eodRepository.findMediaIdsByInMediaIdStatusAndType(List.of(medias.get(0).getId()), storedStatus(), unstructuredLocation())
        ).hasSize(1);


        List<Integer> failedMediaList = eodRepository.findMediaIdsByInMediaIdStatusAndType(
            List.of(medias.get(0).getId(),
                    medias.get(1).getId(),
                    medias.get(2).getId(),
                    medias.get(3).getId(),
                    medias.get(4).getId(),
                    medias.get(5).getId()),
            failedArmRawDataStatus(),
            armLocation()
        );
        assertThat(failedMediaList.size()).isEqualTo(2);

        ArgumentCaptor<String> manifestFileContentCaptor = ArgumentCaptor.forClass(String.class);
        verify(dataStoreToArmHelper).convertStringToBinaryData(manifestFileContentCaptor.capture());
        String manifestFileContents = manifestFileContentCaptor.getValue();

        int expectedNumberOfRows = 6;
        assertEquals(expectedNumberOfRows, manifestFileContents.lines().count());
        log.info("actual response {}", manifestFileContents);

        List<MediaEntity> successfulMedias = medias.stream()
            .filter(mediaEntity -> foundMediaList.contains(mediaEntity.getId())).toList();
        List<ExternalObjectDirectoryEntity> successEods = new ArrayList<>();

        for (MediaEntity mediaEntity : successfulMedias) {
            successEods.addAll(eodRepository.findByMediaAndExternalLocationType(mediaEntity, armLocation()));
        }

        List<MediaEntity> failedMedias = medias.stream()
            .filter(mediaEntity -> failedMediaList.contains(mediaEntity.getId())).toList();
        List<ExternalObjectDirectoryEntity> failedEods = new ArrayList<>();

        for (MediaEntity mediaEntity : failedMedias) {
            failedEods.addAll(eodRepository.findByMediaAndExternalLocationType(mediaEntity, armLocation()));
        }

        assertTrue(successEods.stream().allMatch(eodEntity -> manifestFileContents.contains(
            format("\"relation_id\":\"%d\"", eodEntity.getId())
        )));
        assertTrue(failedEods.stream().allMatch(eodEntity -> !manifestFileContents.contains(
            format("\"relation_id\":\"%d\"", eodEntity.getId())
        )));

    }

    @Test
    void movePendingMediaDataFromUnstructuredToArmStorage() throws IOException {

        //given
        List<MediaEntity> medias = dartsDatabase.getMediaStub().createAndSaveSomeMedias();
        externalObjectDirectoryStub.createAndSaveEod(medias.get(0), STORED, UNSTRUCTURED);
        externalObjectDirectoryStub.createAndSaveEod(medias.get(1), STORED, UNSTRUCTURED);

        //when
        unstructuredToArmProcessor.processUnstructuredToArm(5);

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

        verify(archiveRecordFileGenerator).generateArchiveRecords(manifestFileNameCaptor.capture(), any());


        ArgumentCaptor<String> manifestFileContentCaptor = ArgumentCaptor.forClass(String.class);
        verify(dataStoreToArmHelper).convertStringToBinaryData(manifestFileContentCaptor.capture());
        String manifestFileContent = manifestFileContentCaptor.getValue();

        assertThat(manifestFileContent.lines().count()).isEqualTo(4);
        assertThat(manifestFileContent)
            .contains("\"operation\":\"create_record\"",
                      "\"operation\":\"upload_new_file\"",
                      "\"dz_file_name\":\"" + rawFile0Name,
                      "\"dz_file_name\":\"" + rawFile1Name);

        String manifestFileName = manifestFileNameCaptor.getValue();
        assertThat(armDropZoneEodsMedia0.get(0).getManifestFile()).isEqualTo(manifestFileName);
        assertThat(armDropZoneEodsMedia0.get(0).getLastModifiedBy().getId()).isEqualTo(testUser.getId());
        assertThat(armDropZoneEodsMedia0.get(0).getLastModifiedDateTime()).isCloseToUtcNow(within(1, SECONDS));
        assertThat(armDropZoneEodsMedia1.get(0).getManifestFile()).isEqualTo(manifestFileName);
    }

    @Test
    void movePreviousArmFailedFromUnstructuredToArmStorage() throws IOException {

        //given
        when(unstructuredToArmProcessorConfiguration.getMaxArmManifestItems()).thenReturn(5);

        List<MediaEntity> medias = dartsDatabase.getMediaStub().createAndSaveSomeMedias();
        externalObjectDirectoryStub.createAndSaveEod(medias.get(0), STORED, UNSTRUCTURED);
        externalObjectDirectoryStub.createAndSaveEod(medias.get(0), ARM_RAW_DATA_FAILED, ARM, eod -> eod.setTransferAttempts(2));
        externalObjectDirectoryStub.createAndSaveEod(medias.get(1), STORED, UNSTRUCTURED);
        externalObjectDirectoryStub.createAndSaveEod(medias.get(1), ARM_MANIFEST_FAILED, ARM);
        externalObjectDirectoryStub.createAndSaveEod(medias.get(2), STORED, UNSTRUCTURED);
        externalObjectDirectoryStub.createAndSaveEod(medias.get(2), ARM_MANIFEST_FAILED, ARM, eod -> eod.setTransferAttempts(5));
        externalObjectDirectoryStub.createAndSaveEod(medias.get(3), STORED, UNSTRUCTURED);
        externalObjectDirectoryStub.createAndSaveEod(medias.get(3), ARM_RESPONSE_MANIFEST_FAILED, ARM, eod -> eod.setTransferAttempts(1));

        //when
        unstructuredToArmProcessor.processUnstructuredToArm(5);

        //then
        verify(armDataManagementApi, times(1)).copyBlobDataToArm(any(), matches(".+_.+_"));
        verify(armDataManagementApi, times(1)).saveBlobDataToArm(matches("DARTS_.+\\.a360"), any());
        verify(armDataManagementApi, times(1)).saveBlobDataToArm(any(), any());

        var armDropzoneEodsMedia0 = eodRepository.findByMediaStatusAndType(medias.get(0), armDropZoneStatus(), armLocation());
        assertThat(armDropzoneEodsMedia0).hasSize(1);
        var armDropzoneEodsMedia1 = eodRepository.findByMediaStatusAndType(medias.get(1), armDropZoneStatus(), armLocation());
        assertThat(armDropzoneEodsMedia1).hasSize(1);
        var armDropzoneEodsMedia3 = eodRepository.findByMediaStatusAndType(medias.get(3), armDropZoneStatus(), armLocation());
        assertThat(armDropzoneEodsMedia3).hasSize(0);


        verify(archiveRecordFileGenerator).generateArchiveRecords(manifestFileNameCaptor.capture(), any());
        String manifestFileName = manifestFileNameCaptor.getValue();
        assertThat(armDropzoneEodsMedia0.get(0).getManifestFile()).isEqualTo(manifestFileName);
        assertThat(armDropzoneEodsMedia0.get(0).getLastModifiedBy().getId()).isEqualTo(testUser.getId());
        assertThat(armDropzoneEodsMedia0.get(0).getLastModifiedDateTime()).isCloseToUtcNow(within(1, SECONDS));
        assertThat(armDropzoneEodsMedia1.get(0).getManifestFile()).isEqualTo(manifestFileName);

        ArgumentCaptor<String> manifestFileContentCaptor = ArgumentCaptor.forClass(String.class);
        verify(dataStoreToArmHelper).convertStringToBinaryData(manifestFileContentCaptor.capture());
        String manifestFileContent = manifestFileContentCaptor.getValue();
        assertThat(manifestFileContent.lines().count()).isEqualTo(4);
        assertThat(manifestFileContent).contains(
            format("_%d_", medias.get(0).getId()),
            format("_%d_", medias.get(1).getId())
        );
        assertThat(manifestFileContent).doesNotContain(format("_%d_", medias.get(2).getId()));
    }

    @Test
    void movePreviousArmFailedWithNoCorrespondingUnstructuredFailsAndProcessingContinues() throws IOException {

        //given
        List<MediaEntity> medias = dartsDatabase.getMediaStub().createAndSaveSomeMedias();
        externalObjectDirectoryStub.createAndSaveEod(medias.get(0), ARM_MANIFEST_FAILED, ARM, eod -> eod.setManifestFile("existingManifestFile"));
        externalObjectDirectoryStub.createAndSaveEod(medias.get(1), STORED, UNSTRUCTURED);
        externalObjectDirectoryStub.createAndSaveEod(medias.get(1), ARM_RAW_DATA_FAILED, ARM);

        //when
        unstructuredToArmProcessor.processUnstructuredToArm(5);

        //then
        List<ExternalObjectDirectoryEntity> failedArmEods = eodRepository.findByMediaStatusAndType(
            medias.get(0), failedArmManifestFileStatus(), armLocation());
        assertThat(failedArmEods).hasSize(1);
        assertThat(failedArmEods.get(0).getManifestFile()).isEqualTo("existingManifestFile");
        assertThat(failedArmEods.get(0).getTransferAttempts()).isEqualTo(2);
        assertThat(eodRepository.findByMediaStatusAndType(medias.get(1), armDropZoneStatus(), armLocation())).hasSize(1);

        verify(archiveRecordFileGenerator).generateArchiveRecords(manifestFileNameCaptor.capture(), any());

        ArgumentCaptor<String> manifestFileContentCaptor = ArgumentCaptor.forClass(String.class);
        verify(dataStoreToArmHelper).convertStringToBinaryData(manifestFileContentCaptor.capture());
        String manifestFileContent = manifestFileContentCaptor.getValue();

        assertThat(manifestFileContent.lines().count()).isEqualTo(2);
        assertThat(manifestFileContent).contains(format("_%d_", medias.get(1).getId()));
        assertThat(manifestFileContent).doesNotContain(format("_%d_", medias.get(0).getId()));
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

        doThrow(RuntimeException.class).when(armDataManagementApi).copyBlobDataToArm(any(), matches(".+_.+_3"));

        //when
        unstructuredToArmProcessor.processUnstructuredToArm(5);

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
        testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);
        when(unstructuredToArmProcessorConfiguration.getMaxArmManifestItems()).thenReturn(5);

        //given
        List<MediaEntity> medias = dartsDatabase.getMediaStub().createAndSaveSomeMedias();
        externalObjectDirectoryStub.createAndSaveEod(medias.get(0), STORED, UNSTRUCTURED);
        externalObjectDirectoryStub.createAndSaveEod(medias.get(0), ARM_RAW_DATA_FAILED, ARM, eod -> eod.setManifestFile("existingManifestFile"));
        externalObjectDirectoryStub.createAndSaveEod(medias.get(1), STORED, UNSTRUCTURED);
        externalObjectDirectoryStub.createAndSaveEod(medias.get(1), ARM_MANIFEST_FAILED, ARM);

        doReturn(null).when(mediaArchiveRecordMapper).mapToMediaArchiveRecord(any(), any());

        //when
        unstructuredToArmProcessor.processUnstructuredToArm(5);

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

        doThrow(RuntimeException.class).when(archiveRecordFileGenerator).generateArchiveRecords(anyString(), any());

        //when
        unstructuredToArmProcessor.processUnstructuredToArm(5);

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
        unstructuredToArmProcessor.processUnstructuredToArm(5);

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