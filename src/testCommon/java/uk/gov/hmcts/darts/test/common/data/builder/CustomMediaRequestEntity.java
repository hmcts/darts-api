package uk.gov.hmcts.darts.test.common.data.builder;

import lombok.Builder;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;

import uk.gov.hmcts.darts.audio.enums.MediaRequestStatus;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestType;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.time.OffsetDateTime;

public class CustomMediaRequestEntity extends MediaRequestEntity {

    @Builder
    public CustomMediaRequestEntity(Integer id, HearingEntity hearing, UserAccountEntity currentOwner, UserAccountEntity requestor,
                                    MediaRequestStatus status, AudioRequestType requestType, Integer attempts, OffsetDateTime startTime,
                                    OffsetDateTime endTime, UserAccountEntity createdBy, UserAccountEntity lastModifiedBy,
                                    OffsetDateTime createdAt, OffsetDateTime lastModifiedAt) {
        super();
    }
}