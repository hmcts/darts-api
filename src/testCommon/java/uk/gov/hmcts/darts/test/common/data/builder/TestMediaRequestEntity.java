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

@SuppressWarnings({"PMD.TestClassWithoutTestCases", "PMD.ConstructorCallsOverridableMethod"})
@RequiredArgsConstructor
public class TestMediaRequestEntity extends MediaRequestEntity implements DbInsertable<MediaRequestEntity> {

    @lombok.Builder
    public TestMediaRequestEntity(Integer id, HearingEntity hearing, UserAccountEntity currentOwner, UserAccountEntity requestor,
                                  MediaRequestStatus status, AudioRequestType requestType, Integer attempts, OffsetDateTime startTime,
                                  OffsetDateTime endTime, Integer createdById, Integer lastModifiedById,
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
        setCreatedById(createdById);
        setLastModifiedById(lastModifiedById);
        setCreatedDateTime(createdAt);
        setLastModifiedDateTime(lastModifiedAt);
    }

    @Override
    public MediaRequestEntity getEntity() {
        try {
            MediaRequestEntity mediaRequestEntity = new MediaRequestEntity();
            BeanUtils.copyProperties(mediaRequestEntity, this);
            return mediaRequestEntity;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new AssertionFailure("Assumed that there would be no error on mapping data", e);
        }
    }

    public static class TestMediaBuilderRetrieve implements BuilderHolder<TestMediaRequestEntity, TestMediaRequestEntityBuilder> {

        private final TestMediaRequestEntity.TestMediaRequestEntityBuilder builder = TestMediaRequestEntity.builder();

        @Override
        public TestMediaRequestEntity build() {
            return builder.build();
        }

        @Override
        public TestMediaRequestEntity.TestMediaRequestEntityBuilder getBuilder() {
            return builder;
        }
    }
}