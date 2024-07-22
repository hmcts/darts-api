package uk.gov.hmcts.darts.common.repository;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.Pageable;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.helper.SystemUserHelper;
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
    void testSingleUpdateDirectoryIfArmMediaDateExistedFor24HoursOrBeyond() throws Exception {

        int numberOfRecordsToGenerate = 10;
        int setupHoursBeforeCurrentTime = 25;

        // setup the test data
        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntitiesNotRelevant
            = externalObjectDirectoryStub.generateWithStatusAndInboundLocation(ObjectRecordStatusEnum.ARM_RAW_DATA_FAILED, numberOfRecordsToGenerate);
        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntities
            = externalObjectDirectoryStub.generateWithStatusAndInboundLocation(STORED, numberOfRecordsToGenerate);

        // set half media over threshold and half not
        List<ExternalObjectDirectoryEntity> expectedArmRecordsResultOutside24Hours
            = externalObjectDirectoryStub.generateWithStatusAndMediaAndArmLocation(
                externalObjectDirectoryEntities.subList(0, externalObjectDirectoryEntities.size() / 2), setupHoursBeforeCurrentTime);
        List<ExternalObjectDirectoryEntity> expectedArmRecordsResultWithinTheHour
            = externalObjectDirectoryStub.generateWithStatusAndMediaAndArmLocation(
                externalObjectDirectoryEntities.subList(externalObjectDirectoryEntities.size() / 2, externalObjectDirectoryEntities.size()), 1);

        int expectedRecords = externalObjectDirectoryEntitiesNotRelevant.size() + externalObjectDirectoryEntities.size()
            + expectedArmRecordsResultOutside24Hours.size() + expectedArmRecordsResultWithinTheHour.size();

        // assert that the test has inserted the data into the database
        Assertions.assertEquals(expectedRecords, externalObjectDirectoryRepository.findAll().size());

        int pageSize =  1;
        int hourDurationBeyondHours = setupHoursBeforeCurrentTime; // which no records are

        // exercise the logic
        List<Integer> results = externalObjectDirectoryRepository
            .findAllArmMediaBeforeOrEqualDate(Pageable.ofSize(pageSize),
                                              ExternalLocationTypeEnum.INBOUND.getId(), getCurrentDateTimeWithHoursBefore(hourDurationBeyondHours));

        // assert we have returned one of the expected
        assertExpectedResults(results, expectedArmRecordsResultOutside24Hours, pageSize);

        // now update the records
        List<Integer> updatedResults = externalObjectDirectoryStub.updateExternalDirectoryWithMarkedForDeletionUsingHouseKeeperUser(results);

        // assert the update has occurred
        assertExternalObjectDirectoryUpdate(updatedResults, expectedArmRecordsResultOutside24Hours, pageSize);
    }

    @Test
    void testBatchUpdateAllDirectoryIfArmMediaExistedFor24HoursOrBeyond() throws Exception {

        int numberOfRecordsToGenerate = 10;
        int setupHoursBeforeCurrentTime = 24;

        // setup the test data
        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntitiesNotRelevant
            = externalObjectDirectoryStub.generateWithStatusAndInboundLocation(ObjectRecordStatusEnum.ARM_RAW_DATA_FAILED, numberOfRecordsToGenerate);
        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntities
            = externalObjectDirectoryStub.generateWithStatusAndInboundLocation(STORED, numberOfRecordsToGenerate);

        List<ExternalObjectDirectoryEntity> expectedArmRecordsResultOutside24Hours
            = externalObjectDirectoryStub.generateWithStatusAndMediaAndArmLocation(
                externalObjectDirectoryEntities.subList(0, externalObjectDirectoryEntities.size() / 2), setupHoursBeforeCurrentTime);
        List<ExternalObjectDirectoryEntity> expectedArmRecordsResultWithinTheHour
            = externalObjectDirectoryStub.generateWithStatusAndMediaAndArmLocation(
                externalObjectDirectoryEntities.subList(externalObjectDirectoryEntities.size() / 2, externalObjectDirectoryEntities.size()), 1);

        int expectedRecords = externalObjectDirectoryEntitiesNotRelevant.size() + externalObjectDirectoryEntities.size()
            + expectedArmRecordsResultOutside24Hours.size() + expectedArmRecordsResultWithinTheHour.size();

        // assert that the test has inserted the data into the database
        Assertions.assertEquals(expectedRecords, externalObjectDirectoryRepository.findAll().size());

        int pageSize = 5;
        int hourDurationBeyondHours = setupHoursBeforeCurrentTime; // which no records are

        // exercise the logic
        List<Integer> results = externalObjectDirectoryRepository
            .findAllArmMediaBeforeOrEqualDate(Pageable.ofSize(pageSize),
                                              ExternalLocationTypeEnum.INBOUND.getId(), getCurrentDateTimeWithHoursBefore(hourDurationBeyondHours));

        // assert we have returned the expected records
        assertExpectedResults(results, expectedArmRecordsResultOutside24Hours, pageSize);

        // now update the records
        List<Integer> updatedResults = externalObjectDirectoryStub.updateExternalDirectoryWithMarkedForDeletionUsingHouseKeeperUser(results);

        // assert we have updated the records
        assertExternalObjectDirectoryUpdate(updatedResults, expectedArmRecordsResultOutside24Hours, pageSize);

        // we should not find any more records for processing
        results = externalObjectDirectoryRepository
            .findAllArmMediaBeforeOrEqualDate(Pageable.ofSize(pageSize),
                                              ExternalLocationTypeEnum.INBOUND.getId(), getCurrentDateTimeWithHoursBefore(hourDurationBeyondHours));
        
        Assertions.assertTrue(results.isEmpty());
    }

    @Test
    void testGetDirectoryIfMediaDate24Hours() throws Exception {

        int numberOfRecordsToGenerate = 10;
        int setupHoursBeforeCurrentTime = 24;

        // setup the test data
        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntitiesNotRelevant
            = externalObjectDirectoryStub.generateWithStatusAndInboundLocation(ObjectRecordStatusEnum.ARM_RAW_DATA_FAILED, numberOfRecordsToGenerate);
        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntities
            = externalObjectDirectoryStub.generateWithStatusAndInboundLocation(STORED, numberOfRecordsToGenerate);
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

        int pageSize = 2;
        int hourDurationBeyondHours = setupHoursBeforeCurrentTime; // which no records are

        // excerise the logic
        List<Integer> results = externalObjectDirectoryRepository
            .findAllArmMediaBeforeOrEqualDate(Pageable.ofSize(pageSize),
                                              ExternalLocationTypeEnum.INBOUND.getId(), getCurrentDateTimeWithHoursBefore(hourDurationBeyondHours));

        // assert the logic
        assertExpectedResults(results, armRecordsResultOutside24Hours, pageSize);
    }

    @Test
    void testGetDirectoryIfMediaDateBeyond24Hours() throws Exception {

        int numberOfRecordsToGenerate = 10;
        int setupHoursBeforeCurrentTime = 26;

        // setup the test data
        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntitiesNotRelevant
            = externalObjectDirectoryStub.generateWithStatusAndInboundLocation(ObjectRecordStatusEnum.ARM_RAW_DATA_FAILED, numberOfRecordsToGenerate);
        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntities
            = externalObjectDirectoryStub.generateWithStatusAndInboundLocation(STORED, numberOfRecordsToGenerate);
        List<ExternalObjectDirectoryEntity> armRecordsResultOutside24Hours
            = externalObjectDirectoryStub.generateWithStatusAndMediaAndArmLocation(
                externalObjectDirectoryEntities.subList(0, externalObjectDirectoryEntities.size() / 2),setupHoursBeforeCurrentTime);
        List<ExternalObjectDirectoryEntity> armRecordsResultWithinTheHour
            = externalObjectDirectoryStub.generateWithStatusAndMediaAndArmLocation(
                externalObjectDirectoryEntities.subList(externalObjectDirectoryEntities.size() / 2, externalObjectDirectoryEntities.size()), 2);

        int expectedRecords = externalObjectDirectoryEntitiesNotRelevant.size()
            + externalObjectDirectoryEntities.size()
            + armRecordsResultOutside24Hours.size() + armRecordsResultWithinTheHour.size();

        // assert that the test has inserted the data into the database
        Assertions.assertEquals(expectedRecords, externalObjectDirectoryRepository.findAll().size());

        int pageSize = 2;
        int hourDurationBeyondHours = setupHoursBeforeCurrentTime; // which no records are

        // excerise the logic
        List<Integer> results = externalObjectDirectoryRepository
            .findAllArmMediaBeforeOrEqualDate(Pageable.ofSize(pageSize),
                                              ExternalLocationTypeEnum.INBOUND.getId(), getCurrentDateTimeWithHoursBefore(hourDurationBeyondHours));

        // assert the logic
        assertExpectedResults(results, armRecordsResultOutside24Hours, pageSize);
    }

    @Test
    void testGetDirectoryIfMediaDateNotBeyondThreshold() throws Exception {

        int numberOfRecordsToGenerate = 10;

        // setup the test data
        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntitiesNotRelevant
            = externalObjectDirectoryStub.generateWithStatusAndInboundLocation(ObjectRecordStatusEnum.ARM_RAW_DATA_FAILED, numberOfRecordsToGenerate);
        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntities
            = externalObjectDirectoryStub.generateWithStatusAndInboundLocation(STORED, numberOfRecordsToGenerate);

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

        int pageSize = 2;
        int hourDurationBeyondHours = 24; // which no records are

        // excerise the logic
        List<Integer> results = externalObjectDirectoryRepository
            .findAllArmMediaBeforeOrEqualDate(Pageable.ofSize(pageSize),
                                              ExternalLocationTypeEnum.INBOUND.getId(), getCurrentDateTimeWithHoursBefore(hourDurationBeyondHours));

        // assert the logic
        Assertions.assertTrue(results.isEmpty());
    }

    private void assertExternalObjectDirectoryUpdate(List<Integer> actualResults, List<ExternalObjectDirectoryEntity> expectedResults, int resultCount) {
        // find matching pn expected results
        List<Integer> matchesEntity = new ArrayList<>(
            actualResults.stream().filter(expectedResult -> expectedResults.stream().anyMatch(result -> expectedResult.equals(result.getId()))).toList());

        Assertions.assertEquals(resultCount, matchesEntity.size());

        Assertions.assertTrue(externalObjectDirectoryStub.areObjectDirectoriesMarkedForDeletionWithHousekeeper(actualResults));
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