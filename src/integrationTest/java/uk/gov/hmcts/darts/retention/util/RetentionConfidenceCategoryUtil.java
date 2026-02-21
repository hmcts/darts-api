package uk.gov.hmcts.darts.retention.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceCategoryEnum;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceReasonEnum;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceScoreEnum;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.test.common.data.RetentionConfidenceCategoryMapperTestData;
import uk.gov.hmcts.darts.test.common.data.builder.TestRetentionConfidenceCategoryMapperEntity;
import uk.gov.hmcts.darts.testutils.stubs.DartsPersistence;

@Component
public class RetentionConfidenceCategoryUtil {

    @Autowired
    protected DartsPersistence dartsPersistence;

    public void createAndSaveRetentionConfidenceCategoryMappings() {
        createRetentionConfidenceCategoryMapperEntity(
            RetentionConfidenceCategoryEnum.AGED_CASE_CASE_CLOSED,
            RetentionConfidenceReasonEnum.AGED_CASE,
            RetentionConfidenceScoreEnum.CASE_PERFECTLY_CLOSED
        );
        createRetentionConfidenceCategoryMapperEntity(
            RetentionConfidenceCategoryEnum.AGED_CASE_MAX_EVENT_CLOSED,
            RetentionConfidenceReasonEnum.MAX_EVENT_CLOSED,
            RetentionConfidenceScoreEnum.CASE_NOT_PERFECTLY_CLOSED
        );
        createRetentionConfidenceCategoryMapperEntity(
            RetentionConfidenceCategoryEnum.AGED_CASE_MAX_MEDIA_CLOSED,
            RetentionConfidenceReasonEnum.MAX_MEDIA_CLOSED,
            RetentionConfidenceScoreEnum.CASE_NOT_PERFECTLY_CLOSED
        );
        createRetentionConfidenceCategoryMapperEntity(
            RetentionConfidenceCategoryEnum.AGED_CASE_MAX_HEARING_CLOSED,
            RetentionConfidenceReasonEnum.MAX_HEARING_CLOSED,
            RetentionConfidenceScoreEnum.CASE_NOT_PERFECTLY_CLOSED
        );
        createRetentionConfidenceCategoryMapperEntity(
            RetentionConfidenceCategoryEnum.AGED_CASE_CASE_CREATION_CLOSED,
            RetentionConfidenceReasonEnum.CASE_CREATION_CLOSED,
            RetentionConfidenceScoreEnum.CASE_NOT_PERFECTLY_CLOSED
        );
        createRetentionConfidenceCategoryMapperEntity(
            RetentionConfidenceCategoryEnum.CASE_CLOSED,
            RetentionConfidenceReasonEnum.CASE_CLOSED,
            RetentionConfidenceScoreEnum.CASE_PERFECTLY_CLOSED
        );
    }

    private void createRetentionConfidenceCategoryMapperEntity(RetentionConfidenceCategoryEnum retentionConfidenceCategoryEnum,
                                                               RetentionConfidenceReasonEnum retentionConfidenceReasonEnum,
                                                               RetentionConfidenceScoreEnum retentionConfidenceScoreEnum) {

        RetentionConfidenceCategoryMapperTestData testData = PersistableFactory.getRetentionConfidenceCategoryMapperTestData();
        TestRetentionConfidenceCategoryMapperEntity agedCaseMappingEntity = testData.someMinimalBuilder()
            .confidenceCategory(retentionConfidenceCategoryEnum != null ? retentionConfidenceCategoryEnum.getId() : null)
            .confidenceReason(retentionConfidenceReasonEnum)
            .confidenceScore(retentionConfidenceScoreEnum)
            .build();
        dartsPersistence.save(agedCaseMappingEntity.getEntity());
    }

}
