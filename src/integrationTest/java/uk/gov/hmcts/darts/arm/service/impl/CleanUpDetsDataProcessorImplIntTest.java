package uk.gov.hmcts.darts.arm.service.impl;

import lombok.Getter;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.CaseDocumentEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectStateRecordEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionDocumentEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;
import uk.gov.hmcts.darts.dets.service.DetsApiService;
import uk.gov.hmcts.darts.task.config.CleanUpDetsDataAutomatedTaskConfig;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.darts.test.common.data.ExternalLocationTypeTestData.locationTypeOf;
import static uk.gov.hmcts.darts.test.common.data.ObjectRecordStatusTestData.statusOf;
import static uk.gov.hmcts.darts.test.common.data.PersistableFactory.getAnnotationDocumentTestData;
import static uk.gov.hmcts.darts.test.common.data.PersistableFactory.getCaseDocumentTestData;
import static uk.gov.hmcts.darts.test.common.data.PersistableFactory.getExternalObjectDirectoryTestData;
import static uk.gov.hmcts.darts.test.common.data.PersistableFactory.getMediaTestData;
import static uk.gov.hmcts.darts.test.common.data.PersistableFactory.getTranscriptionDocument;

@Isolated
class CleanUpDetsDataProcessorImplIntTest extends PostgresIntegrationBase {
    private static final Duration DEFAULT_TEST_DATA_AGE = Duration.ofHours(10);

    @Autowired
    private CleanUpDetsDataProcessorImpl cleanUpDetsDataProcessor;

    @Autowired
    private CleanUpDetsDataProcessorImpl.CleanUpDetsDataTransactionalProcessor cleanUpDetsDataTransactionalProcessor;

    @Autowired
    private Clock clock;

    @Autowired
    private DetsCleanupMaintenanceService detsCleanupMaintenanceService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private DataSource dataSource;

    @MockitoBean
    private DetsApiService detsApiService;

    @Test
    void vacuumTables_shouldSuspendAndResumeCallerTransaction_whenTransactionIsActive() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            Long transactionId = jdbcTemplate.queryForObject("SELECT txid_current()", Long.class);

