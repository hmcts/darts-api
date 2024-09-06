package uk.gov.hmcts.darts.test.common.data;

import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.RandomStringUtils.random;
import static uk.gov.hmcts.darts.test.common.data.AnnotationTestData.minimalAnnotationEntity;
import static uk.gov.hmcts.darts.test.common.data.UserAccountTestData.minimalUserAccount;

@SuppressWarnings({"HideUtilityClassConstructor"})
public class AnnotationDocumentTestData {

    public static AnnotationDocumentEntity minimalAnnotationDocument() {
        var postfix = random(10, false, true);
        var annotationDocument = new AnnotationDocumentEntity();
        var annotation = minimalAnnotationEntity();
        annotation.setAnnotationDocuments(new ArrayList<>(Arrays.asList(annotationDocument)));
        annotationDocument.setAnnotation(annotation);
        annotationDocument.setFileName("some-file-name-" + postfix);
        annotationDocument.setFileType("some-file-type");
        annotationDocument.setFileSize(1024);
        annotationDocument.setUploadedDateTime(OffsetDateTime.now());
        annotationDocument.setLastModifiedTimestamp(OffsetDateTime.now());
        annotationDocument.setHidden(false);
        annotationDocument.setDeleted(false);
        var userAccount = minimalUserAccount();
        annotationDocument.setUploadedBy(userAccount);
        annotationDocument.setLastModifiedBy(userAccount);
        annotationDocument.setLastModifiedBy(userAccount);
        return annotationDocument;
    }

    public static AnnotationDocumentEntity createAnnotationDocumentForHearings(List<HearingEntity> hearingEntities) {
        var annotationDocument = minimalAnnotationDocument();
        annotationDocument.getAnnotation().setHearingList(hearingEntities);
        return annotationDocument;
    }

    public static AnnotationDocumentEntity createAnnotationDocumentForHearings(HearingEntity... hearingEntities) {
        return createAnnotationDocumentForHearings(stream(hearingEntities).collect(toList()));
    }
}
