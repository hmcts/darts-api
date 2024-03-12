package uk.gov.hmcts.darts.transcriptions.service.impl;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.InputStreamResource;
import uk.gov.hmcts.darts.audit.api.AuditApi;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.datamanagement.api.DataManagementFacade;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadResponseMetaData;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.TranscriptionRepository;
import uk.gov.hmcts.darts.datamanagement.exception.FileNotDownloadedException;

import java.io.IOException;
import java.nio.charset.Charset;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import static java.time.OffsetDateTime.now;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.rangeClosed;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.INBOUND;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.UNSTRUCTURED;
import static uk.gov.hmcts.darts.transcriptions.exception.TranscriptionApiError.FAILED_TO_DOWNLOAD_TRANSCRIPT;

@ExtendWith(MockitoExtension.class)
class TranscriptionDownloaderTest {

    @Mock
    private UserIdentity userIdentity;
    @Mock
    private TranscriptionRepository transcriptionRepository;
    @Mock
    private DataManagementFacade dataManagementFacade;
    @Mock
    private AuditApi auditApi;

    @Mock
    private DownloadResponseMetaData fileBasedDownloadResponseMetaData;

    private final Random random = new Random();
    private TranscriptionDownloader transcriptionDownloader;

    @BeforeEach
    void setUp() {
        transcriptionDownloader = new TranscriptionDownloader(transcriptionRepository, dataManagementFacade, auditApi, userIdentity);

        var testUser = new UserAccountEntity();
        testUser.setEmailAddress("test.user@example.com");
        when(userIdentity.getUserAccount()).thenReturn(testUser);
    }

    @Test
    void throwsExceptionIfNoTranscriptionFound() {
        when(transcriptionRepository.findById(any())).thenReturn(empty());

        assertThatThrownBy(() -> transcriptionDownloader.downloadTranscript(random.nextInt()))
            .isExactlyInstanceOf(DartsApiException.class)
            .hasFieldOrPropertyWithValue("error", FAILED_TO_DOWNLOAD_TRANSCRIPT);

        verifyNoInteractions(dataManagementFacade);
    }

    @Test
    void throwsExceptionIfTranscriptionHasNoTranscriptionDocuments() {
        var transcription = someTranscriptionWith(emptyList());
        when(transcriptionRepository.findById(transcription.getId())).thenReturn(Optional.of(transcription));

        assertThatThrownBy(() -> transcriptionDownloader.downloadTranscript(transcription.getId()))
            .isExactlyInstanceOf(DartsApiException.class)
            .hasFieldOrPropertyWithValue("error", FAILED_TO_DOWNLOAD_TRANSCRIPT);

        verifyNoInteractions(dataManagementFacade);
    }

    @Test
    void throwsExceptionIfTranscriptionDocumentHasNoExternalObjectDirectories() throws IOException, FileNotDownloadedException {
        var transcriptionDocument = someTranscriptionDocumentWithUploadDate(now());
        transcriptionDocument.setExternalObjectDirectoryEntities(emptyList());

        var transcription = someTranscriptionWith(List.of(transcriptionDocument));
        when(transcriptionRepository.findById(transcription.getId())).thenReturn(Optional.of(transcription));

        when(dataManagementFacade.retrieveFileFromStorage(any(TranscriptionDocumentEntity.class))).thenThrow(new FileNotDownloadedException());

        assertThatThrownBy(() -> transcriptionDownloader.downloadTranscript(transcription.getId()))
            .isExactlyInstanceOf(DartsApiException.class)
            .hasFieldOrPropertyWithValue("error", FAILED_TO_DOWNLOAD_TRANSCRIPT);

        verify(dataManagementFacade).retrieveFileFromStorage(any(TranscriptionDocumentEntity.class));
        verifyNoMoreInteractions(dataManagementFacade, fileBasedDownloadResponseMetaData);
    }


    @Test
    void throwsExceptionIfFailsToGetDownloadResponseInputStream() throws IOException, FileNotDownloadedException {
        var transcriptionDocument = someTranscriptionDocumentWithUploadDate(now());
        transcriptionDocument.setExternalObjectDirectoryEntities(List.of(someExternalObjectDirectoryWithCreationDate(now())));

        var transcription = someTranscriptionWith(List.of(transcriptionDocument));
        when(transcriptionRepository.findById(transcription.getId())).thenReturn(Optional.of(transcription));

        when(fileBasedDownloadResponseMetaData.getInputStream()).thenThrow(new IOException());
        when(dataManagementFacade.retrieveFileFromStorage(any(TranscriptionDocumentEntity.class))).thenReturn(fileBasedDownloadResponseMetaData);

        assertThatThrownBy(() -> transcriptionDownloader.downloadTranscript(transcription.getId()))
            .isExactlyInstanceOf(DartsApiException.class)
            .hasFieldOrPropertyWithValue("error", FAILED_TO_DOWNLOAD_TRANSCRIPT);

        verify(dataManagementFacade).retrieveFileFromStorage(any(TranscriptionDocumentEntity.class));
        verify(fileBasedDownloadResponseMetaData).close();
        verifyNoMoreInteractions(dataManagementFacade, fileBasedDownloadResponseMetaData);
    }

