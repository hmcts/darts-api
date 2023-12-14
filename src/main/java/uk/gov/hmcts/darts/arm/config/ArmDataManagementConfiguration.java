package uk.gov.hmcts.darts.arm.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@Getter
public class ArmDataManagementConfiguration {

    @Value("${darts.storage.arm.connection-string}")
    private String armStorageAccountConnectionString;

    @Value("${darts.storage.arm.container-name}")
    private String armContainerName;

    @Value("${darts.storage.arm.folders.submission}")
    private String armSubmissionDropZone;

    @Value("${darts.storage.arm.max-retry-attempts}")
    private Integer maxRetryAttempts;

    @Value("${darts.storage.arm.publisher}")
    private String publisher;

    @Value("${darts.storage.arm.region}")
    private String region;

    @Value("${darts.storage.arm.media_record_class}")
    private String mediaRecordClass;

    @Value("${darts.storage.arm.transcription_record_class}")
    private String transcriptionRecordClass;

    @Value("${darts.storage.arm.annotation_record_class}")
    private String annotationRecordClass;

    @Value("${darts.storage.arm.temp-blob-workspace}")
    private String tempBlobWorkspace;

    @Value("${darts.storage.arm.date_time_format}")
    private String dateTimeFormat;

    @Value("${darts.storage.arm.file_extension}")
    private String fileExtension;

}
