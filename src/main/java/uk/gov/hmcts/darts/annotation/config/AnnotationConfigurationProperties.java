package uk.gov.hmcts.darts.annotation.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties("darts.annotations")
@Getter
@Setter
@ToString
@Validated
public class AnnotationConfigurationProperties {

    private List<String> allowedExtensions = new ArrayList<>();
    private List<String> allowedContentTypes = new ArrayList<>();
    private Integer maxFileSize;
}
