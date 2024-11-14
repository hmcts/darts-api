package uk.gov.hmcts.darts.datamanagement.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.ExternalObjectDirectoryStub;

import java.lang.reflect.InvocationTargetException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.FAILURE_CHECKSUM_FAILED;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;

class InboundTranscriptionAnnotationDeleterProcessorImplTest extends PostgresIntegrationBase {

    @Autowired
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;

    @Autowired
    private ExternalObjectDirectoryStub externalObjectDirectoryStub;

    @Autowired
    private InboundTranscriptionAnnotationDeleterProcessor inboundTranscriptionAnnotationDeleterProcessor;

    @Autowired
    private CurrentTimeHelper currentTimeHelper;

    private List<ExternalObjectDirectoryEntity> expectedUnstructuredRecordsResultWithinTheHour;

    private List<ExternalObjectDirectoryEntity> entitiesToBeMarkedWithMediaOutsideOfHours;

    @Test
    void processBatchMultipleRecords() throws Exception {
        int setupHoursBeforeCurrentTime = 25;

        generateData(setupHoursBeforeCurrentTime);

        int hourDurationBeyondHours = setupHoursBeforeCurrentTime; // which no records are

        // exercise the logic
        List<Integer> updatedResults = inboundTranscriptionAnnotationDeleterProcessor.markForDeletion(hourDurationBeyondHours);

        // assert the logic
        assertExpectedResults(updatedResults, entitiesToBeMarkedWithMediaOutsideOfHours, entitiesToBeMarkedWithMediaOutsideOfHours.size());

        // assert the logic
        assertExternalObjectDirectoryUpdate(updatedResults, entitiesToBeMarkedWithMediaOutsideOfHours, entitiesToBeMarkedWithMediaOutsideOfHours.size());

        externalObjectDirectoryStub.checkNotMarkedForDeletion(expectedUnstructuredRecordsResultWithinTheHour);
    }

    @Test
    void processBatchMultipleRecordsWithSpringInjected24HourDurationThreshold() throws Exception {
        // assume that spring config is 24 hours
        int setupHoursBeforeCurrentTime = 24;

        generateData(setupHoursBeforeCurrentTime);

        // exercise the logic
        List<Integer> updatedResults = inboundTranscriptionAnnotationDeleterProcessor.markForDeletion(100);

        // assert the logic
        assertExpectedResults(updatedResults, entitiesToBeMarkedWithMediaOutsideOfHours, entitiesToBeMarkedWithMediaOutsideOfHours.size());

        // assert the logic
        assertExternalObjectDirectoryUpdate(updatedResults, entitiesToBeMarkedWithMediaOutsideOfHours, entitiesToBeMarkedWithMediaOutsideOfHours.size());

        externalObjectDirectoryStub.checkNotMarkedForDeletion(expectedUnstructuredRecordsResultWithinTheHour);
    }

    @Test
    void processBatchNoRecords() throws Exception {
        int setupHoursBeforeCurrentTime = 7;

        generateData(setupHoursBeforeCurrentTime);

        int hourDurationBeyondHours = setupHoursBeforeCurrentTime + 1; // which no records are

        List<Integer> updatedResults = inboundTranscriptionAnnotationDeleterProcessor.markForDeletion(hourDurationBeyondHours);

        // assert that the test has inserted the data into the database
        Assertions.assertTrue(updatedResults.isEmpty());
    }

    private void generateData(int hoursBeforeCurrentTime) throws
        NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        int numberOfRecordsToGenerate = 10;
        int setupHoursBeforeCurrentTime = hoursBeforeCurrentTime;

        OffsetDateTime lastModifiedBefore = currentTimeHelper.currentOffsetDateTime().minus(
            setupHoursBeforeCurrentTime,
            ChronoUnit.HOURS
        );

        OffsetDateTime lastModifiedNotBeforeThreshold = currentTimeHelper.currentOffsetDateTime().minus(
            1,
            ChronoUnit.HOURS
        );

        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntitiesNotRelevant = externalObjectDirectoryStub
            .generateWithStatusAndTranscriptionOrAnnotationAndLocation(
                ExternalLocationTypeEnum.INBOUND, FAILURE_CHECKSUM_FAILED, numberOfRecordsToGenerate, Optional.empty());
        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntities = externalObjectDirectoryStub
            .generateWithStatusAndTranscriptionOrAnnotationAndLocation(
                ExternalLocationTypeEnum.INBOUND, STORED, numberOfRecordsToGenerate, Optional.empty());
        entitiesToBeMarkedWithMediaOutsideOfHours
            = externalObjectDirectoryEntities.subList(0, externalObjectDirectoryEntities.size() / 2);

        List<ExternalObjectDirectoryEntity> expectedUnstructuredRecordsResultOutsideHours
            = externalObjectDirectoryStub.generateWithStatusAndTranscriptionAndAnnotationAndLocation(
            externalObjectDirectoryEntities.subList(0, externalObjectDirectoryEntities.size() / 2), Optional.of(lastModifiedBefore),
            ExternalLocationTypeEnum.UNSTRUCTURED);
        expectedUnstructuredRecordsResultWithinTheHour
            = externalObjectDirectoryStub.generateWithStatusAndTranscriptionAndAnnotationAndLocation(
            externalObjectDirectoryEntities
                .subList(externalObjectDirectoryEntities.size() / 2, externalObjectDirectoryEntities.size()), Optional.of(lastModifiedNotBeforeThreshold),
            ExternalLocationTypeEnum.UNSTRUCTURED);

        int expectedRecords = externalObjectDirectoryEntitiesNotRelevant.size() + externalObjectDirectoryEntities.size()
            + expectedUnstructuredRecordsResultOutsideHours.size() + expectedUnstructuredRecordsResultWithinTheHour.size();

        // assert that the test has inserted the data into the database
        Assertions.assertEquals(expectedRecords, externalObjectDirectoryRepository.findAll().size());
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