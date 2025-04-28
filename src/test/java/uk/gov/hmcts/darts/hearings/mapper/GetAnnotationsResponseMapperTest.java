package uk.gov.hmcts.darts.hearings.mapper;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.AnnotationEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;
import uk.gov.hmcts.darts.hearings.model.Annotation;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GetAnnotationsResponseMapperTest {

    @Test
    void given_annotationWithDocuments_mapSuccessfully() {
        UserAccountEntity userAccount = CommonTestDataUtil.createUserAccount();
        AnnotationEntity annotationEntity1 = new AnnotationEntity();
        annotationEntity1.setId(1001);
        annotationEntity1.setCurrentOwner(userAccount);

        HearingEntity hearing = CommonTestDataUtil.createHearing("1001", LocalDate.of(2020, 10, 10));

        annotationEntity1.setHearings(Set.of(hearing));
        annotationEntity1.setTimestamp(OffsetDateTime.of(2020, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC));
        annotationEntity1.setText("annotationText");
        AnnotationDocumentEntity annotationDoc1a = createAnnotationDocumentEntity(11L);
        AnnotationDocumentEntity annotationDoc1b = createAnnotationDocumentEntity(12L);
        AnnotationDocumentEntity annotationDoc1c = createAnnotationDocumentEntity(13L);

        annotationEntity1.setAnnotationDocuments(List.of(annotationDoc1a, annotationDoc1b, annotationDoc1c));

        AnnotationEntity annotationEntity2 = new AnnotationEntity();
        annotationEntity2.setId(1002);
        annotationEntity2.setCurrentOwner(userAccount);

        annotationEntity2.setHearings(Set.of(hearing));
        annotationEntity2.setTimestamp(OffsetDateTime.of(2020, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC));
        annotationEntity2.setText("annotationText");
        AnnotationDocumentEntity annotationDoc2a = createAnnotationDocumentEntity(21L);
        AnnotationDocumentEntity annotationDoc2b = createAnnotationDocumentEntity(22L);
        AnnotationDocumentEntity annotationDoc2c = createAnnotationDocumentEntity(23L);

        annotationEntity2.setAnnotationDocuments(List.of(annotationDoc2a, annotationDoc2b, annotationDoc2c));

        List<AnnotationEntity> annotationList = List.of(annotationEntity1, annotationEntity2);
        List<Annotation> annotations = GetAnnotationsResponseMapper.mapToAnnotations(annotationList, hearing.getId());

        Annotation annotation1 = annotations.getFirst();
        assertEquals(1001, annotation1.getAnnotationId());
        assertEquals(102, annotation1.getHearingId());
        assertEquals(LocalDate.of(2020, 10, 10), annotation1.getHearingDate());
        assertEquals(OffsetDateTime.of(2020, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC), annotation1.getAnnotationTs());
        assertEquals("annotationText", annotation1.getAnnotationText());
        assertEquals(3, annotation1.getAnnotationDocuments().size());
        assertEquals(11, annotation1.getAnnotationDocuments().getFirst().getAnnotationDocumentId());
        assertEquals("filename11", annotation1.getAnnotationDocuments().getFirst().getFileName());
        assertEquals("filetype11", annotation1.getAnnotationDocuments().getFirst().getFileType());
        assertEquals("userFullName11", annotation1.getAnnotationDocuments().getFirst().getUploadedBy());
        assertEquals(OffsetDateTime.of(2020, 10, 10, 10, 11, 0, 0, ZoneOffset.UTC), annotation1.getAnnotationDocuments().getFirst().getUploadedTs());

        Annotation annotation2 = annotations.get(1);
        assertEquals(1002, annotation2.getAnnotationId());
        assertEquals(102, annotation2.getHearingId());
        assertEquals(LocalDate.of(2020, 10, 10), annotation2.getHearingDate());
        assertEquals(OffsetDateTime.of(2020, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC), annotation2.getAnnotationTs());
        assertEquals("annotationText", annotation2.getAnnotationText());
        assertEquals(3, annotation2.getAnnotationDocuments().size());
        assertEquals(21, annotation2.getAnnotationDocuments().getFirst().getAnnotationDocumentId());
        assertEquals("filename21", annotation2.getAnnotationDocuments().getFirst().getFileName());
        assertEquals("filetype21", annotation2.getAnnotationDocuments().getFirst().getFileType());
        assertEquals("userFullName21", annotation2.getAnnotationDocuments().getFirst().getUploadedBy());
        assertEquals("userFullName22", annotation2.getAnnotationDocuments().get(1).getUploadedBy());
        assertEquals("userFullName23", annotation2.getAnnotationDocuments().get(2).getUploadedBy());
        assertEquals(OffsetDateTime.of(2020, 10, 10, 10, 21, 0, 0, ZoneOffset.UTC), annotation2.getAnnotationDocuments().getFirst().getUploadedTs());
    }

    private AnnotationDocumentEntity createAnnotationDocumentEntity(Long id) {
        AnnotationDocumentEntity annotationDoc = new AnnotationDocumentEntity();
        annotationDoc.setId(id);
        annotationDoc.setFileName("filename" + id);
        annotationDoc.setFileType("filetype" + id);
        UserAccountEntity userAccount = CommonTestDataUtil.createUserAccount("user" + id);
        userAccount.setUserFullName("userFullName" + id);
        annotationDoc.setUploadedBy(userAccount);
        annotationDoc.setUploadedDateTime(OffsetDateTime.of(2020, 10, 10, 10, id.intValue(), 0, 0, ZoneOffset.UTC));
        return annotationDoc;
    }


}
