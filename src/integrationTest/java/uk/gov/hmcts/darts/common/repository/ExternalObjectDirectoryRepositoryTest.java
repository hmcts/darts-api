package uk.gov.hmcts.darts.common.repository;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.auditing.DateTimeProvider;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
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
import java.util.Optional;

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

    private List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntitiesNotRelevant;

    private List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntities;

    private List<ExternalObjectDirectoryEntity> expectedArmRecordsResultOutsideHours;

    private List<ExternalObjectDirectoryEntity> expectedArmRecordsResultWithinTheHour;

    private List<ExternalObjectDirectoryEntity> entitiesToBeMarkedWithMediaOrAnnotationOutsideOfArmHours;

    @Test
    void testGetDirectoryIfMediaDate24Hours() throws Exception {

        int setupHoursBeforeCurrentTime = 24;

        // setup the test data
        generateDataWithMedia(setupHoursBeforeCurrentTime);

        int hourDurationBeyondHours = setupHoursBeforeCurrentTime; // which no records are

        // excerise the logic
        List<Integer> results = externalObjectDirectoryRepository
            .findIdsIn2StorageLocationsBeforeTime(
                EodHelper.storedStatus(), EodHelper.storedStatus(),
                EodHelper.inboundLocation(), EodHelper.armLocation(),
                getCurrentDateTimeWithHoursBefore(hourDurationBeyondHours), ExternalObjectDirectoryQueryTypeEnum.MEDIA_QUERY.getIndex());

        // assert the logic
        assertExpectedResults(results, entitiesToBeMarkedWithMediaOrAnnotationOutsideOfArmHours, entitiesToBeMarkedWithMediaOrAnnotationOutsideOfArmHours.size());
    }

    @Test
    void testGetDirectoryIfMediaDateBeyond24Hours() throws Exception {

        int setupHoursBeforeCurrentTime = 26;

        // setup the test data
        generateDataWithMedia(setupHoursBeforeCurrentTime);

        int hourDurationBeyondHours = setupHoursBeforeCurrentTime; // which no records are

        // excerise the logic
        List<Integer> results = externalObjectDirectoryRepository
            .findIdsIn2StorageLocationsBeforeTime(EodHelper.storedStatus(), EodHelper.storedStatus(),
                                                  EodHelper.inboundLocation(), EodHelper.armLocation(),
                                                  getCurrentDateTimeWithHoursBefore(hourDurationBeyondHours),
                                                  ExternalObjectDirectoryQueryTypeEnum.MEDIA_QUERY.getIndex());

        // assert the logic
        assertExpectedResults(results, entitiesToBeMarkedWithMediaOrAnnotationOutsideOfArmHours, entitiesToBeMarkedWithMediaOrAnnotationOutsideOfArmHours.size());
    }

    @Test
    void testGetDirectoryIfAnnotationDate24Hours() throws Exception {

        int setupHoursBeforeCurrentTime = 24;

        // setup the test data
        generateDataWithAnnotation(setupHoursBeforeCurrentTime);


        int hourDurationBeyondHours = setupHoursBeforeCurrentTime; // which no records are

        // excerise the logic
        List<Integer> results = externalObjectDirectoryRepository
            .findIdsIn2StorageLocationsBeforeTime(
                EodHelper.storedStatus(), EodHelper.storedStatus(),
                EodHelper.inboundLocation(), EodHelper.armLocation(),
                getCurrentDateTimeWithHoursBefore(hourDurationBeyondHours), ExternalObjectDirectoryQueryTypeEnum.ANNOTATION_QUERY.getIndex());

        // assert the logic
        assertExpectedResults(results, entitiesToBeMarkedWithMediaOrAnnotationOutsideOfArmHours, entitiesToBeMarkedWithMediaOrAnnotationOutsideOfArmHours.size());
    }

    @Test
    void testGetDirectoryIfAnnotationArmDateAndUnstructuredDateOutsideOfBounds() throws Exception {

        int setupArmHoursBeforeCurrentTime = 24;
        int setupUnstructuredWeeksBeforeCurrentTime = 4;

        // setup the test data
        generateDataWithAnnotation(setupArmHoursBeforeCurrentTime, setupUnstructuredWeeksBeforeCurrentTime);

        // exercise the logic
        List<Integer> results = externalObjectDirectoryRepository
            .findIdsIn2StorageLocationsBeforeTime(
                EodHelper.storedStatus(), EodHelper.storedStatus(),
                EodHelper.unstructuredLocation(), EodHelper.armLocation(),
                getCurrentDateTimeWithWeeksBefore(setupUnstructuredWeeksBeforeCurrentTime),
                getCurrentDateTimeWithHoursBefore(setupArmHoursBeforeCurrentTime));

        // assert the logic
        assertExpectedResults(results, entitiesToBeMarkedWithMediaOrAnnotationOutsideOfArmHours, entitiesToBeMarkedWithMediaOrAnnotationOutsideOfArmHours.size());
    }

    @Test
    void testGetDirectoryIfAnnotationArmDateAndUnstructuredDateNoRecordsFoundDueToArmDate() throws Exception {

        int setupArmHoursBeforeCurrentTime = 24;
        int setupUnstructuredWeeksBeforeCurrentTime = 4;

        // setup the test data
        generateDataWithAnnotation(setupArmHoursBeforeCurrentTime, setupUnstructuredWeeksBeforeCurrentTime);

        // exercise the logic
        List<Integer> results = externalObjectDirectoryRepository
            .findIdsIn2StorageLocationsBeforeTime(
                EodHelper.storedStatus(), EodHelper.storedStatus(),
                EodHelper.unstructuredLocation(), EodHelper.armLocation(),
                getCurrentDateTimeWithWeeksBefore(setupUnstructuredWeeksBeforeCurrentTime),
                getCurrentDateTimeWithHoursBefore(setupArmHoursBeforeCurrentTime + 1));

        // assert the logic
        Assertions.assertTrue(results.isEmpty());
    }

    @Test
    void testGetDirectoryIfMediaDateNotBeyondThreshold() throws Exception {

        int setupHoursBeforeCurrentTime = 22;

        // setup the test data
        generateDataWithMedia(setupHoursBeforeCurrentTime);

        int hourDurationBeyondHours = 24; // which no records are

        // exercise the logic
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

    private OffsetDateTime getCurrentDateTimeWithWeeksBefore(int hours) {
        return currentTimeHelper.currentOffsetDateTime().minus(
            hours,
            ChronoUnit.HOURS
        );
    }

    private void generateDataWithAnnotation(int hoursBeforeCurrentTime) throws Exception {
        int numberOfRecordsToGenerate = 10;
        int setupHoursBeforeCurrentTime = hoursBeforeCurrentTime;

        OffsetDateTime lastModifiedBeforeCurrentTime = currentTimeHelper.currentOffsetDateTime().minus(
            setupHoursBeforeCurrentTime,
            ChronoUnit.HOURS
        );

        OffsetDateTime lastModifiedNotBeforeThreshold = currentTimeHelper.currentOffsetDateTime().minus(
            1,
            ChronoUnit.HOURS
        );

        externalObjectDirectoryEntitiesNotRelevant
            = externalObjectDirectoryStub.generateWithStatusAndTranscriptionAndAnnotationAndLocation(ExternalLocationTypeEnum.INBOUND, ObjectRecordStatusEnum.ARM_RAW_DATA_FAILED, numberOfRecordsToGenerate, Optional.empty());
        externalObjectDirectoryEntities
            = externalObjectDirectoryStub.generateWithStatusAndTranscriptionAndAnnotationAndLocation(ExternalLocationTypeEnum.INBOUND,  STORED, numberOfRecordsToGenerate, Optional.empty());
        entitiesToBeMarkedWithMediaOrAnnotationOutsideOfArmHours
            = externalObjectDirectoryEntities.subList(0, externalObjectDirectoryEntities.size() / 2);

        expectedArmRecordsResultOutsideHours
            = externalObjectDirectoryStub.generateWithStatusAndTranscriptionAndAnnotationAndArmLocation(
            externalObjectDirectoryEntities.subList(0, externalObjectDirectoryEntities.size() / 2), Optional.of(lastModifiedBeforeCurrentTime));
        expectedArmRecordsResultWithinTheHour
            = externalObjectDirectoryStub.generateWithStatusAndTranscriptionAndAnnotationAndArmLocation(
            externalObjectDirectoryEntities.subList(externalObjectDirectoryEntities.size() / 2, externalObjectDirectoryEntities.size()), Optional.of(lastModifiedNotBeforeThreshold));

        int expectedRecords = externalObjectDirectoryEntitiesNotRelevant.size() + externalObjectDirectoryEntities.size()
            + expectedArmRecordsResultOutsideHours.size() + expectedArmRecordsResultWithinTheHour.size();

        // assert that the test has inserted the data into the database
        Assertions.assertEquals(expectedRecords, externalObjectDirectoryRepository.findAll().size());
    }

    private void generateDataWithAnnotation(int hoursBeforeCurrentTimeForArm, int weeksBeforeCurrentTimeForUnstructured) throws Exception {
        int numberOfRecordsToGenerate = 10;
        int setupHoursBeforeCurrentTime = hoursBeforeCurrentTimeForArm;

        OffsetDateTime lastModifiedBeforeCurrentTimeForArm = currentTimeHelper.currentOffsetDateTime().minus(
            setupHoursBeforeCurrentTime,
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

        externalObjectDirectoryEntitiesNotRelevant
            = externalObjectDirectoryStub.generateWithStatusAndTranscriptionAndAnnotationAndLocation(ExternalLocationTypeEnum.UNSTRUCTURED, ObjectRecordStatusEnum.ARM_RAW_DATA_FAILED, numberOfRecordsToGenerate, Optional.of(lastModifiedBeforeCurrentTimeForUnstructured));
        externalObjectDirectoryEntities
            = externalObjectDirectoryStub.generateWithStatusAndTranscriptionAndAnnotationAndLocation(ExternalLocationTypeEnum.UNSTRUCTURED,  STORED, numberOfRecordsToGenerate, Optional.of(lastModifiedBeforeCurrentTimeForUnstructured));
        entitiesToBeMarkedWithMediaOrAnnotationOutsideOfArmHours
            = externalObjectDirectoryEntities.subList(0, externalObjectDirectoryEntities.size() / 2);

        expectedArmRecordsResultOutsideHours
            = externalObjectDirectoryStub.generateWithStatusAndTranscriptionAndAnnotationAndArmLocation(
            externalObjectDirectoryEntities.subList(0, externalObjectDirectoryEntities.size() / 2), Optional.of(lastModifiedBeforeCurrentTimeForArm));
        expectedArmRecordsResultWithinTheHour
            = externalObjectDirectoryStub.generateWithStatusAndTranscriptionAndAnnotationAndArmLocation(
            expectedArmRecordsResultOutsideHours, Optional.of(lastModifiedNotBeforeThreshold));

        int expectedRecords = externalObjectDirectoryEntitiesNotRelevant.size() + externalObjectDirectoryEntities.size()
            + expectedArmRecordsResultOutsideHours.size() + expectedArmRecordsResultWithinTheHour.size();

        // assert that the test has inserted the data into the database
        Assertions.assertEquals(expectedRecords, externalObjectDirectoryRepository.findAll().size());
    }

    private void generateDataWithMedia(int hoursBeforeCurrentTime) throws Exception {
        int numberOfRecordsToGenerate = 10;
        int setupHoursBeforeCurrentTime = hoursBeforeCurrentTime;

        OffsetDateTime lastModifiedBeforeCurrentTime = currentTimeHelper.currentOffsetDateTime().minus(
            setupHoursBeforeCurrentTime,
            ChronoUnit.HOURS
        );

        OffsetDateTime lastModifiedNotBeforeThreshold = currentTimeHelper.currentOffsetDateTime().minus(
            1,
            ChronoUnit.HOURS
        );

        externalObjectDirectoryEntitiesNotRelevant
            = externalObjectDirectoryStub.generateWithStatusAndMediaLocation(ExternalLocationTypeEnum.INBOUND, ObjectRecordStatusEnum.ARM_RAW_DATA_FAILED, numberOfRecordsToGenerate, Optional.empty());
        externalObjectDirectoryEntities
            = externalObjectDirectoryStub.generateWithStatusAndMediaLocation(ExternalLocationTypeEnum.INBOUND,  STORED, numberOfRecordsToGenerate, Optional.empty());
        entitiesToBeMarkedWithMediaOrAnnotationOutsideOfArmHours
            = externalObjectDirectoryEntities.subList(0, externalObjectDirectoryEntities.size() / 2);

        expectedArmRecordsResultOutsideHours
            = externalObjectDirectoryStub.generateWithStatusAndMediaAndArmLocation(
            entitiesToBeMarkedWithMediaOrAnnotationOutsideOfArmHours, Optional.of(lastModifiedBeforeCurrentTime));
        expectedArmRecordsResultWithinTheHour
            = externalObjectDirectoryStub.generateWithStatusAndMediaAndArmLocation(
            externalObjectDirectoryEntities.subList(externalObjectDirectoryEntities.size() / 2, externalObjectDirectoryEntities.size()), Optional.of(lastModifiedNotBeforeThreshold));

        int expectedRecords = externalObjectDirectoryEntitiesNotRelevant.size() + externalObjectDirectoryEntities.size()
            + expectedArmRecordsResultOutsideHours.size() + expectedArmRecordsResultWithinTheHour.size();

        // assert that the test has inserted the data into the database
        Assertions.assertEquals(expectedRecords, externalObjectDirectoryRepository.findAll().size());
    }
}