package uk.gov.hmcts.darts.audio.service;

import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;

import java.util.List;

public interface OutboundDataStoreSoftDeleter {

    List<MediaRequestEntity> markForDeletion();
}
