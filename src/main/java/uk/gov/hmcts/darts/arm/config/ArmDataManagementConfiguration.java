package uk.gov.hmcts.darts.arm.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "darts.storage.arm")
@Getter
@Setter
public class ArmDataManagementConfiguration {

    @Value("${darts.storage.arm.connection-string}")
    private String armStorageAccountConnectionString;

    @Value("${darts.storage.arm.container-name}")
    private String armContainerName;

    @Value("${darts.storage.arm.folders.submission}")
    private String armSubmissionDropZone;

    @Value("${darts.storage.arm.folders.collected}")
    private String armCollectedDropZone;

    @Value("${darts.storage.arm.folders.response}")
    private String armResponseDropZone;
    private String sasEndpoint;
    private String containerName;
    private Folders folders;
    private Integer maxRetryAttempts;
    private String publisher;
    private String region;
    private String mediaRecordClass;
    private String transcriptionRecordClass;
    private String annotationRecordClass;
    private String tempBlobWorkspace;
    private String dateTimeFormat;
    private String fileExtension;

    @Getter
    @Setter
    public static class Folders {
        private String submission;
    }

}
