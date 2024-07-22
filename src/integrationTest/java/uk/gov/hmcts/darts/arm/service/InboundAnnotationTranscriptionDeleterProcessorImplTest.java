package uk.gov.hmcts.darts.arm.service;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.ExternalObjectDirectoryStub;

import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;

class InboundAnnotationTranscriptionDeleterProcessorImplTest extends PostgresIntegrationBase {

    @Autowired
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;

    @Autowired
    private ExternalObjectDirectoryStub externalObjectDirectoryStub;

    @Autowired
    private InboundAnnotationTranscriptionDeleterProcessor armTranscriptionAndAnnotationDeleterProcessor;

    @Test
    void processBatchSingleRecords() throws Exception {
        int numberOfRecordsToGenerate = 10;
        int setupHoursBeforeCurrentTime = 24;

        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntitiesNotRelevant
            = externalObjectDirectoryStub.generateWithStatusAndInboundLocation(
            ObjectRecordStatusEnum.ARM_RAW_DATA_FAILED, 10);
        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntities
            = externalObjectDirectoryStub.generateWithStatusAndInboundLocation(STORED, numberOfRecordsToGenerate);

        List<ExternalObjectDirectoryEntity> expectedArmRecordsResultOutsideHours
            = externalObjectDirectoryStub.generateWithStatusAndMediaAndArmLocation(
                externalObjectDirectoryEntities.subList(0, externalObjectDirectoryEntities.size() / 2), setupHoursBeforeCurrentTime);
        List<ExternalObjectDirectoryEntity> expectedArmRecordsResultWithinTheHour
            = externalObjectDirectoryStub.generateWithStatusAndMediaAndArmLocation(
                externalObjectDirectoryEntities.subList(externalObjectDirectoryEntities.size() / 2, externalObjectDirectoryEntities.size()), 1);

        int expectedRecords = externalObjectDirectoryEntitiesNotRelevant.size() + externalObjectDirectoryEntities.size()
            + expectedArmRecordsResultOutsideHours.size() + expectedArmRecordsResultWithinTheHour.size();

        // assert that the test has inserted the data into the database
        Assertions.assertEquals(expectedRecords, externalObjectDirectoryRepository.findAll().size());

        int pageSize = 5;
        int hourDurationBeyondHours = setupHoursBeforeCurrentTime; // which no records are


        // excerise the logic
        List<Integer> updatedResults = armTranscriptionAndAnnotationDeleterProcessor.processDeletionIfPreceding(pageSize, hourDurationBeyondHours);

        // assert the logic
        assertExpectedResults(updatedResults, expectedArmRecordsResultOutsideHours, pageSize);

        // assert the logic
        assertExternalObjectDirectoryUpdate(updatedResults, expectedArmRecordsResultOutsideHours, pageSize);

        updatedResults = armTranscriptionAndAnnotationDeleterProcessor.processDeletionIfPreceding(pageSize, hourDurationBeyondHours);

        Assertions.assertTrue(updatedResults.isEmpty());
    }

    @Test
    void processBatchMultipleRecords() throws Exception {
        int numberOfRecordsToGenerate = 10;
        int setupHoursBeforeCurrentTime = 25;

        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntitiesNotRelevant
            = externalObjectDirectoryStub.generateWithStatusAndInboundLocation(
            ObjectRecordStatusEnum.ARM_RAW_DATA_FAILED, numberOfRecordsToGenerate);
        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntities
            = externalObjectDirectoryStub.generateWithStatusAndInboundLocation(STORED, numberOfRecordsToGenerate);

        List<ExternalObjectDirectoryEntity> expectedArmRecordsResultOutsideHours
            = externalObjectDirectoryStub.generateWithStatusAndMediaAndArmLocation(
                externalObjectDirectoryEntities.subList(0, externalObjectDirectoryEntities.size() / 2), setupHoursBeforeCurrentTime);
        List<ExternalObjectDirectoryEntity> expectedArmRecordsResultWithinTheHour
            = externalObjectDirectoryStub.generateWithStatusAndMediaAndArmLocation(
                externalObjectDirectoryEntities.subList(externalObjectDirectoryEntities.size() / 2, externalObjectDirectoryEntities.size()), 1);

        int expectedRecords = externalObjectDirectoryEntitiesNotRelevant.size() + externalObjectDirectoryEntities.size()
            + expectedArmRecordsResultOutsideHours.size() + expectedArmRecordsResultWithinTheHour.size();

        // assert that the test has inserted the data into the database
        Assertions.assertEquals(expectedRecords, externalObjectDirectoryRepository.findAll().size());

        int pageSize = 1;
        int hourDurationBeyondHours = setupHoursBeforeCurrentTime; // which no records are

        // exercise the logic
        List<Integer> updatedResults = armTranscriptionAndAnnotationDeleterProcessor.processDeletionIfPreceding(pageSize, hourDurationBeyondHours);

        // assert the logic
        assertExpectedResults(updatedResults, expectedArmRecordsResultOutsideHours, pageSize);

        // assert the logic
        assertExternalObjectDirectoryUpdate(updatedResults, expectedArmRecordsResultOutsideHours, 1);

        List<Integer> updatedResults2 = armTranscriptionAndAnnotationDeleterProcessor.processDeletionIfPreceding(pageSize, hourDurationBeyondHours);

        // assert the logic
        assertExpectedResults(updatedResults2, expectedArmRecordsResultOutsideHours, pageSize);

        // assert the update
        assertExternalObjectDirectoryUpdate(updatedResults2, expectedArmRecordsResultOutsideHours, pageSize);

        // check the results are unique between processing
        Assertions.assertFalse(CollectionUtils.containsAny(updatedResults, updatedResults2));

        externalObjectDirectoryStub.checkNotMarkedForDeletion(expectedArmRecordsResultWithinTheHour);
    }

