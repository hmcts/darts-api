package uk.gov.hmcts.darts.test.common.data.builder;

import lombok.RequiredArgsConstructor;
import org.apache.commons.beanutils.BeanUtils;
import org.hibernate.AssertionFailure;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.MediaLinkedCaseEntity;
import uk.gov.hmcts.darts.common.entity.ObjectAdminActionEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.retention.enums.RetentionConfidenceScoreEnum;

import java.lang.reflect.InvocationTargetException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"PMD.TestClassWithoutTestCases", "PMD.ConstructorCallsOverridableMethod"})
@RequiredArgsConstructor
public class TestMediaEntity extends MediaEntity implements DbInsertable<MediaEntity> {

    @lombok.Builder
    public TestMediaEntity(Integer id, CourtroomEntity courtroom,
                           String legacyObjectId, Integer channel, Integer totalChannels, OffsetDateTime start, OffsetDateTime end,
                           List<MediaLinkedCaseEntity> mediaLinkedCaseList, String legacyVersionLabel,
                           String mediaFile, String mediaFormat, Long fileSize, String checksum,
                           Character mediaType, String contentObjectId, String clipId,
                           String chronicleId, String antecedentId, boolean isHidden,
                           boolean isDeleted, Boolean isCurrent, UserAccountEntity deletedBy,
                           OffsetDateTime deletedTimestamp, String mediaStatus,
                           List<HearingEntity> hearingList, OffsetDateTime retainUntilTs,
                           List<ObjectAdminActionEntity> objectAdminActions, RetentionConfidenceScoreEnum retConfScore,
                           String retConfReason, OffsetDateTime createdDateTime,
                           UserAccountEntity createdBy, OffsetDateTime lastModifiedDateTime,
                           UserAccountEntity lastModifiedBy) {
        setId(id);
        setCourtroom(courtroom);
        setLegacyObjectId(legacyObjectId);
        setChannel(channel);
        setTotalChannels(totalChannels);
        setStart(start);
        setEnd(end);
        setMediaLinkedCaseList(mediaLinkedCaseList);
        setLegacyVersionLabel(legacyVersionLabel);
        setMediaFile(mediaFile);
        setMediaFormat(mediaFormat);
        setFileSize(fileSize);
        setChecksum(checksum);
        setMediaType(mediaType);
        setContentObjectId(contentObjectId);
        setClipId(clipId);
        setChronicleId(chronicleId);
        setAntecedentId(antecedentId);
        setHidden(isHidden);
        setDeleted(isDeleted);
        setIsCurrent(isCurrent);
        setDeletedBy(deletedBy);
        setDeletedTimestamp(deletedTimestamp);
        setMediaStatus(mediaStatus);
        setHearingList(hearingList != null ? hearingList : new ArrayList<>());
        setRetainUntilTs(retainUntilTs);
        setObjectAdminActions(objectAdminActions != null ? objectAdminActions : new ArrayList<>());
        setRetConfScore(retConfScore);
        setRetConfReason(retConfReason);
        setCreatedDateTime(createdDateTime);
        setCreatedBy(createdBy);
        setLastModifiedDateTime(lastModifiedDateTime);
        setLastModifiedBy(lastModifiedBy);
    }

    @Override
    public MediaEntity getEntity() {
        try {
            MediaEntity mediaRequestEntity = new MediaEntity();
            BeanUtils.copyProperties(mediaRequestEntity, this);
            return mediaRequestEntity;
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new AssertionFailure("Assumed that there would be no error on mapping data", e);
        }
    }

    public static class TestMediaBuilderRetrieve implements BuilderHolder<TestMediaEntity, TestMediaEntityBuilder> {
        public TestMediaBuilderRetrieve() {
        }

        private TestMediaEntity.TestMediaEntityBuilder builder = TestMediaEntity.builder();

        @Override
        public TestMediaEntity build() {
            return builder.build();
        }

        @Override
        public TestMediaEntity.TestMediaEntityBuilder getBuilder() {
            return builder;
        }
    }
}