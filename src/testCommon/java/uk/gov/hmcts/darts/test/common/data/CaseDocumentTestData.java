package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.CaseDocumentEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.test.common.data.builder.CustomCaseDocumentEntity;
import uk.gov.hmcts.darts.test.common.data.builder.CustomCourtCaseEntity;

public class CaseDocumentTestData implements Persistable< CustomCaseDocumentEntity.CustomCaseDocumentEntityBuilderRetrieve>  {

    private CourtCaseEntity courtCaseEntity = PersistableFactory.getCourtCaseTestData().someMinimalCase();

    CaseDocumentTestData() {

    }

    @Override
    public CustomCaseDocumentEntity.CustomCaseDocumentEntityBuilderRetrieve someMinimal() {
        CustomCaseDocumentEntity.CustomCaseDocumentEntityBuilderRetrieve retrieve = new  CustomCaseDocumentEntity.CustomCaseDocumentEntityBuilderRetrieve();
        retrieve.getBuilder().courtCase(courtCaseEntity)
            .createdBy(UserAccountTestData.minimalUserAccount())
            .lastModifiedBy(UserAccountTestData.minimalUserAccount());
        return retrieve;
    }

    @Override
    public CustomCaseDocumentEntity.CustomCaseDocumentEntityBuilderRetrieve someMaximal() {
        return someMinimal();
    }
}