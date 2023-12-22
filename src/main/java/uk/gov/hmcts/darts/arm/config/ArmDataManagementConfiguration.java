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

    private String connectionString;
    private String containerName;
    private String foldersSubmission;
    private Integer maxRetryAttempts;
    private String publisher;
    private String region;
    private String mediaRecordClass;
    private String transcriptionRecordClass;
    private String annotationRecordClass;
    private String tempBlobWorkspace;
    private String dateTimeFormat;
    private String fileExtension;

}
