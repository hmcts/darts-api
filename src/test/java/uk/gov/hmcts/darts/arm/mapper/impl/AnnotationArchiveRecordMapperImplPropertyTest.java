package uk.gov.hmcts.darts.arm.mapper.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.model.record.AnnotationArchiveRecord;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.util.PropertyFileLoader;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceScoreEnum;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.test.common.data.builder.TestExternalObjectDirectoryEntity;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnnotationArchiveRecordMapperImplPropertyTest {

    private AnnotationArchiveRecordMapperImpl annotationArchiveRecordMapper;

    @Mock
    private ArmDataManagementConfiguration configuration;
    @Mock
    private CurrentTimeHelper currentTimeHelper;

    private MockedStatic<PropertyFileLoader> propertyFileLoader;

    @BeforeEach
    void setUp() {
        annotationArchiveRecordMapper = new AnnotationArchiveRecordMapperImpl(configuration, currentTimeHelper);
    }

    @Nested
    class MapToAnnotationArchiveRecordTest {

        @BeforeEach
        void setUp() {
            when(configuration.getDateTimeFormat()).thenReturn("yyyy-MM-dd'T'HH:mm:ss.SSSX");

            propertyFileLoader = mockStatic(PropertyFileLoader.class);
            propertyFileLoader.when(() -> PropertyFileLoader.loadPropertiesFromFile(any()))
                .thenReturn(new Properties());
        }

        @AfterEach
        void tearDown() {
            propertyFileLoader.close();
        }

        @Test
        void shouldProduceRecordMetadata_withRetConfScore_whenSuppliedWithRetConfScore() {
            // Given
            TestExternalObjectDirectoryEntity externalObjectDirectoryEntity = PersistableFactory.getExternalObjectDirectoryTestData()
                .someMinimalBuilder()
                .annotationDocumentEntity(PersistableFactory.getAnnotationDocumentTestData()
                                              .someMinimalBuilder().retConfScore(RetentionConfidenceScoreEnum.CASE_PERFECTLY_CLOSED)
                                              .build())
                .build();

            // When
            AnnotationArchiveRecord annotationArchiveRecord = annotationArchiveRecordMapper.mapToAnnotationArchiveRecord(externalObjectDirectoryEntity,
                                                                                                                         "someFilename");

            // Then
            assertEquals(RetentionConfidenceScoreEnum.CASE_PERFECTLY_CLOSED.getId(),
                         annotationArchiveRecord.getAnnotationCreateArchiveRecordOperation().getRecordMetadata().getRetentionConfidenceScore());
        }

        @Test
        void shouldProduceRecordMetadata_withNullRetConfScore_whenSuppliedWithNullRetConfScore() {
            // Given
            TestExternalObjectDirectoryEntity externalObjectDirectoryEntity = PersistableFactory.getExternalObjectDirectoryTestData()
                .someMinimalBuilder()
                .annotationDocumentEntity(PersistableFactory.getAnnotationDocumentTestData()
                                              .someMinimalBuilder().retConfScore(null)
                                              .build())
                .build();

            // When
            AnnotationArchiveRecord annotationArchiveRecord = annotationArchiveRecordMapper.mapToAnnotationArchiveRecord(externalObjectDirectoryEntity,
                                                                                                                         "someFilename");

            // Then
            assertNull(annotationArchiveRecord.getAnnotationCreateArchiveRecordOperation().getRecordMetadata().getRetentionConfidenceScore());
        }

    }

}
