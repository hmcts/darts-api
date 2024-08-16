package uk.gov.hmcts.darts.annotation;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.darts.annotation.service.AnnotationUploadService;
import uk.gov.hmcts.darts.annotations.model.Annotation;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.AnnotationEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.darts.test.common.data.HearingTestData.createSomeMinimalHearing;

@Disabled("Impacted by V1_363__adding_not_null_constraints_part_4.sql")
@SuppressWarnings("VariableDeclarationUsageDistance")
@Slf4j
@Disabled("Impacted by V1_364_*.sql")
class AnnotationUploadServiceTest extends IntegrationBase {
    private static final String REQUESTER_EMAIL = "test.user@example.com";

    @Autowired
    private AnnotationUploadService uploadService;

    @BeforeEach
    void setUp() {
        Jwt jwt = Jwt.withTokenValue("test")
            .header("alg", "RS256")
            .claim("sub", UUID.randomUUID().toString())
            .claim("emails", List.of(REQUESTER_EMAIL))
            .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        dartsDatabase.createTestUserAccount();
    }

    @Test
    void persistsAnnotationCorrectly() {
        var hearing = dartsDatabase.save(createSomeMinimalHearing());
        var annotation = someAnnotationFor(hearing);
        var document = someMultipartFile();

        var annotationId = uploadService.upload(document, annotation);

        assertThat(dartsDatabase.findAnnotationById(annotationId)).isInstanceOf(AnnotationEntity.class);
    }

    @Test
    void addsAnnotationToCorrectHearing() {
        var hearing = dartsDatabase.save(createSomeMinimalHearing());
        var annotation = someAnnotationFor(hearing);
        var document = someMultipartFile();

        var annotationId = uploadService.upload(document, annotation);

        var annotations = dartsDatabase.findAnnotationsFor(hearing.getId());
        assertThat(annotations).extracting("id").containsExactly(annotationId);
    }


    @Test
    void persistsAnnotationDocumentCorrectly() {
        var hearing = dartsDatabase.save(createSomeMinimalHearing());
        var annotation = someAnnotationFor(hearing);
        var document = someMultipartFile();

        var annotationId = uploadService.upload(document, annotation);

        assertThat(dartsDatabase.findAnnotationDocumentFor(annotationId)).isInstanceOf(AnnotationDocumentEntity.class);
    }

    @Test
    void persistExternalObjectDirectoryCorrectly() {
        var hearing = dartsDatabase.save(createSomeMinimalHearing());
        var annotation = someAnnotationFor(hearing);
        var document = someMultipartFile();

        var annotationId = uploadService.upload(document, annotation);

        Optional<AnnotationEntity> annotationEntityOptional = dartsDatabase.getAnnotationRepository().findById(annotationId);
        assertTrue(annotationEntityOptional.isPresent());

        List<AnnotationEntity> annotationByHearing = dartsDatabase.getAnnotationRepository().findByHearingId(hearing.getId());
        assertFalse(annotationByHearing.isEmpty());

        assertThat(dartsDatabase.findExternalObjectDirectoryFor(annotationId).size()).isEqualTo(2);
    }

    private MultipartFile someMultipartFile() {
        return new MockMultipartFile(
            "some-multi-part-file",
            "original-filename.doc",
            "application/msword",
            "some-content".getBytes());
    }

    private Annotation someAnnotationFor(HearingEntity hearing) {
        var annotation = new Annotation();
        annotation.setHearingId(hearing.getId());
        return annotation;
    }

}
