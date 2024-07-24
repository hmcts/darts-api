package uk.gov.hmcts.darts.common.repository;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.auditing.DateTimeProvider;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.helper.SystemUserHelper;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.ExternalObjectDirectoryStub;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;

class ExternalObjectDirectoryRepositoryTest  extends PostgresIntegrationBase {

    @Autowired
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;

    @Autowired
    private ExternalObjectDirectoryStub externalObjectDirectoryStub;

    @Autowired
    private SystemUserHelper systemUserHelper;

    @MockBean
    private DateTimeProvider dateTimeProvider;

    @Autowired
    private ObjectRecordStatusRepository objectRecordStatusRepository;

    @Autowired
    private CurrentTimeHelper currentTimeHelper;

    @Test
    void testGetDirectoryIfMediaDate24Hours() throws Exception {

        int numberOfRecordsToGenerate = 10;
        int setupHoursBeforeCurrentTime = 24;

        // setup the test data
        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntitiesNotRelevant
            = externalObjectDirectoryStub.generateWithStatusAndMediaAndInboundLocation(ObjectRecordStatusEnum.ARM_RAW_DATA_FAILED, numberOfRecordsToGenerate);
        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntities
            = externalObjectDirectoryStub.generateWithStatusAndMediaAndInboundLocation(STORED, numberOfRecordsToGenerate);
        List<ExternalObjectDirectoryEntity> entitiesToBeMarkedWithMediaOutsideOfHours
            = externalObjectDirectoryEntities.subList(0, externalObjectDirectoryEntities.size() / 2);
        List<ExternalObjectDirectoryEntity> armRecordsResultOutside24Hours
            = externalObjectDirectoryStub.generateWithStatusAndMediaAndArmLocation(
            entitiesToBeMarkedWithMediaOutsideOfHours,setupHoursBeforeCurrentTime);
        List<ExternalObjectDirectoryEntity> armRecordsResultWithinTheHour
            = externalObjectDirectoryStub.generateWithStatusAndMediaAndArmLocation(
                externalObjectDirectoryEntities.subList(externalObjectDirectoryEntities.size() / 2, externalObjectDirectoryEntities.size()), 2);

        int expectedRecords = externalObjectDirectoryEntitiesNotRelevant.size() + externalObjectDirectoryEntities.size()
            + armRecordsResultOutside24Hours.size() + armRecordsResultWithinTheHour.size();

        // assert that the test has inserted the data into the database
        Assertions.assertEquals(expectedRecords, externalObjectDirectoryRepository.findAll().size());

        int hourDurationBeyondHours = setupHoursBeforeCurrentTime; // which no records are

        // excerise the logic
        List<Integer> results = externalObjectDirectoryRepository
            .findIdsIn2StorageLocationsBeforeTime(
                EodHelper.storedStatus(), EodHelper.storedStatus(),
                EodHelper.inboundLocation(), EodHelper.armLocation(),
                getCurrentDateTimeWithHoursBefore(hourDurationBeyondHours), ExternalObjectDirectoryQueryTypeEnum.MEDIA_QUERY.getIndex());

        // assert the logic
        assertExpectedResults(results, entitiesToBeMarkedWithMediaOutsideOfHours, entitiesToBeMarkedWithMediaOutsideOfHours.size());
    }

    @Test
    void testGetDirectoryIfMediaDateBeyond24Hours() throws Exception {

        int numberOfRecordsToGenerate = 10;
        int setupHoursBeforeCurrentTime = 26;

        // setup the test data
        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntitiesNotRelevant
            = externalObjectDirectoryStub.generateWithStatusAndMediaAndInboundLocation(ObjectRecordStatusEnum.ARM_RAW_DATA_FAILED, numberOfRecordsToGenerate);
        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntities
            = externalObjectDirectoryStub.generateWithStatusAndMediaAndInboundLocation(STORED, numberOfRecordsToGenerate);

        List<ExternalObjectDirectoryEntity> entitiesToBeMarkedWithMediaOutsideOfHours
            = externalObjectDirectoryEntities.subList(0, externalObjectDirectoryEntities.size() / 2);

        List<ExternalObjectDirectoryEntity> armRecordsResultOutside24Hours
            = externalObjectDirectoryStub.generateWithStatusAndMediaAndArmLocation(
            entitiesToBeMarkedWithMediaOutsideOfHours,setupHoursBeforeCurrentTime);
        List<ExternalObjectDirectoryEntity> armRecordsResultWithinTheHour
            = externalObjectDirectoryStub.generateWithStatusAndMediaAndArmLocation(
                externalObjectDirectoryEntities.subList(externalObjectDirectoryEntities.size() / 2, externalObjectDirectoryEntities.size()), 2);

        int expectedRecords = externalObjectDirectoryEntitiesNotRelevant.size()
            + externalObjectDirectoryEntities.size()
            + armRecordsResultOutside24Hours.size() + armRecordsResultWithinTheHour.size();

        // assert that the test has inserted the data into the database
        Assertions.assertEquals(expectedRecords, externalObjectDirectoryRepository.findAll().size());

        int hourDurationBeyondHours = setupHoursBeforeCurrentTime; // which no records are

        // excerise the logic
        List<Integer> results = externalObjectDirectoryRepository
            .findIdsIn2StorageLocationsBeforeTime(EodHelper.storedStatus(), EodHelper.storedStatus(),
                                                  EodHelper.inboundLocation(), EodHelper.armLocation(),
                                                  getCurrentDateTimeWithHoursBefore(hourDurationBeyondHours),
                                                  ExternalObjectDirectoryQueryTypeEnum.MEDIA_QUERY.getIndex());

        // assert the logic
        assertExpectedResults(results, entitiesToBeMarkedWithMediaOutsideOfHours, entitiesToBeMarkedWithMediaOutsideOfHours.size());
    }

