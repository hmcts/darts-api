package uk.gov.hmcts.darts.arm.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.mapper.AnnotationArchiveRecordMapper;
import uk.gov.hmcts.darts.arm.mapper.CaseArchiveRecordMapper;
import uk.gov.hmcts.darts.arm.mapper.MediaArchiveRecordMapper;
import uk.gov.hmcts.darts.arm.mapper.TranscriptionArchiveRecordMapper;
import uk.gov.hmcts.darts.arm.model.ArchiveRecord;
import uk.gov.hmcts.darts.arm.model.record.AnnotationArchiveRecord;
import uk.gov.hmcts.darts.arm.model.record.CaseArchiveRecord;
import uk.gov.hmcts.darts.arm.model.record.MediaArchiveRecord;
import uk.gov.hmcts.darts.arm.model.record.TranscriptionArchiveRecord;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.CaseDocumentEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.exception.DartsException;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;

import java.io.File;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Slf4j
class ArchiveRecordServiceImplTest {

    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSX";
    private static final String DARTS = "DARTS";
    private static final String REGION = "GBR";
    private static final long EODID = 1234;
    private static final String FILE_EXTENSION = "a360";

    @Mock(lenient = true)
    private ArmDataManagementConfiguration armDataManagementConfiguration;
    @Mock
    private ExternalObjectDirectoryEntity externalObjectDirectoryEntity;
    @Mock
    private MediaEntity mediaEntity;
    @Mock
    private TranscriptionDocumentEntity transcriptionDocumentEntity;
    @Mock
    private AnnotationDocumentEntity annotationDocumentEntity;
    @Mock
    private CaseDocumentEntity caseDocumentEntity;

    @Mock
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;

    @Mock
    private MediaArchiveRecordMapper mediaArchiveRecordMapperMock;
    @Mock
    private TranscriptionArchiveRecordMapper transcriptionArchiveRecordMapperMock;
    @Mock
    private AnnotationArchiveRecordMapper annotationArchiveRecordMapperMock;
    @Mock
    private CaseArchiveRecordMapper caseArchiveRecordMapperMock;

    @TempDir
    private File tempDirectory;

    private ArchiveRecordServiceImpl archiveRecordService;

    @BeforeEach
    void setUp() {

        String fileLocation = tempDirectory.getAbsolutePath();
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);
        when(armDataManagementConfiguration.getDateTimeFormat()).thenReturn(DATE_TIME_FORMAT);
        when(armDataManagementConfiguration.getPublisher()).thenReturn(DARTS);
        when(armDataManagementConfiguration.getRegion()).thenReturn(REGION);
        when(armDataManagementConfiguration.getFileExtension()).thenReturn(FILE_EXTENSION);

