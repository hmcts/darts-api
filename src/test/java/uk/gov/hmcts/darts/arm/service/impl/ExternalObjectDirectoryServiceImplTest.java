package uk.gov.hmcts.darts.arm.service.impl;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.CaseDocumentEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.AnnotationDocumentRepository;
import uk.gov.hmcts.darts.common.repository.CaseDocumentRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.MediaRepository;
import uk.gov.hmcts.darts.common.repository.TranscriptionDocumentRepository;
import uk.gov.hmcts.darts.common.service.impl.EodHelperMocks;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.util.EodHelper.armLocation;
import static uk.gov.hmcts.darts.common.util.EodHelper.awaitingVerificationStatus;
import static uk.gov.hmcts.darts.common.util.EodHelper.failedArmStatuses;
import static uk.gov.hmcts.darts.common.util.EodHelper.inboundLocation;
import static uk.gov.hmcts.darts.common.util.EodHelper.storedStatus;
import static uk.gov.hmcts.darts.common.util.EodHelper.unstructuredLocation;

@ExtendWith(MockitoExtension.class)
class ExternalObjectDirectoryServiceImplTest {

    @Mock(lenient = true)
    ExternalObjectDirectoryRepository eodRepository;
    @Mock
    ArmDataManagementConfiguration armConfig;
    @Mock
    ExternalObjectDirectoryEntity eod;
    @Mock
    MediaEntity media1;
    @Mock
    MediaEntity media2;
    @Mock
    private AnnotationDocumentEntity annotationDocument;
    @Mock
    private TranscriptionDocumentEntity transcriptionDocument;
    @Mock
    UserAccountEntity userAccountEntity;
    @Mock
    CaseDocumentEntity caseDocumentEntity;
    @Mock
    private MediaRepository mediaRepository;
    @Mock
    private CaseDocumentRepository caseDocumentRepository;
    @Mock
    private TranscriptionDocumentRepository transcriptionDocumentRepository;
    @Mock
    private AnnotationDocumentRepository annotationDocumentRepository;

    @Captor
    ArgumentCaptor<ExternalObjectDirectoryEntity> eodCaptor;

    private static final EodHelperMocks EOD_HELPER_MOCKS = new EodHelperMocks();

    ExternalObjectDirectoryServiceImpl eodService;

    @AfterAll
    public static void close() {
        EOD_HELPER_MOCKS.close();
    }

    @BeforeEach
    void setup() {
        eodService = new ExternalObjectDirectoryServiceImpl(eodRepository,
                                                            armConfig,
                                                            mediaRepository,
                                                            annotationDocumentRepository,
                                                            caseDocumentRepository,
                                                            transcriptionDocumentRepository);
    }

    @Test
    void testFindFailedStillRetriableArmEodsInvokesRepositoryCorrectly() {
        var eods = List.of(eod);
        when(eodRepository.findNotFinishedAndNotExceededRetryInStorageLocation(any(), any(), any(), any())).thenReturn(eods);

        var pageable = Pageable.ofSize(3);
        var result = eodService.findFailedStillRetriableArmEods(pageable);

        assertThat(result).isEqualTo(eods);
        verify(eodRepository).findNotFinishedAndNotExceededRetryInStorageLocation(
            failedArmStatuses(),
            armLocation(),
            armConfig.getMaxRetryAttempts(),
            pageable);
    }

    @ParameterizedTest
    @CsvSource({
        "false,false,true",
        "false,true,false",
        "true,false,false",
        "true,true,false",
    })
    void testHasAllMediaBeenCopied(boolean isMedia1NotCopied, boolean isMedia2NotCopied, boolean expectedResult) {
        var medias = List.of(media1, media2);
        when(eodRepository.hasMediaNotBeenCopiedFromInboundStorage(eq(media1), any(), any(), any(), any())).thenReturn(isMedia1NotCopied);
        when(eodRepository.hasMediaNotBeenCopiedFromInboundStorage(eq(media2), any(), any(), any(), any())).thenReturn(isMedia2NotCopied);

        var result = eodService.hasAllMediaBeenCopiedFromInboundStorage(medias);

        assertThat(result).isEqualTo(expectedResult);
        verify(eodRepository).hasMediaNotBeenCopiedFromInboundStorage(
            media1,
            storedStatus(),
            inboundLocation(),
            awaitingVerificationStatus(),
            List.of(unstructuredLocation(), armLocation())
        );
    }

