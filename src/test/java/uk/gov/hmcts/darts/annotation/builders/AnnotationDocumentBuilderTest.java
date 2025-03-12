package uk.gov.hmcts.darts.annotation.builders;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.AnnotationDocumentEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnnotationDocumentBuilderTest {

    @Mock
    private UserIdentity userIdentity;

    private AnnotationDocumentBuilder annotationDocumentBuilder;
    private UserAccountEntity userAccountEntity;

    @BeforeEach
    void setUp() {
        annotationDocumentBuilder = new AnnotationDocumentBuilder(userIdentity);
        userAccountEntity = new UserAccountEntity();
    }

    @Test
    void buildsAnnotationDocumentCorrectly() {
        when(userIdentity.getUserAccount()).thenReturn(userAccountEntity);
        var document = someMultipartFile();

        assertThat(annotationDocumentBuilder.buildFrom(document, "some-checksum"))
            .isInstanceOf(AnnotationDocumentEntity.class)
            .hasFieldOrPropertyWithValue("uploadedBy", userAccountEntity)
            .hasFieldOrPropertyWithValue("checksum", "some-checksum")
            .hasFieldOrPropertyWithValue("fileName", "original-filename")
            .hasFieldOrPropertyWithValue("fileType", "some-content-type")
            .hasFieldOrPropertyWithValue("fileSize", 12);
    }

    private MultipartFile someMultipartFile() {
        return new MockMultipartFile(
            "some-multi-part-file",
            "original-filename",
            "some-content-type",
            "some-content".getBytes());
    }
    
}
