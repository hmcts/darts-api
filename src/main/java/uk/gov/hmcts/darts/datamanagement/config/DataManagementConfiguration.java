package uk.gov.hmcts.darts.datamanagement.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import uk.gov.hmcts.darts.common.datamanagement.StorageConfiguration;

import java.time.Duration;

@Slf4j
@Configuration
@Getter
public class DataManagementConfiguration extends StorageConfiguration {

    @Autowired
    private Environment environment;

    @Value("${darts.storage.blob.client.connection-string}")
    private String blobStorageAccountConnectionString;

    @Value("${darts.storage.blob.client.block-size-bytes}")
    private long blobClientBlockSizeBytes;

    @Value("${darts.storage.blob.client.max-single-upload-size-bytes}")
    private long blobClientMaxSingleUploadSizeBytes;

    @Value("${darts.storage.blob.client.max-concurrency}")
    private int blobClientMaxConcurrency;

    @Value("${darts.storage.blob.client.timeout}")
    private Duration blobClientTimeout;

    @Value("${darts.storage.blob.container-name.unstructured}")
    private String unstructuredContainerName;

    @Value("${darts.storage.blob.container-name.inbound}")
    private String inboundContainerName;

    @Value("${darts.storage.blob.container-name.outbound}")
    private String outboundContainerName;

    @Value("${darts.storage.blob.container-name.arm}")
    private String armContainerName;

    @Value("${darts.storage.blob.delete.timeout:60}")
    private int deleteTimeout;

    @Value("${darts.storage.blob.az-copy-executable}")
    private String azCopyExecutable;

    @Value("${darts.storage.blob.az-copy-preserve-access-tier}")
    private String azCopyPreserveAccessTier;

    @Value("${darts.storage.blob.az-copy-log-level}")
    private String azCopyLogLevel;

    @Override
    @Value("${darts.storage.blob.temp-blob-workspace}")
    public void setTempBlobWorkspace(String tempBlobWorkspace) {
        super.setTempBlobWorkspace(tempBlobWorkspace);
    }

    public String getContainerSasUrl(String containerName) {
        return environment.getProperty(String.format("darts.storage.blob.sas-url.%s", containerName));
    }
}