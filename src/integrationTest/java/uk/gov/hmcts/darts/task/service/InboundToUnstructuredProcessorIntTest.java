package uk.gov.hmcts.darts.task.service;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.models.BlobStorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.datamanagement.service.DataManagementService;
import uk.gov.hmcts.darts.datamanagement.service.InboundToUnstructuredProcessor;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.data.MediaTestData;
import uk.gov.hmcts.darts.testutils.stubs.ExternalObjectDirectoryStub;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.INBOUND;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.UNSTRUCTURED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.FAILURE;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.FAILURE_CHECKSUM_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.FAILURE_EMPTY_FILE;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.FAILURE_FILE_NOT_FOUND;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.FAILURE_FILE_TYPE_CHECK_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;
import static uk.gov.hmcts.darts.testutils.stubs.TranscriptionStub.getBinaryTranscriptionDocumentData;
import static uk.gov.hmcts.darts.testutils.stubs.TranscriptionStub.getEmptyFile;

@Disabled
class InboundToUnstructuredProcessorIntTest extends IntegrationBase {

    private ExternalObjectDirectoryStub externalObjectDirectoryStub;

    @SpyBean
    ExternalObjectDirectoryRepository eodRepository;
    @MockBean
    DataManagementService dataManagementService;

    @Autowired
    private InboundToUnstructuredProcessor inboundToUnstructuredProcessor;

    @BeforeEach
    public void setup() {
        externalObjectDirectoryStub = dartsDatabase.getExternalObjectDirectoryStub();
        when(dataManagementService.saveBlobData(any(), any(BinaryData.class))).thenReturn(UUID.randomUUID());
    }

    @Test
    void processInboundMediasToUnstructured() {
        // given
        List<MediaEntity> medias = dartsDatabase.getMediaStub().createAndSaveSomeMedias();

        //matches because no corresponding unstructured
        var media1 = medias.get(0);
        externalObjectDirectoryStub.createAndSaveEod(media1, STORED, INBOUND);

        //matches because unstructured failed with no max attempts reached
        var media2 = medias.get(1);
        externalObjectDirectoryStub.createAndSaveEod(media2, STORED, INBOUND);
        externalObjectDirectoryStub.createAndSaveEod(media2, FAILURE, UNSTRUCTURED);

        //does not match because corresponding unstructured is stored
        var media3 = medias.get(2);
        externalObjectDirectoryStub.createAndSaveEod(media3, STORED, INBOUND);
        externalObjectDirectoryStub.createAndSaveEod(media3, STORED, UNSTRUCTURED);

        //does not match because unstructured failed with max attempts reached
        var media4 = medias.get(3);
        externalObjectDirectoryStub.createAndSaveEod(media4, STORED, INBOUND);
        var failed = externalObjectDirectoryStub.createAndSaveEod(media4, FAILURE, UNSTRUCTURED);
        failed.setTransferAttempts(10);
        eodRepository.save(failed);

        when(dataManagementService.getBlobData(any(), any())).thenReturn(MediaTestData.getBinaryData());

        // when
        inboundToUnstructuredProcessor.processInboundToUnstructured();

        // then
        assertThat(externalObjectDirectoryStub.findByMediaStatusAndType(media1, STORED, UNSTRUCTURED)).hasSize(1);
        assertThat(externalObjectDirectoryStub.findByMediaStatusAndType(media2, STORED, UNSTRUCTURED)).hasSize(1);
        var argument = ArgumentCaptor.forClass(ExternalObjectDirectoryEntity.class);
        verify(eodRepository, atLeastOnce()).saveAndFlush(argument.capture());
        List<ExternalObjectDirectoryEntity> createdUnstructured = argument.getAllValues();
        List<Integer> createdUnstructuredMediaIds = createdUnstructured.stream().map(eod -> eod.getMedia().getId()).collect(toList());
        assertThat(createdUnstructuredMediaIds).contains(media1.getId(), media2.getId());
        assertThat(createdUnstructuredMediaIds).doesNotContain(media3.getId());
        assertThat(createdUnstructuredMediaIds).doesNotContain(media4.getId());
    }

