package uk.gov.hmcts.darts.datamanagement.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@Getter
public class DataManagementConfiguration {

    @Value("${darts.storage.blob.connection-string}")
    private String blobStorageAccountConnectionString;

    @Value("${darts.storage.blob.container-name.unstructured}")
    private String unstructuredContainerName;

    @Value("${darts.storage.blob.container-name.inbound}")
    private String inboundContainerName;

    @Value("${darts.storage.blob.container-name.outbound}")
    private String outboundContainerName;
}
