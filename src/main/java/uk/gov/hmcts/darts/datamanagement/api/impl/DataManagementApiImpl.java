package uk.gov.hmcts.darts.datamanagement.api.impl;

import com.azure.core.util.BinaryData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;
import uk.gov.hmcts.darts.datamanagement.config.DataManagementConfiguration;
import uk.gov.hmcts.darts.datamanagement.service.DataManagementService;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DataManagementApiImpl implements DataManagementApi {

    private final DataManagementService dataManagementService;
    private final DataManagementConfiguration dataManagementConfiguration;

    @Override
    public BinaryData getBlobDataFromUnstructuredContainer(UUID blobId) {
        return dataManagementService.getBlobData(dataManagementConfiguration.getUnstructuredContainerName(), blobId);
    }

    @Override
    public UUID saveBlobDataToOutboundContainer(BinaryData binaryData) {
        return dataManagementService.saveBlobData(dataManagementConfiguration.getOutboundContainerName(), binaryData);
    }

    @Override
    public void deleteBlobDataFromOutboundContainer(UUID blobId) {
        dataManagementService.deleteBlobData(dataManagementConfiguration.getOutboundContainerName(), blobId);
    }

}
