package uk.gov.hmcts.darts.task.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionDocumentRepository;
import uk.gov.hmcts.darts.datamanagement.service.InboundToUnstructuredProcessor;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.DataManagementServiceStubImpl;
import uk.gov.hmcts.darts.testutils.stubs.ExternalObjectDirectoryStub;

import java.util.List;
import java.util.UUID;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.INBOUND;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.UNSTRUCTURED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.FAILURE;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;
import static uk.gov.hmcts.darts.common.util.EodHelper.failureStatus;
import static uk.gov.hmcts.darts.common.util.EodHelper.storedStatus;
import static uk.gov.hmcts.darts.common.util.EodHelper.unstructuredLocation;

class InboundToUnstructuredProcessorIntTest extends IntegrationBase {

    private ExternalObjectDirectoryStub externalObjectDirectoryStub;

    @SpyBean
    ExternalObjectDirectoryRepository eodRepository;

    @Autowired
    private InboundToUnstructuredProcessor inboundToUnstructuredProcessor;
    @Autowired
    private TranscriptionDocumentRepository transcriptionDocumentRepository;

    @BeforeEach
    public void setup() {
        externalObjectDirectoryStub = dartsDatabase.getExternalObjectDirectoryStub();
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
        externalObjectDirectoryStub.createAndSaveEod(media4, FAILURE, UNSTRUCTURED, eod -> eod.setTransferAttempts(10));

        // when
        inboundToUnstructuredProcessor.processInboundToUnstructured(100);

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
        dartsDatabase.getTranscriptionStub().updateTranscriptionWithDocument(transcription, STORED, INBOUND, UUID.randomUUID());

        var existingUnstructuredStored = eodRepository.findByStatusAndType(storedStatus(), unstructuredLocation());
        assertThat(existingUnstructuredStored).isEmpty();

        // when
        inboundToUnstructuredProcessor.processInboundToUnstructured(100);

        // then
        var createdUnstructuredStored = eodRepository.findByStatusAndType(storedStatus(), unstructuredLocation());
        assertThat(createdUnstructuredStored).hasSize(1);
        ExternalObjectDirectoryEntity createdUnstructured = createdUnstructuredStored.get(0);
        TranscriptionDocumentEntity createdUnstructuredStoredTranscriptionDocument = transcriptionDocumentRepository.findById(
            createdUnstructured.getTranscriptionDocumentEntity().getId()).get();
        assertThat(createdUnstructuredStoredTranscriptionDocument.getTranscription().getId()).isEqualTo(transcription.getId());
    }

    @Test
    void skipsProcessInboundToUnstructuredWhenAlreadyStoredInUnstructured() {
        // given
        var transcription = dartsDatabase.getTranscriptionStub().createMinimalTranscription();

        dartsDatabase.getTranscriptionStub().updateTranscriptionWithDocument(transcription, STORED, INBOUND, UUID.randomUUID());

        dartsDatabase.getExternalObjectDirectoryStub().createAndSaveExternalObjectDirectory(
            transcription.getTranscriptionDocumentEntities().get(0).getId(),
            dartsDatabase.getObjectRecordStatusEntity(STORED),
            dartsDatabase.getExternalLocationTypeEntity(UNSTRUCTURED)
        );

        var unstructuredBeforeProcessing = eodRepository.findByStatusAndType(storedStatus(), unstructuredLocation());
        assertThat(unstructuredBeforeProcessing).hasSize(1);

        // when
        inboundToUnstructuredProcessor.processInboundToUnstructured(100);

        // then
        var unstructuredAfterProcessing = eodRepository.findByStatusAndType(storedStatus(), unstructuredLocation());
        assertThat(unstructuredAfterProcessing).hasSize(1);
        assertThat(unstructuredAfterProcessing.get(0).getId()).isEqualTo(unstructuredBeforeProcessing.get(0).getId());
    }

    @Test
    void processAgainInboundToUnstructuredWhenFailedInUnstructured() {
        // given
        var transcription = dartsDatabase.getTranscriptionStub().createMinimalTranscription();

        dartsDatabase.getTranscriptionStub().updateTranscriptionWithDocument(transcription, STORED, INBOUND, UUID.randomUUID());

        dartsDatabase.getExternalObjectDirectoryStub().createAndSaveExternalObjectDirectory(
            transcription.getTranscriptionDocumentEntities().get(0).getId(),
            dartsDatabase.getObjectRecordStatusEntity(FAILURE),
            dartsDatabase.getExternalLocationTypeEntity(UNSTRUCTURED)
        );

        var unstructuredBeforeProcessing = eodRepository.findByStatusAndType(failureStatus(), unstructuredLocation());
        assertThat(unstructuredBeforeProcessing).hasSize(1);

        // when
        inboundToUnstructuredProcessor.processInboundToUnstructured(100);

        // then
        var unstructuredAfterProcessing = eodRepository.findByStatusAndType(storedStatus(), unstructuredLocation());
        assertThat(unstructuredAfterProcessing).hasSize(1);
        assertThat(unstructuredAfterProcessing.get(0).getId()).isEqualTo(unstructuredBeforeProcessing.get(0).getId());
    }

    @Test
    void processInboundObjectSetsUnstructuredToFailureStatusOnCopyFailure() {
        // given
        var transcription = dartsDatabase.getTranscriptionStub().createMinimalTranscription();
        dartsDatabase.getTranscriptionStub().updateTranscriptionWithDocument(transcription, STORED, INBOUND, DataManagementServiceStubImpl.FAILURE_UUID);

        var existingUnstructuredStored = eodRepository.findByStatusAndType(storedStatus(), unstructuredLocation());
        assertThat(existingUnstructuredStored).isEmpty();

        // when
        inboundToUnstructuredProcessor.processInboundToUnstructured(100);

        // then
        var createdUnstructuredFailed = eodRepository.findByStatusAndType(failureStatus(), unstructuredLocation());
        assertThat(createdUnstructuredFailed).hasSize(1);
        assertThat(createdUnstructuredFailed.get(0).getTransferAttempts()).isEqualTo(1);
    }
}
