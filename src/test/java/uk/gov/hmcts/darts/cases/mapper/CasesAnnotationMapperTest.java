package uk.gov.hmcts.darts.cases.mapper;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.darts.cases.model.Annotation;
import uk.gov.hmcts.darts.common.entity.AnnotationEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CasesAnnotationMapperTest {

    @Test
    void testMapperEntityToModel() {
        AnnotationEntity annotationEntity = CommonTestDataUtil.createAnnotationEntity(1);
        HearingEntity hearingEntity = CommonTestDataUtil.createHearing("1234", LocalDate.now());

        CasesAnnotationMapper annotationMapper = new CasesAnnotationMapper();

        Annotation annotation = annotationMapper.map(hearingEntity, annotationEntity);
        assertEquals(1, annotation.getAnnotationId());
        assertEquals(hearingEntity.getId(), annotation.getHearingId());
        assertEquals(hearingEntity.getHearingDate(), annotation.getHearingDate());
        assertEquals("Some text", annotation.getAnnotationText());

        assertEquals(2, annotation.getAnnotationDocuments().size());
        assertEquals(1, annotation.getAnnotationDocuments().getFirst().getAnnotationDocumentId());
        assertEquals("filename", annotation.getAnnotationDocuments().getFirst().getFileName());
        assertEquals("filetype", annotation.getAnnotationDocuments().getFirst().getFileType());
        assertEquals("annotator user", annotation.getAnnotationDocuments().getFirst().getUploadedBy());

        assertEquals(2, annotation.getAnnotationDocuments().get(1).getAnnotationDocumentId());
        assertEquals(2, annotation.getAnnotationDocuments().get(1).getAnnotationDocumentId());
        assertEquals("filename", annotation.getAnnotationDocuments().get(1).getFileName());
        assertEquals("filetype", annotation.getAnnotationDocuments().get(1).getFileType());
        assertEquals("annotator user", annotation.getAnnotationDocuments().get(1).getUploadedBy());
    }
}
