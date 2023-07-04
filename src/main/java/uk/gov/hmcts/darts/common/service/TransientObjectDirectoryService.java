package uk.gov.hmcts.darts.common.service;

import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;

import java.util.UUID;

public interface TransientObjectDirectoryService {

    TransientObjectDirectoryEntity saveTransientDataLocation(MediaRequestEntity mediaRequest, UUID externalLocation);

}