    @Test
    void processInboundTranscriptionDocumentToUnstructured() {
        // given
        var transcription = dartsDatabase.getTranscriptionStub().createMinimalTranscription();
        dartsDatabase.getTranscriptionStub().updateTranscriptionWithDocument(transcription, STORED, INBOUND);

        when(dataManagementService.getBlobData(any(), any())).thenReturn(getBinaryTranscriptionDocumentData());
        when(dataManagementService.saveBlobData(any(), any(BinaryData.class))).thenReturn(UUID.randomUUID());

        var existingUnstructuredStored = eodRepository.findByStatusAndType(dartsDatabase.getObjectRecordStatusEntity(STORED),
                                                                           dartsDatabase.getExternalLocationTypeEntity(UNSTRUCTURED));
        assertThat(existingUnstructuredStored).isEmpty();

        when(dataManagementService.getBlobData(any(), any())).thenReturn(getBinaryTranscriptionDocumentData());

        // when
        inboundToUnstructuredProcessor.processInboundToUnstructured();

        // then
        var createdUnstructuredStored = eodRepository.findByStatusAndType(dartsDatabase.getObjectRecordStatusEntity(STORED),
                                                                          dartsDatabase.getExternalLocationTypeEntity(UNSTRUCTURED));
        assertThat(createdUnstructuredStored).hasSize(1);
        var createdUnstructuredCaptor = ArgumentCaptor.forClass(ExternalObjectDirectoryEntity.class);
        verify(eodRepository, atLeastOnce()).saveAndFlush(createdUnstructuredCaptor.capture());
        List<ExternalObjectDirectoryEntity> createdUnstructured = createdUnstructuredCaptor.getAllValues();
        List<Integer> createdUnstructuredTranscriptionId = createdUnstructured.stream().map(
            eod -> eod.getTranscriptionDocumentEntity().getTranscription().getId()).collect(toList());
        assertThat(createdUnstructuredTranscriptionId).contains(transcription.getId());
        verify(dataManagementService).saveBlobData(any(), any(BinaryData.class));
    }

    @Test
    void skipsProcessInboundTranscriptionDocumentToUnstructuredWhenAlreadyStoredInUnstructured() {
        // given
        var transcription = dartsDatabase.getTranscriptionStub().createMinimalTranscription();

        dartsDatabase.getTranscriptionStub().updateTranscriptionWithDocument(transcription, STORED, INBOUND);

        dartsDatabase.getExternalObjectDirectoryStub().createAndSaveExternalObjectDirectory(
            transcription.getTranscriptionDocumentEntities().get(0).getId(),
            dartsDatabase.getObjectRecordStatusEntity(STORED),
            dartsDatabase.getExternalLocationTypeEntity(UNSTRUCTURED),
            UUID.randomUUID());

        var unstructuredBeforeProcessing = eodRepository.findByStatusAndType(dartsDatabase.getObjectRecordStatusEntity(STORED),
                                                                                   dartsDatabase.getExternalLocationTypeEntity(UNSTRUCTURED));
        assertThat(unstructuredBeforeProcessing).hasSize(1);

        when(dataManagementService.getBlobData(any(), any())).thenReturn(getBinaryTranscriptionDocumentData());

        // when
        inboundToUnstructuredProcessor.processInboundToUnstructured();

        // then
        var unstructuredAfterProcessing = eodRepository.findByStatusAndType(dartsDatabase.getObjectRecordStatusEntity(STORED),
                                                                                  dartsDatabase.getExternalLocationTypeEntity(UNSTRUCTURED));
        assertThat(unstructuredAfterProcessing).hasSize(1);
        assertThat(unstructuredAfterProcessing.get(0).getId()).isEqualTo(unstructuredBeforeProcessing.get(0).getId());
        verify(dataManagementService, never()).saveBlobData(any(), any(BinaryData.class));
    }

    @Test
    void processInboundTranscriptionDocumentToUnstructuredWhenFailedInUnstructured() {
        // given
        var transcription = dartsDatabase.getTranscriptionStub().createMinimalTranscription();

        dartsDatabase.getTranscriptionStub().updateTranscriptionWithDocument(transcription, STORED, INBOUND);

        dartsDatabase.getExternalObjectDirectoryStub().createAndSaveExternalObjectDirectory(
            transcription.getTranscriptionDocumentEntities().get(0).getId(),
            dartsDatabase.getObjectRecordStatusEntity(FAILURE),
            dartsDatabase.getExternalLocationTypeEntity(UNSTRUCTURED),
            UUID.randomUUID());

        var unstructuredBeforeProcessing = eodRepository.findByStatusAndType(dartsDatabase.getObjectRecordStatusEntity(FAILURE),
                                                                             dartsDatabase.getExternalLocationTypeEntity(UNSTRUCTURED));
        assertThat(unstructuredBeforeProcessing).hasSize(1);

        when(dataManagementService.getBlobData(any(), any())).thenReturn(getBinaryTranscriptionDocumentData());

        // when
        inboundToUnstructuredProcessor.processInboundToUnstructured();

        // then
        var unstructuredAfterProcessing = eodRepository.findByStatusAndType(dartsDatabase.getObjectRecordStatusEntity(STORED),
                                                                            dartsDatabase.getExternalLocationTypeEntity(UNSTRUCTURED));
        assertThat(unstructuredAfterProcessing).hasSize(1);
        assertThat(unstructuredAfterProcessing.get(0).getId()).isEqualTo(unstructuredBeforeProcessing.get(0).getId());
        verify(dataManagementService).saveBlobData(any(), any(BinaryData.class));
    }

