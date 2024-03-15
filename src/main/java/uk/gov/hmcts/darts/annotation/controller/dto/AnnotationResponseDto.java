package uk.gov.hmcts.darts.annotation.controller.dto;

import lombok.Builder;
import lombok.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

@Builder
@Value
public class AnnotationResponseDto {

    Resource resource;
    String fileName;
    MediaType mediaType;
    Integer annotationDocumentId;

}
