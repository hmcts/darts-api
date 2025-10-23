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
    void findIdsIn2StorageLocationsBeforeTime_WhereMediaDate24Hours() throws Exception {

        int setupHoursBeforeCurrentTime = 24;

        // set up the test data
        generateDataWithMediaForInbound(setupHoursBeforeCurrentTime);

        // exercise the logic
        List<Long> results = externalObjectDirectoryRepository
            .findIdsIn2StorageLocationsBeforeTime(
                EodHelper.storedStatus(), EodHelper.storedStatus(),
                EodHelper.inboundLocation(), EodHelper.armLocation(),
                getCurrentDateTimeWithHoursBefore(setupHoursBeforeCurrentTime),
                ExternalObjectDirectoryQueryTypeEnum.MEDIA_QUERY.getIndex(),
                Limit.of(100_000));

        // assert the logic
        assertFindIdsIn2StorageLocationsBeforeTimeExpectedResults(results, entitiesToBeMarkedWithMediaOrAnnotationOutsideOfArmHours,
                                                                  entitiesToBeMarkedWithMediaOrAnnotationOutsideOfArmHours.size());
    }

    @Test
    void findIdsIn2StorageLocationsBeforeTime_WhereMediaDateBeyond24Hours() throws Exception {

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
        assertFindIdsIn2StorageLocationsBeforeTimeExpectedResults(results, entitiesToBeMarkedWithMediaOrAnnotationOutsideOfArmHours,
                                                                  entitiesToBeMarkedWithMediaOrAnnotationOutsideOfArmHours.size());
    }

    @Test
    void findIdsIn2StorageLocationsBeforeTime_WhereAnnotationDate24Hours() throws Exception {

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
        assertFindIdsIn2StorageLocationsBeforeTimeExpectedResults(results, entitiesToBeMarkedWithMediaOrAnnotationOutsideOfArmHours,
                                                                  entitiesToBeMarkedWithMediaOrAnnotationOutsideOfArmHours.size());
    }

    @Test
    void findIdsIn2StorageLocationsBeforeTime_WhereAnnotationArmDateAndUnstructuredDateOutsideOfBounds() throws Exception {

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
        assertFindIdsIn2StorageLocationsBeforeTimeExpectedResults(results, entitiesToBeMarkedWithMediaOrAnnotationOutsideOfArmHours,
                                                                  entitiesToBeMarkedWithMediaOrAnnotationOutsideOfArmHours.size());
    }

    @Test
    void findIdsIn2StorageLocationsBeforeTime_WhereAnnotationArmDateAndUnstructuredDateWithNoRecordsFoundDueToArmDateBeingAcceptable() throws Exception {

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
    void findIdsIn2StorageLocationsBeforeTime_WhereMediaDateNotBeyondThreshold() throws Exception {

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
    void findStoredInInboundAndUnstructuredByMediaId_ShouldReturnAsExpected() throws Exception {
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
    void findStoredInInboundAndUnstructuredByTranscriptionId_ShouldReturnAsExpected() throws Exception {
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
    void findAllByStatusAndInputUploadProcessedTsBetweenAndLimit_ShouldReturnAsExpected()
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
    void findAllByStatusAndDataIngestionTsBetweenAndLimit_ShouldReturnAsExpected()
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
                eod.setDataIngestionTs(currentTimeHelper.currentOffsetDateTime().minusHours(26));
            } else if (eod.getId() % 3 == 0) {
                // before the time range
                eod.setCreatedDateTime(currentTimeHelper.currentOffsetDateTime().minusHours(40));
                eod.setDataIngestionTs(currentTimeHelper.currentOffsetDateTime().minusHours(31));
            } else {
                // after the time range
                eod.setCreatedDateTime(currentTimeHelper.currentOffsetDateTime().minusHours(15));
                eod.setDataIngestionTs(currentTimeHelper.currentOffsetDateTime().minusHours(10));
            }
        });
        dartsPersistence.saveAll(externalObjectDirectoryEntities);

        // Verify test data setup
        List<ExternalObjectDirectoryEntity> allEntities = externalObjectDirectoryRepository.findAll();
        assertEquals(20, allEntities.size(), "Test data setup failed: Expected 20 entities in the database");

        // when
        ingestionEndDateTime = currentTimeHelper.currentOffsetDateTime().minusHours(24);
        var results = externalObjectDirectoryRepository.findAllByStatusAndDataIngestionTsBetweenAndLimit(
            EodHelper.armRpoPendingStatus(), ingestionStartDateTime, ingestionEndDateTime,
            Limit.of(20));

        // then
        assertEquals(7, results.size(), "Query returned unexpected number of results");
        results.forEach(eod -> {
            assertTrue(eod.getDataIngestionTs().isAfter(ingestionStartDateTime));
            assertTrue(eod.getDataIngestionTs().isBefore(ingestionEndDateTime));
        });
    }

    @Test
    void findIdsByStatusAndLastModifiedBetweenAndLocationAndLimit_ShouldReturnAsExpected() throws Exception {
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
    void findIdsByStatusAndLastModifiedBetweenAndLocationAndLimit_ShouldReturnNoResults() {
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
    void findByStatusAndDataIngestionTsWithPaging_ShouldReturnAsExpected() throws Exception {
        // Given
        OffsetDateTime pastCurrentDateTime1 = OffsetDateTime.now().minusHours(2);
        OffsetDateTime pastCurrentDateTime2 = OffsetDateTime.now().minusHours(20);

        List<ExternalObjectDirectoryEntity> matchingEods = externalObjectDirectoryStub.generateWithStatusAndMediaLocation(
            ExternalLocationTypeEnum.ARM, ARM_RPO_PENDING, 11, Optional.of(pastCurrentDateTime1));
        matchingEods.forEach(eod -> {
            eod.setDataIngestionTs(pastCurrentDateTime1);
        });
        dartsPersistence.saveAll(matchingEods);
        assertEquals(11, matchingEods.size());

        List<ExternalObjectDirectoryEntity> nonMatchingEods = externalObjectDirectoryStub.generateWithStatusAndMediaLocation(
            ExternalLocationTypeEnum.ARM, ARM_RPO_PENDING, 4, Optional.of(pastCurrentDateTime2));
        nonMatchingEods.forEach(eod -> {
            eod.setDataIngestionTs(pastCurrentDateTime2);
        });
        dartsPersistence.saveAll(nonMatchingEods);
        assertEquals(4, nonMatchingEods.size());

        OffsetDateTime startDateTime = currentTimeHelper.currentOffsetDateTime().minusHours(10);
        OffsetDateTime endDateTime = currentTimeHelper.currentOffsetDateTime().minusHours(1);

        Pageable pageable = PageRequest.of(0, 10);
        ObjectRecordStatusEntity status = EodHelper.armRpoPendingStatus();

        // When
        Page<ExternalObjectDirectoryEntity> result = externalObjectDirectoryRepository.findByStatusAndDataIngestionTsWithPaging(
            status, startDateTime, endDateTime, pageable
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(10);
        assertThat(result.getTotalElements()).isEqualTo(11);
        result.getContent().forEach(entity -> {
            assertThat(entity.getStatus().getId()).isEqualTo(status.getId());
            assertThat(entity.getDataIngestionTs()).isBetween(startDateTime, endDateTime);
        });
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
    void updateEodStatusAndTransferAttemptsWhereIdIn_ShouldUpdatesValues() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
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
            assertThat(entity.getDataIngestionTs()).isNull();
        });

    }

    @Nested
    @DisplayName("findByExternalLocationTypeAndUpdateRetention")
    class FindByExternalLocationTypeAndUpdateRetention {

        @ParameterizedTest
        @EnumSource(EodItemType.class)
        void shouldReturnEmptyList_whenNoDataMatchingExternalLocationTypeIsFound(EodItemType eodItemType) {
            ExternalLocationTypeEntity externalLocationTypeEntity = EodHelper.armLocation();
            //Create Eod with non matching external location type
            createEod(eodItemType, EodHelper.inboundLocation(), OffsetDateTime.now(), true);

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
            createEod(eodItemType, externalLocationTypeEntity, null, true);

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
            createEod(eodItemType, externalLocationTypeEntity, OffsetDateTime.now(), true);

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
                createEod(eodItemType, externalLocationTypeEntity, OffsetDateTime.now(), true);

            ExternalObjectDirectoryEntity eod2 =
                createEod(eodItemType, externalLocationTypeEntity, OffsetDateTime.now(), true);

            ExternalObjectDirectoryEntity eod3 =
                createEod(eodItemType, externalLocationTypeEntity, OffsetDateTime.now(), true);

            //Create EOD with updateRetention set to false should not be returned
            createEod(eodItemType, externalLocationTypeEntity, OffsetDateTime.now(), false);

            assertThat(
                externalObjectDirectoryRepository.findByExternalLocationTypeAndUpdateRetention(
                    externalLocationTypeEntity, true, Limit.of(10))
            ).hasSize(3)
                .contains(eod1.getId(), eod2.getId(), eod3.getId());
        }

        @ParameterizedTest
        @EnumSource(EodItemType.class)
        void shouldReturnIds_shouldLimitToBatchSize(EodItemType eodItemType) {
            ExternalLocationTypeEntity externalLocationTypeEntity = EodHelper.armLocation();
            //Create Eod with non matching external location type
            ExternalObjectDirectoryEntity eod1 =
                createEod(eodItemType, externalLocationTypeEntity, OffsetDateTime.now(), true);

            ExternalObjectDirectoryEntity eod2 =
                createEod(eodItemType, externalLocationTypeEntity, OffsetDateTime.now(), true);

            //Should not be returned as outside the limit
            createEod(eodItemType, externalLocationTypeEntity, OffsetDateTime.now(), true);

            assertThat(
                externalObjectDirectoryRepository.findByExternalLocationTypeAndUpdateRetention(
                    externalLocationTypeEntity, true, Limit.of(2))
            ).hasSize(2)
                .contains(eod1.getId(), eod2.getId());
        }

        @FunctionalInterface
        interface CreateAndAssociateToFunction {
            void createAndAssociateTo(
                DartsPersistence dartsPersistence,
                ExternalObjectDirectoryEntity eod,
                OffsetDateTime retainUntilTs);
        }

        @AllArgsConstructor
        enum EodItemType {
            MEDIA((dartsPersistence, eod, retainUntilTs) -> {
                MediaEntity media =
                    dartsPersistence.save(
                        PersistableFactory.getMediaTestData()
                            .someMinimalBuilder()
                            .retainUntilTs(retainUntilTs)
                            .build()
                            .getEntity()
                    );
                eod.setMedia(media);
            }),
            TRANSCRIPTION_DOCUMENT((dartsPersistence, eod, retainUntilTs) -> {
                TranscriptionDocumentEntity transcriptionDocumentEntity =
                    dartsPersistence.save(
                        PersistableFactory.getTranscriptionDocument()
                            .someMinimalBuilder()
                            .retainUntilTs(retainUntilTs)
                            .build()
                            .getEntity()
                    );
                eod.setTranscriptionDocumentEntity(transcriptionDocumentEntity);
            }),
            ANNOTATION_DOCUMENT((dartsPersistence, eod, retainUntilTs) -> {
                AnnotationDocumentEntity annotationDocument =
                    dartsPersistence.save(
                        PersistableFactory.getAnnotationDocumentTestData()
                            .someMinimalBuilder()
                            .retainUntilTs(retainUntilTs)
                            .build()
                            .getEntity()
                    );
                eod.setAnnotationDocumentEntity(annotationDocument);
            }),
            CASE_DOCUMENT((dartsPersistence, eod, retainUntilTs) -> {
                CaseDocumentEntity caseDocumentEntity =
                    dartsPersistence.save(
                        PersistableFactory.getCaseDocumentTestData()
                            .someMinimalBuilder()
                            .retainUntilTs(retainUntilTs)
                            .build()
                            .getEntity()
                    );
                eod.setCaseDocument(caseDocumentEntity);
            });

            private final CreateAndAssociateToFunction createAndAssociateToFunction;

            public void createAndAssociateTo(
                DartsPersistence dartsPersistence,
                ExternalObjectDirectoryEntity eod,
                OffsetDateTime retainUntilTs) {
                createAndAssociateToFunction.createAndAssociateTo(dartsPersistence, eod, retainUntilTs);
            }
        }

        private ExternalObjectDirectoryEntity createEod(
            EodItemType type,
            ExternalLocationTypeEntity externalLocationType,
            OffsetDateTime retainUntilTs,
            boolean updateRetention
        ) {
            ExternalObjectDirectoryEntity eod = PersistableFactory.getExternalObjectDirectoryTestData()
                .someMinimalBuilder()
                .externalLocationType(externalLocationType)
                .updateRetention(updateRetention)
                .build()
                .getEntity();
            type.createAndAssociateTo(dartsPersistence, eod, retainUntilTs);
            return dartsPersistence.save(eod);
        }
    }

    @Test
    void findByAnnotationDocumentIdAndExternalLocationTypes_ShouldReturnArmEod() {
        AnnotationDocumentEntity annotationDocumentArm =
            dartsPersistence.save(
                PersistableFactory.getAnnotationDocumentTestData()
                    .someMinimalBuilder()
                    .build()
                    .getEntity()
            );
        ExternalObjectDirectoryEntity eodArm = PersistableFactory.getExternalObjectDirectoryTestData()
            .someMinimalBuilder()
            .externalLocationType(EodHelper.armLocation())
            .annotationDocumentEntity(annotationDocumentArm)
            .build()
            .getEntity();
        dartsPersistence.save(eodArm);

        AnnotationDocumentEntity annotationDocumentDets =
            dartsPersistence.save(
                PersistableFactory.getAnnotationDocumentTestData()
                    .someMinimalBuilder()
                    .build()
                    .getEntity()
            );
        ExternalObjectDirectoryEntity eodDets = PersistableFactory.getExternalObjectDirectoryTestData()
            .someMinimalBuilder()
            .externalLocationType(EodHelper.detsLocation())
            .annotationDocumentEntity(annotationDocumentDets)
            .build()
            .getEntity();
        dartsPersistence.save(eodDets);

        List<ExternalObjectDirectoryEntity> results = externalObjectDirectoryRepository
            .findByAnnotationDocumentIdAndExternalLocationTypes(
                annotationDocumentArm.getId(),
                List.of(EodHelper.armLocation(), EodHelper.detsLocation())
            );

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(eodArm.getId());
    }

    @Test
    void findByAnnotationDocumentIdAndExternalLocationTypes_ShouldReturnDetsEod() {
        AnnotationDocumentEntity annotationDocumentArm =
            dartsPersistence.save(
                PersistableFactory.getAnnotationDocumentTestData()
                    .someMinimalBuilder()
                    .build()
                    .getEntity()
            );
        ExternalObjectDirectoryEntity eodArm = PersistableFactory.getExternalObjectDirectoryTestData()
            .someMinimalBuilder()
            .externalLocationType(EodHelper.armLocation())
            .annotationDocumentEntity(annotationDocumentArm)
            .build()
            .getEntity();
        dartsPersistence.save(eodArm);

        AnnotationDocumentEntity annotationDocumentDets =
            dartsPersistence.save(
                PersistableFactory.getAnnotationDocumentTestData()
                    .someMinimalBuilder()
                    .build()
                    .getEntity()
            );
        ExternalObjectDirectoryEntity eodDets = PersistableFactory.getExternalObjectDirectoryTestData()
            .someMinimalBuilder()
            .externalLocationType(EodHelper.detsLocation())
            .annotationDocumentEntity(annotationDocumentDets)
            .build()
            .getEntity();
        dartsPersistence.save(eodDets);

        List<ExternalObjectDirectoryEntity> results = externalObjectDirectoryRepository
            .findByAnnotationDocumentIdAndExternalLocationTypes(
                annotationDocumentDets.getId(),
                List.of(EodHelper.armLocation(), EodHelper.detsLocation())
            );

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(eodDets.getId());
    }

    @Test
    void findByAnnotationDocumentIdAndExternalLocationTypes_ShouldReturnArmAndDetsEods() {
        AnnotationDocumentEntity annotationDocument =
            dartsPersistence.save(
                PersistableFactory.getAnnotationDocumentTestData()
                    .someMinimalBuilder()
                    .build()
                    .getEntity()
            );
        ExternalObjectDirectoryEntity eodArm = PersistableFactory.getExternalObjectDirectoryTestData()
            .someMinimalBuilder()
            .externalLocationType(EodHelper.armLocation())
            .annotationDocumentEntity(annotationDocument)
            .build()
            .getEntity();
        dartsPersistence.save(eodArm);

        ExternalObjectDirectoryEntity eodDets = PersistableFactory.getExternalObjectDirectoryTestData()
            .someMinimalBuilder()
            .externalLocationType(EodHelper.detsLocation())
            .annotationDocumentEntity(annotationDocument)
            .build()
            .getEntity();
        dartsPersistence.save(eodDets);

        List<ExternalObjectDirectoryEntity> results = externalObjectDirectoryRepository
            .findByAnnotationDocumentIdAndExternalLocationTypes(
                annotationDocument.getId(),
                List.of(EodHelper.armLocation(), EodHelper.detsLocation())
            );

        assertThat(results).hasSize(2);

    }

    @Test
    void findByTranscriptionDocumentIdAndExternalLocationTypes_ShouldReturnArmEod() {
        TranscriptionDocumentEntity transcriptionDocumentArm =
            dartsPersistence.save(
                PersistableFactory.getTranscriptionDocument()
                    .someMinimalBuilder()
                    .build()
                    .getEntity()
            );
        ExternalObjectDirectoryEntity eodArm = PersistableFactory.getExternalObjectDirectoryTestData()
            .someMinimalBuilder()
            .externalLocationType(EodHelper.armLocation())
            .transcriptionDocumentEntity(transcriptionDocumentArm)
            .build()
            .getEntity();
        dartsPersistence.save(eodArm);

        TranscriptionDocumentEntity transcriptionDocumentDets =
            dartsPersistence.save(
                PersistableFactory.getTranscriptionDocument()
                    .someMinimalBuilder()
                    .build()
                    .getEntity()
            );
        ExternalObjectDirectoryEntity eodDets = PersistableFactory.getExternalObjectDirectoryTestData()
            .someMinimalBuilder()
            .externalLocationType(EodHelper.detsLocation())
            .transcriptionDocumentEntity(transcriptionDocumentDets)
            .build()
            .getEntity();
        dartsPersistence.save(eodDets);

        List<ExternalObjectDirectoryEntity> results = externalObjectDirectoryRepository.findByTranscriptionDocumentIdAndExternalLocationTypes(
            transcriptionDocumentArm.getId(), List.of(EodHelper.armLocation(), EodHelper.detsLocation()));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(eodArm.getId());
    }

    @Test
    void findByTranscriptionDocumentIdAndExternalLocationTypes_ShouldReturnDetsEod() {
        TranscriptionDocumentEntity transcriptionDocumentArm =
            dartsPersistence.save(
                PersistableFactory.getTranscriptionDocument()
                    .someMinimalBuilder()
                    .build()
                    .getEntity()
            );
        ExternalObjectDirectoryEntity eodArm = PersistableFactory.getExternalObjectDirectoryTestData()
            .someMinimalBuilder()
            .externalLocationType(EodHelper.armLocation())
            .transcriptionDocumentEntity(transcriptionDocumentArm)
            .build()
            .getEntity();
        dartsPersistence.save(eodArm);

        TranscriptionDocumentEntity transcriptionDocumentDets =
            dartsPersistence.save(
                PersistableFactory.getTranscriptionDocument()
                    .someMinimalBuilder()
                    .build()
                    .getEntity()
            );
        ExternalObjectDirectoryEntity eodDets = PersistableFactory.getExternalObjectDirectoryTestData()
            .someMinimalBuilder()
            .externalLocationType(EodHelper.detsLocation())
            .transcriptionDocumentEntity(transcriptionDocumentDets)
            .build()
            .getEntity();
        dartsPersistence.save(eodDets);

        List<ExternalObjectDirectoryEntity> results = externalObjectDirectoryRepository.findByTranscriptionDocumentIdAndExternalLocationTypes(
            transcriptionDocumentDets.getId(), List.of(EodHelper.armLocation(), EodHelper.detsLocation()));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(eodDets.getId());
    }

    @Test
    void findByTranscriptionDocumentIdAndExternalLocationTypes_ShouldReturnArmAndDetsEod() {
        TranscriptionDocumentEntity transcriptionDocument =
            dartsPersistence.save(
                PersistableFactory.getTranscriptionDocument()
                    .someMinimalBuilder()
                    .build()
                    .getEntity()
            );
        ExternalObjectDirectoryEntity eodArm = PersistableFactory.getExternalObjectDirectoryTestData()
            .someMinimalBuilder()
            .externalLocationType(EodHelper.armLocation())
            .transcriptionDocumentEntity(transcriptionDocument)
            .build()
            .getEntity();
        dartsPersistence.save(eodArm);

        ExternalObjectDirectoryEntity eodDets = PersistableFactory.getExternalObjectDirectoryTestData()
            .someMinimalBuilder()
            .externalLocationType(EodHelper.detsLocation())
            .transcriptionDocumentEntity(transcriptionDocument)
            .build()
            .getEntity();
        dartsPersistence.save(eodDets);

        List<ExternalObjectDirectoryEntity> results = externalObjectDirectoryRepository.findByTranscriptionDocumentIdAndExternalLocationTypes(
            transcriptionDocument.getId(), List.of(EodHelper.armLocation(), EodHelper.detsLocation()));

        assertThat(results).hasSize(2);
    }

    @Test
    void findByMediaIdAndExternalLocationTypes_ShouldReturnArmEod() {
        MediaEntity mediaArm =
            dartsPersistence.save(
                PersistableFactory.getMediaTestData()
                    .someMinimalBuilder()
                    .build()
                    .getEntity()
            );
        ExternalObjectDirectoryEntity eodArm = PersistableFactory.getExternalObjectDirectoryTestData()
            .someMinimalBuilder()
            .externalLocationType(EodHelper.armLocation())
            .media(mediaArm)
            .build()
            .getEntity();
        dartsPersistence.save(eodArm);

        MediaEntity mediaDets =
            dartsPersistence.save(
                PersistableFactory.getMediaTestData()
                    .someMinimalBuilder()
                    .build()
                    .getEntity()
            );
        ExternalObjectDirectoryEntity eodDets = PersistableFactory.getExternalObjectDirectoryTestData()
            .someMinimalBuilder()
            .externalLocationType(EodHelper.detsLocation())
            .media(mediaDets)
            .build()
            .getEntity();
        dartsPersistence.save(eodDets);

        List<ExternalObjectDirectoryEntity> results = externalObjectDirectoryRepository.findByMediaIdAndExternalLocationTypes(
            mediaArm.getId(), List.of(EodHelper.armLocation(), EodHelper.detsLocation()));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(eodArm.getId());
    }

    @Test
    void findByMediaIdAndExternalLocationTypes_ShouldReturnDetsEod() {
        MediaEntity mediaArm =
            dartsPersistence.save(
                PersistableFactory.getMediaTestData()
                    .someMinimalBuilder()
                    .build()
                    .getEntity()
            );
        ExternalObjectDirectoryEntity eodArm = PersistableFactory.getExternalObjectDirectoryTestData()
            .someMinimalBuilder()
            .externalLocationType(EodHelper.armLocation())
            .media(mediaArm)
            .build()
            .getEntity();
        dartsPersistence.save(eodArm);

        MediaEntity mediaDets =
            dartsPersistence.save(
                PersistableFactory.getMediaTestData()
                    .someMinimalBuilder()
                    .build()
                    .getEntity()
            );
        ExternalObjectDirectoryEntity eodDets = PersistableFactory.getExternalObjectDirectoryTestData()
            .someMinimalBuilder()
            .externalLocationType(EodHelper.detsLocation())
            .media(mediaDets)
            .build()
            .getEntity();
        dartsPersistence.save(eodDets);

        List<ExternalObjectDirectoryEntity> results = externalObjectDirectoryRepository.findByMediaIdAndExternalLocationTypes(
            mediaDets.getId(), List.of(EodHelper.armLocation(), EodHelper.detsLocation()));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(eodDets.getId());
    }

    @Test
    void findByMediaIdAndExternalLocationTypes_ShouldReturnArmAndDetsEod() {
        MediaEntity media =
            dartsPersistence.save(
                PersistableFactory.getMediaTestData()
                    .someMinimalBuilder()
                    .build()
                    .getEntity()
            );
        ExternalObjectDirectoryEntity eodArm = PersistableFactory.getExternalObjectDirectoryTestData()
            .someMinimalBuilder()
            .externalLocationType(EodHelper.armLocation())
            .media(media)
            .build()
            .getEntity();
        dartsPersistence.save(eodArm);

        ExternalObjectDirectoryEntity eodDets = PersistableFactory.getExternalObjectDirectoryTestData()
            .someMinimalBuilder()
            .externalLocationType(EodHelper.detsLocation())
            .media(media)
            .build()
            .getEntity();
        dartsPersistence.save(eodDets);

        List<ExternalObjectDirectoryEntity> results = externalObjectDirectoryRepository.findByMediaIdAndExternalLocationTypes(
            media.getId(), List.of(EodHelper.armLocation(), EodHelper.detsLocation()));

        assertThat(results).hasSize(2);

    }

    @Test
    void findByCaseDocumentIdAndExternalLocationTypes_ShouldReturnArmEod() {
        CaseDocumentEntity caseDocumentArm =
            dartsPersistence.save(
                PersistableFactory.getCaseDocumentTestData()
                    .someMinimalBuilder()
                    .build()
                    .getEntity()
            );
        ExternalObjectDirectoryEntity eodArm = PersistableFactory.getExternalObjectDirectoryTestData()
            .someMinimalBuilder()
            .externalLocationType(EodHelper.armLocation())
            .caseDocument(caseDocumentArm)
            .build()
            .getEntity();
        dartsPersistence.save(eodArm);

        CaseDocumentEntity caseDocumentDets =
            dartsPersistence.save(
                PersistableFactory.getCaseDocumentTestData()
                    .someMinimalBuilder()
                    .build()
                    .getEntity()
            );
        ExternalObjectDirectoryEntity eodDets = PersistableFactory.getExternalObjectDirectoryTestData()
            .someMinimalBuilder()
            .externalLocationType(EodHelper.detsLocation())
            .caseDocument(caseDocumentDets)
            .build()
            .getEntity();
        dartsPersistence.save(eodDets);

        List<ExternalObjectDirectoryEntity> results = externalObjectDirectoryRepository.findByCaseDocumentIdAndExternalLocationTypes(
            caseDocumentArm.getId(), List.of(EodHelper.armLocation(), EodHelper.detsLocation()));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(eodArm.getId());
    }

    @Test
    void findByCaseDocumentIdAndExternalLocationTypes_ShouldReturnDetsEod() {
        CaseDocumentEntity caseDocumentArm =
            dartsPersistence.save(
                PersistableFactory.getCaseDocumentTestData()
                    .someMinimalBuilder()
                    .build()
                    .getEntity()
            );
        ExternalObjectDirectoryEntity eodArm = PersistableFactory.getExternalObjectDirectoryTestData()
            .someMinimalBuilder()
            .externalLocationType(EodHelper.armLocation())
            .caseDocument(caseDocumentArm)
            .build()
            .getEntity();
        dartsPersistence.save(eodArm);

        CaseDocumentEntity caseDocumentDets =
            dartsPersistence.save(
                PersistableFactory.getCaseDocumentTestData()
                    .someMinimalBuilder()
                    .build()
                    .getEntity()
            );
        ExternalObjectDirectoryEntity eodDets = PersistableFactory.getExternalObjectDirectoryTestData()
            .someMinimalBuilder()
            .externalLocationType(EodHelper.detsLocation())
            .caseDocument(caseDocumentDets)
            .build()
            .getEntity();
        dartsPersistence.save(eodDets);

        List<ExternalObjectDirectoryEntity> results = externalObjectDirectoryRepository.findByCaseDocumentIdAndExternalLocationTypes(
            caseDocumentDets.getId(), List.of(EodHelper.armLocation(), EodHelper.detsLocation()));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(eodDets.getId());
    }

    @Test
    void findByCaseDocumentIdAndExternalLocationTypes_ShouldReturnArmAndDetsEod() {
        CaseDocumentEntity caseDocument =
            dartsPersistence.save(
                PersistableFactory.getCaseDocumentTestData()
                    .someMinimalBuilder()
                    .build()
                    .getEntity()
            );
        ExternalObjectDirectoryEntity eodArm = PersistableFactory.getExternalObjectDirectoryTestData()
            .someMinimalBuilder()
            .externalLocationType(EodHelper.armLocation())
            .caseDocument(caseDocument)
            .build()
            .getEntity();
        dartsPersistence.save(eodArm);

        ExternalObjectDirectoryEntity eodDets = PersistableFactory.getExternalObjectDirectoryTestData()
            .someMinimalBuilder()
            .externalLocationType(EodHelper.detsLocation())
            .caseDocument(caseDocument)
            .build()
            .getEntity();
        dartsPersistence.save(eodDets);

        List<ExternalObjectDirectoryEntity> results = externalObjectDirectoryRepository.findByCaseDocumentIdAndExternalLocationTypes(
            caseDocument.getId(), List.of(EodHelper.armLocation(), EodHelper.detsLocation()));

        assertThat(results).hasSize(2);

    }

    private void assertFindIdsIn2StorageLocationsBeforeTimeExpectedResults(
        List<Long> actualResults, List<ExternalObjectDirectoryEntity> expectedResults, int resultCount) {

        List<Long> matchesEntity = new ArrayList<>(actualResults.stream().filter(
            expectedResult -> expectedResults.stream().anyMatch(result -> expectedResult.equals(result.getId()))).toList());

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

}