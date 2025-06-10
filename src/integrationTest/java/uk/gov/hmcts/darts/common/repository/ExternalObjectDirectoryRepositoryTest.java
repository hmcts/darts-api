package uk.gov.hmcts.darts.common.repository;

import lombok.AllArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.CaseDocumentEntity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectRecordStatusEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.util.EodHelper;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceScoreEnum;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.DartsPersistence;
import uk.gov.hmcts.darts.testutils.stubs.ExternalObjectDirectoryStub;

import java.lang.reflect.InvocationTargetException;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.ARM_RPO_PENDING;
import static uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum.STORED;

class ExternalObjectDirectoryRepositoryTest extends PostgresIntegrationBase {

    @Autowired
    private ExternalObjectDirectoryRepository externalObjectDirectoryRepository;

    @Autowired
    private ExternalObjectDirectoryStub externalObjectDirectoryStub;

    @Autowired
    private CurrentTimeHelper currentTimeHelper;

    private List<ExternalObjectDirectoryEntity> entitiesToBeMarkedWithMediaOrAnnotationOutsideOfArmHours;
    private OffsetDateTime ingestionEndDateTime;

    @Test
    void testGetDirectoryIfMediaDate24Hours() throws Exception {

        int setupHoursBeforeCurrentTime = 24;

        // setup the test data
        generateDataWithMediaForInbound(setupHoursBeforeCurrentTime);

        int hourDurationBeyondHours = setupHoursBeforeCurrentTime; // which no records are

        // exercise the logic
        List<Long> results = externalObjectDirectoryRepository
            .findIdsIn2StorageLocationsBeforeTime(
                EodHelper.storedStatus(), EodHelper.storedStatus(),
                EodHelper.inboundLocation(), EodHelper.armLocation(),
                getCurrentDateTimeWithHoursBefore(hourDurationBeyondHours),
                ExternalObjectDirectoryQueryTypeEnum.MEDIA_QUERY.getIndex(),
                Limit.of(100_000));

        // assert the logic
        assertExpectedResults(results, entitiesToBeMarkedWithMediaOrAnnotationOutsideOfArmHours,
                              entitiesToBeMarkedWithMediaOrAnnotationOutsideOfArmHours.size());
    }

    @Test
    void testGetDirectoryIfMediaDateBeyond24Hours() throws Exception {

        int setupHoursBeforeCurrentTime = 26;

        // setup the test data
        generateDataWithMediaForInbound(setupHoursBeforeCurrentTime);

        int hourDurationBeyondHours = setupHoursBeforeCurrentTime; // which no records are

        // excerise the logic
        List<Long> results = externalObjectDirectoryRepository
            .findIdsIn2StorageLocationsBeforeTime(EodHelper.storedStatus(), EodHelper.storedStatus(),
                                                  EodHelper.inboundLocation(), EodHelper.armLocation(),
                                                  getCurrentDateTimeWithHoursBefore(hourDurationBeyondHours),
                                                  ExternalObjectDirectoryQueryTypeEnum.MEDIA_QUERY.getIndex(),
                                                  Limit.of(100_000));

        // assert the logic
        assertExpectedResults(results, entitiesToBeMarkedWithMediaOrAnnotationOutsideOfArmHours,
                              entitiesToBeMarkedWithMediaOrAnnotationOutsideOfArmHours.size());
    }

    @Test
    void testGetDirectoryIfAnnotationDate24Hours() throws Exception {

        int setupHoursBeforeCurrentTime = 24;

        // setup the test data
        generateDataWithAnnotationForInbound(setupHoursBeforeCurrentTime);

        int hourDurationBeyondHours = setupHoursBeforeCurrentTime; // which no records are

        // exercise the logic
        List<Long> results = externalObjectDirectoryRepository
            .findIdsIn2StorageLocationsBeforeTime(
                EodHelper.storedStatus(), EodHelper.storedStatus(),
                EodHelper.inboundLocation(), EodHelper.armLocation(),
                getCurrentDateTimeWithHoursBefore(hourDurationBeyondHours), ExternalObjectDirectoryQueryTypeEnum.ANNOTATION_QUERY.getIndex(),
                Limit.of(100_000));

        // assert the logic
        assertExpectedResults(results, entitiesToBeMarkedWithMediaOrAnnotationOutsideOfArmHours,
                              entitiesToBeMarkedWithMediaOrAnnotationOutsideOfArmHours.size());
    }