    @Test
    void processBatchMultipleRecordsWithSpringInjected24HourDurationThreshold() throws Exception {
        int numberOfRecordsToGenerate = 10;

        // assume that spring config is 24 hours
        int setupHoursBeforeCurrentTime = 24;

        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntitiesNotRelevant
            = externalObjectDirectoryStub.generateWithStatusAndInboundLocation(
            ObjectRecordStatusEnum.ARM_RAW_DATA_FAILED, numberOfRecordsToGenerate);
        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntities
            = externalObjectDirectoryStub.generateWithStatusAndInboundLocation(STORED, numberOfRecordsToGenerate);

        List<ExternalObjectDirectoryEntity> expectedArmRecordsResultOutsideHours
            = externalObjectDirectoryStub.generateWithStatusAndMediaAndArmLocation(
            externalObjectDirectoryEntities.subList(0, externalObjectDirectoryEntities.size() / 2), setupHoursBeforeCurrentTime);
        List<ExternalObjectDirectoryEntity> expectedArmRecordsResultWithinTheHour
            = externalObjectDirectoryStub.generateWithStatusAndMediaAndArmLocation(
            externalObjectDirectoryEntities.subList(externalObjectDirectoryEntities.size() / 2, externalObjectDirectoryEntities.size()), 1);

        int expectedRecords = externalObjectDirectoryEntitiesNotRelevant.size() + externalObjectDirectoryEntities.size()
            + expectedArmRecordsResultOutsideHours.size() + expectedArmRecordsResultWithinTheHour.size();

        // assert that the test has inserted the data into the database
        Assertions.assertEquals(expectedRecords, externalObjectDirectoryRepository.findAll().size());

        int pageSize = 1;

        // exercise the logic
        List<Integer> updatedResults = armTranscriptionAndAnnotationDeleterProcessor.processDeletionIfPreceding(pageSize);

        // assert the logic
        assertExpectedResults(updatedResults, expectedArmRecordsResultOutsideHours, pageSize);

        // assert the logic
        assertExternalObjectDirectoryUpdate(updatedResults, expectedArmRecordsResultOutsideHours, 1);

        List<Integer> updatedResults2 = armTranscriptionAndAnnotationDeleterProcessor.processDeletionIfPreceding(pageSize);

        // assert the logic
        assertExpectedResults(updatedResults2, expectedArmRecordsResultOutsideHours, pageSize);

        // assert the update
        assertExternalObjectDirectoryUpdate(updatedResults2, expectedArmRecordsResultOutsideHours, pageSize);

        // check the results are unique between processing
        Assertions.assertFalse(CollectionUtils.containsAny(updatedResults, updatedResults2));

        externalObjectDirectoryStub.checkNotMarkedForDeletion(expectedArmRecordsResultWithinTheHour);
    }

    @Test
    void processBatchNoRecords() throws Exception {
        int numberOfRecordsToGenerate = 10;
        int setupHoursBeforeCurrentTime = 7;

        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntitiesNotRelevant
            = externalObjectDirectoryStub.generateWithStatusAndInboundLocation(
            ObjectRecordStatusEnum.ARM_RAW_DATA_FAILED, numberOfRecordsToGenerate);
        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntities
            = externalObjectDirectoryStub.generateWithStatusAndInboundLocation(STORED, numberOfRecordsToGenerate);

        List<ExternalObjectDirectoryEntity> expectedArmRecordsResultOutsideHours
            = externalObjectDirectoryStub.generateWithStatusAndMediaAndArmLocation(
                externalObjectDirectoryEntities.subList(0, externalObjectDirectoryEntities.size() / 2), setupHoursBeforeCurrentTime);
        List<ExternalObjectDirectoryEntity> expectedArmRecordsResultWithinTheHour
            = externalObjectDirectoryStub.generateWithStatusAndMediaAndArmLocation(
                externalObjectDirectoryEntities.subList(externalObjectDirectoryEntities.size() / 2, externalObjectDirectoryEntities.size()), 1);

        int expectedRecords = externalObjectDirectoryEntitiesNotRelevant.size() + externalObjectDirectoryEntities.size()
            + expectedArmRecordsResultOutsideHours.size() + expectedArmRecordsResultWithinTheHour.size();

        // assert that the test has inserted the data into the database
        Assertions.assertEquals(expectedRecords, externalObjectDirectoryRepository.findAll().size());

        int pageSize = 1;
        int hourDurationBeyondHours = setupHoursBeforeCurrentTime + 1; // which no records are

        List<Integer> updatedResults = armTranscriptionAndAnnotationDeleterProcessor.processDeletionIfPreceding(pageSize, hourDurationBeyondHours);

        // assert that the test has inserted the data into the database
        Assertions.assertTrue(updatedResults.isEmpty());
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
}