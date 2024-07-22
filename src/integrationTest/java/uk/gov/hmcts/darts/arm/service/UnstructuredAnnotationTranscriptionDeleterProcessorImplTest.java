package uk.gov.hmcts.darts.arm.service;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.ExternalObjectDirectoryStub;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;

class UnstructuredAnnotationTranscriptionDeleterProcessorImplTest extends PostgresIntegrationBase {

    @Autowired
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;

    @Autowired
    private ExternalObjectDirectoryStub externalObjectDirectoryStub;

    @Autowired
    private UnstructuredTranscriptionAndAnnotationDeleterProcessor armTranscriptionAndAnnotationDeleterProcessor;

    @Autowired
    private CurrentTimeHelper currentTimeHelper;

    @Test
    void procesDeletionIfPrecedingWithSingleRecordFound() throws Exception {
        int numberOfRecordsToGenerate = 10;

        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntitiesNotRelevant
            = externalObjectDirectoryStub.generateWithStatusAndUnstructuredLocation(
            ObjectRecordStatusEnum.ARM_RAW_DATA_FAILED, 10);
        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntities
            = externalObjectDirectoryStub.generateWithStatusAndUnstructuredLocation(STORED, numberOfRecordsToGenerate);

        List<ExternalObjectDirectoryEntity> expectedArmRecordsResultOutsideDuration
            = externalObjectDirectoryStub.generateWithStatusAndMediaAndArmLocation(
                externalObjectDirectoryEntities.subList(0, externalObjectDirectoryEntities.size() / 2), getDateTimePrecedingCurrentDateTimeByWeeks(3));
        List<ExternalObjectDirectoryEntity> expectedArmRecordsResultWithinTheHour
            = externalObjectDirectoryStub.generateWithStatusAndMediaAndArmLocation(
                externalObjectDirectoryEntities.subList(externalObjectDirectoryEntities.size() / 2, externalObjectDirectoryEntities.size()), 1);

        int expectedRecords = externalObjectDirectoryEntitiesNotRelevant.size() + externalObjectDirectoryEntities.size()
            + expectedArmRecordsResultOutsideDuration.size() + expectedArmRecordsResultWithinTheHour.size();

        // assert that the test has inserted the data into the database
        Assertions.assertEquals(expectedRecords, externalObjectDirectoryRepository.findAll().size());

        int pageSize = 5;
        int weeksPrecedingCurrent = 3; // which no records are

        // exercise the logic
        List<Integer> updatedResults = armTranscriptionAndAnnotationDeleterProcessor.processDeletionIfPreceding(pageSize,  weeksPrecedingCurrent);

        // assert the logic
        assertExpectedResults(updatedResults, expectedArmRecordsResultOutsideDuration, pageSize);

        // assert the logic
        assertExternalObjectDirectoryUpdate(updatedResults, expectedArmRecordsResultOutsideDuration, pageSize);

        updatedResults = armTranscriptionAndAnnotationDeleterProcessor.processDeletionIfPreceding(pageSize, weeksPrecedingCurrent);

        Assertions.assertTrue(updatedResults.isEmpty());
    }

    @Test
    void processDeletionIfPrecedingWithMultipleRecordsFound() throws Exception {
        int numberOfRecordsToGenerate = 10;

        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntitiesNotRelevant
            = externalObjectDirectoryStub.generateWithStatusAndUnstructuredLocation(
            ObjectRecordStatusEnum.ARM_RAW_DATA_FAILED, numberOfRecordsToGenerate);
        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntities
            = externalObjectDirectoryStub.generateWithStatusAndUnstructuredLocation(STORED, numberOfRecordsToGenerate);

        List<ExternalObjectDirectoryEntity> expectedArmRecordsResultOutsideDuration
            = externalObjectDirectoryStub.generateWithStatusAndMediaAndArmLocation(
                externalObjectDirectoryEntities.subList(0, externalObjectDirectoryEntities.size() / 2), getDateTimePrecedingCurrentDateTimeByWeeks(3));
        List<ExternalObjectDirectoryEntity> expectedArmRecordsResultWithinTheHour
            = externalObjectDirectoryStub.generateWithStatusAndMediaAndArmLocation(
                externalObjectDirectoryEntities.subList(externalObjectDirectoryEntities.size() / 2, externalObjectDirectoryEntities.size()), 1);

        int expectedRecords = externalObjectDirectoryEntitiesNotRelevant.size() + externalObjectDirectoryEntities.size()
            + expectedArmRecordsResultOutsideDuration.size() + expectedArmRecordsResultWithinTheHour.size();

        // assert that the test has inserted the data into the database
        Assertions.assertEquals(expectedRecords, externalObjectDirectoryRepository.findAll().size());

        int pageSize = 1;

        // exercise the logic
        List<Integer> updatedResults = armTranscriptionAndAnnotationDeleterProcessor.processDeletionIfPreceding(pageSize, 3);

        // assert the logic
        assertExpectedResults(updatedResults, expectedArmRecordsResultOutsideDuration, pageSize);

        // assert the logic
        assertExternalObjectDirectoryUpdate(updatedResults, expectedArmRecordsResultOutsideDuration, 1);

        List<Integer> updatedResults2 = armTranscriptionAndAnnotationDeleterProcessor.processDeletionIfPreceding(pageSize, 3);

        // assert the logic
        assertExpectedResults(updatedResults2, expectedArmRecordsResultOutsideDuration, pageSize);

        // assert the update
        assertExternalObjectDirectoryUpdate(updatedResults2, expectedArmRecordsResultOutsideDuration, pageSize);

        // check the results are unique between processing
        Assertions.assertFalse(CollectionUtils.containsAny(updatedResults, updatedResults2));

        externalObjectDirectoryStub.checkNotMarkedForDeletion(expectedArmRecordsResultWithinTheHour);
    }

