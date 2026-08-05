package uk.gov.hmcts.darts.common.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.arm.service.impl.CleanUpDetsDataProcessorImpl;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectStateRecordEntity;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.ARM;
import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.DETS;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;
import static uk.gov.hmcts.darts.test.common.data.PersistableFactory.getMediaTestData;

class ObjectStateRecordRepositoryIntTest extends PostgresIntegrationBase {

    @Autowired
    private ObjectStateRecordRepository objectStateRecordRepository;

    private static final LocalDateTime HEARING_DATE = LocalDateTime.of(2023, 9, 26, 10, 0, 0);

    private MediaEntity media1;
    private MediaEntity media2;
    private MediaEntity media3;
    private MediaEntity media4;
    private MediaEntity media5;
    private MediaEntity media6;

    @BeforeEach
    void setUp() {

        HearingEntity hearing = dartsDatabase.createHearing(
            "Bristol",
            "Int Test Courtroom 1",
            "1",
            HEARING_DATE
        );

        media1 = dartsPersistence.save(
            getMediaTestData().createMediaWith(
                hearing.getCourtroom(),
                OffsetDateTime.parse("2023-09-26T13:00:00Z"),
                OffsetDateTime.parse("2023-09-26T13:45:00Z"),
                1
            ));
        media1 = dartsDatabase.save(media1);

        media2 = dartsPersistence.save(
            getMediaTestData().createMediaWith(
                hearing.getCourtroom(),
                OffsetDateTime.parse("2023-09-26T13:00:00Z"),
                OffsetDateTime.parse("2023-09-26T13:45:00Z"),
                2
            ));
        media2 = dartsDatabase.save(media2);

        media3 = dartsPersistence.save(
            getMediaTestData().createMediaWith(
                hearing.getCourtroom(),
                OffsetDateTime.parse("2023-09-26T13:00:00Z"),
                OffsetDateTime.parse("2023-09-26T13:45:00Z"),
                3
            ));
        media3 = dartsDatabase.save(media3);

        media4 = dartsPersistence.save(
            getMediaTestData().createMediaWith(
                hearing.getCourtroom(),
                OffsetDateTime.parse("2023-09-26T13:00:00Z"),
                OffsetDateTime.parse("2023-09-26T13:45:00Z"),
                4
            ));
        media4 = dartsDatabase.save(media4);

        media5 = dartsPersistence.save(
            getMediaTestData().createMediaWith(
                hearing.getCourtroom(),
                OffsetDateTime.parse("2023-09-27T13:00:00Z"),
                OffsetDateTime.parse("2023-09-27T13:45:00Z"),
                1
            ));
        media5 = dartsDatabase.save(media5);

        media6 = dartsPersistence.save(
            getMediaTestData().createMediaWith(
                hearing.getCourtroom(),
                OffsetDateTime.parse("2023-09-27T13:00:00Z"),
                OffsetDateTime.parse("2023-09-27T13:45:00Z"),
                2
            ));
        media6 = dartsDatabase.save(media6);

    }

    @Test
    void cleanUpDetsDataProcedure_shouldReturnData() {
        // OSR 1
        ObjectStateRecordEntity objectStateRecordEntity1 = dartsDatabase.getObjectStateRecordRepository()
            .save(createObjectStateRecordEntity(111L));
        dartsDatabase.getObjectStateRecordRepository().save(objectStateRecordEntity1);

        var detsEod1 = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            media1,
            STORED,
            DETS,
            UUID.randomUUID().toString()
        );
        detsEod1.setOsrUuid(objectStateRecordEntity1.getUuid());
        dartsDatabase.save(detsEod1);