            detsCleanupMaintenanceService.vacuumTables(30);

            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
            assertThat(jdbcTemplate.queryForObject("SELECT txid_current()", Long.class)).isEqualTo(transactionId);
            status.setRollbackOnly();
        });
    }

    @Test
    @SuppressWarnings("PMD.DoNotUseThreads") // A separate connection must run vacuum while the test holds a conflicting lock.
    void vacuumTables_shouldCancelStatement_whenTableIsLocked() throws SQLException {
        try (ExecutorService executor = Executors.newSingleThreadExecutor();
             Connection lockConnection = dataSource.getConnection()) {
            lockConnection.setAutoCommit(false);
            try (Statement lockStatement = lockConnection.createStatement()) {
                lockStatement.execute("LOCK TABLE darts.external_object_directory IN ACCESS EXCLUSIVE MODE");
                Future<?> vacuum = executor.submit(() -> detsCleanupMaintenanceService.vacuumTables(1));

                assertThatThrownBy(() -> vacuum.get(10, TimeUnit.SECONDS))
                    .isInstanceOf(ExecutionException.class)
                    .rootCause()
                    .isInstanceOfSatisfying(SQLException.class, exception -> assertThat(exception.getSQLState()).isEqualTo("57014"));

                assertThat(jdbcTemplate.queryForObject("""
                    SELECT count(*) FROM pg_stat_activity
                    WHERE query LIKE 'VACUUM (ANALYZE)%' AND state = 'active'
                    """, Integer.class)).isZero();
            } finally {
                lockConnection.rollback();
            }
        }
    }

    @Test
    @SuppressWarnings("PMD.DoNotUseThreads") // Verify server cancellation on a separate connection while holding a conflicting lock.
    void callDetsCleanUpStoredProcedure_shouldCancelAndPreserveData_whenStatementTimeoutExpires() throws SQLException {
        TestData<?> validData = new MediaTestData().createDataThatShouldBeCleanedUp();
        OffsetDateTime minimumStoredAge = OffsetDateTime.now(clock).minus(DEFAULT_TEST_DATA_AGE);

        try (ExecutorService executor = Executors.newSingleThreadExecutor();
             Connection lockConnection = dataSource.getConnection()) {
            lockConnection.setAutoCommit(false);
            try (Statement lockStatement = lockConnection.createStatement()) {
                lockStatement.execute("LOCK TABLE darts.external_object_directory IN ACCESS EXCLUSIVE MODE");
                Future<?> cleanup = executor.submit(() ->
                    cleanUpDetsDataTransactionalProcessor.callDetsCleanUpStoredProcedure(10, minimumStoredAge, 1));

                assertThatThrownBy(() -> cleanup.get(10, TimeUnit.SECONDS))
                    .isInstanceOf(ExecutionException.class)
                    .cause().isInstanceOf(QueryTimeoutException.class)
                    .rootCause()
                    .isInstanceOfSatisfying(SQLException.class, exception -> assertThat(exception.getSQLState()).isEqualTo("57014"));
            } finally {
                lockConnection.rollback();
            }
        }

        assertNoChange(validData);
        assertThat(cleanUpDetsDataTransactionalProcessor.callDetsCleanUpStoredProcedure(10, minimumStoredAge, 30)).hasSize(1);
        assertSuccessfull(validData);
    }

    @Test
    void cleanUpDetsData_typical() {
        //Valid data that should be cleaned up
        List<TestData<?>> validData = List.of(
            new MediaTestData().createDataThatShouldBeCleanedUp(),
            new TranscriptionDocumentTestData().createDataThatShouldBeCleanedUp(),
            new AnnotationDocumentTestData().createDataThatShouldBeCleanedUp(),
            new CaseDocumentTestData().createDataThatShouldBeCleanedUp()
        );
        //Inject some data that should not be picked up
        List<TestData<?>> ineligibleData = List.of(
            new MediaTestData().createDetsRecordButWithoutArmRecord(),
            new MediaTestData().createDetsRecordButWithArmRecordButNotInStoredStatus(),
            new MediaTestData().createDetsRecordButWithArmStoredButWithinMiniumStoredTime(),
            new AnnotationDocumentTestData().createDetsRecordButOsrAndDetsEodLocationsDoNotAlign()
        );

        cleanUpDetsDataProcessor.processCleanUpDetsData(20, getCleanUpDetsDataAutomatedTaskConfig());

        assertSuccessfull(validData);
        assertNoChange(ineligibleData);
    }

    @Test
    void cleanUpDetsData_shouldPickUpAnyUnprocessedDeletes() {
        //Valid data that should be cleaned up
        TestData<?> fullCleanUpData1 = new MediaTestData().createDataThatShouldBeCleanedUp(true);
        TestData<?> fullCleanUpData2 = new MediaTestData().createDataThatShouldBeCleanedUp(true);
        TestData<?> partialCleanUpData1 = new MediaTestData().createDataThatShouldBeCleanedUp(false);

        cleanUpDetsDataProcessor.processCleanUpDetsData(3, getCleanUpDetsDataAutomatedTaskConfig());

        assertSuccessfull(fullCleanUpData1);
        assertPartialSuccess(partialCleanUpData1);
        assertSuccessfull(fullCleanUpData2);

        //Reset mock
        //Check to ensure that the failed blob delete is attempted again but the successful database deletes are not attempted again
        Mockito.clearInvocations(detsApiService);
        TestData<?> fullCleanUpData3 = new MediaTestData().createDataThatShouldBeCleanedUp(true);

        cleanUpDetsDataProcessor.processCleanUpDetsData(20, getCleanUpDetsDataAutomatedTaskConfig());
        assertSuccessfull(partialCleanUpData1);
        assertSuccessfull(fullCleanUpData3);
    }

    @Test
    void cleanUpDetsData_shouldLeaveIneligibleRecordsUnchanged_whenLinksOrLocationsAreMissingOrMismatched() {
        TestData<?> missingDetsLink = new MediaTestData().createDataThatShouldBeCleanedUp();
        missingDetsLink.getObjectStateRecordEntity().setEodId(null);
        TestData<?> missingArmLink = new MediaTestData().createDataThatShouldBeCleanedUp();
        missingArmLink.getObjectStateRecordEntity().setArmEodId(null);
        TestData<?> missingDetsLocation = new MediaTestData().createDataThatShouldBeCleanedUp();
        missingDetsLocation.getObjectStateRecordEntity().setDetsLocation(null);
        TestData<?> mismatchedDetsLocation = new MediaTestData().createDataThatShouldBeCleanedUp();
        mismatchedDetsLocation.getObjectStateRecordEntity().setDetsLocation(UUID.randomUUID().toString());
        List<TestData<?>> ineligibleRecords = List.of(missingDetsLink, missingArmLink, missingDetsLocation, mismatchedDetsLocation);
        ineligibleRecords.forEach(testData -> dartsPersistence.getObjectStateRecordRepository().saveAndFlush(testData.getObjectStateRecordEntity()));
        TestData<?> eligibleRecord = new MediaTestData().createDataThatShouldBeCleanedUp();

        cleanUpDetsDataProcessor.processCleanUpDetsData(10, getCleanUpDetsDataAutomatedTaskConfig());

        assertSuccessfull(eligibleRecord);
        for (TestData<?> testData : ineligibleRecords) {
            assertThat(dartsPersistence.getExternalObjectDirectoryRepository().existsById(testData.getDetsEod().getId())).isTrue();
            assertThat(dartsPersistence.getExternalObjectDirectoryRepository().existsById(testData.getArmEod().getId())).isTrue();
            ObjectStateRecordEntity objectStateRecordEntity = dartsPersistence.getObjectStateRecordRepository()
                .findById(testData.getObjectStateRecordEntity().getUuid()).orElseThrow();
            assertThat(objectStateRecordEntity.getEodId()).isEqualTo(testData.getObjectStateRecordEntity().getEodId());
            assertThat(objectStateRecordEntity.getDateFileDetsCleanup()).isNull();
            assertThat(objectStateRecordEntity.getFlagFileDetsCleanupStatus()).isNull();
        }
    }

    @Test
    void cleanUpDetsData_shouldRespectBatchLimitAndSkipCompletedRecords_whenProcessingSuccessiveChunks() {
        TestData<?> firstRecord = new MediaTestData().createDataThatShouldBeCleanedUp();
        TestData<?> secondRecord = new MediaTestData().createDataThatShouldBeCleanedUp();
        TestData<?> thirdRecord = new MediaTestData().createDataThatShouldBeCleanedUp();
        CleanUpDetsDataAutomatedTaskConfig config = getCleanUpDetsDataAutomatedTaskConfig();
        config.setChunkSize(1);

        cleanUpDetsDataProcessor.processCleanUpDetsData(2, config);

        assertSuccessfull(List.of(firstRecord, secondRecord));
        assertNoChange(thirdRecord);

        cleanUpDetsDataProcessor.processCleanUpDetsData(2, config);

        assertSuccessfull(List.of(firstRecord, secondRecord, thirdRecord));
        assertThat(dartsPersistence.getObjectStateRecordRepository().cleanUpDetsDataProcedure(1, OffsetDateTime.now())).isEmpty();
    }

    private void assertNoChange(List<TestData<?>> testDataList) {
        testDataList.forEach(this::assertNoChange);
    }

    @SneakyThrows
    private void assertNoChange(TestData<?> testData) {
        assertCommon(testData);
        //Check that the EOD records still exist
        assertThat(dartsPersistence.getExternalObjectDirectoryRepository().existsById(testData.getDetsEod().getId()))
            .as("DETS EOD record should still exist").isTrue();
        //Check that the Object State Record still exists and has the same dets eod id and arm eod id
        ObjectStateRecordEntity objectStateRecordEntity = dartsPersistence.getObjectStateRecordRepository()
            .findById(testData.getObjectStateRecordEntity().getUuid()).orElseThrow();
        assertThat(objectStateRecordEntity.getEodId()).isEqualTo(testData.getDetsEod().getId());
        assertThat(objectStateRecordEntity.getDateFileDetsCleanup()).isNull();
        assertThat(objectStateRecordEntity.getFlagFileDetsCleanupStatus()).isNull();
        verify(detsApiService, never()).deleteBlobDataFromContainer(testData.getDetsEod().getLocation());
    }

    private void assertSuccessfull(List<TestData<?>> testDataList) {
        testDataList.forEach(this::assertSuccessfull);
    }

    @SneakyThrows
    private void assertSuccessfull(TestData<?> testData) {
        assertCommon(testData);
        //Check that the Dets EOD records has been deleted
        assertThat(dartsPersistence.getExternalObjectDirectoryRepository().existsById(testData.getDetsEod().getId()))
            .as("DETS EOD record should not exist").isFalse();
        //Check that the Object State Record has been updated
        ObjectStateRecordEntity objectStateRecordEntity = dartsPersistence.getObjectStateRecordRepository()
            .findById(testData.getObjectStateRecordEntity().getUuid()).orElseThrow();
        assertThat(objectStateRecordEntity.getEodId()).isNull();
        assertThat(objectStateRecordEntity.getDateFileDetsCleanup())
            .isCloseTo(OffsetDateTime.now(), within(1, SECONDS));
        assertThat(objectStateRecordEntity.getFlagFileDetsCleanupStatus()).isTrue();
    }

    @SneakyThrows
    private void assertPartialSuccess(TestData<?> testData) {
        assertCommon(testData);
        //Check that the Dets EOD records has been deleted
        assertThat(dartsPersistence.getExternalObjectDirectoryRepository().existsById(testData.getDetsEod().getId()))
            .as("DETS EOD record should not exist").isFalse();
        //Check that the Object State Record has been updated
        ObjectStateRecordEntity objectStateRecordEntity = dartsPersistence.getObjectStateRecordRepository()
            .findById(testData.getObjectStateRecordEntity().getUuid()).orElseThrow();
        assertThat(objectStateRecordEntity.getEodId()).isNull();
        assertThat(objectStateRecordEntity.getDateFileDetsCleanup())
            .isCloseTo(OffsetDateTime.now(), within(1, SECONDS));
        
        assertThat(objectStateRecordEntity.getFlagFileDetsCleanupStatus()).isTrue();
    }

    private void assertCommon(TestData<?> testData) {
        //Check that the ARM EOD record still exists
        if (testData.getArmEod() != null) {
            assertThat(dartsPersistence.getExternalObjectDirectoryRepository().existsById(testData.getArmEod().getId()))
                .as("ARM EOD record should still exist").isTrue();
        }
    }

    private CleanUpDetsDataAutomatedTaskConfig getCleanUpDetsDataAutomatedTaskConfig() {
        CleanUpDetsDataAutomatedTaskConfig config = new CleanUpDetsDataAutomatedTaskConfig();
        config.setMinimumStoredAge(DEFAULT_TEST_DATA_AGE);
        config.setChunkSize(10);
        config.setCleanupTimeout(Duration.ofHours(1));
        config.setVacuumTimeout(Duration.ofSeconds(30));
        CleanUpDetsDataAutomatedTaskConfig.Lock lock = new CleanUpDetsDataAutomatedTaskConfig.Lock();
        lock.setAtMostFor(Duration.ofMinutes(115));
        config.setLock(lock);
        config.setThreads(2);
        config.setAsyncTimeout(Duration.ofMinutes(5));
        return config;
    }

    //Test Data
    public class MediaTestData extends TestData<MediaEntity> {
        @Override
        protected MediaEntity createConfidenceAware() {
            return dartsPersistence.save(getMediaTestData().someMinimal());
        }

        @Override
        protected void assignContextAwareToexternalObjectDirectoryEntity(ExternalObjectDirectoryEntity externalObjectDirectoryEntity,
                                                                         MediaEntity confidenceAware) {
            externalObjectDirectoryEntity.setMedia(confidenceAware);
        }
    }

    public class TranscriptionDocumentTestData extends TestData<TranscriptionDocumentEntity> {
        @Override
        protected TranscriptionDocumentEntity createConfidenceAware() {
            return dartsPersistence.save(getTranscriptionDocument().someMinimal());
        }

        @Override
        protected void assignContextAwareToexternalObjectDirectoryEntity(ExternalObjectDirectoryEntity externalObjectDirectoryEntity,
                                                                         TranscriptionDocumentEntity confidenceAware) {
            externalObjectDirectoryEntity.setTranscriptionDocumentEntity(confidenceAware);
        }
    }

    public class AnnotationDocumentTestData extends TestData<AnnotationDocumentEntity> {
        @Override
        protected AnnotationDocumentEntity createConfidenceAware() {
            return dartsPersistence.save(getAnnotationDocumentTestData().someMinimal());
        }

        @Override
        protected void assignContextAwareToexternalObjectDirectoryEntity(ExternalObjectDirectoryEntity externalObjectDirectoryEntity,
                                                                         AnnotationDocumentEntity confidenceAware) {
            externalObjectDirectoryEntity.setAnnotationDocumentEntity(confidenceAware);
        }
    }

    public class CaseDocumentTestData extends TestData<CaseDocumentEntity> {
        @Override
        protected CaseDocumentEntity createConfidenceAware() {
            return dartsPersistence.save(getCaseDocumentTestData().someMinimal());
        }

        @Override
        protected void assignContextAwareToexternalObjectDirectoryEntity(ExternalObjectDirectoryEntity externalObjectDirectoryEntity,
                                                                         CaseDocumentEntity confidenceAware) {
            externalObjectDirectoryEntity.setCaseDocument(confidenceAware);
        }
    }

    @Getter
    public abstract class TestData<T> {
        private static long uniqueCounter;
        ExternalObjectDirectoryEntity detsEod;
        ExternalObjectDirectoryEntity armEod;
        ObjectStateRecordEntity objectStateRecordEntity;
        T confidenceAware;

        public TestData<T> createDataThatShouldBeCleanedUp() {
            return createDataThatShouldBeCleanedUp(true);
        }

        public TestData<T> createDataThatShouldBeCleanedUp(boolean shouldBlobDeleteSucceed) {
            confidenceAware = createConfidenceAware();
            detsEod = createExternalObjectDirectoryEntity(ExternalLocationTypeEnum.DETS, ObjectRecordStatusEnum.STORED, confidenceAware);
            armEod = createExternalObjectDirectoryEntity(ExternalLocationTypeEnum.ARM, ObjectRecordStatusEnum.STORED, confidenceAware);
            objectStateRecordEntity = createObjectStateRecordEntity(detsEod, armEod);
            return this;
        }

        public TestData<T> createDetsRecordButWithoutArmRecord() {
            confidenceAware = createConfidenceAware();
            detsEod = createExternalObjectDirectoryEntity(ExternalLocationTypeEnum.DETS, ObjectRecordStatusEnum.STORED, confidenceAware);
            objectStateRecordEntity = createObjectStateRecordEntity(detsEod, null);
            return this;
        }

        public TestData<T> createDetsRecordButWithArmRecordButNotInStoredStatus() {
            confidenceAware = createConfidenceAware();
            detsEod = createExternalObjectDirectoryEntity(ExternalLocationTypeEnum.DETS, ObjectRecordStatusEnum.STORED, confidenceAware);
            armEod = createExternalObjectDirectoryEntity(ExternalLocationTypeEnum.ARM, ObjectRecordStatusEnum.ARM_DROP_ZONE, confidenceAware);
            objectStateRecordEntity = createObjectStateRecordEntity(detsEod, armEod);
            return this;
        }

        public TestData<T> createDetsRecordButWithArmStoredButWithinMiniumStoredTime() {
            confidenceAware = createConfidenceAware();
            detsEod = createExternalObjectDirectoryEntity(ExternalLocationTypeEnum.DETS, ObjectRecordStatusEnum.STORED, confidenceAware);
            armEod = createExternalObjectDirectoryEntity(ExternalLocationTypeEnum.ARM, ObjectRecordStatusEnum.ARM_DROP_ZONE, OffsetDateTime.now(),
                                                         confidenceAware);
            objectStateRecordEntity = createObjectStateRecordEntity(detsEod, armEod);
            return this;
        }

        public TestData<T> createDetsRecordButOsrAndDetsEodLocationsDoNotAlign() {
            confidenceAware = createConfidenceAware();
            detsEod = createExternalObjectDirectoryEntity(ExternalLocationTypeEnum.DETS, ObjectRecordStatusEnum.STORED, confidenceAware);
            armEod = createExternalObjectDirectoryEntity(ExternalLocationTypeEnum.ARM, ObjectRecordStatusEnum.ARM_DROP_ZONE, confidenceAware);
            objectStateRecordEntity = createObjectStateRecordEntity(detsEod, armEod);
            objectStateRecordEntity.setDetsLocation("Some random location that does not align with the dets eod location");
            dartsPersistence.getObjectStateRecordRepository().save(objectStateRecordEntity);
            return this;
        }
        
        private ObjectStateRecordEntity createObjectStateRecordEntity(ExternalObjectDirectoryEntity detsEod, ExternalObjectDirectoryEntity armEod) {
            ObjectStateRecordEntity osrEntity = new ObjectStateRecordEntity();
            osrEntity.setUuid(uniqueCounter++);
            osrEntity.setEodId(detsEod.getId());
            if (armEod != null) {
                osrEntity.setArmEodId(armEod.getId());
            }
            osrEntity.setDetsLocation(detsEod.getLocation());
            return dartsPersistence.getObjectStateRecordRepository().save(osrEntity);
        }

        public ExternalObjectDirectoryEntity createExternalObjectDirectoryEntity(ExternalLocationTypeEnum externalLocationTypeEnum,
                                                                                 ObjectRecordStatusEnum objectRecordStatusEnum,
                                                                                 T confidenceAware) {
            return createExternalObjectDirectoryEntity(externalLocationTypeEnum,
                                                       objectRecordStatusEnum,
                                                       OffsetDateTime.now().minus(DEFAULT_TEST_DATA_AGE).minusHours(1),
                                                       confidenceAware);
        }

        public ExternalObjectDirectoryEntity createExternalObjectDirectoryEntity(ExternalLocationTypeEnum externalLocationTypeEnum,
                                                                                 ObjectRecordStatusEnum objectRecordStatusEnum,
                                                                                 OffsetDateTime lastModifiedDateTime,
                                                                                 T confidenceAware) {
            ExternalObjectDirectoryEntity externalObjectDirectoryEntity = getExternalObjectDirectoryTestData().someMinimal();
            externalObjectDirectoryEntity.setStatus(statusOf(objectRecordStatusEnum));
            externalObjectDirectoryEntity.setExternalLocationType(locationTypeOf(externalLocationTypeEnum));
            externalObjectDirectoryEntity.setExternalLocation(UUID.randomUUID().toString());
            externalObjectDirectoryEntity.setLastModifiedDateTime(lastModifiedDateTime);
            assignContextAwareToexternalObjectDirectoryEntity(externalObjectDirectoryEntity, confidenceAware);

            externalObjectDirectoryEntity = dartsPersistence.save(externalObjectDirectoryEntity);
            dartsPersistence.overrideLastModifiedBy(externalObjectDirectoryEntity, lastModifiedDateTime);
            return externalObjectDirectoryEntity;
        }

        protected abstract T createConfidenceAware();

        protected abstract void assignContextAwareToexternalObjectDirectoryEntity(
            ExternalObjectDirectoryEntity externalObjectDirectoryEntity, T confidenceAware);
    }
}
