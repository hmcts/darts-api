package uk.gov.hmcts.darts.arm.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;
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

import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;

class UnstructuredAnnotationTranscriptionDeleterProcessorImplTest extends PostgresIntegrationBase {

    private static final String USER_EMAIL_ADDRESS = "system_UnstructuredTranscriptionAnnotationDeleter@hmcts.net";

    @Autowired
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;

    @Autowired
    private ExternalObjectDirectoryStub externalObjectDirectoryStub;

    @Autowired
    private UnstructuredTranscriptionAndAnnotationDeleterProcessor armTranscriptionAndAnnotationDeleterProcessor;

    @Autowired
    private CurrentTimeHelper currentTimeHelper;

    private List<ExternalObjectDirectoryEntity> expectedArmRecordsResultWithinTheHour;

    private List<ExternalObjectDirectoryEntity> entitiesToBeMarkedWithMediaOutsideOfWeeksAndHours;

    @Test
    void processBatchMultipleRecords() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        int setupHoursBeforeCurrentTimeInArm = 25;
        int setupWeeksBeforeCurrentTimeInUnstructured = 3;

        anAuthenticatedUserFor(USER_EMAIL_ADDRESS);
        generateDataWithAnnotation(setupWeeksBeforeCurrentTimeInUnstructured, setupHoursBeforeCurrentTimeInArm);

        // exercise the logic
        List<Long> updatedResults
            = armTranscriptionAndAnnotationDeleterProcessor.markForDeletion(setupWeeksBeforeCurrentTimeInUnstructured, setupHoursBeforeCurrentTimeInArm, 1000);

        // assert the logic
        assertExpectedResults(updatedResults, entitiesToBeMarkedWithMediaOutsideOfWeeksAndHours, entitiesToBeMarkedWithMediaOutsideOfWeeksAndHours.size());

        // assert the logic
        assertExternalObjectDirectoryUpdate(updatedResults,
                                            entitiesToBeMarkedWithMediaOutsideOfWeeksAndHours, entitiesToBeMarkedWithMediaOutsideOfWeeksAndHours.size());