    @Test
    void testGetDirectoryIfAnnotationDate24Hours() throws Exception {

        int numberOfRecordsToGenerate = 10;
        int setupHoursBeforeCurrentTime = 24;

        // setup the test data
        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntitiesNotRelevant
            = externalObjectDirectoryStub
            .generateWithStatusAndTranscriptionAndAnnotationAndInboundLocation(ObjectRecordStatusEnum.ARM_RAW_DATA_FAILED, numberOfRecordsToGenerate);
        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntities
            = externalObjectDirectoryStub.generateWithStatusAndTranscriptionAndAnnotationAndInboundLocation(STORED, numberOfRecordsToGenerate);
        List<ExternalObjectDirectoryEntity> entitiesToBeMarkedWithMediaOutsideOfHours
            = externalObjectDirectoryEntities.subList(0, externalObjectDirectoryEntities.size() / 2);
        List<ExternalObjectDirectoryEntity> armRecordsResultOutside24Hours
            = externalObjectDirectoryStub.generateWithStatusAndTranscriptionAndAnnotationAndArmLocation(
            entitiesToBeMarkedWithMediaOutsideOfHours,setupHoursBeforeCurrentTime);
        List<ExternalObjectDirectoryEntity> armRecordsResultWithinTheHour
            = externalObjectDirectoryStub.generateWithStatusAndTranscriptionAndAnnotationAndArmLocation(
            externalObjectDirectoryEntities.subList(externalObjectDirectoryEntities.size() / 2, externalObjectDirectoryEntities.size()), 2);

        int expectedRecords = externalObjectDirectoryEntitiesNotRelevant.size() + externalObjectDirectoryEntities.size()
            + armRecordsResultOutside24Hours.size() + armRecordsResultWithinTheHour.size();

        // assert that the test has inserted the data into the database
        Assertions.assertEquals(expectedRecords, externalObjectDirectoryRepository.findAll().size());

        int hourDurationBeyondHours = setupHoursBeforeCurrentTime; // which no records are

        // excerise the logic
        List<Integer> results = externalObjectDirectoryRepository
            .findIdsIn2StorageLocationsBeforeTime(
                EodHelper.storedStatus(), EodHelper.storedStatus(),
                EodHelper.inboundLocation(), EodHelper.armLocation(),
                getCurrentDateTimeWithHoursBefore(hourDurationBeyondHours), ExternalObjectDirectoryQueryTypeEnum.ANNOTATION_QUERY.getIndex());

        // assert the logic
        assertExpectedResults(results, entitiesToBeMarkedWithMediaOutsideOfHours, entitiesToBeMarkedWithMediaOutsideOfHours.size());
    }

    @Test
    void testGetDirectoryIfMediaDateNotBeyondThreshold() throws Exception {

        int numberOfRecordsToGenerate = 10;

        // setup the test data
        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntitiesNotRelevant
            = externalObjectDirectoryStub.generateWithStatusAndMediaAndInboundLocation(ObjectRecordStatusEnum.ARM_RAW_DATA_FAILED, numberOfRecordsToGenerate);
        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntities
            = externalObjectDirectoryStub.generateWithStatusAndMediaAndInboundLocation(STORED, numberOfRecordsToGenerate);

        int setupHoursBeforeCurrentTime = 10;
        List<ExternalObjectDirectoryEntity> armRecordsResultOutside24Hours
            = externalObjectDirectoryStub.generateWithStatusAndMediaAndArmLocation(
                externalObjectDirectoryEntities.subList(0, externalObjectDirectoryEntities.size() / 2),setupHoursBeforeCurrentTime);
        List<ExternalObjectDirectoryEntity> armRecordsResultWithinTheHour
            = externalObjectDirectoryStub.generateWithStatusAndMediaAndArmLocation(
                externalObjectDirectoryEntities.subList(externalObjectDirectoryEntities.size() / 2, externalObjectDirectoryEntities.size()), 2);

        int expectedRecords = externalObjectDirectoryEntitiesNotRelevant.size() + externalObjectDirectoryEntities.size()
            + armRecordsResultOutside24Hours.size() + armRecordsResultWithinTheHour.size();

        // assert that the test has inserted the data into the database
        Assertions.assertEquals(expectedRecords, externalObjectDirectoryRepository.findAll().size());

        int hourDurationBeyondHours = 24; // which no records are

        // excerise the logic
        List<Integer> results = externalObjectDirectoryRepository
            .findIdsIn2StorageLocationsBeforeTime(
                EodHelper.storedStatus(), EodHelper.storedStatus(),
                EodHelper.inboundLocation(), EodHelper.armLocation(),
                getCurrentDateTimeWithHoursBefore(hourDurationBeyondHours),
                ExternalObjectDirectoryQueryTypeEnum.MEDIA_QUERY.getIndex());

        // assert the logic
        Assertions.assertTrue(results.isEmpty());
    }

    private void assertExpectedResults(List<Integer> actualResults, List<ExternalObjectDirectoryEntity> expectedResults, int resultCount) {
        List<Integer> matchesEntity = new ArrayList<>(
            actualResults.stream().filter(expectedResult -> expectedResults.stream().anyMatch(result -> expectedResult.equals(result.getId()))).toList());

        Assertions.assertEquals(resultCount, matchesEntity.size());
    }

    private OffsetDateTime getCurrentDateTimeWithHoursBefore(int hours) {
        return currentTimeHelper.currentOffsetDateTime().minus(
            hours,
            ChronoUnit.HOURS
        );
    }
}