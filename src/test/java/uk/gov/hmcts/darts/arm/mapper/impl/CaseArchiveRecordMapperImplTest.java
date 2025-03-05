package uk.gov.hmcts.darts.arm.mapper.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.model.record.CaseArchiveRecord;
import uk.gov.hmcts.darts.arm.model.record.metadata.RecordMetadata;
import uk.gov.hmcts.darts.common.entity.CaseDocumentEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.test.common.data.builder.TestExternalObjectDirectoryEntity;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseArchiveRecordMapperImplTest {

    private static final String T_10_30_00_Z = "2025-01-23T10:30:00Z";
    private static final OffsetDateTime START = OffsetDateTime.parse(T_10_30_00_Z);

    @Mock
    private ArmDataManagementConfiguration armDataManagementConfiguration;

    @Mock
    private CurrentTimeHelper currentTimeHelper;

    @InjectMocks
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
        caseDocument.setCreatedDateTime(START);
        caseDocument.setChecksum("checksum123");

//        externalObjectDirectory = new ExternalObjectDirectoryEntity();
//        externalObjectDirectory.setCaseDocument(caseDocument);
//        externalObjectDirectory.setId(1);
    }

    @Test
    void mapToCaseArchiveRecord_ShouldReturnRecord_WhenValidInput() {
        // given
        when(armDataManagementConfiguration.getDateTimeFormat()).thenReturn("yyyy-MM-dd'T'HH:mm:ss'Z'");
        when(armDataManagementConfiguration.getPublisher()).thenReturn("publisher");
        when(armDataManagementConfiguration.getCaseRecordClass()).thenReturn("recordClass");
        when(armDataManagementConfiguration.getRegion()).thenReturn("region");
        when(armDataManagementConfiguration.getCaseRecordPropertiesFile()).thenReturn(
            "Tests/arm/properties/case-record.properties");
        when(armDataManagementConfiguration.getCaseRecordClass()).thenReturn("Case");

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
    void mapToCaseArchiveRecord_ShouldReturnNull_WhenIOExceptionOccurs() {
        when(armDataManagementConfiguration.getDateTimeFormat()).thenReturn("yyyy-MM-dd'T'HH:mm:ss'Z'");
        when(armDataManagementConfiguration.getCaseRecordPropertiesFile()).thenReturn("invalid-file.properties");

        CaseArchiveRecord result = caseArchiveRecordMapper.mapToCaseArchiveRecord(externalObjectDirectory, "rawFilename");

        assertNull(result);
    }

    private void assertMetadataSuccess(RecordMetadata metadata) {
        assertEquals("Case", metadata.getBf001());
        assertEquals("case-1", metadata.getBf002());
        assertEquals(caseDocument.getFileType(), metadata.getBf003());
        assertNull(metadata.getBf004());
        assertEquals(caseDocument.getChecksum(), metadata.getBf005());
        assertNull(metadata.getBf006());
        assertNull(metadata.getBf007());
        assertNull(metadata.getBf008());
        assertNull(metadata.getBf009());
        assertNull(metadata.getBf010());
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
}