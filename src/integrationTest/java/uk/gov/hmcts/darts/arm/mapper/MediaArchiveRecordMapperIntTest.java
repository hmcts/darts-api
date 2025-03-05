package uk.gov.hmcts.darts.arm.mapper;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import uk.gov.hmcts.darts.arm.api.ArmDataManagementApi;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.model.record.MediaArchiveRecord;
import uk.gov.hmcts.darts.arm.model.record.metadata.RecordMetadata;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectStateRecordEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RAW_DATA_PUSHED;

@Slf4j
class MediaArchiveRecordMapperIntTest extends IntegrationBase {

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
    private MediaArchiveRecordMapper mediaArchiveRecordMapper;

    @BeforeEach
    void setupData() {
        UserAccountEntity testUser = dartsDatabase.getUserAccountStub().getIntegrationTestUserAccountEntity();
        when(userIdentity.getUserAccount()).thenReturn(testUser);
    }

    @Test
    void mediaArchiveRecordMapper_Success() throws Exception {
        // given
        OffsetDateTime validDateTime = OffsetDateTime.now().minusMinutes(20);

        var eods = dartsDatabase.getExternalObjectDirectoryStub().generateWithStatusAndMediaLocation(
            ExternalLocationTypeEnum.ARM, ARM_RAW_DATA_PUSHED, 1, Optional.of(validDateTime));
        eods.forEach(eod -> {
            ObjectStateRecordEntity objectStateRecordEntity = dartsDatabase.getObjectStateRecordRepository()
                .save(createObjectStateRecordEntity(Long.valueOf(eod.getId())));
            objectStateRecordEntity.setEodId(String.valueOf(eod.getId()));
            dartsDatabase.getObjectStateRecordRepository().save(objectStateRecordEntity);

            eod.setOsrUuid(objectStateRecordEntity.getUuid());
            eod.setTransferAttempts(1);

            eod.getMedia().setCreatedDateTime(START);
            eod.getMedia().setStart(START);
            eod.getMedia().setEnd(END);
            dartsPersistence.getExternalObjectDirectoryRepository().saveAndFlush(eod);
        });

        var eod = eods.getFirst();
        MediaEntity media = eod.getMedia();
        String rawFilename = String.format("%s_%s_%s", eod.getId(), media.getId(), eod.getTransferAttempts());

        // when
        MediaArchiveRecord mediaArchiveRecord = mediaArchiveRecordMapper.mapToMediaArchiveRecord(eod, rawFilename);

        // then
        assertNotNull(mediaArchiveRecord);
        assertNotNull(mediaArchiveRecord.getMediaCreateArchiveRecord());
        assertNotNull(mediaArchiveRecord.getUploadNewFileRecord());

        assertEquals(String.valueOf(eod.getId()), mediaArchiveRecord.getMediaCreateArchiveRecord().getRelationId());
        assertNotNull(mediaArchiveRecord.getMediaCreateArchiveRecord().getRecordMetadata());

        assertEquals("upload_new_file", mediaArchiveRecord.getUploadNewFileRecord().getOperation());
        assertEquals(String.valueOf(eod.getId()), mediaArchiveRecord.getUploadNewFileRecord().getRelationId());
        assertNotNull(mediaArchiveRecord.getUploadNewFileRecord().getFileMetadata());

        RecordMetadata metadata = mediaArchiveRecord.getMediaCreateArchiveRecord().getRecordMetadata();
        assertEquals(armDataManagementConfiguration.getPublisher(), metadata.getPublisher());
        assertEquals(armDataManagementConfiguration.getMediaRecordClass(), metadata.getRecordClass());
        assertNotNull(metadata.getRecordDate());
        assertNotNull(metadata.getEventDate());
        assertEquals(armDataManagementConfiguration.getRegion(), metadata.getRegion());
        assertEquals(media.getMediaFile(), metadata.getTitle());
        assertEquals(String.valueOf(eod.getId()), metadata.getClientId());

        assertMetadata(metadata, media, eod);

    }

    private static void assertMetadata(RecordMetadata metadata, MediaEntity media, ExternalObjectDirectoryEntity eod) {
        assertEquals("Media", metadata.getBf001());
        assertNull(metadata.getBf002());
        assertEquals(media.getMediaFormat(), metadata.getBf003());
        assertNull(metadata.getBf004());
        assertEquals(media.getChecksum(), metadata.getBf005());
        assertNull(metadata.getBf006());
        assertNull(metadata.getBf007());
        assertNull(metadata.getBf008());
        assertNull(metadata.getBf009());
        assertEquals(T_10_30_00_Z, metadata.getBf010());
        assertEquals(T_10_30_00_Z, metadata.getBf011());
        assertEquals(eod.getId(), metadata.getBf012());
        assertEquals(media.getId(), metadata.getBf013());
        assertEquals(media.getChannel(), metadata.getBf014());
        assertEquals(media.getTotalChannels(), metadata.getBf015());
        assertNull(metadata.getBf016());
        assertEquals(T_11_30_00_Z, metadata.getBf017());
        assertNull(metadata.getBf018());
        assertEquals("TESTCOURTHOUSE", metadata.getBf019());
        assertEquals("TESTCOURTROOM", metadata.getBf020());
    }

    private ObjectStateRecordEntity createObjectStateRecordEntity(Long uuid) {
        ObjectStateRecordEntity objectStateRecordEntity = new ObjectStateRecordEntity();
        objectStateRecordEntity.setUuid(uuid);
        return objectStateRecordEntity;
    }
}
