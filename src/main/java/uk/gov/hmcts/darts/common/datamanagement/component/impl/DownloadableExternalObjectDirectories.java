package uk.gov.hmcts.darts.common.datamanagement.component.impl;

import lombok.Getter;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;

import java.io.IOException;
import java.util.Collection;

@Getter
public class DownloadableExternalObjectDirectories {
    private final Collection<ExternalObjectDirectoryEntity> entities;
    private final DownloadResponseMetaData response;

    public DownloadableExternalObjectDirectories(Collection<ExternalObjectDirectoryEntity> entities, DownloadResponseMetaData response) {
        this.entities = entities;
        this.response = response;
    }

    public static DownloadableExternalObjectDirectories getFileBasedDownload(Collection<ExternalObjectDirectoryEntity> entities) throws IOException {
        return new DownloadableExternalObjectDirectories(entities, new FileBasedDownloadResponseMetaData());
    }
}