        archiveRecordService = new ArchiveRecordServiceImpl(
            mediaArchiveRecordMapperMock,
            transcriptionArchiveRecordMapperMock,
            annotationArchiveRecordMapperMock,
            caseArchiveRecordMapperMock,
            externalObjectDirectoryRepository
        );
    }

    @Test
    void generateArchiveRecordInfo_forMedia_delegatesToCorrectMapper() {
        // given
        when(externalObjectDirectoryRepository.findById(any())).thenReturn(Optional.of(externalObjectDirectoryEntity));
        when(externalObjectDirectoryEntity.getMedia()).thenReturn(mediaEntity);
        when(mediaArchiveRecordMapperMock.mapToMediaArchiveRecord(any(), any())).thenReturn(MediaArchiveRecord.builder().build());

        // when
        ArchiveRecord archiveRecord = archiveRecordService.generateArchiveRecordInfo(EODID, "1234_1_1");

        // then
        assertThat(archiveRecord).isInstanceOf(MediaArchiveRecord.class);
        verify(mediaArchiveRecordMapperMock).mapToMediaArchiveRecord(externalObjectDirectoryEntity, "1234_1_1");
        verifyNoInteractions(annotationArchiveRecordMapperMock);
        verifyNoInteractions(caseArchiveRecordMapperMock);
        verifyNoInteractions(transcriptionArchiveRecordMapperMock);
    }

    @Test
    void generateArchiveRecordInfo_forTranscription_delegatesToCorrectMapper() {
        // given
        when(externalObjectDirectoryRepository.findById(any())).thenReturn(Optional.of(externalObjectDirectoryEntity));
        when(externalObjectDirectoryEntity.getTranscriptionDocumentEntity()).thenReturn(transcriptionDocumentEntity);
        when(transcriptionArchiveRecordMapperMock.mapToTranscriptionArchiveRecord(any(), any())).thenReturn(TranscriptionArchiveRecord.builder().build());

        // when
        ArchiveRecord archiveRecord = archiveRecordService.generateArchiveRecordInfo(EODID, "1234_1_1");

        // then
        assertThat(archiveRecord).isInstanceOf(TranscriptionArchiveRecord.class);
        verify(transcriptionArchiveRecordMapperMock).mapToTranscriptionArchiveRecord(externalObjectDirectoryEntity, "1234_1_1");
        verifyNoInteractions(annotationArchiveRecordMapperMock);
        verifyNoInteractions(caseArchiveRecordMapperMock);
        verifyNoInteractions(mediaArchiveRecordMapperMock);
    }

    @Test
    void generateArchiveRecordInfo_forAnnotation_delegatesToCorrectMapper() {
        // given
        when(externalObjectDirectoryRepository.findById(any())).thenReturn(Optional.of(externalObjectDirectoryEntity));
        when(externalObjectDirectoryEntity.getAnnotationDocumentEntity()).thenReturn(annotationDocumentEntity);
        when(annotationArchiveRecordMapperMock.mapToAnnotationArchiveRecord(any(), any())).thenReturn(AnnotationArchiveRecord.builder().build());

        // when
        ArchiveRecord archiveRecord = archiveRecordService.generateArchiveRecordInfo(EODID, "1234_1_1");

        // then
        assertThat(archiveRecord).isInstanceOf(AnnotationArchiveRecord.class);
        verify(annotationArchiveRecordMapperMock).mapToAnnotationArchiveRecord(externalObjectDirectoryEntity, "1234_1_1");
        verifyNoInteractions(transcriptionArchiveRecordMapperMock);
        verifyNoInteractions(caseArchiveRecordMapperMock);
        verifyNoInteractions(mediaArchiveRecordMapperMock);
    }

    @Test
    void generateArchiveRecordInfo_forCaseDocument_delegatesToCorrectMapper() {
        // given
        when(externalObjectDirectoryRepository.findById(any())).thenReturn(Optional.of(externalObjectDirectoryEntity));
        when(externalObjectDirectoryEntity.getCaseDocument()).thenReturn(caseDocumentEntity);
        when(caseArchiveRecordMapperMock.mapToCaseArchiveRecord(any(), any())).thenReturn(CaseArchiveRecord.builder().build());

        // when
        ArchiveRecord archiveRecord = archiveRecordService.generateArchiveRecordInfo(EODID, "1234_1_1");

        // then
        assertThat(archiveRecord).isInstanceOf(CaseArchiveRecord.class);
        verify(caseArchiveRecordMapperMock).mapToCaseArchiveRecord(externalObjectDirectoryEntity, "1234_1_1");
        verifyNoInteractions(transcriptionArchiveRecordMapperMock);
        verifyNoInteractions(annotationArchiveRecordMapperMock);
        verifyNoInteractions(mediaArchiveRecordMapperMock);
    }

    @Test
    void generateArchiveRecordInfo_throwsError_whenMapperReturnsNoResult() {
        // given
        when(externalObjectDirectoryRepository.findById(any())).thenReturn(Optional.of(externalObjectDirectoryEntity));
        when(externalObjectDirectoryEntity.getTranscriptionDocumentEntity()).thenReturn(transcriptionDocumentEntity);

        // then
        assertThrows(DartsException.class, () -> archiveRecordService.generateArchiveRecordInfo(EODID, "1234_1_1"));
    }

    @Test
    void generateArchiveRecordInfo_throwsError_whenCannotFindEod() {
        // given
        when(externalObjectDirectoryRepository.findById(any())).thenReturn(Optional.empty());

        // then
        assertThrows(DartsException.class, () -> archiveRecordService.generateArchiveRecordInfo(EODID, "1234_1_1"));
    }

    @Test
    void generateArchiveRecordInfo_throwsError_whenUnknownArchiveRecordType() {
        // given
        when(externalObjectDirectoryRepository.findById(any())).thenReturn(Optional.of(externalObjectDirectoryEntity));

        // then
        assertThrows(DartsException.class, () -> archiveRecordService.generateArchiveRecordInfo(EODID, "1234_1_1"));
    }

}