    @Test
    void processInboundMediasToUnstructuredBlobException() {
        // given
        List<MediaEntity> medias = dartsDatabase.getMediaStub().createAndSaveSomeMedias();

        //matches because no corresponding unstructured
        var media1 = medias.get(0);
        externalObjectDirectoryStub.createAndSaveEod(media1, STORED, INBOUND);

        when(dataManagementService.getBlobData(any(), any())).thenThrow(new BlobStorageException("No blob", null, null));

        // when
        inboundToUnstructuredProcessor.processInboundToUnstructured();

        // then
        assertThat(externalObjectDirectoryStub.findByMediaStatusAndType(media1, FAILURE_FILE_NOT_FOUND, UNSTRUCTURED)).hasSize(1);
        var argument = ArgumentCaptor.forClass(ExternalObjectDirectoryEntity.class);
        verify(eodRepository, atLeastOnce()).saveAndFlush(argument.capture());
    }

    @Test
    void processInboundMediasFailedType() {
        // given
        MediaEntity media = dartsDatabase.getMediaStub().createMediaEntity("testCourthouse", "testCourtroom",
           OffsetDateTime.parse("2023-01-01T12:00:00Z"), OffsetDateTime.parse("2023-01-01T12:00:00Z"), 1, "some.other");

        externalObjectDirectoryStub.createAndSaveEod(media, STORED, INBOUND);

        when(dataManagementService.getBlobData(any(), any())).thenReturn(MediaTestData.getBinaryData());

        // when
        inboundToUnstructuredProcessor.processInboundToUnstructured();

        // then
        assertThat(externalObjectDirectoryStub.findByMediaStatusAndType(media, FAILURE_FILE_TYPE_CHECK_FAILED, UNSTRUCTURED)).hasSize(1);
        var argument = ArgumentCaptor.forClass(ExternalObjectDirectoryEntity.class);
        verify(eodRepository, atLeastOnce()).saveAndFlush(argument.capture());
    }

    @Test
    void processInboundMediasFailedChecksum() {
        // given
        MediaEntity media = dartsDatabase.getMediaStub().createMediaEntity("testCourthouse", "testCourtroom",
           OffsetDateTime.parse("2023-01-01T12:00:00Z"), OffsetDateTime.parse("2023-01-01T12:00:00Z"), 1);
        externalObjectDirectoryStub.createAndSaveEod(media, STORED, INBOUND);

        //generates a different checksum
        when(dataManagementService.getBlobData(any(), any())).thenReturn(getBinaryTranscriptionDocumentData());

        // when
        inboundToUnstructuredProcessor.processInboundToUnstructured();

        // then
        assertThat(externalObjectDirectoryStub.findByMediaStatusAndType(media, FAILURE_CHECKSUM_FAILED, UNSTRUCTURED)).hasSize(1);
        var argument = ArgumentCaptor.forClass(ExternalObjectDirectoryEntity.class);
        verify(eodRepository, atLeastOnce()).saveAndFlush(argument.capture());
    }

    @Test
    void processInboundMediasFailedAsEmptyFile() {
        // given
        MediaEntity media = dartsDatabase.getMediaStub().createMediaEntity("testCourthouse", "testCourtroom",
               OffsetDateTime.parse("2023-01-01T12:00:00Z"), OffsetDateTime.parse("2023-01-01T12:00:00Z"), 1);
        media.setChecksum("1B2M2Y8AsgTpgAmY7PhCfg==");
        media.setFileSize(0L);
        dartsDatabase.save(media);
        externalObjectDirectoryStub.createAndSaveEod(media, STORED, INBOUND);

        when(dataManagementService.getBlobData(any(), any())).thenReturn(getEmptyFile());

        // when
        inboundToUnstructuredProcessor.processInboundToUnstructured();

        // then
        assertThat(externalObjectDirectoryStub.findByMediaStatusAndType(media, FAILURE_EMPTY_FILE, UNSTRUCTURED)).hasSize(1);
        var argument = ArgumentCaptor.forClass(ExternalObjectDirectoryEntity.class);
        verify(eodRepository, atLeastOnce()).saveAndFlush(argument.capture());
    }
}
