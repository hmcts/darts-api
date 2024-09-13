package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.test.common.data.builder.CustomAnnotationDocumentEntity;

import java.time.OffsetDateTime;
import java.util.List;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.RandomStringUtils.random;
import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.minimalUserAccount;

@SuppressWarnings({"HideUtilityClassConstructor"})
public class AnnotationDocumentTestData implements Persistable<CustomAnnotationDocumentEntity.CustomAnnotationDocumentEntityRetrieve> {
    @Override
    public CustomAnnotationDocumentEntity.CustomAnnotationDocumentEntityRetrieve someMinimal() {
        CustomAnnotationDocumentEntity.CustomAnnotationDocumentEntityRetrieve retrieve =
            PersistableFactory.getAnnotationDocumentTestData().someMinimal();

        var postfix = random(10, false, true);
        var userAccount = minimalUserAccount();

        retrieve.getBuilder().annotation(PersistableFactory.getAnnotationTestData().someMinimal().build())
                    .fileType("some-file-type")
                    .fileName("some-file-name-" + postfix)
                    .fileSize(1024)
                    .uploadedDateTime(OffsetDateTime.now())
                    .lastModifiedTimestamp(OffsetDateTime.now())
                    .isHidden(false)
                    .isDeleted(false)
                    .uploadedBy(userAccount)
                    .lastModifiedBy(userAccount);

        return retrieve;
    }

    @Override
    public CustomAnnotationDocumentEntity.CustomAnnotationDocumentEntityRetrieve someMaximal() {
        return someMinimal();
    }

    public AnnotationDocumentEntity minimalAnnotationDocument() {
       return someMinimal().build();

    }

    public AnnotationDocumentEntity createAnnotationDocumentForHearings(List<HearingEntity> hearingEntities) {
        var annotationDocumentEntityRetrieve = someMinimal();
        AnnotationDocumentEntity annotationDocument = annotationDocumentEntityRetrieve.getBuilder().build();
        annotationDocument.getAnnotation().setHearingList(hearingEntities);
        return annotationDocument;
    }

    public AnnotationDocumentEntity createAnnotationDocumentForHearings(HearingEntity... hearingEntities) {
        return createAnnotationDocumentForHearings(stream(hearingEntities).collect(toList()));
    }


}