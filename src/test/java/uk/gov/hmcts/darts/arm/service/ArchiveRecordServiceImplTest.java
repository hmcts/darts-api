package uk.gov.hmcts.darts.arm.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.component.ArchiveRecordFileGenerator;
import uk.gov.hmcts.darts.arm.component.impl.ArchiveRecordFileGeneratorImpl;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.mapper.AnnotationArchiveRecordMapper;
import uk.gov.hmcts.darts.arm.mapper.CaseArchiveRecordMapper;
import uk.gov.hmcts.darts.arm.mapper.MediaArchiveRecordMapper;
import uk.gov.hmcts.darts.arm.mapper.TranscriptionArchiveRecordMapper;
import uk.gov.hmcts.darts.arm.mapper.impl.AnnotationArchiveRecordMapperImpl;
import uk.gov.hmcts.darts.arm.mapper.impl.CaseArchiveRecordMapperImpl;
import uk.gov.hmcts.darts.arm.mapper.impl.MediaArchiveRecordMapperImpl;
import uk.gov.hmcts.darts.arm.mapper.impl.TranscriptionArchiveRecordMapperImpl;
import uk.gov.hmcts.darts.arm.model.ArchiveRecord;
import uk.gov.hmcts.darts.arm.model.record.AnnotationArchiveRecord;
import uk.gov.hmcts.darts.arm.model.record.CaseArchiveRecord;
import uk.gov.hmcts.darts.arm.model.record.MediaArchiveRecord;
import uk.gov.hmcts.darts.arm.model.record.TranscriptionArchiveRecord;
import uk.gov.hmcts.darts.arm.service.impl.ArchiveRecordServiceImpl;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.AnnotationEntity;
import uk.gov.hmcts.darts.common.entity.CaseDocumentEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionCommentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionTypeEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionUrgencyEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionWorkflowEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsException;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.test.common.TestUtils.getObjectMapper;