    @Test
    void testGetDirectoryIfAnnotationArmDateAndUnstructuredDateOutsideOfBounds() throws Exception {

        int setupArmHoursBeforeCurrentTime = 24;
        int setupUnstructuredWeeksBeforeCurrentTime = 4;

        // setup the test data
        generateDataWithAnnotationForUnstructured(setupArmHoursBeforeCurrentTime, setupUnstructuredWeeksBeforeCurrentTime);

        // exercise the logic
        List<Long> results = externalObjectDirectoryRepository
            .findIdsIn2StorageLocationsBeforeTime(
                EodHelper.storedStatus(), EodHelper.storedStatus(),
                EodHelper.unstructuredLocation(), EodHelper.armLocation(),
                getCurrentDateTimeWithWeeksBefore(setupUnstructuredWeeksBeforeCurrentTime),
                getCurrentDateTimeWithHoursBefore(setupArmHoursBeforeCurrentTime),
                Limit.of(100_000));

        // assert the logic
        assertExpectedResults(results, entitiesToBeMarkedWithMediaOrAnnotationOutsideOfArmHours,
                              entitiesToBeMarkedWithMediaOrAnnotationOutsideOfArmHours.size());
    }

    @Test
    void testGetDirectoryIfAnnotationArmDateAndUnstructuredDateWithNoRecordsFoundDueToArmDateBeingAcceptable() throws Exception {

        int setupArmHoursBeforeCurrentTime = 24;
        int setupUnstructuredWeeksBeforeCurrentTime = 4;

        // setup the test data
        generateDataWithAnnotationForUnstructured(setupArmHoursBeforeCurrentTime, setupUnstructuredWeeksBeforeCurrentTime);

        // exercise the logic
        List<Long> results = externalObjectDirectoryRepository
            .findIdsIn2StorageLocationsBeforeTime(
                EodHelper.storedStatus(), EodHelper.storedStatus(),
                EodHelper.unstructuredLocation(), EodHelper.armLocation(),
                getCurrentDateTimeWithWeeksBefore(setupUnstructuredWeeksBeforeCurrentTime),
                getCurrentDateTimeWithHoursBefore(setupArmHoursBeforeCurrentTime + 1),
                Limit.of(100_000));

        // assert the logic
        assertTrue(results.isEmpty());
    }

    @Test
    void testGetDirectoryIfMediaDateNotBeyondThreshold() throws Exception {

        int setupHoursBeforeCurrentTime = 22;

        // setup the test data
        generateDataWithMediaForInbound(setupHoursBeforeCurrentTime);

        int hourDurationBeyondHours = 24; // which no records are

        // exercise the logic
        List<Long> results = externalObjectDirectoryRepository
            .findIdsIn2StorageLocationsBeforeTime(
                EodHelper.storedStatus(), EodHelper.storedStatus(),
                EodHelper.inboundLocation(), EodHelper.armLocation(),
                getCurrentDateTimeWithHoursBefore(hourDurationBeyondHours),
                ExternalObjectDirectoryQueryTypeEnum.MEDIA_QUERY.getIndex(),
                Limit.of(100_000));

        // assert the logic
        assertTrue(results.isEmpty());
    }

    @Test
    void testFindStoredInInboundAndUnstructuredByMediaId() throws Exception {
        // Setup
        int hoursBeforeCurrentTime = 24;
        generateDataWithMediaForInbound(hoursBeforeCurrentTime);

        // Get a media entity to test with
        List<ExternalObjectDirectoryEntity> allEntities = externalObjectDirectoryRepository.findAll();
        ExternalObjectDirectoryEntity testEntity = allEntities.stream()
            .filter(e -> e.getMedia() != null && Objects.equals(e.getStatus().getId(), STORED.getId())
                && (Objects.equals(e.getExternalLocationType().getId(), ExternalLocationTypeEnum.INBOUND.getId())
                || Objects.equals(e.getExternalLocationType().getId(), ExternalLocationTypeEnum.UNSTRUCTURED.getId())))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No suitable test entity found"));

        // Exercise
        List<ExternalObjectDirectoryEntity> result = externalObjectDirectoryRepository.findStoredInInboundAndUnstructuredByMediaId(
            testEntity.getMedia().getId());

        // Verify
        assertFalse(result.isEmpty(), "Result should not be empty");
        assertTrue(result.stream().allMatch(e -> e.getMedia().getId().equals(testEntity.getMedia().getId())),
                   "All results should have the correct media ID");
        assertTrue(result.stream().allMatch(e -> Objects.equals(e.getStatus().getId(), STORED.getId())),
                   "All results should have the STORED status");
        assertTrue(result.stream().allMatch(e -> Objects.equals(e.getExternalLocationType().getId(), ExternalLocationTypeEnum.INBOUND.getId())
                       || Objects.equals(e.getExternalLocationType().getId(), ExternalLocationTypeEnum.UNSTRUCTURED.getId())),
                   "All results should have either INBOUND or UNSTRUCTURED location type");
    }

