package uk.gov.hmcts.darts.arm.service.impl;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.arm.dao.ArmDataManagementDao;
import uk.gov.hmcts.darts.arm.service.ArmService;

@Service
@Slf4j
@Profile("!intTest")
@RequiredArgsConstructor
public class ArmServiceImpl implements ArmService {

    private final ArmDataManagementDao armDataManagementDao;
    private final ArmDataManagementConfiguration armDataManagementConfiguration;

    @Override
    public String saveBlobData(String containerName, String filename, BinaryData binaryData) {

        String blobPathAndName = armDataManagementConfiguration.getArmSubmissionDropZone() + filename;
        BlobContainerClient containerClient = armDataManagementDao.getBlobContainerClient(containerName);
        BlobClient client = armDataManagementDao.getBlobClient(containerClient, blobPathAndName);
        client.upload(binaryData);

        return filename;
    }


}