    @Test
    void testCreateAndSaveExternalObjectDirectory() {
        String checksum = "checksum";
        UUID externalLocation = UUID.randomUUID();
        when(caseDocumentEntity.getChecksum()).thenReturn(checksum);

        eodService.createAndSaveCaseDocumentEod(
            externalLocation,
            userAccountEntity,
            caseDocumentEntity,
            unstructuredLocation()
        );

        verify(eodRepository).save(eodCaptor.capture());
        ExternalObjectDirectoryEntity savedEod = eodCaptor.getValue();
        assertThat(savedEod.getCaseDocument()).isEqualTo(caseDocumentEntity);
        assertThat(savedEod.getMedia()).isNull();
        assertThat(savedEod.getTranscriptionDocumentEntity()).isNull();
        assertThat(savedEod.getAnnotationDocumentEntity()).isNull();
        assertThat(savedEod.getChecksum()).isEqualTo(checksum);
        assertThat(savedEod.getExternalLocation()).isEqualTo(externalLocation);
        assertThat(savedEod.getTransferAttempts()).isNull();
        assertThat(savedEod.getVerificationAttempts()).isEqualTo(1);
        assertThat(savedEod.getStatus()).isEqualTo(storedStatus());
        assertThat(savedEod.getExternalLocationType()).isEqualTo(unstructuredLocation());
        assertThat(savedEod.getCreatedBy()).isEqualTo(userAccountEntity);
        assertThat(savedEod.getLastModifiedBy()).isEqualTo(userAccountEntity);
    }

    @ParameterizedTest
    @CsvSource({
        "1, 1000",
        "2, 2000",
        "3, 3000"
    })
    void getFileSizeReturnsCorrectSizeForMedia(Integer mediaId, Long expectedSize) {
        when(mediaRepository.findById(mediaId)).thenReturn(Optional.of(media1));
        when(media1.getFileSize()).thenReturn(expectedSize);

        ExternalObjectDirectoryEntity eod = new ExternalObjectDirectoryEntity();
        eod.setMedia(media1);

        Long fileSize = eodService.getFileSize(eod);

        assertThat(fileSize).isEqualTo(expectedSize);
    }

    @ParameterizedTest
    @CsvSource({
        "1, 1000",
        "2, 2000",
        "3, 3000"
    })
    void getFileSizeReturnsCorrectSizeForAnnotationDocument(Integer annotationId, Long expectedSize) {
        when(annotationDocumentRepository.findById(annotationId)).thenReturn(Optional.of(annotationDocument));
        when(annotationDocument.getFileSize()).thenReturn(expectedSize.intValue());

        ExternalObjectDirectoryEntity eod = new ExternalObjectDirectoryEntity();
        eod.setAnnotationDocumentEntity(annotationDocument);

        Long fileSize = eodService.getFileSize(eod);

        assertThat(fileSize).isEqualTo(expectedSize);
    }

    @ParameterizedTest
    @CsvSource({
        "1, 1000",
        "2, 2000",
        "3, 3000"
    })
    void getFileSizeReturnsCorrectSizeForCaseDocument(Integer caseDocumentId, Long expectedSize) {
        when(caseDocumentRepository.findById(caseDocumentId)).thenReturn(Optional.of(caseDocumentEntity));
        when(caseDocumentEntity.getFileSize()).thenReturn(expectedSize.intValue());

        ExternalObjectDirectoryEntity eod = new ExternalObjectDirectoryEntity();
        eod.setCaseDocument(caseDocumentEntity);

        Long fileSize = eodService.getFileSize(eod);

        assertThat(fileSize).isEqualTo(expectedSize);
    }

    @ParameterizedTest
    @CsvSource({
        "1, 1000",
        "2, 2000",
        "3, 3000"
    })
    void getFileSizeReturnsCorrectSizeForTranscriptionDocument(Integer transcriptionId, Long expectedSize) {
        when(transcriptionDocumentRepository.findById(transcriptionId)).thenReturn(Optional.of(transcriptionDocument));
        when(transcriptionDocument.getFileSize()).thenReturn(expectedSize.intValue());

        ExternalObjectDirectoryEntity eod = new ExternalObjectDirectoryEntity();
        eod.setTranscriptionDocumentEntity(transcriptionDocument);

        Long fileSize = eodService.getFileSize(eod);

        assertThat(fileSize).isEqualTo(expectedSize);
    }

    @Test
    void getFileSizeReturnsNullWhenNoDocumentIsSet() {
        ExternalObjectDirectoryEntity eod = new ExternalObjectDirectoryEntity();

        Long fileSize = eodService.getFileSize(eod);

        assertThat(fileSize).isNull();
    }
}