    @Test
    void processDeletionIfPrecedingWithMultipleRecordsFoundUsingSpringInjectedWeeks() throws Exception {
        int numberOfRecordsToGenerate = 10;

        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntitiesNotRelevant
            = externalObjectDirectoryStub.generateWithStatusAndUnstructuredLocation(
            ObjectRecordStatusEnum.ARM_RAW_DATA_FAILED, numberOfRecordsToGenerate);
        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntities
            = externalObjectDirectoryStub.generateWithStatusAndUnstructuredLocation(STORED, numberOfRecordsToGenerate);

        List<ExternalObjectDirectoryEntity> expectedArmRecordsResultOutsideDuration
            = externalObjectDirectoryStub.generateWithStatusAndMediaAndArmLocation(
            externalObjectDirectoryEntities.subList(0, externalObjectDirectoryEntities.size() / 2), getDateTimePrecedingCurrentDateTimeByWeeks(40));
        List<ExternalObjectDirectoryEntity> expectedArmRecordsResultWithinTheHour
            = externalObjectDirectoryStub.generateWithStatusAndMediaAndArmLocation(
            externalObjectDirectoryEntities.subList(externalObjectDirectoryEntities.size() / 2, externalObjectDirectoryEntities.size()), 1);

        int expectedRecords = externalObjectDirectoryEntitiesNotRelevant.size() + externalObjectDirectoryEntities.size()
            + expectedArmRecordsResultOutsideDuration.size() + expectedArmRecordsResultWithinTheHour.size();

        // assert that the test has inserted the data into the database
        Assertions.assertEquals(expectedRecords, externalObjectDirectoryRepository.findAll().size());

        int pageSize = 1;

        // exercise the logic
        List<Integer> updatedResults = armTranscriptionAndAnnotationDeleterProcessor.processDeletionIfPreceding(pageSize);

        // assert the logic
        assertExpectedResults(updatedResults, expectedArmRecordsResultOutsideDuration, pageSize);

        // assert the logic
        assertExternalObjectDirectoryUpdate(updatedResults, expectedArmRecordsResultOutsideDuration, 1);

        List<Integer> updatedResults2 = armTranscriptionAndAnnotationDeleterProcessor.processDeletionIfPreceding(pageSize);

        // assert the logic
        assertExpectedResults(updatedResults2, expectedArmRecordsResultOutsideDuration, pageSize);

        // assert the update
        assertExternalObjectDirectoryUpdate(updatedResults2, expectedArmRecordsResultOutsideDuration, pageSize);

        // check the results are unique between processing
        Assertions.assertFalse(CollectionUtils.containsAny(updatedResults, updatedResults2));

        externalObjectDirectoryStub.checkNotMarkedForDeletion(expectedArmRecordsResultWithinTheHour);
    }

    @Test
    void  processDeletionIfPrecedingWithNoRecordsFound() throws Exception {
        int numberOfRecordsToGenerate = 10;

        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntitiesNotRelevant
            = externalObjectDirectoryStub.generateWithStatusAndUnstructuredLocation(
            ObjectRecordStatusEnum.ARM_RAW_DATA_FAILED, numberOfRecordsToGenerate);
        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntities
            = externalObjectDirectoryStub.generateWithStatusAndUnstructuredLocation(STORED, numberOfRecordsToGenerate);

        List<ExternalObjectDirectoryEntity> expectedArmRecordsResultOutsideDuration
            = externalObjectDirectoryStub.generateWithStatusAndMediaAndArmLocation(
                externalObjectDirectoryEntities.subList(0, externalObjectDirectoryEntities.size() / 2), getDateTimePrecedingCurrentDateTimeByWeeks(2));
        List<ExternalObjectDirectoryEntity> expectedArmRecordsResultWithinTheHour
            = externalObjectDirectoryStub.generateWithStatusAndMediaAndArmLocation(
                externalObjectDirectoryEntities.subList(externalObjectDirectoryEntities.size() / 2, externalObjectDirectoryEntities.size()), 1);

        int expectedRecords = externalObjectDirectoryEntitiesNotRelevant.size() + externalObjectDirectoryEntities.size()
            + expectedArmRecordsResultOutsideDuration.size() + expectedArmRecordsResultWithinTheHour.size();

        // assert that the test has inserted the data into the database
        Assertions.assertEquals(expectedRecords, externalObjectDirectoryRepository.findAll().size());

        int pageSize = 1;

        List<Integer> updatedResults = armTranscriptionAndAnnotationDeleterProcessor.processDeletionIfPreceding(pageSize, 3);

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

    private OffsetDateTime getDateTimePrecedingCurrentDateTimeByWeeks(int weeks) {
        return currentTimeHelper.currentOffsetDateTime().minus(
            weeks,
            ChronoUnit.WEEKS
        );
    }

}