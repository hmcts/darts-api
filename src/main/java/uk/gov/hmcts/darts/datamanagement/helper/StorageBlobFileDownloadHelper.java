package uk.gov.hmcts.darts.datamanagement.helper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.datamanagement.api.DataManagementFacade;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadResponseMetaData;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadableExternalObjectDirectories;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;

import java.util.List;

@Component
@RequiredArgsConstructor
public class StorageBlobFileDownloadHelper {

    private final DataManagementFacade dataManagementFacade;

    public DownloadResponseMetaData getDownloadResponse(List<ExternalObjectDirectoryEntity> externalObjectDirectories) {
        DownloadableExternalObjectDirectories downloadableExternalObjectDirectories = DownloadableExternalObjectDirectories.getFileBasedDownload(
            externalObjectDirectories);

        dataManagementFacade.getDataFromUnstructuredArmAndDetsBlobs(downloadableExternalObjectDirectories);

        return downloadableExternalObjectDirectories.getResponse();
    }

}