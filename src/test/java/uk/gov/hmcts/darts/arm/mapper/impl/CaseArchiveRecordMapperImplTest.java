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
import uk.gov.hmcts.darts.arm.model.record.CaseArchiveRecord;
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
class CaseArchiveRecordMapperImplTest {

    private CaseArchiveRecordMapperImpl caseArchiveRecordMapper;

    @Mock
    private ArmDataManagementConfiguration configuration;
    @Mock
    private CurrentTimeHelper currentTimeHelper;

    MockedStatic<PropertyFileLoader> propertyFileLoader;

    @BeforeEach
    void setUp() {
        caseArchiveRecordMapper = new CaseArchiveRecordMapperImpl(configuration, currentTimeHelper);
    }

    @Nested
    class MapToCaseArchiveRecordTest {

        @BeforeEach
        void setUp() {
            when(configuration.getDateTimeFormat()).thenReturn("yyyy-MM-dd'T'HH:mm:ssX");
            when(configuration.getDateFormat()).thenReturn("yyyy-MM-dd");

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
                .caseDocument(PersistableFactory.getCaseDocumentTestData()
                                        .someMinimalBuilder().retConfScore(RetentionConfidenceScoreEnum.CASE_PERFECTLY_CLOSED)
                                        .build())
                .build();

            // When
            CaseArchiveRecord caseArchiveRecord = caseArchiveRecordMapper.mapToCaseArchiveRecord(externalObjectDirectoryEntity,
                                                                                                 "someFilename");

            // Then
            assertEquals(RetentionConfidenceScoreEnum.CASE_PERFECTLY_CLOSED.getId(),
                         caseArchiveRecord.getCaseCreateArchiveRecordOperation().getRecordMetadata().getRetentionConfidenceScore());
        }

        @Test
        void shouldProduceRecordMetadata_withNullRetConfScore_whenSuppliedWithNullRetConfScore() {
            // Given
            TestExternalObjectDirectoryEntity externalObjectDirectoryEntity = PersistableFactory.getExternalObjectDirectoryTestData()
                .someMinimalBuilder()
                .caseDocument(PersistableFactory.getCaseDocumentTestData()
                                        .someMinimalBuilder().retConfScore(null)
                                        .build())
                .build();

            // When
            CaseArchiveRecord caseArchiveRecord = caseArchiveRecordMapper.mapToCaseArchiveRecord(externalObjectDirectoryEntity,
                                                                                                 "someFilename");

            // Then
            assertNull(caseArchiveRecord.getCaseCreateArchiveRecordOperation().getRecordMetadata().getRetentionConfidenceScore());
        }

    }

}