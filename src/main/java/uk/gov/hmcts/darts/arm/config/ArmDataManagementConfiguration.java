package uk.gov.hmcts.darts.arm.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ArmApiConfigurationProperties.class)
@ConfigurationProperties(prefix = "darts.storage.arm")
@Getter
@Setter
public class ArmDataManagementConfiguration {

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
    private String dateFormat;
    private String fileExtension;
    private String listBlobsTimeoutDuration;
    private String mediaRecordPropertiesFile;
    private String transcriptionRecordPropertiesFile;
    private String annotationRecordPropertiesFile;
    private String caseRecordPropertiesFile;

    @Getter
    @Setter
    public static class Folders {
        private String submission;
        private String collected;
        private String response;
    }

}
