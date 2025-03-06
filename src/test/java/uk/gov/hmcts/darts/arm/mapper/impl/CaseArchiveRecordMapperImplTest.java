package uk.gov.hmcts.darts.arm.mapper.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.model.record.CaseArchiveRecord;
import uk.gov.hmcts.darts.arm.model.record.metadata.RecordMetadata;
import uk.gov.hmcts.darts.common.entity.CaseDocumentEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
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
class CaseArchiveRecordMapperImplTest {

    public static final String T_10_30_00_000_Z = "2025-01-23T10:30:00.000Z";
    public static final String CASE_1 = "case-1";
    public static final String CASE = "Case";
    @Mock
    private ArmDataManagementConfiguration armDataManagementConfiguration;

    @Mock
    private CurrentTimeHelper currentTimeHelper;

    private CaseArchiveRecordMapperImpl caseArchiveRecordMapper;

    private ExternalObjectDirectoryEntity externalObjectDirectory;
    private CaseDocumentEntity caseDocument;
    private CourtCaseEntity courtCase;

    @BeforeEach
    void setUp() {

        TestExternalObjectDirectoryEntity testEod = PersistableFactory.getExternalObjectDirectoryTestData()
            .someMinimalBuilder()
            .caseDocument(PersistableFactory.getCaseDocumentTestData()
                              .someMinimalBuilder()
                              .build())
            .build();
        externalObjectDirectory = testEod.getEntity();
        caseDocument = testEod.getCaseDocument();
        courtCase = testEod.getCaseDocument().getCourtCase();
        caseDocument.setFileName("test-file.txt");
        caseDocument.setFileType("text/plain");
        caseDocument.setCreatedDateTime(OffsetDateTime.parse("2025-01-23T10:30:00Z"));
        caseDocument.setChecksum("checksum123");

        caseArchiveRecordMapper = new CaseArchiveRecordMapperImpl(armDataManagementConfiguration, currentTimeHelper);
    }

    @Test
    void mapToCaseArchiveRecord_ShouldReturnRecord_WhenValidInput() {
        // given
        when(armDataManagementConfiguration.getDateTimeFormat()).thenReturn("yyyy-MM-dd'T'HH:mm:ss.SSSX");
        when(armDataManagementConfiguration.getPublisher()).thenReturn("publisher");
        when(armDataManagementConfiguration.getRegion()).thenReturn("region");
        when(armDataManagementConfiguration.getCaseRecordPropertiesFile()).thenReturn(
            "Tests/arm/properties/case-record.properties");
        when(armDataManagementConfiguration.getCaseRecordClass()).thenReturn(CASE);

        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(OffsetDateTime.now());

        // when
        CaseArchiveRecord result = caseArchiveRecordMapper.mapToCaseArchiveRecord(externalObjectDirectory, "rawFilename");

        // then
        assertNotNull(result);
        assertNotNull(result.getCaseCreateArchiveRecordOperation());
        assertNotNull(result.getUploadNewFileRecord());

        assertMetadataSuccess(result.getCaseCreateArchiveRecordOperation().getRecordMetadata());
    }

    @Test
    void mapToCaseArchiveRecord_ShouldReturnEmptyData_WhenEodEmptyCaseDocument() {
        // given
        when(armDataManagementConfiguration.getDateTimeFormat()).thenReturn("yyyy-MM-dd'T'HH:mm:ss.SSSX");
        when(armDataManagementConfiguration.getCaseRecordPropertiesFile()).thenReturn(
            "Tests/arm/properties/case-record.properties");

        CourthouseEntity courthouse = new CourthouseEntity();
        CourtCaseEntity courtCaseEntity = new CourtCaseEntity();
        courtCaseEntity.setCourthouse(courthouse);
        CaseDocumentEntity caseDocumentEntity2 = new CaseDocumentEntity();
        caseDocumentEntity2.setCourtCase(courtCaseEntity);
        ExternalObjectDirectoryEntity externalObjectDirectory2 = new ExternalObjectDirectoryEntity();
        externalObjectDirectory2.setCaseDocument(caseDocumentEntity2);

        // when
        CaseArchiveRecord result = caseArchiveRecordMapper.mapToCaseArchiveRecord(externalObjectDirectory2, "rawFilename");

        // then
        assertNotNull(result);
        assertNotNull(result.getCaseCreateArchiveRecordOperation());
        assertNotNull(result.getUploadNewFileRecord());
        assertMetadataEmpty(result.getCaseCreateArchiveRecordOperation().getRecordMetadata());
    }

