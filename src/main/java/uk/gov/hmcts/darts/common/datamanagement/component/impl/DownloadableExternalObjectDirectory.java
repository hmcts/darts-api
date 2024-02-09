package uk.gov.hmcts.darts.common.datamanagement.component.impl;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;

@Getter
@RequiredArgsConstructor
@Builder
public class DownloadableExternalObjectDirectory {
    private final ExternalObjectDirectoryEntity directory;
    private final ResponseMetaData response;
}