        var armEod1 = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            media1,
            STORED,
            ARM,
            detsEod1.getExternalLocation() // must match DETS external_location == osr.dets_location
        );
        armEod1.setInputUploadProcessedTs(OffsetDateTime.now().minusHours(26));
        armEod1.setOsrUuid(objectStateRecordEntity1.getUuid());
        dartsDatabase.save(armEod1);

        objectStateRecordEntity1.setEodId(detsEod1.getId());
        objectStateRecordEntity1.setArmEodId(armEod1.getId());
        objectStateRecordEntity1.setDetsLocation(detsEod1.getExternalLocation());
        dartsDatabase.getObjectStateRecordRepository().saveAndFlush(objectStateRecordEntity1);

        // OSR 2
        ObjectStateRecordEntity objectStateRecordEntity2 = dartsDatabase.getObjectStateRecordRepository()
            .save(createObjectStateRecordEntity(222L));
        dartsDatabase.getObjectStateRecordRepository().save(objectStateRecordEntity2);

        var detsEod2 = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            media2,
            STORED,
            DETS,
            UUID.randomUUID().toString()
        );
        detsEod2.setOsrUuid(objectStateRecordEntity2.getUuid());
        dartsDatabase.save(detsEod2);

        var armEod2 = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            media2,
            STORED,
            ARM,
            detsEod2.getExternalLocation()
        );
        armEod2.setInputUploadProcessedTs(OffsetDateTime.now().minusHours(6));
        armEod2.setOsrUuid(objectStateRecordEntity2.getUuid());
        dartsDatabase.save(armEod2);

        objectStateRecordEntity2.setEodId(detsEod2.getId());
        objectStateRecordEntity2.setArmEodId(armEod2.getId());
        objectStateRecordEntity2.setDetsLocation(detsEod2.getExternalLocation());
        dartsDatabase.getObjectStateRecordRepository().saveAndFlush(objectStateRecordEntity2);

        // OSR 3
        ObjectStateRecordEntity objectStateRecordEntity3 = dartsDatabase.getObjectStateRecordRepository()
            .save(createObjectStateRecordEntity(333L));
        dartsDatabase.getObjectStateRecordRepository().save(objectStateRecordEntity3);

        var detsEod3 = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            media3,
            STORED,
            DETS,
            UUID.randomUUID().toString()
        );
        detsEod3.setOsrUuid(objectStateRecordEntity3.getUuid());
        dartsDatabase.save(detsEod3);

        var armEod3 = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            media3,
            STORED,
            ARM,
            detsEod3.getExternalLocation()
        );
        armEod3.setInputUploadProcessedTs(OffsetDateTime.now().minusHours(30));
        armEod3.setOsrUuid(objectStateRecordEntity3.getUuid());
        dartsDatabase.getExternalObjectDirectoryRepository().saveAndFlush(armEod3);

        objectStateRecordEntity3.setEodId(detsEod3.getId());
        objectStateRecordEntity3.setArmEodId(armEod3.getId());
        objectStateRecordEntity3.setDetsLocation(detsEod3.getExternalLocation());
        dartsDatabase.getObjectStateRecordRepository().saveAndFlush(objectStateRecordEntity3);

        // Noise records that should not be returned
        var externalObjectDirectoryEntity4 = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            media4,
            STORED,
            ARM,
            UUID.randomUUID().toString()
        );
        externalObjectDirectoryEntity4.setInputUploadProcessedTs(OffsetDateTime.now().minusHours(28));
        dartsDatabase.save(externalObjectDirectoryEntity4);

        var externalObjectDirectoryEntity5 = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            media5,
            STORED,
            ARM,
            UUID.randomUUID().toString()
        );
        dartsDatabase.save(externalObjectDirectoryEntity5);
        dartsDatabase.getExternalObjectDirectoryRepository().delete(externalObjectDirectoryEntity5);

        var externalObjectDirectoryEntity6 = dartsDatabase.getExternalObjectDirectoryStub().createExternalObjectDirectory(
            media6,
            STORED,
            ARM,
            UUID.randomUUID().toString()
        );
        externalObjectDirectoryEntity6.setInputUploadProcessedTs(OffsetDateTime.now().minusHours(30));
        dartsDatabase.save(externalObjectDirectoryEntity6);

        OffsetDateTime lastModifiedBefore = OffsetDateTime.now().plusHours(1);

        List<CleanUpDetsDataProcessorImpl.CleanUpDetsProcedureResponse> cleanUpDetsProcedureResponses =
            objectStateRecordRepository.cleanUpDetsDataProcedure(10, lastModifiedBefore);

        assertThat(cleanUpDetsProcedureResponses).hasSize(3);
    }

    private ObjectStateRecordEntity createObjectStateRecordEntity(long osrUuid) {
        ObjectStateRecordEntity objectStateRecordEntity = new ObjectStateRecordEntity();
        objectStateRecordEntity.setUuid(osrUuid);
        return objectStateRecordEntity;
    }
}
