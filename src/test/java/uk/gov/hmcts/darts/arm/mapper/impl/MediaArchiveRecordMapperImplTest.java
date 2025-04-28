package uk.gov.hmcts.darts.arm.mapper.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.mapper.MediaArchiveRecordMapper;
import uk.gov.hmcts.darts.arm.model.record.MediaArchiveRecord;
import uk.gov.hmcts.darts.arm.model.record.metadata.RecordMetadata;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.test.common.data.builder.TestExternalObjectDirectoryEntity;

import java.time.OffsetDateTime;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MediaArchiveRecordMapperImplTest {

    @Mock
    private ArmDataManagementConfiguration armDataManagementConfiguration;

    @Mock
    private CurrentTimeHelper currentTimeHelper;

    private MediaArchiveRecordMapper mediaArchiveRecordMapper;

    private ExternalObjectDirectoryEntity externalObjectDirectory;
    private MediaEntity mediaEntity;

    @BeforeEach
    void setUp() {

        TestExternalObjectDirectoryEntity testEod = PersistableFactory.getExternalObjectDirectoryTestData()
            .someMinimalBuilder()
            .media(PersistableFactory.getMediaTestData()
                       .someMinimalBuilder()
                       .start(OffsetDateTime.parse("2025-01-23T10:30:00Z"))
                       .end(OffsetDateTime.parse("2025-01-23T17:30:00Z"))
                       .build())

            .build();
        externalObjectDirectory = testEod.getEntity();
        mediaEntity = testEod.getMedia();

        mediaArchiveRecordMapper = new MediaArchiveRecordMapperImpl(armDataManagementConfiguration, currentTimeHelper);
    }

    @Test
    void mapToMediaArchiveRecord_ShouldReturnRecord_WhenValidInput() {
        // given
        when(armDataManagementConfiguration.getDateTimeFormat()).thenReturn("yyyy-MM-dd'T'HH:mm:ss.SSSX");
        when(armDataManagementConfiguration.getPublisher()).thenReturn("publisher");
        when(armDataManagementConfiguration.getRegion()).thenReturn("region");
        when(armDataManagementConfiguration.getMediaRecordPropertiesFile()).thenReturn(
            "Tests/arm/properties/media-record.properties");
        when(armDataManagementConfiguration.getMediaRecordClass()).thenReturn("Media");

        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(OffsetDateTime.now());

        // when
        MediaArchiveRecord result = mediaArchiveRecordMapper.mapToMediaArchiveRecord(externalObjectDirectory, "rawFilename");

        // then
        assertNotNull(result);
        assertNotNull(result.getMediaCreateArchiveRecord());
        assertNotNull(result.getUploadNewFileRecord());

        assertMetadataSuccess(result.getMediaCreateArchiveRecord().getRecordMetadata());
    }

    @Test
    void mapToMediaArchiveRecord_ShouldReturnEmptyData_WhenEodEmptyMedia() {
        // given
        when(armDataManagementConfiguration.getDateTimeFormat()).thenReturn("yyyy-MM-dd'T'HH:mm:ss.SSSX");
        when(armDataManagementConfiguration.getMediaRecordPropertiesFile()).thenReturn(
            "Tests/arm/properties/media-record.properties");

        MediaEntity mediaEntity2 = new MediaEntity();
        ExternalObjectDirectoryEntity externalObjectDirectory2 = new ExternalObjectDirectoryEntity();
        externalObjectDirectory2.setMedia(mediaEntity2);

        // when
        MediaArchiveRecord result = mediaArchiveRecordMapper.mapToMediaArchiveRecord(externalObjectDirectory2, "rawFilename");

        // then
        assertNotNull(result);
        assertNotNull(result.getMediaCreateArchiveRecord());
        assertNotNull(result.getUploadNewFileRecord());
        assertMetadataEmpty(result.getMediaCreateArchiveRecord().getRecordMetadata());
    }

    @Test
    void mapToMediaArchiveRecord_ShouldThrowNullPointerException_WhenDateTimeNotSet() {
        // given
        when(armDataManagementConfiguration.getDateTimeFormat()).thenReturn(null);

        // when
        NullPointerException exception =
            assertThrows(NullPointerException.class, () ->
                mediaArchiveRecordMapper.mapToMediaArchiveRecord(externalObjectDirectory, "rawFilename"));

        // then
        assertThat(exception.getMessage(), containsString("pattern"));
    }

    @Test
    void mapToMediaArchiveRecord_ShouldReturnRecord_WithAllProperties() {
        // given
        when(armDataManagementConfiguration.getDateTimeFormat()).thenReturn("yyyy-MM-dd'T'HH:mm:ss.SSSX");
        when(armDataManagementConfiguration.getPublisher()).thenReturn("publisher");
        when(armDataManagementConfiguration.getRegion()).thenReturn("region");
        when(armDataManagementConfiguration.getMediaRecordPropertiesFile()).thenReturn(
            "Tests/arm/properties/all_properties/media-record.properties");
        when(armDataManagementConfiguration.getMediaRecordClass()).thenReturn("Media");

        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(OffsetDateTime.now());

        // when
        MediaArchiveRecord result = mediaArchiveRecordMapper.mapToMediaArchiveRecord(externalObjectDirectory, "rawFilename");

        // then
        assertNotNull(result);
        assertNotNull(result.getMediaCreateArchiveRecord());
        assertNotNull(result.getUploadNewFileRecord());

        assertMetadataAllProperties(result.getMediaCreateArchiveRecord().getRecordMetadata());
    }

    @Test
    void mapToMediaArchiveRecord_ShouldReturnRecord_UsingInvalidProperties() {
        // given
        when(armDataManagementConfiguration.getDateTimeFormat()).thenReturn("yyyy-MM-dd'T'HH:mm:ss.SSSX");
        when(armDataManagementConfiguration.getPublisher()).thenReturn("publisher");
        when(armDataManagementConfiguration.getRegion()).thenReturn("region");
        when(armDataManagementConfiguration.getMediaRecordPropertiesFile()).thenReturn(
            "Tests/arm/all_properties/invalid-record.properties");
        when(armDataManagementConfiguration.getMediaRecordClass()).thenReturn("Media");

        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(OffsetDateTime.now());

        // when
        MediaArchiveRecord result = mediaArchiveRecordMapper.mapToMediaArchiveRecord(externalObjectDirectory, "rawFilename");

        // then
        assertNotNull(result);
        assertNotNull(result.getMediaCreateArchiveRecord());
        assertNotNull(result.getUploadNewFileRecord());

        assertNull(result.getMediaCreateArchiveRecord().getRecordMetadata().getBf001());
    }

    private void assertMetadataSuccess(RecordMetadata metadata) {
        assertEquals("Media", metadata.getBf001());
        assertNull(metadata.getBf002());
        assertEquals(mediaEntity.getMediaFormat(), metadata.getBf003());
        assertNull(metadata.getBf004());
        assertEquals(mediaEntity.getChecksum(), metadata.getBf005());
        assertNull(metadata.getBf006());
        assertNull(metadata.getBf007());
        assertNull(metadata.getBf008());
        assertNull(metadata.getBf009());
        assertNotNull(metadata.getBf010());
        assertEquals("2025-01-23T10:30:00.000Z", metadata.getBf011());
        assertEquals(mediaEntity.getId(), metadata.getBf012());
        assertEquals(mediaEntity.getId(), metadata.getBf013());
        assertEquals(mediaEntity.getChannel().longValue(), metadata.getBf014());
        assertEquals(mediaEntity.getTotalChannels().longValue(), metadata.getBf015());
        assertNull(metadata.getBf016());
        assertEquals("2025-01-23T17:30:00.000Z", metadata.getBf017());
        assertNull(metadata.getBf018());
        assertEquals("Some Courthouse", metadata.getBf019());
        assertNotNull(metadata.getBf020());
    }

    private void assertMetadataAllProperties(RecordMetadata metadata) {
        assertEquals("Media", metadata.getBf001());
        assertNull(metadata.getBf002());
        assertEquals(mediaEntity.getMediaFormat(), metadata.getBf003());
        assertNull(metadata.getBf004());
        assertEquals(mediaEntity.getChecksum(), metadata.getBf005());
        assertNull(metadata.getBf006());
        assertNull(metadata.getBf007());
        assertNull(metadata.getBf008());
        assertNull(metadata.getBf009());
        assertNotNull(metadata.getBf010());
        assertNotNull(metadata.getBf011());
        assertEquals(mediaEntity.getId(), metadata.getBf012());
        assertEquals(mediaEntity.getId(), metadata.getBf013());
        assertEquals(mediaEntity.getChannel().longValue(), metadata.getBf014());
        assertEquals(mediaEntity.getTotalChannels().longValue(), metadata.getBf015());
        assertNull(metadata.getBf016());
        assertEquals("2025-01-23T10:30:00.000Z", metadata.getBf017());
        assertEquals("2025-01-23T17:30:00.000Z", metadata.getBf018());
        assertEquals("Some Courthouse", metadata.getBf019());
        assertNotNull(metadata.getBf020());
    }

    private void assertMetadataEmpty(RecordMetadata metadata) {
        assertEquals("Media", metadata.getBf001());
        assertNull(metadata.getBf002());
        assertNull(metadata.getBf003());
        assertNull(metadata.getBf004());
        assertNull(metadata.getBf005());
        assertNull(metadata.getBf006());
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
