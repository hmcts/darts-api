package uk.gov.hmcts.darts.common.datamanagement.component.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;

@Getter
@RequiredArgsConstructor
public class DownloadableExternalObjectDirectory {
    private final ExternalObjectDirectoryEntity directory;
    private final ResponseMetaData response;
}