    @Test
    void retrievesTranscriptionFromUnstructuredContainer() throws IOException, FileNotDownloadedException {
        // Given
        var inboundExternalObjectDirectoryCreatedToday = someExternalObjectDirectoryWithCreationDate(now());
        inboundExternalObjectDirectoryCreatedToday.setExternalLocationType(externalLocationTypeFor(INBOUND));
        var externalObjectDirectoryCreatedToday = someExternalObjectDirectoryWithCreationDate(now());
        externalObjectDirectoryCreatedToday.setExternalLocationType(externalLocationTypeFor(UNSTRUCTURED));

        var transcriptionDocuments = someTranscriptionDocumentsUploadedAtLeast2DaysAgo(3);
        var transcriptionDocumentUploadedToday = someTranscriptionDocumentWithUploadDate(now());
        transcriptionDocumentUploadedToday.setExternalObjectDirectoryEntities(
            List.of(inboundExternalObjectDirectoryCreatedToday, externalObjectDirectoryCreatedToday));
        transcriptionDocuments.add(transcriptionDocumentUploadedToday);

        var transcription = someTranscriptionWith(transcriptionDocuments);
        when(transcriptionRepository.findById(transcription.getId())).thenReturn(Optional.of(transcription));
        when(dataManagementFacade.retrieveFileFromStorage(any(TranscriptionDocumentEntity.class))).thenReturn(fileBasedDownloadResponseMetaData);
        when(fileBasedDownloadResponseMetaData.getInputStream()).thenReturn(IOUtils.toInputStream("test-transcription", Charset.defaultCharset()));

        // When
        var downloadTranscriptResponse = transcriptionDownloader.downloadTranscript(transcription.getId());

        // Then
        assertThat(downloadTranscriptResponse.getTranscriptionDocumentId()).isEqualTo(transcriptionDocumentUploadedToday.getId());
        assertThat(downloadTranscriptResponse.getFileName()).isEqualTo(transcriptionDocumentUploadedToday.getFileName());
        assertThat(downloadTranscriptResponse.getContentType()).isEqualTo(transcriptionDocumentUploadedToday.getFileType());
        assertThat(downloadTranscriptResponse.getResource()).isInstanceOf(InputStreamResource.class);

        verify(fileBasedDownloadResponseMetaData).getInputStream();
        verify(fileBasedDownloadResponseMetaData).close();
        verifyNoMoreInteractions(dataManagementFacade, fileBasedDownloadResponseMetaData);

    }

    private ExternalLocationTypeEntity externalLocationTypeFor(ExternalLocationTypeEnum externalLocationTypeEnum) {
        var externalLocationType = new ExternalLocationTypeEntity();
        externalLocationType.setId(externalLocationTypeEnum.getId());
        return externalLocationType;
    }

    private List<TranscriptionDocumentEntity> someTranscriptionDocumentsUploadedAtLeast2DaysAgo(int quantity) {
        return rangeClosed(1, quantity).boxed()
            .map(i -> {
                var uploadedDateTime = now().minusDays(2).minusDays(i);
                var transcriptionDocument = someTranscriptionDocumentWithUploadDate(uploadedDateTime);
                transcriptionDocument.setExternalObjectDirectoryEntities(someExternalObjectDirectoriesCreatedAtLeast2DaysAgo(i));
                return transcriptionDocument;
            }).collect(toList());
    }

    private TranscriptionDocumentEntity someTranscriptionDocumentWithUploadDate(OffsetDateTime uploadedDateTime) {
        var transcriptionDocument = new TranscriptionDocumentEntity();
        transcriptionDocument.setId(random.nextInt());
        transcriptionDocument.setUploadedDateTime(uploadedDateTime);
        transcriptionDocument.setFileName("some-file-name-" + random.nextInt());
        transcriptionDocument.setFileType("some-file-type-" + random.nextInt());
        return transcriptionDocument;
    }

    private List<ExternalObjectDirectoryEntity> someExternalObjectDirectoriesCreatedAtLeast2DaysAgo(int quantity) {
        return rangeClosed(1, quantity).boxed()
            .map(i -> someExternalObjectDirectoryWithCreationDate(now().minusDays(2).minusDays(i)))
            .collect(toList());
    }

    private ExternalObjectDirectoryEntity someExternalObjectDirectoryWithCreationDate(OffsetDateTime createdDateTime) {
        var externalObjectDirectory = new ExternalObjectDirectoryEntity();
        externalObjectDirectory.setId(random.nextInt());
        externalObjectDirectory.setCreatedDateTime(createdDateTime);
        externalObjectDirectory.setExternalLocation(randomUUID());
        externalObjectDirectory.setExternalLocationType(someExternalLocation());
        return externalObjectDirectory;
    }

    private ExternalLocationTypeEntity someExternalLocation() {
        var externalLocation = new ExternalLocationTypeEntity();
        externalLocation.setId(random.nextInt(INBOUND.getId(), UNSTRUCTURED.getId()));
        return externalLocation;
    }

    private TranscriptionEntity someTranscriptionWith(List<TranscriptionDocumentEntity> transcriptionDocuments) {
        var transcriptionEntity = new TranscriptionEntity();
        transcriptionEntity.setId(random.nextInt());
        transcriptionEntity.setTranscriptionDocumentEntities(transcriptionDocuments);
        return transcriptionEntity;
    }
}