@SuppressWarnings("PMD.AssignmentInOperand")
@ExtendWith(MockitoExtension.class)
@Slf4j
class ArchiveRecordServiceImplTest {
    public static final String TEST_ARCHIVE_FILENAME = "1234-1-1.a360";
    public static final String MP_2 = "mp2";
    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssX";
    public static final String DARTS = "DARTS";
    public static final String REGION = "GBR";
    public static final int EODID = 1234;
    public static final String FILE_EXTENSION = "a360";
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final int RETENTION_CONFIDENCE_SCORE = 2;
    public static final String RETENTION_CONFIDENCE_REASON = "RetentionConfidenceReason";

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);


    @Mock(lenient = true)
    private ArmDataManagementConfiguration armDataManagementConfiguration;
    @Mock
    private ExternalObjectDirectoryEntity externalObjectDirectoryEntity;
    @Mock
    private MediaEntity mediaEntity;
    @Mock
    private TranscriptionEntity transcriptionEntity;
    @Mock
    private TranscriptionDocumentEntity transcriptionDocumentEntity;
    @Mock
    private AnnotationDocumentEntity annotationDocumentEntity;
    @Mock
    private CaseDocumentEntity caseDocumentEntity;
    @Mock
    private CourtroomEntity courtroomEntity;
    @Mock
    private CourthouseEntity courthouseEntity;
    @Mock
    private TranscriptionCommentEntity transcriptionCommentEntity;
    @Mock
    private TranscriptionUrgencyEntity transcriptionUrgencyEntity;
    @Mock
    private TranscriptionTypeEntity transcriptionTypeEntity;
    @Mock
    private TranscriptionWorkflowEntity transcriptionWorkflowEntity;
    @Mock
    private TranscriptionStatusEntity transcriptionStatusEntity;
    @Mock
    private AnnotationEntity annotationEntity;
    @Mock
    private HearingEntity hearingEntity1;
    @Mock
    private HearingEntity hearingEntity2;
    @Mock
    private HearingEntity hearingEntity3;
    @Mock
    private CourtCaseEntity courtCaseEntity1;
    @Mock
    private CourtCaseEntity courtCaseEntity2;
    @Mock
    private CourtCaseEntity courtCaseEntity3;
    @Mock
    private UserAccountEntity userAccountEntity;

    @Mock
    private CurrentTimeHelper currentTimeHelper;

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

    private ArchiveRecordFileGenerator archiveRecordFileGenerator;

    @TempDir
    private File tempDirectory;

    @InjectMocks
    private ArchiveRecordServiceImpl archiveRecordService;

    @BeforeEach
    void setUp() {
        archiveRecordFileGenerator = new ArchiveRecordFileGeneratorImpl(getObjectMapper());

        MediaArchiveRecordMapper mediaArchiveRecordMapper = new MediaArchiveRecordMapperImpl(armDataManagementConfiguration, currentTimeHelper);
        TranscriptionArchiveRecordMapper transcriptionArchiveRecordMapper = new TranscriptionArchiveRecordMapperImpl(
            armDataManagementConfiguration,
            currentTimeHelper
        );
        AnnotationArchiveRecordMapper annotationArchiveRecordMapper = new AnnotationArchiveRecordMapperImpl(armDataManagementConfiguration, currentTimeHelper);
        CaseArchiveRecordMapper caseArchiveRecordMapper = new CaseArchiveRecordMapperImpl(armDataManagementConfiguration, currentTimeHelper);

        archiveRecordService = new ArchiveRecordServiceImpl(
            mediaArchiveRecordMapper,
            transcriptionArchiveRecordMapper,
            annotationArchiveRecordMapper,
            caseArchiveRecordMapper,
            externalObjectDirectoryRepository
        );

        String fileLocation = tempDirectory.getAbsolutePath();
        when(armDataManagementConfiguration.getTempBlobWorkspace()).thenReturn(fileLocation);
        when(armDataManagementConfiguration.getDateTimeFormat()).thenReturn(DATE_TIME_FORMAT);
        when(armDataManagementConfiguration.getPublisher()).thenReturn(DARTS);
        when(armDataManagementConfiguration.getRegion()).thenReturn(REGION);
        when(armDataManagementConfiguration.getFileExtension()).thenReturn(FILE_EXTENSION);

    }

    @Test
    void generateArchiveRecordInfo_forMedia_delegatesToCorrectMapper() {
        // given
        when(externalObjectDirectoryRepository.findById(any())).thenReturn(Optional.of(externalObjectDirectoryEntity));
        when(externalObjectDirectoryEntity.getMedia()).thenReturn(mediaEntity);
        when(mediaArchiveRecordMapperMock.mapToMediaArchiveRecord(any(), any())).thenReturn(MediaArchiveRecord.builder().build());

        ArchiveRecordServiceImpl archiveRecordService = new ArchiveRecordServiceImpl(
            mediaArchiveRecordMapperMock,
            transcriptionArchiveRecordMapperMock,
            annotationArchiveRecordMapperMock,
            caseArchiveRecordMapperMock,
            externalObjectDirectoryRepository
        );

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

        ArchiveRecordServiceImpl archiveRecordService = new ArchiveRecordServiceImpl(
            mediaArchiveRecordMapperMock,
            transcriptionArchiveRecordMapperMock,
            annotationArchiveRecordMapperMock,
            caseArchiveRecordMapperMock,
            externalObjectDirectoryRepository
        );

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

        ArchiveRecordServiceImpl archiveRecordService = new ArchiveRecordServiceImpl(
            mediaArchiveRecordMapperMock,
            transcriptionArchiveRecordMapperMock,
            annotationArchiveRecordMapperMock,
            caseArchiveRecordMapperMock,
            externalObjectDirectoryRepository
        );

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

        ArchiveRecordServiceImpl archiveRecordService = new ArchiveRecordServiceImpl(
            mediaArchiveRecordMapperMock,
            transcriptionArchiveRecordMapperMock,
            annotationArchiveRecordMapperMock,
            caseArchiveRecordMapperMock,
            externalObjectDirectoryRepository
        );

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

        ArchiveRecordServiceImpl archiveRecordService = new ArchiveRecordServiceImpl(
            mediaArchiveRecordMapperMock,
            transcriptionArchiveRecordMapperMock,
            annotationArchiveRecordMapperMock,
            caseArchiveRecordMapperMock,
            externalObjectDirectoryRepository
        );

        // then
        assertThrows(DartsException.class, () -> archiveRecordService.generateArchiveRecordInfo(EODID, "1234_1_1"));
    }

    @Test
    void generateArchiveRecordInfo_throwsError_whenCannotFindEod() {
        // given
        when(externalObjectDirectoryRepository.findById(any())).thenReturn(Optional.empty());

        ArchiveRecordServiceImpl archiveRecordService = new ArchiveRecordServiceImpl(
            mediaArchiveRecordMapperMock,
            transcriptionArchiveRecordMapperMock,
            annotationArchiveRecordMapperMock,
            caseArchiveRecordMapperMock,
            externalObjectDirectoryRepository
        );

        // then
        assertThrows(DartsException.class, () -> archiveRecordService.generateArchiveRecordInfo(EODID, "1234_1_1"));
    }

    @Test
    void generateArchiveRecordInfo_throwsError_whenUnknownArchiveRecordType() {
        // given
        when(externalObjectDirectoryRepository.findById(any())).thenReturn(Optional.of(externalObjectDirectoryEntity));

        ArchiveRecordServiceImpl archiveRecordService = new ArchiveRecordServiceImpl(
            mediaArchiveRecordMapperMock,
            transcriptionArchiveRecordMapperMock,
            annotationArchiveRecordMapperMock,
            caseArchiveRecordMapperMock,
            externalObjectDirectoryRepository
        );

        // then
        assertThrows(DartsException.class, () -> archiveRecordService.generateArchiveRecordInfo(EODID, "1234_1_1"));
    }

}