        externalObjectDirectoryStub.checkNotMarkedForDeletion(expectedArmRecordsResultWithinTheHour);
    }

    @Test
    void processBatchMultipleRecordsWithSpringInjectedDurationThreshold() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        int setupHoursBeforeCurrentTimeInArm = 24;
        int setupWeeksBeforeCurrentTimeInUnstructured = 30;
        anAuthenticatedUserFor(USER_EMAIL_ADDRESS);
        generateDataWithAnnotation(setupWeeksBeforeCurrentTimeInUnstructured, setupHoursBeforeCurrentTimeInArm);

        // exercise the logic
        List<Long> updatedResults = armTranscriptionAndAnnotationDeleterProcessor.markForDeletion(1000);

        // assert the logic
        assertExpectedResults(updatedResults,
                              entitiesToBeMarkedWithMediaOutsideOfWeeksAndHours, entitiesToBeMarkedWithMediaOutsideOfWeeksAndHours.size());

        // assert the logic
        assertExternalObjectDirectoryUpdate(updatedResults,
                                            entitiesToBeMarkedWithMediaOutsideOfWeeksAndHours, entitiesToBeMarkedWithMediaOutsideOfWeeksAndHours.size());

        externalObjectDirectoryStub.checkNotMarkedForDeletion(expectedArmRecordsResultWithinTheHour);
    }

    @Test
    void processRecordsWithNoMarkForDeletion() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        int setupHoursBeforeCurrentTimeInArm = 25;
        int setupWeeksBeforeCurrentTimeInUnstructured = 3;

        List<ExternalObjectDirectoryEntity> unstructuredEntities
            = generateDataWithAnnotation(setupWeeksBeforeCurrentTimeInUnstructured, setupHoursBeforeCurrentTimeInArm);

        // exercise the logic
        List<Long> updatedResults
            = armTranscriptionAndAnnotationDeleterProcessor.markForDeletion(setupWeeksBeforeCurrentTimeInUnstructured, setupHoursBeforeCurrentTimeInArm + 1,
                                                                            1000);

        Assertions.assertTrue(updatedResults.isEmpty());
        externalObjectDirectoryStub.checkNotMarkedForDeletion(unstructuredEntities);
    }

    private List<ExternalObjectDirectoryEntity> generateDataWithAnnotation(int weeksBeforeCurrentTimeForUnstructured,
                                                                           int hoursBeforeCurrentTimeForArm)
        throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        int numberOfRecordsToGenerate = 10;

        OffsetDateTime lastModifiedBeforeCurrentTimeForArm = currentTimeHelper.currentOffsetDateTime().minus(
            hoursBeforeCurrentTimeForArm,
            ChronoUnit.HOURS
        );

        OffsetDateTime lastModifiedBeforeCurrentTimeForUnstructured = currentTimeHelper.currentOffsetDateTime().minus(
            weeksBeforeCurrentTimeForUnstructured,
            ChronoUnit.WEEKS
        );

        OffsetDateTime lastModifiedNotBeforeThreshold = currentTimeHelper.currentOffsetDateTime().minus(
            1,
            ChronoUnit.HOURS
        );

        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntitiesNotRelevant = externalObjectDirectoryStub
            .generateWithStatusAndTranscriptionOrAnnotationAndLocation(
                ExternalLocationTypeEnum.UNSTRUCTURED,
                ObjectRecordStatusEnum.ARM_RAW_DATA_FAILED, numberOfRecordsToGenerate, Optional.of(lastModifiedBeforeCurrentTimeForUnstructured));
        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntities = externalObjectDirectoryStub
            .generateWithStatusAndTranscriptionOrAnnotationAndLocation(
                ExternalLocationTypeEnum.UNSTRUCTURED, STORED, numberOfRecordsToGenerate, Optional.of(lastModifiedBeforeCurrentTimeForUnstructured));
        entitiesToBeMarkedWithMediaOutsideOfWeeksAndHours
            = externalObjectDirectoryEntities.subList(0, externalObjectDirectoryEntities.size() / 2);

        List<ExternalObjectDirectoryEntity> expectedArmRecordsResultOutsideHours
            = externalObjectDirectoryStub.generateWithStatusAndTranscriptionAndAnnotationAndArmLocation(
            entitiesToBeMarkedWithMediaOutsideOfWeeksAndHours, Optional.of(lastModifiedBeforeCurrentTimeForArm));
        expectedArmRecordsResultWithinTheHour
            = externalObjectDirectoryStub.generateWithStatusAndTranscriptionAndAnnotationAndArmLocation(
            expectedArmRecordsResultOutsideHours, Optional.of(lastModifiedNotBeforeThreshold));

        int expectedRecords = externalObjectDirectoryEntitiesNotRelevant.size() + externalObjectDirectoryEntities.size()
            + expectedArmRecordsResultOutsideHours.size() + expectedArmRecordsResultWithinTheHour.size();

        // assert that the test has inserted the data into the database
        Assertions.assertEquals(expectedRecords, externalObjectDirectoryRepository.findAll().size());

        List<ExternalObjectDirectoryEntity> allUnstructuredEntities = new ArrayList<>();
        allUnstructuredEntities.addAll(externalObjectDirectoryEntitiesNotRelevant);
        allUnstructuredEntities.addAll(externalObjectDirectoryEntities);
        return allUnstructuredEntities;
    }


    private void assertExternalObjectDirectoryUpdate(List<Long> actualResults, List<ExternalObjectDirectoryEntity> expectedResults, int resultCount) {
        // find matching pn expected results
        List<Long> matchesEntity = new ArrayList<>(
            actualResults.stream().filter(expectedResult -> expectedResults.stream().anyMatch(result -> expectedResult.equals(result.getId()))).toList());

        Assertions.assertEquals(resultCount, matchesEntity.size());

        Assertions.assertTrue(externalObjectDirectoryStub.areObjectDirectoriesMarkedForDeletionWithUser(actualResults, USER_EMAIL_ADDRESS));
    }


    private void assertExpectedResults(List<Long> actualResults, List<ExternalObjectDirectoryEntity> expectedResults, int resultCount) {
        List<Long> matchesEntity = new ArrayList<>(
            actualResults.stream().filter(expectedResult -> expectedResults.stream().anyMatch(result -> expectedResult.equals(result.getId()))).toList());

        Assertions.assertEquals(resultCount, matchesEntity.size());
    }

}