    @Test
    void testFindStoredInInboundAndUnstructuredByTranscriptionId() throws Exception {
        // Setup
        int hoursBeforeCurrentTime = 24;
        generateDataWithAnnotationForInbound(hoursBeforeCurrentTime);

        // Get a transcription document entity to test with
        List<ExternalObjectDirectoryEntity> allEntities = externalObjectDirectoryRepository.findAll();
        ExternalObjectDirectoryEntity testEntity = allEntities.stream()
            .filter(e -> e.getTranscriptionDocumentEntity() != null && Objects.equals(e.getStatus().getId(), STORED.getId())
                && (Objects.equals(e.getExternalLocationType().getId(), ExternalLocationTypeEnum.INBOUND.getId())
                || Objects.equals(e.getExternalLocationType().getId(), ExternalLocationTypeEnum.UNSTRUCTURED.getId())))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No suitable test entity found"));

        // Exercise
        List<ExternalObjectDirectoryEntity> result = externalObjectDirectoryRepository.findStoredInInboundAndUnstructuredByTranscriptionId(
            testEntity.getTranscriptionDocumentEntity().getId());

        // Verify
        assertFalse(result.isEmpty(), "Result should not be empty");
        assertTrue(result.stream().allMatch(e -> e.getTranscriptionDocumentEntity().getId().equals(testEntity.getTranscriptionDocumentEntity().getId())),
                   "All results should have the correct transcription document ID");
        assertTrue(result.stream().allMatch(e -> Objects.equals(e.getStatus().getId(), STORED.getId())),
                   "All results should have the STORED status");
        assertTrue(result.stream().allMatch(e -> Objects.equals(e.getExternalLocationType().getId(), ExternalLocationTypeEnum.INBOUND.getId())
                       || Objects.equals(e.getExternalLocationType().getId(), ExternalLocationTypeEnum.UNSTRUCTURED.getId())),
                   "All results should have either INBOUND or UNSTRUCTURED location type");
    }

    @Test
    void findAllByStatusAndInputUploadProcessedTsBetweenAndLimit_ReturnsResults()
        throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        // given
        OffsetDateTime now = currentTimeHelper.currentOffsetDateTime();
        OffsetDateTime pastCurrentDateTime = now.minusHours(30);

        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntities
            = externalObjectDirectoryStub.generateWithStatusAndMediaLocation(
            ExternalLocationTypeEnum.ARM, ARM_RPO_PENDING, 20, Optional.of(pastCurrentDateTime));

        OffsetDateTime ingestionStartDateTime = currentTimeHelper.currentOffsetDateTime().minusHours(30);
        externalObjectDirectoryEntities.forEach(eod -> {
            if (eod.getId() % 2 == 0 && (eod.getId() % 3 != 0)) {
                // within the time range
                eod.setCreatedDateTime(ingestionStartDateTime);
                eod.setInputUploadProcessedTs(currentTimeHelper.currentOffsetDateTime().minusHours(26));
            } else if (eod.getId() % 3 == 0) {
                // before the time range
                eod.setCreatedDateTime(currentTimeHelper.currentOffsetDateTime().minusHours(40));
                eod.setInputUploadProcessedTs(currentTimeHelper.currentOffsetDateTime().minusHours(31));
            } else {
                // after the time range
                eod.setCreatedDateTime(currentTimeHelper.currentOffsetDateTime().minusHours(15));
                eod.setInputUploadProcessedTs(currentTimeHelper.currentOffsetDateTime().minusHours(10));
            }
        });
        dartsPersistence.saveAll(externalObjectDirectoryEntities);

        // Verify test data setup
        List<ExternalObjectDirectoryEntity> allEntities = externalObjectDirectoryRepository.findAll();
        assertEquals(20, allEntities.size(), "Test data setup failed: Expected 20 entities in the database");

