package uk.gov.hmcts.darts.datamanagement.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.darts.common.datamanagement.StorageConfiguration;

@Slf4j
@Configuration
@Getter
public class DataManagementConfiguration extends StorageConfiguration {

    @Value("${darts.storage.blob.connection-string}")
    private String blobStorageAccountConnectionString;

    @Value("${darts.storage.blob.container-name.unstructured}")
    private String unstructuredContainerName;

    @Value("${darts.storage.blob.container-name.inbound}")
    private String inboundContainerName;

    @Value("${darts.storage.blob.container-name.outbound}")
    private String outboundContainerName;

    @Value("${darts.storage.blob.delete.timeout:60}")
    private int deleteTimeout;

    @Override
    @Value("${darts.storage.blob.temp-blob-workspace}")
    public void setTempBlobWorkspace(String tempBlobWorkspace) {
        super.setTempBlobWorkspace(tempBlobWorkspace);
    }
}