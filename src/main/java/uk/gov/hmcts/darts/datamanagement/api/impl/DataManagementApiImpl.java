package uk.gov.hmcts.darts.datamanagement.api.impl;

import com.azure.core.util.BinaryData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.ObjectDirectoryStatusEntity;
import uk.gov.hmcts.darts.common.repository.ExternalLocationTypeRepository;
import uk.gov.hmcts.darts.common.repository.ExternalObjectDirectoryRepository;
import uk.gov.hmcts.darts.common.repository.ObjectDirectoryStatusRepository;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;
import uk.gov.hmcts.darts.datamanagement.config.DataManagementConfiguration;
import uk.gov.hmcts.darts.datamanagement.service.DataManagementService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum.UNSTRUCTURED;
import static uk.gov.hmcts.darts.common.enums.ObjectDirectoryStatusEnum.STORED;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataManagementApiImpl implements DataManagementApi {

    private final DataManagementService dataManagementService;
    private final DataManagementConfiguration dataManagementConfiguration;
    private final ObjectDirectoryStatusRepository objectDirectoryStatusRepository;
    private final ExternalObjectDirectoryRepository externalObjectDirectoryRepository;
    private final ExternalLocationTypeRepository externalLocationTypeRepository;

    @Override
    public BinaryData getBlobDataFromUnstructuredContainer(UUID blobId) {
        return dataManagementService.getBlobData(dataManagementConfiguration.getUnstructuredContainerName(), blobId);
    }

    @Override
    public BinaryData getBlobDataFromOutboundContainer(UUID blobId) {
        return dataManagementService.getBlobData(dataManagementConfiguration.getOutboundContainerName(), blobId);
    }

    @Override
    public UUID saveBlobDataToOutboundContainer(BinaryData binaryData) {
        return dataManagementService.saveBlobData(dataManagementConfiguration.getOutboundContainerName(), binaryData);
    }

    @Override
    public void deleteBlobDataFromOutboundContainer(UUID blobId) {
        dataManagementService.deleteBlobData(dataManagementConfiguration.getOutboundContainerName(), blobId);
    }

    @Override
    public Optional<UUID> getMediaLocation(MediaEntity media) {
        Optional<UUID> externalLocation = Optional.empty();

        ObjectDirectoryStatusEntity objectDirectoryStatus = objectDirectoryStatusRepository.getReferenceById(STORED.getId());
        ExternalLocationTypeEntity externalLocationType = externalLocationTypeRepository.getReferenceById(UNSTRUCTURED.getId());
        List<ExternalObjectDirectoryEntity> externalObjectDirectoryEntityList = externalObjectDirectoryRepository.findByMediaStatusAndType(
            media, objectDirectoryStatus, externalLocationType
        );

        if (!externalObjectDirectoryEntityList.isEmpty()) {
            if (externalObjectDirectoryEntityList.size() != 1) {
                log.warn(
                    "Only one External Object Directory expected, but found {} for mediaId={}, statusEnum={}, externalLocationTypeId={}",
                    externalObjectDirectoryEntityList.size(),
                    media.getId(),
                    STORED,
                    externalLocationType.getId()
                );
            }
            externalLocation = Optional.ofNullable(externalObjectDirectoryEntityList.get(0).getExternalLocation());
        }

        return externalLocation;
    }

}
