package uk.gov.hmcts.darts.arm.mapper;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.model.record.AnnotationArchiveRecord;
import uk.gov.hmcts.darts.arm.model.record.metadata.RecordMetadata;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.AnnotationEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceScoreEnum;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.test.common.data.builder.TestAnnotationEntity;
import uk.gov.hmcts.darts.testutils.GivenBuilder;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.ARM;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;
import static uk.gov.hmcts.darts.retention.enums.RetentionConfidenceScoreEnum.CASE_PERFECTLY_CLOSED;

@Slf4j
class AnnotationArchiveRecordMapperIntTest extends IntegrationBase {

    private static final String T_10_30_00_Z = "2025-01-23T10:30:00Z";
    private static final String T_11_30_00_Z = "2025-01-23T11:30:00Z";
    private static final OffsetDateTime END = OffsetDateTime.parse(T_11_30_00_Z);
    private static final OffsetDateTime START = OffsetDateTime.parse(T_10_30_00_Z);

    @MockitoBean
    private UserIdentity userIdentity;
    @MockitoBean
    private ArmDataManagementApi armDataManagementApi;
    @MockitoSpyBean
    private ArmDataManagementConfiguration armDataManagementConfiguration;

    @Autowired
    private GivenBuilder given;

    @Autowired
    private AnnotationArchiveRecordMapper annotationArchiveRecordMapper;

    @BeforeEach
    void setupData() {
        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);
    }

    @Transactional
    @Test
    void mapToAnnotationArchiveRecord_Success() {
        // given
        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        String testAnnotation = "TestAnnotation";

        TestAnnotationEntity.TestAnnotationEntityRetrieve annotationEntityRetrieve
            = PersistableFactory.getAnnotationTestData().someMinimalBuilderHolder();

        AnnotationEntity annotation = annotationEntityRetrieve.getBuilder().text(testAnnotation).build().getEntity();

        when(userIdentity.getUserAccount()).thenReturn(testUser);
        final String fileName = "judges-notes.txt";
        final String fileType = "text/plain";
        final int fileSize = 123;
        final OffsetDateTime uploadedDateTime = OffsetDateTime.now();
        final String checksum = "123";
        final String confidenceReason = "reason";
        final RetentionConfidenceScoreEnum confidenceScore = CASE_PERFECTLY_CLOSED;
        final String externalRecordId = "recordId";

        AnnotationDocumentEntity annotationDocument = createAnnotationDocumentEntity(annotation, fileName, fileType, fileSize, testUser,
                                                                                     uploadedDateTime, checksum, confidenceScore,
                                                                                     confidenceReason);

        ExternalObjectDirectoryEntity armEod = PersistableFactory.getExternalObjectDirectoryTestData()
            .someMinimalBuilder().annotationDocumentEntity(annotationDocument).media(null)
            .status(dartsDatabase.getObjectRecordStatusEntity(STORED))
            .externalLocationType(dartsDatabase.getExternalLocationTypeEntity(ARM))
            .externalLocation(UUID.randomUUID().toString()).build().getEntity();

        armEod.setExternalRecordId(externalRecordId);
        armEod.setEventDateTs(END);
        armEod.setUpdateRetention(true);
        armEod = dartsPersistence.save(armEod);
        String rawFilename = String.format("%s_%s_%s", armEod.getId(), annotationDocument.getId(), armEod.getTransferAttempts());

        // when
        AnnotationArchiveRecord annotationArchiveRecord = annotationArchiveRecordMapper.mapToAnnotationArchiveRecord(armEod, rawFilename);

        // then
        assertNotNull(annotationArchiveRecord);
        assertNotNull(annotationArchiveRecord.getArchiveRecordOperation());
        assertNotNull(annotationArchiveRecord.getUploadNewFileRecord());

        assertEquals(String.valueOf(armEod.getId()), annotationArchiveRecord.getAnnotationCreateArchiveRecordOperation().getRelationId());
        assertNotNull(annotationArchiveRecord.getAnnotationCreateArchiveRecordOperation().getRecordMetadata());

        assertEquals("upload_new_file", annotationArchiveRecord.getUploadNewFileRecord().getOperation());
        assertEquals(String.valueOf(armEod.getId()), annotationArchiveRecord.getUploadNewFileRecord().getRelationId());
        assertNotNull(annotationArchiveRecord.getUploadNewFileRecord().getFileMetadata());

        RecordMetadata metadata = annotationArchiveRecord.getAnnotationCreateArchiveRecordOperation().getRecordMetadata();
        assertEquals(armDataManagementConfiguration.getPublisher(), metadata.getPublisher());
        assertEquals(armDataManagementConfiguration.getMediaRecordClass(), metadata.getRecordClass());
        assertNotNull(metadata.getRecordDate());
        assertNotNull(metadata.getEventDate());
        assertEquals(armDataManagementConfiguration.getRegion(), metadata.getRegion());
        assertEquals(annotationDocument.getFileName(), metadata.getTitle());
        assertEquals(String.valueOf(armEod.getId()), metadata.getClientId());

        assertMetadata(metadata, annotationDocument, armEod);

    }

    private AnnotationDocumentEntity createAnnotationDocumentEntity(AnnotationEntity annotation, String fileName, String fileType, int fileSize,
                                                                    UserAccountEntity testUser, OffsetDateTime uploadedDateTime, String checksum,
                                                                    RetentionConfidenceScoreEnum confidenceScore, String confidenceReason) {
        AnnotationDocumentEntity annotationDocument = PersistableFactory
            .getAnnotationDocumentTestData().someMinimalBuilder().annotation(annotation)
            .fileName(fileName)
            .fileType(fileType)
            .fileSize(fileSize)
            .lastModifiedBy(testUser)
            .lastModifiedTimestamp(uploadedDateTime)
            .checksum(checksum)
            .retConfScore(confidenceScore)
            .retConfReason(confidenceReason)
            .retainUntilTs(END).build().getEntity();

        return dartsPersistence.save(annotationDocument);
    }

    private static void assertMetadata(RecordMetadata metadata, AnnotationDocumentEntity annotationDocumentEntity, ExternalObjectDirectoryEntity eod) {
        assertEquals("Annotation", metadata.getBf001());
        assertNull(metadata.getBf002());
        assertEquals(annotationDocumentEntity.getFileType(), metadata.getBf003());
        assertNull(metadata.getBf004());
        assertEquals(annotationDocumentEntity.getChecksum(), metadata.getBf005());
        assertNull(metadata.getBf006());
        assertNull(metadata.getBf007());
        assertNull(metadata.getBf008());
        assertEquals(annotationDocumentEntity.getAnnotation().getText(), metadata.getBf009());
        assertNotNull(metadata.getBf010());
        assertNull(metadata.getBf011());
        assertEquals(eod.getId(), metadata.getBf012());
        assertEquals(annotationDocumentEntity.getId(), metadata.getBf013());
        assertNull(metadata.getBf014());
        assertNull(metadata.getBf015());
        assertEquals(String.valueOf(annotationDocumentEntity.getUploadedBy().getId()), metadata.getBf016());
        assertNotNull(metadata.getBf017());
        assertNull(metadata.getBf018());
        assertEquals("TESTCOURTHOUSE", metadata.getBf019());
        assertEquals("TESTCOURTROOM", metadata.getBf020());
    }
}
