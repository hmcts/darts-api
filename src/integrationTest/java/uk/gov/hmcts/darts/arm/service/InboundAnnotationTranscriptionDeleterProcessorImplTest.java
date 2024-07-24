package uk.gov.hmcts.darts.arm.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
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


    @Autowired
    private CurrentTimeHelper currentTimeHelper;

    @Test
    void processBatchMultipleRecords() throws Exception {
        int numberOfRecordsToGenerate = 10;
        int setupHoursBeforeCurrentTime = 25;

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

        // exercise the logic
        List<Integer> updatedResults = armTranscriptionAndAnnotationDeleterProcessor.markForDeletion(hourDurationBeyondHours);

        // assert the logic
        assertExpectedResults(updatedResults, entitiesToBeMarkedWithMediaOutsideOfHours, entitiesToBeMarkedWithMediaOutsideOfHours.size());

        // assert the logic
        assertExternalObjectDirectoryUpdate(updatedResults, entitiesToBeMarkedWithMediaOutsideOfHours, entitiesToBeMarkedWithMediaOutsideOfHours.size());

        externalObjectDirectoryStub.checkNotMarkedForDeletion(armRecordsResultWithinTheHour);
    }

    @Test
    void processBatchMultipleRecordsWithSpringInjected24HourDurationThreshold() throws Exception {
        int numberOfRecordsToGenerate = 10;

        // assume that spring config is 24 hours
        int setupHoursBeforeCurrentTime = 24;

        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntitiesNotRelevant
            = externalObjectDirectoryStub.generateWithStatusAndTranscriptionAndAnnotationAndInboundLocation(
            ObjectRecordStatusEnum.ARM_RAW_DATA_FAILED, numberOfRecordsToGenerate);
        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntities
            = externalObjectDirectoryStub.generateWithStatusAndTranscriptionAndAnnotationAndInboundLocation(STORED, numberOfRecordsToGenerate);

        List<ExternalObjectDirectoryEntity> entitiesToBeMarkedWithMediaOutsideOfHours
            = externalObjectDirectoryEntities.subList(0, externalObjectDirectoryEntities.size() / 2);

        List<ExternalObjectDirectoryEntity> expectedArmRecordsResultOutsideHours
            = externalObjectDirectoryStub.generateWithStatusAndTranscriptionAndAnnotationAndArmLocation(
            entitiesToBeMarkedWithMediaOutsideOfHours, setupHoursBeforeCurrentTime);
        List<ExternalObjectDirectoryEntity> expectedArmRecordsResultWithinTheHour
            = externalObjectDirectoryStub.generateWithStatusAndTranscriptionAndAnnotationAndArmLocation(
            externalObjectDirectoryEntities.subList(externalObjectDirectoryEntities.size() / 2, externalObjectDirectoryEntities.size()), 1);

        int expectedRecords = externalObjectDirectoryEntitiesNotRelevant.size() + externalObjectDirectoryEntities.size()
            + expectedArmRecordsResultOutsideHours.size() + expectedArmRecordsResultWithinTheHour.size();

        // assert that the test has inserted the data into the database
        Assertions.assertEquals(expectedRecords, externalObjectDirectoryRepository.findAll().size());

        // exercise the logic
        List<Integer> updatedResults = armTranscriptionAndAnnotationDeleterProcessor.markForDeletion();

        // assert the logic
        assertExpectedResults(updatedResults, entitiesToBeMarkedWithMediaOutsideOfHours, entitiesToBeMarkedWithMediaOutsideOfHours.size());

        // assert the logic
        assertExternalObjectDirectoryUpdate(updatedResults, entitiesToBeMarkedWithMediaOutsideOfHours, entitiesToBeMarkedWithMediaOutsideOfHours.size());

        externalObjectDirectoryStub.checkNotMarkedForDeletion(expectedArmRecordsResultWithinTheHour);
    }

    @Test
    void processBatchNoRecords() throws Exception {
        int numberOfRecordsToGenerate = 10;
        int setupHoursBeforeCurrentTime = 7;

        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntitiesNotRelevant
            = externalObjectDirectoryStub.generateWithStatusAndTranscriptionAndAnnotationAndInboundLocation(
            ObjectRecordStatusEnum.ARM_RAW_DATA_FAILED, numberOfRecordsToGenerate);
        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntities
            = externalObjectDirectoryStub.generateWithStatusAndTranscriptionAndAnnotationAndInboundLocation(STORED, numberOfRecordsToGenerate);

        List<ExternalObjectDirectoryEntity> expectedArmRecordsResultOutsideHours
            = externalObjectDirectoryStub.generateWithStatusAndTranscriptionAndAnnotationAndArmLocation(
                externalObjectDirectoryEntities.subList(0, externalObjectDirectoryEntities.size() / 2), setupHoursBeforeCurrentTime);
        List<ExternalObjectDirectoryEntity> expectedArmRecordsResultWithinTheHour
            = externalObjectDirectoryStub.generateWithStatusAndTranscriptionAndAnnotationAndArmLocation(
                externalObjectDirectoryEntities.subList(externalObjectDirectoryEntities.size() / 2, externalObjectDirectoryEntities.size()), 1);

        int expectedRecords = externalObjectDirectoryEntitiesNotRelevant.size() + externalObjectDirectoryEntities.size()
            + expectedArmRecordsResultOutsideHours.size() + expectedArmRecordsResultWithinTheHour.size();

        // assert that the test has inserted the data into the database
        Assertions.assertEquals(expectedRecords, externalObjectDirectoryRepository.findAll().size());

        int hourDurationBeyondHours = setupHoursBeforeCurrentTime + 1; // which no records are

        List<Integer> updatedResults = armTranscriptionAndAnnotationDeleterProcessor.markForDeletion(hourDurationBeyondHours);

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