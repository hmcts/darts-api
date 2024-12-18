package uk.gov.hmcts.darts.common.repository;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import uk.gov.hmcts.darts.common.entity.RetentionConfidenceCategoryMapperEntity;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceCategoryEnum;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceReasonEnum;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceScoreEnum;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.test.common.data.RetentionConfidenceCategoryMapperTestData;
import uk.gov.hmcts.darts.test.common.data.builder.TestRetentionConfidenceCategoryMapperEntity;
import uk.gov.hmcts.darts.testutils.PostgresIntegrationBase;

import java.sql.SQLException;
import java.util.Optional;
import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RetentionConfidenceCategoryMapperRepositoryTest extends PostgresIntegrationBase {

    @Autowired
    private RetentionConfidenceCategoryMapperRepository repository;

    @Autowired
    private DataSource dataSource;

    private static final RetentionConfidenceCategoryMapperTestData CATEGORY_MAPPER_TEST_DATA =
        PersistableFactory.getRetentionConfidenceCategoryMapperTestData();


    @Nested
    class SaveTest {

        @Test
        @SuppressWarnings("PMD.CheckResultSet")
        void shouldSaveEntity_withStringValueForReason_andNumericsForCategoryAndScore() throws SQLException {
            // Given
            TestRetentionConfidenceCategoryMapperEntity mapperTestEntity = CATEGORY_MAPPER_TEST_DATA.someMinimalBuilder()
                .confidenceCategory(RetentionConfidenceCategoryEnum.MANUAL_OVERRIDE)
                .confidenceReason(RetentionConfidenceReasonEnum.MANUAL_OVERRIDE)
                .confidenceScore(RetentionConfidenceScoreEnum.CASE_PERFECTLY_CLOSED)
                .build();

            // When
            var preparedEntity = dartsPersistence.save(mapperTestEntity.getEntity());

            // Then
            String query = "SELECT confidence_category, ret_conf_reason, ret_conf_score FROM retention_confidence_category_mapper WHERE rcc_id = ?";
            try (var connection = dataSource.getConnection();
                 var preparedStatement = connection.prepareStatement(query)) {
                preparedStatement.setInt(1, preparedEntity.getId());
                try (var resultSet = preparedStatement.executeQuery()) {
                    assertTrue(resultSet.next());

                    assertEquals(RetentionConfidenceReasonEnum.MANUAL_OVERRIDE.name(), resultSet.getString("ret_conf_reason"));
                    assertEquals(RetentionConfidenceCategoryEnum.MANUAL_OVERRIDE.getId(), resultSet.getInt("confidence_category"));
                    assertEquals(RetentionConfidenceScoreEnum.CASE_PERFECTLY_CLOSED.getId(), resultSet.getInt("ret_conf_score"));
                }
            }
        }

    }

    @Nested
    class FindByConfidenceCategoryTest {

        @Test
        void shouldReturnSingleResult_whenSingleMatchExists() {
            // Given
            RetentionConfidenceCategoryEnum category = RetentionConfidenceCategoryEnum.CASE_CLOSED;

            TestRetentionConfidenceCategoryMapperEntity mapperTestEntity = CATEGORY_MAPPER_TEST_DATA.someMinimalBuilder()
                .confidenceCategory(category)
                .build();

            var preparedEntity = dartsPersistence.save(mapperTestEntity.getEntity());

            // When
            Optional<RetentionConfidenceCategoryMapperEntity> returnedEntity = repository.findByConfidenceCategory(category);

            // Then
            assertTrue(returnedEntity.isPresent());
            var result = returnedEntity.get();
            assertEquals(preparedEntity.getId(), result.getId());
        }

        @Test
        void shouldThrowException_whenMultipleMatchesExist() {
            // Given
            RetentionConfidenceCategoryEnum category = RetentionConfidenceCategoryEnum.CASE_CLOSED;

            TestRetentionConfidenceCategoryMapperEntity mapperTestEntity1 = CATEGORY_MAPPER_TEST_DATA.someMinimalBuilder()
                .confidenceCategory(category)
                .build();
            dartsPersistence.save(mapperTestEntity1.getEntity());

            TestRetentionConfidenceCategoryMapperEntity mapperTestEntity2 = CATEGORY_MAPPER_TEST_DATA.someMinimalBuilder()
                .confidenceCategory(category)
                .build();
            dartsPersistence.save(mapperTestEntity2.getEntity());

            // When
            IncorrectResultSizeDataAccessException exception = assertThrows(IncorrectResultSizeDataAccessException.class, () ->
                repository.findByConfidenceCategory(category));
            assertEquals("Incorrect result size: expected 1, actual 2", exception.getMessage());
        }

        @Test
        void shouldReturnEmptyOptional_whenNoMatchExists() {
            Optional<RetentionConfidenceCategoryMapperEntity> result = repository.findByConfidenceCategory(RetentionConfidenceCategoryEnum.CASE_CLOSED);

            assertFalse(result.isPresent());
        }

        @Test
        void shouldReturnEmptyOptional_whenProvidedWithNullParam() {
            Optional<RetentionConfidenceCategoryMapperEntity> result = repository.findByConfidenceCategory(null);

            assertFalse(result.isPresent());
        }

    }

}
