package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.CaseDocumentEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.test.common.data.builder.TestCaseDocumentEntity;

import java.time.OffsetDateTime;

public final class CaseDocumentTestData implements Persistable<TestCaseDocumentEntity.TestCaseDocumentEntityBuilderRetrieve,
    CaseDocumentEntity, TestCaseDocumentEntity.TestCaseDocumentEntityBuilder> {

    private final CourtCaseEntity courtCaseEntity = PersistableFactory.getCourtCaseTestData().someMinimalCase();

    CaseDocumentTestData() {
        // This constructor is intentionally empty. Nothing special is needed here.
    }

    @Override
    public CaseDocumentEntity someMinimal() {
        return someMinimalBuilder().build().getEntity();
    }


    @Override
    public TestCaseDocumentEntity.TestCaseDocumentEntityBuilder someMinimalBuilder() {
        return someMinimalBuilderHolder().getBuilder();
    }

    @Override
    public TestCaseDocumentEntity.TestCaseDocumentEntityBuilderRetrieve someMinimalBuilderHolder() {
        TestCaseDocumentEntity.TestCaseDocumentEntityBuilderRetrieve retrieve =
            new TestCaseDocumentEntity.TestCaseDocumentEntityBuilderRetrieve();
        retrieve.getBuilder().courtCase(courtCaseEntity)
            .createdById(0)
            .lastModifiedById(0)
            .fileName("some-file-name")
            .fileType("some-file-type")
            .fileSize(1024)
            .hidden(false)
            .lastModifiedDateTime(OffsetDateTime.now())
            .isDeleted(false);
        return retrieve;
    }
}