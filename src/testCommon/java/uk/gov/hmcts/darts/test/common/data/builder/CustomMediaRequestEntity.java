package uk.gov.hmcts.darts.test.common.data.builder;

import lombok.RequiredArgsConstructor;
import org.apache.commons.beanutils.BeanUtils;
import org.hibernate.AssertionFailure;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.enums.MediaRequestStatus;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestType;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;

import java.lang.reflect.InvocationTargetException;
import java.time.OffsetDateTime;

@RequiredArgsConstructor
public class CustomMediaRequestEntity extends MediaRequestEntity {

    @lombok.Builder
    public CustomMediaRequestEntity(Integer id, HearingEntity hearing, UserAccountEntity currentOwner, UserAccountEntity requestor,
                                    MediaRequestStatus status, AudioRequestType requestType, Integer attempts, OffsetDateTime startTime,
                                    OffsetDateTime endTime, UserAccountEntity createdBy, UserAccountEntity lastModifiedBy,
                                    OffsetDateTime createdAt, OffsetDateTime lastModifiedAt) {
        setId(id);
        setHearing(hearing);
        setCurrentOwner(currentOwner);
        setRequestor(requestor);
        setStatus(status);
        setRequestType(requestType);
        setAttempts(attempts);
        setStartTime(startTime);
        setEndTime(endTime);
        setCreatedBy(createdBy);
        setLastModifiedBy(lastModifiedBy);
        setCreatedDateTime(createdAt);
        setLastModifiedDateTime(lastModifiedAt);
    }

    public static class CustomMediaBuilderRetrieve implements BuilderHolder<MediaRequestEntity, CustomMediaRequestEntityBuilder> {

        private CustomMediaRequestEntity.CustomMediaRequestEntityBuilder builder = CustomMediaRequestEntity.builder();

        @Override
        public MediaRequestEntity build() {
            try {
                MediaRequestEntity mediaRequestEntity = new MediaRequestEntity();
                BeanUtils.copyProperties(mediaRequestEntity, builder.build());
                return mediaRequestEntity;
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new AssertionFailure("Assumed that there would be no error on mapping data", e);
            }
        }

        @Override
        public CustomMediaRequestEntity.CustomMediaRequestEntityBuilder getBuilder() {
             return builder;
        }
    }
}