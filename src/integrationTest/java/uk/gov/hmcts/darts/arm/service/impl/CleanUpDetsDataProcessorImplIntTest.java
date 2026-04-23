package uk.gov.hmcts.darts.arm.service.impl;

import lombok.Getter;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.lenient;
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
    private Clock clock;

    @MockitoBean
    private DetsApiService detsApiService;

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
        List<TestData<?>> inelibileData = List.of(
            new MediaTestData().createDetsRecordButWithoutArmRecord(),
            new MediaTestData().createDetsRecordButWithArmRecordButNotInStoredStatus(),
            new MediaTestData().createDetsRecordButWithArmStoredButWithinMiniumStoredTime(),
            new AnnotationDocumentTestData().createDetsRecordButOsrAndDetsEodLocationsDoNotAlign()
        );

        cleanUpDetsDataProcessor.processCleanUpDetsData(20, getCleanUpDetsDataAutomatedTaskConfig());

        assertSuccessfull(validData);
        assertNoChange(inelibileData);
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
        partialCleanUpData1.stubBlobStoreDelete(true);
        TestData<?> fullCleanUpData3 = new MediaTestData().createDataThatShouldBeCleanedUp(true);

        cleanUpDetsDataProcessor.processCleanUpDetsData(20, getCleanUpDetsDataAutomatedTaskConfig());
        assertSuccessfull(partialCleanUpData1);
        assertSuccessfull(fullCleanUpData3);
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
        verify(detsApiService).deleteBlobDataFromContainer(testData.getDetsEod().getLocation());
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
        verify(detsApiService).deleteBlobDataFromContainer(testData.getDetsEod().getLocation());

        //If database delete was successful but blob store was not this flag should not be set
        assertThat(objectStateRecordEntity.getFlagFileDetsCleanupStatus()).isNull();
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
            stubBlobStoreDelete(shouldBlobDeleteSucceed);
            return this;
        }

        public TestData<T> createDetsRecordButWithoutArmRecord() {
            confidenceAware = createConfidenceAware();
            detsEod = createExternalObjectDirectoryEntity(ExternalLocationTypeEnum.DETS, ObjectRecordStatusEnum.STORED, confidenceAware);
            objectStateRecordEntity = createObjectStateRecordEntity(detsEod, null);
            stubBlobStoreDelete(true);
            return this;
        }

        public TestData<T> createDetsRecordButWithArmRecordButNotInStoredStatus() {
            confidenceAware = createConfidenceAware();
            detsEod = createExternalObjectDirectoryEntity(ExternalLocationTypeEnum.DETS, ObjectRecordStatusEnum.STORED, confidenceAware);
            armEod = createExternalObjectDirectoryEntity(ExternalLocationTypeEnum.ARM, ObjectRecordStatusEnum.ARM_DROP_ZONE, confidenceAware);
            objectStateRecordEntity = createObjectStateRecordEntity(detsEod, armEod);
            stubBlobStoreDelete(true);
            return this;
        }

        public TestData<T> createDetsRecordButWithArmStoredButWithinMiniumStoredTime() {
            confidenceAware = createConfidenceAware();
            detsEod = createExternalObjectDirectoryEntity(ExternalLocationTypeEnum.DETS, ObjectRecordStatusEnum.STORED, confidenceAware);
            armEod = createExternalObjectDirectoryEntity(ExternalLocationTypeEnum.ARM, ObjectRecordStatusEnum.ARM_DROP_ZONE, OffsetDateTime.now(),
                                                         confidenceAware);
            objectStateRecordEntity = createObjectStateRecordEntity(detsEod, armEod);
            stubBlobStoreDelete(true);
            return this;
        }

        public TestData<T> createDetsRecordButOsrAndDetsEodLocationsDoNotAlign() {
            confidenceAware = createConfidenceAware();
            detsEod = createExternalObjectDirectoryEntity(ExternalLocationTypeEnum.DETS, ObjectRecordStatusEnum.STORED, confidenceAware);
            armEod = createExternalObjectDirectoryEntity(ExternalLocationTypeEnum.ARM, ObjectRecordStatusEnum.ARM_DROP_ZONE, confidenceAware);
            objectStateRecordEntity = createObjectStateRecordEntity(detsEod, armEod);
            objectStateRecordEntity.setDetsLocation("Some random location that does not align with the dets eod location");
            dartsPersistence.getObjectStateRecordRepository().save(objectStateRecordEntity);
            stubBlobStoreDelete(true);
            return this;
        }

        @SneakyThrows
        public void stubBlobStoreDelete(boolean shouldBlobDeleteSucceed) {
            lenient().when(detsApiService.deleteBlobDataFromContainer(detsEod.getLocation()))
                .thenReturn(shouldBlobDeleteSucceed);
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