        // when
        ingestionEndDateTime = currentTimeHelper.currentOffsetDateTime().minusHours(24);
        var results = externalObjectDirectoryRepository.findAllByStatusAndInputUploadProcessedTsBetweenAndLimit(
            EodHelper.armRpoPendingStatus(), ingestionStartDateTime, ingestionEndDateTime,
            Limit.of(20));

        // then
        assertEquals(7, results.size(), "Query returned unexpected number of results");
        results.forEach(eod -> {
            assertTrue(eod.getInputUploadProcessedTs().isAfter(ingestionStartDateTime));
            assertTrue(eod.getInputUploadProcessedTs().isBefore(ingestionEndDateTime));
        });
    }

    @Test
    void findIdsByStatusAndLastModifiedBetweenAndLocationAndLimit_Success() throws Exception {
        // given
        ObjectRecordStatusEntity status = EodHelper.armRpoPendingStatus();
        ExternalLocationTypeEntity locationType = EodHelper.armLocation();
        OffsetDateTime pastCurrentDateTime1 = OffsetDateTime.now().minusHours(2);
        OffsetDateTime pastCurrentDateTime2 = OffsetDateTime.now().minusDays(2);

        externalObjectDirectoryStub.generateWithStatusAndMediaLocation(
            ExternalLocationTypeEnum.ARM, ARM_RPO_PENDING, 2, Optional.of(pastCurrentDateTime1));

        externalObjectDirectoryStub.generateWithStatusAndMediaLocation(
            ExternalLocationTypeEnum.ARM, ARM_RPO_PENDING, 2, Optional.of(pastCurrentDateTime2));

        OffsetDateTime startDateTime = currentTimeHelper.currentOffsetDateTime().minusHours(10);
        OffsetDateTime endDateTime = currentTimeHelper.currentOffsetDateTime().minusHours(1);

        // when
        List<Long> result = externalObjectDirectoryRepository.findIdsByStatusAndLastModifiedBetweenAndLocationAndLimit(
            status, startDateTime, endDateTime, locationType, Limit.of(10)
        );

        // then
        assertThat(result).hasSize(2);
    }

    @Test
    void findIdsByStatusAndLastModifiedBetweenAndLocationAndLimit_NoResults() {
        ObjectRecordStatusEntity status = EodHelper.armRpoPendingStatus();
        ExternalLocationTypeEntity locationType = EodHelper.armLocation();
        OffsetDateTime newStartDateTime = currentTimeHelper.currentOffsetDateTime().minusDays(2);
        OffsetDateTime newEndDateTime = currentTimeHelper.currentOffsetDateTime().minusDays(1);

        List<Long> result = externalObjectDirectoryRepository.findIdsByStatusAndLastModifiedBetweenAndLocationAndLimit(
            status, newStartDateTime, newEndDateTime, locationType, Limit.of(10)
        );

        assertThat(result).isEmpty();
    }

    @Test
    void findByStatusAndInputUploadProcessedTsWithPaging_ReturnsResults() throws Exception {
        // Given
        OffsetDateTime pastCurrentDateTime1 = OffsetDateTime.now().minusHours(2);
        OffsetDateTime pastCurrentDateTime2 = OffsetDateTime.now().minusHours(20);

        List<ExternalObjectDirectoryEntity> matchingEods = externalObjectDirectoryStub.generateWithStatusAndMediaLocation(
            ExternalLocationTypeEnum.ARM, ARM_RPO_PENDING, 11, Optional.of(pastCurrentDateTime1));
        matchingEods.forEach(eod -> {
            eod.setInputUploadProcessedTs(pastCurrentDateTime1);
        });
        dartsPersistence.saveAll(matchingEods);
        assertEquals(11, matchingEods.size());

        List<ExternalObjectDirectoryEntity> nonMatchingEods = externalObjectDirectoryStub.generateWithStatusAndMediaLocation(
            ExternalLocationTypeEnum.ARM, ARM_RPO_PENDING, 4, Optional.of(pastCurrentDateTime2));
        nonMatchingEods.forEach(eod -> {
            eod.setInputUploadProcessedTs(pastCurrentDateTime2);
        });
        dartsPersistence.saveAll(nonMatchingEods);
        assertEquals(4, nonMatchingEods.size());

        OffsetDateTime startDateTime = currentTimeHelper.currentOffsetDateTime().minusHours(10);
        OffsetDateTime endDateTime = currentTimeHelper.currentOffsetDateTime().minusHours(1);

        Pageable pageable = PageRequest.of(0, 10);
        ObjectRecordStatusEntity status = EodHelper.armRpoPendingStatus();


        // When
        Page<ExternalObjectDirectoryEntity> result = externalObjectDirectoryRepository.findByStatusAndInputUploadProcessedTsWithPaging(
            status, startDateTime, endDateTime, pageable
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(10);
        assertThat(result.getTotalElements()).isEqualTo(11);
        result.getContent().forEach(entity -> {
            assertThat(entity.getStatus().getId()).isEqualTo(status.getId());
            assertThat(entity.getInputUploadProcessedTs()).isBetween(startDateTime, endDateTime);
        });
    }

    @Transactional
    @Test
    void updateEodStatusAndTransferAttemptsWhereIdIn_UpdatesValues() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        // Given
        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntities = externalObjectDirectoryStub.generateWithStatusAndMediaLocation(
            ExternalLocationTypeEnum.ARM, ARM_RPO_PENDING, 10, Optional.empty());
        dartsPersistence.saveAll(externalObjectDirectoryEntities);

        List<Long> idsToUpdate = externalObjectDirectoryEntities.stream().map(ExternalObjectDirectoryEntity::getId).toList();

        // When
        externalObjectDirectoryRepository.updateEodStatusAndTransferAttemptsWhereIdIn(
            EodHelper.storedStatus(), 0, 0, idsToUpdate);

        // Then
        List<ExternalObjectDirectoryEntity> updatedEntities = externalObjectDirectoryRepository.findAllById(idsToUpdate);
        assertThat(updatedEntities).hasSize(idsToUpdate.size());
        updatedEntities.forEach(entity -> {
            assertThat(entity.getStatus().getId()).isEqualTo(EodHelper.storedStatus().getId());
            assertThat(entity.getTransferAttempts()).isEqualTo(0);
            assertThat(entity.getInputUploadProcessedTs()).isNull();
        });

    }

    private void assertExpectedResults(List<Long> actualResults, List<ExternalObjectDirectoryEntity> expectedResults, int resultCount) {
        List<Long> matchesEntity = new ArrayList<>(
            actualResults.stream().filter(expectedResult -> expectedResults.stream().anyMatch(result -> expectedResult.equals(result.getId()))).toList());

        assertEquals(resultCount, matchesEntity.size());
    }

    private OffsetDateTime getCurrentDateTimeWithHoursBefore(int hours) {
        return currentTimeHelper.currentOffsetDateTime().minusHours(hours);
    }

    private OffsetDateTime getCurrentDateTimeWithWeeksBefore(int hours) {
        return currentTimeHelper.currentOffsetDateTime().minusHours(hours);
    }

    private void generateDataWithAnnotationForInbound(int hoursBeforeCurrentTime)
        throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        int numberOfRecordsToGenerate = 10;

        OffsetDateTime lastModifiedBeforeCurrentTime = currentTimeHelper.currentOffsetDateTime().minusHours(hoursBeforeCurrentTime);

        OffsetDateTime lastModifiedNotBeforeThreshold = currentTimeHelper.currentOffsetDateTime().minusHours(1);
        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntitiesNotRelevant;
        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntities;
        List<ExternalObjectDirectoryEntity> expectedArmRecordsResultOutsideHours;
        List<ExternalObjectDirectoryEntity> expectedArmRecordsResultWithinTheHour;

        externalObjectDirectoryEntitiesNotRelevant
            = externalObjectDirectoryStub
            .generateWithStatusAndTranscriptionOrAnnotationAndLocation(
                ExternalLocationTypeEnum.INBOUND, ObjectRecordStatusEnum.ARM_RAW_DATA_FAILED, numberOfRecordsToGenerate, Optional.empty());
        externalObjectDirectoryEntities
            = externalObjectDirectoryStub
            .generateWithStatusAndTranscriptionOrAnnotationAndLocation(
                ExternalLocationTypeEnum.INBOUND, STORED, numberOfRecordsToGenerate, Optional.empty());
        entitiesToBeMarkedWithMediaOrAnnotationOutsideOfArmHours
            = externalObjectDirectoryEntities.subList(0, externalObjectDirectoryEntities.size() / 2);

        expectedArmRecordsResultOutsideHours
            = externalObjectDirectoryStub
            .generateWithStatusAndTranscriptionAndAnnotationAndArmLocation(
                externalObjectDirectoryEntities.subList(0, externalObjectDirectoryEntities.size() / 2), Optional.of(lastModifiedBeforeCurrentTime));
        expectedArmRecordsResultWithinTheHour
            = externalObjectDirectoryStub
            .generateWithStatusAndTranscriptionAndAnnotationAndArmLocation(
                externalObjectDirectoryEntities
                    .subList(externalObjectDirectoryEntities.size() / 2, externalObjectDirectoryEntities.size()), Optional.of(lastModifiedNotBeforeThreshold));

        int expectedRecords = externalObjectDirectoryEntitiesNotRelevant.size() + externalObjectDirectoryEntities.size()
            + expectedArmRecordsResultOutsideHours.size() + expectedArmRecordsResultWithinTheHour.size();

        // assert that the test has inserted the data into the database
        assertEquals(expectedRecords, externalObjectDirectoryRepository.findAll().size());
    }

    private void generateDataWithAnnotationForUnstructured(int hoursBeforeCurrentTimeForArm,
                                                           int weeksBeforeCurrentTimeForUnstructured)
        throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        int numberOfRecordsToGenerate = 10;

        OffsetDateTime lastModifiedBeforeCurrentTimeForArm = currentTimeHelper.currentOffsetDateTime().minusHours(hoursBeforeCurrentTimeForArm);

        OffsetDateTime lastModifiedBeforeCurrentTimeForUnstructured = currentTimeHelper.currentOffsetDateTime().minus(
            weeksBeforeCurrentTimeForUnstructured,
            ChronoUnit.WEEKS
        );

        OffsetDateTime lastModifiedNotBeforeThreshold = currentTimeHelper.currentOffsetDateTime().minusHours(1);

        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntitiesNotRelevant;
        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntities;
        List<ExternalObjectDirectoryEntity> expectedArmRecordsResultOutsideHours;
        List<ExternalObjectDirectoryEntity> expectedArmRecordsResultWithinTheHour;

        externalObjectDirectoryEntitiesNotRelevant
            = externalObjectDirectoryStub
            .generateWithStatusAndTranscriptionOrAnnotationAndLocation(
                ExternalLocationTypeEnum.UNSTRUCTURED,
                ObjectRecordStatusEnum.ARM_RAW_DATA_FAILED, numberOfRecordsToGenerate, Optional.of(lastModifiedBeforeCurrentTimeForUnstructured));
        externalObjectDirectoryEntities
            = externalObjectDirectoryStub
            .generateWithStatusAndTranscriptionOrAnnotationAndLocation(
                ExternalLocationTypeEnum.UNSTRUCTURED, STORED, numberOfRecordsToGenerate, Optional.of(lastModifiedBeforeCurrentTimeForUnstructured));
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
        assertEquals(expectedRecords, externalObjectDirectoryRepository.findAll().size());
    }

    private void generateDataWithMediaForInbound(int hoursBeforeCurrentTime)
        throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        int numberOfRecordsToGenerate = 10;

        OffsetDateTime lastModifiedBeforeCurrentTime = currentTimeHelper.currentOffsetDateTime().minusHours(hoursBeforeCurrentTime);

        OffsetDateTime lastModifiedNotBeforeThreshold = currentTimeHelper.currentOffsetDateTime().minusHours(1);

        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntitiesNotRelevant;
        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntities;
        List<ExternalObjectDirectoryEntity> expectedArmRecordsResultOutsideHours;
        List<ExternalObjectDirectoryEntity> expectedArmRecordsResultWithinTheHour;
        externalObjectDirectoryEntitiesNotRelevant
            = externalObjectDirectoryStub
            .generateWithStatusAndMediaLocation(
                ExternalLocationTypeEnum.INBOUND, ObjectRecordStatusEnum.ARM_RAW_DATA_FAILED, numberOfRecordsToGenerate, Optional.empty());
        externalObjectDirectoryEntities
            = externalObjectDirectoryStub
            .generateWithStatusAndMediaLocation(
                ExternalLocationTypeEnum.INBOUND, STORED, numberOfRecordsToGenerate, Optional.empty());
        entitiesToBeMarkedWithMediaOrAnnotationOutsideOfArmHours
            = externalObjectDirectoryEntities.subList(0, externalObjectDirectoryEntities.size() / 2);

        expectedArmRecordsResultOutsideHours
            = externalObjectDirectoryStub.generateWithStatusAndMediaAndArmLocation(
            entitiesToBeMarkedWithMediaOrAnnotationOutsideOfArmHours, Optional.of(lastModifiedBeforeCurrentTime));
        expectedArmRecordsResultWithinTheHour
            = externalObjectDirectoryStub.generateWithStatusAndMediaAndArmLocation(
            externalObjectDirectoryEntities.subList(
                externalObjectDirectoryEntities.size() / 2, externalObjectDirectoryEntities.size()), Optional.of(lastModifiedNotBeforeThreshold));

        int expectedRecords = externalObjectDirectoryEntitiesNotRelevant.size() + externalObjectDirectoryEntities.size()
            + expectedArmRecordsResultOutsideHours.size() + expectedArmRecordsResultWithinTheHour.size();

        // assert that the test has inserted the data into the database
        assertEquals(expectedRecords, externalObjectDirectoryRepository.findAll().size());
    }

    @Nested
    @DisplayName("findByExternalLocationTypeAndUpdateRetention")
    class FindByExternalLocationTypeAndUpdateRetention {

        @ParameterizedTest
        @EnumSource(EodItemType.class)
        void shouldReturnEmptyList_whenNoDataMatchingExternalLocationTypeIsFound(EodItemType eodItemType) {
            ExternalLocationTypeEntity externalLocationTypeEntity = EodHelper.armLocation();
            //Create Eod with non matching external location type
            createEod(eodItemType, EodHelper.inboundLocation(), OffsetDateTime.now(),
                      RetentionConfidenceScoreEnum.CASE_PERFECTLY_CLOSED, true);

            assertThat(
                externalObjectDirectoryRepository.findByExternalLocationTypeAndUpdateRetention(
                    externalLocationTypeEntity, true, Limit.of(10))
            ).isEmpty();
        }

        @ParameterizedTest
        @EnumSource(EodItemType.class)
        void shouldReturnEmptyList_whenRetainUntilTsIsNotSet(EodItemType eodItemType) {
            ExternalLocationTypeEntity externalLocationTypeEntity = EodHelper.armLocation();
            //Create Eod with retainUntilTs not set
            createEod(eodItemType, externalLocationTypeEntity, null,
                      RetentionConfidenceScoreEnum.CASE_PERFECTLY_CLOSED, true);

            assertThat(
                externalObjectDirectoryRepository.findByExternalLocationTypeAndUpdateRetention(
                    externalLocationTypeEntity, true, Limit.of(10))
            ).isEmpty();
        }

        @ParameterizedTest
        @EnumSource(EodItemType.class)
        void shouldReturnEmptyList_whenRetConfScoreTsIsNotSet(EodItemType eodItemType) {
            ExternalLocationTypeEntity externalLocationTypeEntity = EodHelper.armLocation();
            //Create Eod with non matching external location type
            createEod(eodItemType, externalLocationTypeEntity, OffsetDateTime.now(),
                      null, true);

            assertThat(
                externalObjectDirectoryRepository.findByExternalLocationTypeAndUpdateRetention(
                    externalLocationTypeEntity, true, Limit.of(10))
            ).isEmpty();
        }

        @ParameterizedTest
        @EnumSource(EodItemType.class)
        void shouldReturnEmptyList_whenUpdateRetentionDoesNotMatch(EodItemType eodItemType) {
            ExternalLocationTypeEntity externalLocationTypeEntity = EodHelper.armLocation();
            //Create Eod with non matching external location type
            createEod(eodItemType, externalLocationTypeEntity, OffsetDateTime.now(),
                      null, true);

            assertThat(
                externalObjectDirectoryRepository.findByExternalLocationTypeAndUpdateRetention(
                    externalLocationTypeEntity, false, Limit.of(10))
            ).isEmpty();
        }

        @ParameterizedTest
        @EnumSource(EodItemType.class)
        void shouldReturnIds_whenRetainUntilTsAndRetConfScoreIsSet(EodItemType eodItemType) {
            ExternalLocationTypeEntity externalLocationTypeEntity = EodHelper.armLocation();
            //Create Eod with non matching external location type
            ExternalObjectDirectoryEntity eod1 =
                createEod(eodItemType, externalLocationTypeEntity, OffsetDateTime.now(),
                          RetentionConfidenceScoreEnum.CASE_PERFECTLY_CLOSED, true);

            ExternalObjectDirectoryEntity eod2 =
                createEod(eodItemType, externalLocationTypeEntity, OffsetDateTime.now(),
                          RetentionConfidenceScoreEnum.CASE_NOT_PERFECTLY_CLOSED, true);

            ExternalObjectDirectoryEntity eod3 =
                createEod(eodItemType, externalLocationTypeEntity, OffsetDateTime.now(),
                          RetentionConfidenceScoreEnum.CASE_PERFECTLY_CLOSED, true);

            //Create EOD with updateRetention set to false should not be returned
            createEod(eodItemType, externalLocationTypeEntity, OffsetDateTime.now(),
                      RetentionConfidenceScoreEnum.CASE_PERFECTLY_CLOSED, false);

            assertThat(
                externalObjectDirectoryRepository.findByExternalLocationTypeAndUpdateRetention(
                    externalLocationTypeEntity, true, Limit.of(10))
            ).hasSize(3)
                .contains(eod1.getId(), eod2.getId(), eod3.getId());
        }

        @FunctionalInterface
        interface CreateAndAssociateToFunction {
            void createAndAssociateTo(
                DartsPersistence dartsPersistence,
                ExternalObjectDirectoryEntity eod,
                OffsetDateTime retainUntilTs,
                RetentionConfidenceScoreEnum retConfScore);
        }

        @AllArgsConstructor
        enum EodItemType {
            MEDIA((dartsPersistence, eod, retainUntilTs, retConfScore) -> {
                MediaEntity media =
                    dartsPersistence.save(
                        PersistableFactory.getMediaTestData()
                            .someMinimalBuilder()
                            .retainUntilTs(retainUntilTs)
                            .retConfScore(retConfScore)
                            .build()
                            .getEntity()
                    );
                eod.setMedia(media);
            }),
            TRANSCRIPTION_DOCUMENT((dartsPersistence, eod, retainUntilTs, retConfScore) -> {
                TranscriptionDocumentEntity transcriptionDocumentEntity =
                    dartsPersistence.save(
                        PersistableFactory.getTranscriptionDocument()
                            .someMinimalBuilder()
                            .retainUntilTs(retainUntilTs)
                            .retConfScore(retConfScore)
                            .build()
                            .getEntity()
                    );
                eod.setTranscriptionDocumentEntity(transcriptionDocumentEntity);
            }),
            ANNOTATION_DOCUMENT((dartsPersistence, eod, retainUntilTs, retConfScore) -> {
                AnnotationDocumentEntity annotationDocument =
                    dartsPersistence.save(
                        PersistableFactory.getAnnotationDocumentTestData()
                            .someMinimalBuilder()
                            .retainUntilTs(retainUntilTs)
                            .retConfScore(retConfScore)
                            .build()
                            .getEntity()
                    );
                eod.setAnnotationDocumentEntity(annotationDocument);
            }),
            CASE_DOCUMENT((dartsPersistence, eod, retainUntilTs, retConfScore) -> {
                CaseDocumentEntity caseDocumentEntity =
                    dartsPersistence.save(
                        PersistableFactory.getCaseDocumentTestData()
                            .someMinimalBuilder()
                            .retainUntilTs(retainUntilTs)
                            .retConfScore(retConfScore)
                            .build()
                            .getEntity()
                    );
                eod.setCaseDocument(caseDocumentEntity);
            });

            private final CreateAndAssociateToFunction createAndAssociateToFunction;

            public void createAndAssociateTo(
                DartsPersistence dartsPersistence,
                ExternalObjectDirectoryEntity eod,
                OffsetDateTime retainUntilTs,
                RetentionConfidenceScoreEnum retConfScore) {
                createAndAssociateToFunction.createAndAssociateTo(dartsPersistence, eod, retainUntilTs, retConfScore);
            }
        }

        private ExternalObjectDirectoryEntity createEod(
            EodItemType type,
            ExternalLocationTypeEntity externalLocationType,
            OffsetDateTime retainUntilTs,
            RetentionConfidenceScoreEnum retConfScore,
            boolean updateRetention
        ) {
            ExternalObjectDirectoryEntity eod = PersistableFactory.getExternalObjectDirectoryTestData()
                .someMinimalBuilder()
                .externalLocationType(externalLocationType)
                .updateRetention(updateRetention)
                .build()
                .getEntity();
            type.createAndAssociateTo(dartsPersistence, eod, retainUntilTs, retConfScore);
            return dartsPersistence.save(eod);
        }
    }
}