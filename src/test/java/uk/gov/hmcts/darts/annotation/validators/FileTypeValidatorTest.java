package uk.gov.hmcts.darts.annotation.validators;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.darts.annotation.config.AnnotationConfigurationProperties;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static uk.gov.hmcts.darts.annotation.errors.AnnotationApiError.BAD_REQUEST_CONTENT_TYPE;
import static uk.gov.hmcts.darts.annotation.errors.AnnotationApiError.BAD_REQUEST_DOC_TYPE;
import static uk.gov.hmcts.darts.annotation.errors.AnnotationApiError.BAD_REQUEST_FILE_SIZE;

class FileTypeValidatorTest {

    private static final List<String> VALID_FILE_EXTENSIONS = List.of("pdf", "doc");
    private static final List<String> VALID_CONTENT_TYPES = List.of("some-type", "some-other-type");
    private static final int MAX_FILE_SIZE = 100;

    private FileTypeValidator fileTypeValidator;

    @BeforeEach
    void setUp() {
        fileTypeValidator = new FileTypeValidator(someAnnotationConfigurationProps());
    }

    @Test
    void throwsIfFileExtensionNotAllowed() {
        var multipartFile = someMultipartFileWithFilename("some-file.badext");

        assertThatThrownBy(() -> fileTypeValidator.validate(multipartFile))
            .isInstanceOf(DartsApiException.class)
            .hasFieldOrPropertyWithValue("error", BAD_REQUEST_DOC_TYPE);
    }

    @Test
    void throwsIfFileSizeGreaterThanMaximum() {
        var multipartFile = someMultipartFileWithContent(generateBytesOfSize(MAX_FILE_SIZE + 1));

        assertThatThrownBy(() -> fileTypeValidator.validate(multipartFile))
            .isInstanceOf(DartsApiException.class)
            .hasFieldOrPropertyWithValue("error", BAD_REQUEST_FILE_SIZE);
    }

    @Test
    void throwsIfContentTypeNotAllowed() {
        var multipartFile = someMultipartFileWithContentType("some-bad-content-type");

        assertThatThrownBy(() -> fileTypeValidator.validate(multipartFile))
            .isInstanceOf(DartsApiException.class)
            .hasFieldOrPropertyWithValue("error", BAD_REQUEST_CONTENT_TYPE);
    }

    @Test
    void doesntThrowIfMultipartFileValid() {
        assertThatNoException().isThrownBy(() -> fileTypeValidator.validate(someValidMultipartFile()));
    }

    private MultipartFile someValidMultipartFile() {
        return new MockMultipartFile(
            "some-multi-part-file",
            "some-filename." + VALID_FILE_EXTENSIONS.getFirst(),
            VALID_CONTENT_TYPES.getFirst(),
            "some-content".getBytes()
        );
    }

    private MultipartFile someMultipartFileWithContentType(String contentType) {
        return new MockMultipartFile(
            "some-multi-part-file",
            "some-filename." + VALID_FILE_EXTENSIONS.getFirst(),
            contentType,
            "some-content".getBytes()
        );
    }

    private MultipartFile someMultipartFileWithFilename(String filename) {
        return new MockMultipartFile(
            "some-multi-part-file",
            filename,
            VALID_CONTENT_TYPES.getFirst(),
            "some-content".getBytes()
        );
    }

    private MultipartFile someMultipartFileWithContent(byte[] fileContent) {
        return new MockMultipartFile(
            "some-multi-part-file",
            "some-filename." + VALID_FILE_EXTENSIONS.getFirst(),
            VALID_CONTENT_TYPES.getFirst(),
            fileContent
        );
    }

    private byte[] generateBytesOfSize(int numberOfBytes) {
        byte[] bytes = new byte[numberOfBytes];
        new Random().nextBytes(bytes);
        return bytes;
    }

    private AnnotationConfigurationProperties someAnnotationConfigurationProps() {
        var properties = new AnnotationConfigurationProperties();
        properties.setAllowedExtensions(VALID_FILE_EXTENSIONS);
        properties.setMaxFileSize(MAX_FILE_SIZE);
        properties.setAllowedContentTypes(VALID_CONTENT_TYPES);
        return properties;
    }
}