    @Test
    void mapToAnnotationArchiveRecord_ShouldThrowNullPointerException_WhenDateTimeNotSet() {
        // given
        when(armDataManagementConfiguration.getDateTimeFormat()).thenReturn(null);

        // when
        NullPointerException exception =
            assertThrows(NullPointerException.class, () ->
                caseArchiveRecordMapper.mapToCaseArchiveRecord(externalObjectDirectory, "rawFilename"));

        // then
        assertThat(exception.getMessage(), containsString("pattern"));
    }

    @Test
    void mapToCaseArchiveRecord_ShouldReturnRecord_WithAllProperties() {
        // given
        when(armDataManagementConfiguration.getDateTimeFormat()).thenReturn("yyyy-MM-dd'T'HH:mm:ss.SSSX");
        when(armDataManagementConfiguration.getPublisher()).thenReturn("publisher");
        when(armDataManagementConfiguration.getRegion()).thenReturn("region");
        when(armDataManagementConfiguration.getCaseRecordPropertiesFile()).thenReturn(
            "Tests/arm/properties/all_properties/case-record.properties");
        when(armDataManagementConfiguration.getCaseRecordClass()).thenReturn(CASE);

        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(OffsetDateTime.now());

        // when
        CaseArchiveRecord result = caseArchiveRecordMapper.mapToCaseArchiveRecord(externalObjectDirectory, "rawFilename");

        // then
        assertNotNull(result);
        assertNotNull(result.getCaseCreateArchiveRecordOperation());
        assertNotNull(result.getUploadNewFileRecord());

        assertMetadataAllProperties(result.getCaseCreateArchiveRecordOperation().getRecordMetadata());
    }

    private void assertMetadataSuccess(RecordMetadata metadata) {
        assertEquals(CASE, metadata.getBf001());
        assertEquals(CASE_1, metadata.getBf002());
        assertEquals(caseDocument.getFileType(), metadata.getBf003());
        assertNull(metadata.getBf004());
        assertEquals(caseDocument.getChecksum(), metadata.getBf005());
        assertNull(metadata.getBf006());
        assertNull(metadata.getBf007());
        assertNull(metadata.getBf008());
        assertNull(metadata.getBf009());
        assertEquals(T_10_30_00_000_Z, metadata.getBf010());
        assertNull(metadata.getBf011());
        assertEquals(caseDocument.getId(), metadata.getBf012());
        assertEquals(courtCase.getId(), metadata.getBf013());
        assertNull(metadata.getBf014());
        assertNull(metadata.getBf015());
        assertNull(metadata.getBf016());
        assertNull(metadata.getBf017());
        assertNull(metadata.getBf018());
        assertEquals(courtCase.getCourthouse().getDisplayName(), metadata.getBf019());
        assertNull(metadata.getBf020());
    }

    private void assertMetadataAllProperties(RecordMetadata metadata) {
        assertEquals(CASE, metadata.getBf001());
        assertEquals(CASE_1, metadata.getBf002());
        assertEquals(caseDocument.getFileType(), metadata.getBf003());
        assertNull(metadata.getBf004());
        assertEquals(caseDocument.getChecksum(), metadata.getBf005());
        assertNull(metadata.getBf006());
        assertNull(metadata.getBf007());
        assertNull(metadata.getBf008());
        assertNull(metadata.getBf009());
        assertEquals(T_10_30_00_000_Z, metadata.getBf010());
        assertEquals(T_10_30_00_000_Z, metadata.getBf011());
        assertEquals(caseDocument.getId(), metadata.getBf012());
        assertEquals(courtCase.getId(), metadata.getBf013());
        assertNull(metadata.getBf014());
        assertNull(metadata.getBf015());
        assertNull(metadata.getBf016());
        assertNull(metadata.getBf017());
        assertNull(metadata.getBf018());
        assertEquals(courtCase.getCourthouse().getDisplayName(), metadata.getBf019());
        assertNull(metadata.getBf020());
    }

    private void assertMetadataEmpty(RecordMetadata metadata) {
        assertEquals(CASE, metadata.getBf001());
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