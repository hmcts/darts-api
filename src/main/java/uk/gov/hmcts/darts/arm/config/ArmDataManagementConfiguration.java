package uk.gov.hmcts.darts.arm.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.darts.common.datamanagement.StorageConfiguration;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(ArmApiConfigurationProperties.class)
@ConfigurationProperties(prefix = "darts.storage.arm")
@Getter
@Setter
public class ArmDataManagementConfiguration extends StorageConfiguration {

    private String sasEndpoint;
    private String containerName;
    private Folders folders;
    private Integer maxRetryAttempts;
    private String publisher;
    private String region;
    private String mediaRecordClass;
    private String transcriptionRecordClass;
    private String annotationRecordClass;
    private String caseRecordClass;
    private String dateTimeFormat;
    private String dateFormat;
    private String fileExtension;
    private String listBlobsTimeoutDuration;
    private String mediaRecordPropertiesFile;
    private String transcriptionRecordPropertiesFile;
    private String annotationRecordPropertiesFile;
    private String caseRecordPropertiesFile;
    private Integer responseCleanupBufferDays;
    private String manifestFilePrefix;
    private String armClient;
    private String continuationTokenDuration;
    private Integer eventDateAdjustmentYears;
    private Integer maxContinuationBatchSize;
    private Duration armMissingResponseDuration;
    private String inputUploadResponseTimestampFormat;


    @Getter
    @Setter
    public static class Folders {
        private String submission;
        private String collected;
        private String response;
    }

}