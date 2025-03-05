package uk.gov.hmcts.darts.arm.mapper.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.model.record.AnnotationArchiveRecord;
import uk.gov.hmcts.darts.arm.model.record.metadata.RecordMetadata;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.AnnotationEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnnotationArchiveRecordMapperImplTest {

    @Mock
    private ArmDataManagementConfiguration armDataManagementConfiguration;

    @Mock
    private CurrentTimeHelper currentTimeHelper;

    @InjectMocks
    private AnnotationArchiveRecordMapperImpl annotationArchiveRecordMapper;

    private ExternalObjectDirectoryEntity externalObjectDirectory;
    private AnnotationDocumentEntity annotationDocument;
    private AnnotationEntity annotationEntity;

    @BeforeEach
    void setUp() {

        UserAccountEntity userAccount = CommonTestDataUtil.createUserAccount();
        annotationEntity = new AnnotationEntity();
        annotationEntity.setId(1001);
        annotationEntity.setCurrentOwner(userAccount);

        HearingEntity hearing = CommonTestDataUtil.createHearing("1001", LocalDate.of(2020, 10, 10));

        annotationEntity.setHearingList(List.of(hearing));
        annotationEntity.setTimestamp(OffsetDateTime.of(2020, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC));
        annotationEntity.setText("annotationText");
        annotationDocument = createAnnotationDocumentEntity(11);
        annotationDocument.setAnnotation(annotationEntity);
        annotationEntity.setAnnotationDocuments(List.of(annotationDocument));

        externalObjectDirectory = new ExternalObjectDirectoryEntity();
        externalObjectDirectory.setAnnotationDocumentEntity(annotationDocument);
        externalObjectDirectory.setId(1);
    }

    @Test
    void mapToAnnotationArchiveRecord_ShouldReturnRecord_WhenValidInput() {
        // given
        when(armDataManagementConfiguration.getDateTimeFormat()).thenReturn("yyyy-MM-dd'T'HH:mm:ss'Z'");
        when(armDataManagementConfiguration.getPublisher()).thenReturn("publisher");
        when(armDataManagementConfiguration.getAnnotationRecordClass()).thenReturn("recordClass");
        when(armDataManagementConfiguration.getRegion()).thenReturn("region");
        when(armDataManagementConfiguration.getAnnotationRecordPropertiesFile()).thenReturn(
            "Tests/arm/properties/annotation-record.properties");
        when(armDataManagementConfiguration.getAnnotationRecordClass()).thenReturn("Annotation");

        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(OffsetDateTime.now());

        // when
        AnnotationArchiveRecord result = annotationArchiveRecordMapper.mapToAnnotationArchiveRecord(externalObjectDirectory, "rawFilename");

        // then
        assertNotNull(result);
        assertNotNull(result.getAnnotationCreateArchiveRecordOperation());
        assertNotNull(result.getUploadNewFileRecord());

        assertMetadataSuccess(result.getAnnotationCreateArchiveRecordOperation().getRecordMetadata());
    }

    @Test
    void mapToAnnotationArchiveRecord_ShouldReturnEmptyData_WhenEodEmptyAnnotation() {
        // given
        when(armDataManagementConfiguration.getDateTimeFormat()).thenReturn("yyyy-MM-dd'T'HH:mm:ss'Z'");
        when(armDataManagementConfiguration.getAnnotationRecordPropertiesFile()).thenReturn(
            "Tests/arm/properties/annotation-record.properties");

        AnnotationEntity annotation = new AnnotationEntity();
        AnnotationDocumentEntity annotationDocument2 = new AnnotationDocumentEntity();
        annotationDocument2.setAnnotation(annotation);
        ExternalObjectDirectoryEntity externalObjectDirectory2 = new ExternalObjectDirectoryEntity();
        externalObjectDirectory2.setAnnotationDocumentEntity(annotationDocument2);

        // when
        AnnotationArchiveRecord result = annotationArchiveRecordMapper.mapToAnnotationArchiveRecord(externalObjectDirectory2, "rawFilename");

        // then
        assertNotNull(result);
        assertNotNull(result.getAnnotationCreateArchiveRecordOperation());
        assertNotNull(result.getUploadNewFileRecord());
        assertMetadataEmpty(result.getAnnotationCreateArchiveRecordOperation().getRecordMetadata());
    }

    @Test
    void mapToAnnotationArchiveRecord_ShouldThrowNullPointerException_WhenDateTimeNotSet() {
        // given
        when(armDataManagementConfiguration.getDateTimeFormat()).thenReturn(null);

        // when
        NullPointerException exception =
            assertThrows(NullPointerException.class, () ->
                annotationArchiveRecordMapper.mapToAnnotationArchiveRecord(externalObjectDirectory, "rawFilename"));

        // then
        assertThat(exception.getMessage(), containsString("pattern"));
    }

    private void assertMetadataSuccess(RecordMetadata metadata) {
        assertEquals("Annotation", metadata.getBf001());
        assertEquals("1001", metadata.getBf002());
        assertEquals(annotationDocument.getFileType(), metadata.getBf003());
        assertEquals("2020-10-10T00:00:00Z", metadata.getBf004());
        assertEquals(annotationDocument.getChecksum(), metadata.getBf005());
        assertNull(metadata.getBf006());
        assertNull(metadata.getBf007());
        assertNull(metadata.getBf008());
        assertEquals(annotationDocument.getAnnotation().getText(), metadata.getBf009());
        assertEquals("2020-10-10T10:11:00Z", metadata.getBf010());
        assertNull(metadata.getBf011());
        assertEquals(annotationDocument.getId(), metadata.getBf012());
        assertEquals(annotationEntity.getId(), metadata.getBf013());
        assertNull(metadata.getBf014());
        assertNull(metadata.getBf015());
        assertEquals(String.valueOf(annotationDocument.getUploadedBy().getId()), metadata.getBf016());
        assertNull(metadata.getBf017());
        assertNull(metadata.getBf018());
        assertEquals("case_courthouse", metadata.getBf019());
        assertEquals("1", metadata.getBf020());
    }

    private void assertMetadataEmpty(RecordMetadata metadata) {
        assertEquals("Annotation", metadata.getBf001());
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

    private AnnotationDocumentEntity createAnnotationDocumentEntity(Integer id) {
        AnnotationDocumentEntity annotationDoc = new AnnotationDocumentEntity();
        annotationDoc.setId(id);
        annotationDoc.setFileName("filename" + id);
        annotationDoc.setFileType("filetype" + id);
        UserAccountEntity userAccount = CommonTestDataUtil.createUserAccount("user" + id);
        userAccount.setUserFullName("userFullName" + id);
        annotationDoc.setUploadedBy(userAccount);
        annotationDoc.setUploadedDateTime(OffsetDateTime.of(2020, 10, 10, 10, id, 0, 0, ZoneOffset.UTC));
        return annotationDoc;
    }

}