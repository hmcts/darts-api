package uk.gov.hmcts.darts.arm.mapper.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.mapper.TranscriptionArchiveRecordMapper;
import uk.gov.hmcts.darts.arm.model.record.TranscriptionArchiveRecord;
import uk.gov.hmcts.darts.arm.model.record.metadata.RecordMetadata;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.test.common.data.builder.TestExternalObjectDirectoryEntity;

import java.time.OffsetDateTime;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TranscriptionArchiveRecordMapperImplTest {

    @Mock
    private ArmDataManagementConfiguration armDataManagementConfiguration;

    @Mock
    private CurrentTimeHelper currentTimeHelper;

    private TranscriptionArchiveRecordMapper transcriptionArchiveRecordMapper;

    private ExternalObjectDirectoryEntity externalObjectDirectory;
    private TranscriptionDocumentEntity transcriptionDocument;
    private TranscriptionEntity transcriptionEntity;

    @BeforeEach
    void setUp() {

        TestExternalObjectDirectoryEntity testEod = PersistableFactory.getExternalObjectDirectoryTestData()
            .someMinimalBuilder()
            .transcriptionDocumentEntity(PersistableFactory.getTranscriptionDocument()
                                             .someMinimalBuilder()
                                             .build())

            .build();
        externalObjectDirectory = testEod.getEntity();
        transcriptionDocument = testEod.getTranscriptionDocumentEntity();
        transcriptionEntity = transcriptionDocument.getTranscription();
        transcriptionDocument.getUploadedBy().setId(1);
        transcriptionArchiveRecordMapper = new TranscriptionArchiveRecordMapperImpl(armDataManagementConfiguration, currentTimeHelper);
    }

    @Test
    void mapToTranscriptionArchiveRecord_ShouldReturnRecord_WhenValidInput() {
        // given
        when(armDataManagementConfiguration.getDateTimeFormat()).thenReturn("yyyy-MM-dd'T'HH:mm:ss.SSSX");
        when(armDataManagementConfiguration.getPublisher()).thenReturn("publisher");
        when(armDataManagementConfiguration.getRegion()).thenReturn("region");
        when(armDataManagementConfiguration.getTranscriptionRecordPropertiesFile()).thenReturn(
            "Tests/arm/properties/transcription-record.properties");
        when(armDataManagementConfiguration.getTranscriptionRecordClass()).thenReturn("Transcription");

        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(OffsetDateTime.now());

        // when
        TranscriptionArchiveRecord result = transcriptionArchiveRecordMapper.mapToTranscriptionArchiveRecord(externalObjectDirectory, "rawFilename");

        // then
        assertNotNull(result);
        assertNotNull(result.getTranscriptionCreateArchiveRecordOperation());
        assertNotNull(result.getUploadNewFileRecord());

        assertMetadataSuccess(result.getTranscriptionCreateArchiveRecordOperation().getRecordMetadata());
    }

    @Test
    void mapToTranscriptionArchiveRecord_ShouldReturnEmptyData_WhenEodEmptyTranscription() {
        // given
        when(armDataManagementConfiguration.getDateTimeFormat()).thenReturn("yyyy-MM-dd'T'HH:mm:ss.SSSX");
        when(armDataManagementConfiguration.getTranscriptionRecordPropertiesFile()).thenReturn(
            "Tests/arm/properties/transcription-record.properties");

        CourthouseEntity courthouse = new CourthouseEntity();
        CourtCaseEntity courtCase = new CourtCaseEntity();
        courtCase.setCourthouse(courthouse);
        TranscriptionEntity transcriptionEntity2 = new TranscriptionEntity();
        transcriptionEntity2.setCourtCases(List.of(courtCase));
        TranscriptionDocumentEntity transcriptionDocumentEntity2 = new TranscriptionDocumentEntity();
        transcriptionDocumentEntity2.setTranscription(transcriptionEntity2);
        ExternalObjectDirectoryEntity externalObjectDirectory2 = new ExternalObjectDirectoryEntity();
        externalObjectDirectory2.setTranscriptionDocumentEntity(transcriptionDocumentEntity2);

        // when
        TranscriptionArchiveRecord result = transcriptionArchiveRecordMapper.mapToTranscriptionArchiveRecord(externalObjectDirectory2, "rawFilename");

        // then
        assertNotNull(result);
        assertNotNull(result.getTranscriptionCreateArchiveRecordOperation());
        assertNotNull(result.getUploadNewFileRecord());
        assertMetadataEmpty(result.getTranscriptionCreateArchiveRecordOperation().getRecordMetadata());
    }

    @Test
    void mapToTranscriptionArchiveRecord_ShouldThrowNullPointerException_WhenDateTimeNotSet() {
        // given
        when(armDataManagementConfiguration.getDateTimeFormat()).thenReturn(null);

        // when
        NullPointerException exception =
            assertThrows(NullPointerException.class, () ->
                transcriptionArchiveRecordMapper.mapToTranscriptionArchiveRecord(externalObjectDirectory, "rawFilename"));

        // then
        assertThat(exception.getMessage(), containsString("pattern"));
    }

    @Test
    void mapToTranscriptionArchiveRecord_ShouldReturnRecord_WithAllProperties() {
        // given
        when(armDataManagementConfiguration.getDateTimeFormat()).thenReturn("yyyy-MM-dd'T'HH:mm:ss.SSSX");
        when(armDataManagementConfiguration.getPublisher()).thenReturn("publisher");
        when(armDataManagementConfiguration.getRegion()).thenReturn("region");
        when(armDataManagementConfiguration.getTranscriptionRecordPropertiesFile()).thenReturn(
            "Tests/arm/properties/all_properties/transcription-record.properties");
        when(armDataManagementConfiguration.getTranscriptionRecordClass()).thenReturn("Transcription");

        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(OffsetDateTime.now());

        // when
        TranscriptionArchiveRecord result = transcriptionArchiveRecordMapper.mapToTranscriptionArchiveRecord(externalObjectDirectory, "rawFilename");

        // then
        assertNotNull(result);
        assertNotNull(result.getTranscriptionCreateArchiveRecordOperation());
        assertNotNull(result.getUploadNewFileRecord());

        assertMetadataWithAllProperties(result.getTranscriptionCreateArchiveRecordOperation().getRecordMetadata());
    }

    private void assertMetadataSuccess(RecordMetadata metadata) {
        assertEquals("Transcription", metadata.getBf001());
        assertNotNull(metadata.getBf002());
        assertEquals("some-file-type", metadata.getBf003());
        assertNull(metadata.getBf004());
        assertEquals(transcriptionDocument.getChecksum(), metadata.getBf005());
        assertEquals("Automatic", metadata.getBf006());
        assertNull(metadata.getBf007());
        assertNull(metadata.getBf008());
        assertNull(metadata.getBf009());
        assertNotNull(metadata.getBf010());
        assertNull(metadata.getBf011());
        assertEquals(transcriptionDocument.getId(), metadata.getBf012());
        assertEquals(transcriptionEntity.getId(), metadata.getBf013());
        assertNull(metadata.getBf014());
        assertNull(metadata.getBf015());
        assertEquals("1", metadata.getBf016());
        assertNull(metadata.getBf017());
        assertNull(metadata.getBf018());
        assertNotNull(metadata.getBf019());
        assertNull(metadata.getBf020());
    }

    private void assertMetadataWithAllProperties(RecordMetadata metadata) {
        assertEquals("Transcription", metadata.getBf001());
        assertNotNull(metadata.getBf002());
        assertEquals("some-file-type", metadata.getBf003());
        assertNull(metadata.getBf004());
        assertEquals(transcriptionDocument.getChecksum(), metadata.getBf005());
        assertEquals("Automatic", metadata.getBf006());
        assertNull(metadata.getBf007());
        assertNull(metadata.getBf008());
        assertNull(metadata.getBf009());
        assertNotNull(metadata.getBf010());
        assertNotNull(metadata.getBf011());
        assertEquals(transcriptionDocument.getId(), metadata.getBf012());
        assertEquals(transcriptionEntity.getId(), metadata.getBf013());
        assertNull(metadata.getBf014());
        assertNull(metadata.getBf015());
        assertEquals("1", metadata.getBf016());
        assertNull(metadata.getBf017());
        assertNull(metadata.getBf018());
        assertNotNull(metadata.getBf019());
        assertNull(metadata.getBf020());
    }

    private void assertMetadataEmpty(RecordMetadata metadata) {
        assertEquals("Transcription", metadata.getBf001());
        assertNull(metadata.getBf002());
        assertNull(metadata.getBf003());
        assertNull(metadata.getBf004());
        assertNull(metadata.getBf005());
        assertEquals("Automatic", metadata.getBf006());
        assertNull(metadata.getBf007());
        assertNull(metadata.getBf008());
        assertNull(metadata.getBf009());
        assertNull(metadata.getBf010());
        assertNull(metadata.getBf011());
        assertNull(metadata.getBf012());
        assertNull(metadata.getBf013());
        assertNull(metadata.getBf014());
        assertNull(metadata.getBf015());
        assertNull(metadata.getBf016());
        assertNull(metadata.getBf017());
        assertNull(metadata.getBf018());
        assertNull(metadata.getBf019());
        assertNull(metadata.getBf020());
    }

}
