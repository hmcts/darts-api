package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.test.common.data.builder.TestAnnotationDocumentEntity;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;
import static org.apache.commons.lang3.RandomStringUtils.random;
import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.minimalUserAccount;

public final class AnnotationDocumentTestData implements Persistable<TestAnnotationDocumentEntity.TestAnnotationDocumentEntityRetrieve,
    AnnotationDocumentEntity, TestAnnotationDocumentEntity.TestAnnotationDocumentEntityBuilder> {
    @Override
    public AnnotationDocumentEntity someMinimal() {
        return someMinimalBuilder().build().getEntity();
    }

    public AnnotationDocumentEntity minimalAnnotationDocument() {
        return someMinimal();
    }

    public AnnotationDocumentEntity createAnnotationDocumentForHearings(Set<HearingEntity> hearingEntities) {
        var annotationDocumentEntityRetrieve = someMinimalBuilder();
        AnnotationDocumentEntity annotationDocument = annotationDocumentEntityRetrieve.build().getEntity();
        annotationDocument.getAnnotation().setHearings(hearingEntities);
        return annotationDocument;
    }

    public AnnotationDocumentEntity createAnnotationDocumentForHearings(HearingEntity... hearingEntities) {
        return createAnnotationDocumentForHearings(stream(hearingEntities).collect(Collectors.toSet()));
    }

    @Override
    public TestAnnotationDocumentEntity.TestAnnotationDocumentEntityBuilder someMinimalBuilder() {
        return someMinimalBuilderHolder().getBuilder();
    }

    @Override
    public TestAnnotationDocumentEntity.TestAnnotationDocumentEntityRetrieve someMinimalBuilderHolder() {
        TestAnnotationDocumentEntity.TestAnnotationDocumentEntityRetrieve retrieve =
            new TestAnnotationDocumentEntity.TestAnnotationDocumentEntityRetrieve();

        var postfix = random(10, false, true);
        var userAccount = minimalUserAccount();

        retrieve.getBuilder().annotation(PersistableFactory.getAnnotationTestData().someMinimal())
            .fileType("some-file-type")
            .fileName("some-file-name-" + postfix)
            .fileSize(1024)
            .uploadedDateTime(OffsetDateTime.now())
            .lastModifiedTimestamp(OffsetDateTime.now())
            .isHidden(false)
            .isDeleted(false)
            .uploadedBy(userAccount)
            .lastModifiedById(0);

        return retrieve;
    }
}