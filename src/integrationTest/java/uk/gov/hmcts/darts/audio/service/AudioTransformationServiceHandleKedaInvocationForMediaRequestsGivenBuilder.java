package uk.gov.hmcts.darts.audio.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audiorequests.model.AudioRequestType;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.enums.ObjectRecordStatusEnum;
import uk.gov.hmcts.darts.testutils.stubs.DartsDatabaseStub;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;
import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.OPEN;

@Transactional
@Service
@Getter
@RequiredArgsConstructor
@Setter
@Scope(scopeName = SCOPE_PROTOTYPE)
@SuppressWarnings("MethodName")
public class AudioTransformationServiceHandleKedaInvocationForMediaRequestsGivenBuilder {

    private static final OffsetDateTime TIME_12_00 = OffsetDateTime.parse("2023-01-01T12:00Z");
    private static final OffsetDateTime TIME_12_01 = OffsetDateTime.parse("2023-01-01T12:01Z");
    private static final OffsetDateTime TIME_12_20 = OffsetDateTime.parse("2023-01-01T12:20Z");
    private static final OffsetDateTime TIME_12_40 = OffsetDateTime.parse("2023-01-01T12:40Z");
    private static final OffsetDateTime TIME_13_00 = OffsetDateTime.parse("2023-01-01T13:00Z");
    private static final OffsetDateTime TIME_13_01 = OffsetDateTime.parse("2023-01-01T13:01Z");
    private static final OffsetDateTime TIME_13_30 = OffsetDateTime.parse("2023-01-01T13:30Z");
    private static final OffsetDateTime TIME_14_00 = OffsetDateTime.parse("2023-01-01T14:00Z");
    private static final OffsetDateTime TIME_20_00 = OffsetDateTime.parse("2023-01-01T20:00Z");
    private static final OffsetDateTime TIME_20_30 = OffsetDateTime.parse("2023-01-01T20:30Z");

    private final DartsDatabaseStub dartsDatabaseStub;

    private MediaRequestEntity mediaRequestEntity;
    private HearingEntity hearingEntity;
    private UserAccountEntity userAccountEntity;

    public void aMediaEntityGraph() {

        var inboundLocation = ExternalLocationTypeEnum.INBOUND;
        var unstructuredLocation = ExternalLocationTypeEnum.UNSTRUCTURED;
        var storedStatus = ObjectRecordStatusEnum.STORED;

        for (int channelNumber = 1; channelNumber <= 4; channelNumber++) {

            var mediaEntity = dartsDatabaseStub.createMediaEntity("testCourthouse", "testCourtroom",
                                                                  TIME_12_01,
                                                                  TIME_12_20,
                                                                  channelNumber
            );
            var mediaEntity2 = dartsDatabaseStub.createMediaEntity("testCourthouse", "testCourtroom",
                                                                   TIME_12_20,
                                                                   TIME_12_40,
                                                                   channelNumber
            );

            var mediaEntity3 = dartsDatabaseStub.createMediaEntity("testCourthouse", "testCourtroom",
                                                                   TIME_12_40,
                                                                   TIME_13_01,
                                                                   channelNumber
            );

            var mediaEntity4 = dartsDatabaseStub.createMediaEntity("testCourthouse", "testCourtroom",
                                                                   TIME_13_30,
                                                                   TIME_14_00,
                                                                   channelNumber
            );
            var mediaEntity5 = dartsDatabaseStub.createMediaEntity("testCourthouse", "testCourtroom",
                                                                   TIME_20_00,
                                                                   TIME_20_30,
                                                                   channelNumber
            );

            hearingEntity.addMedia(mediaEntity);
            hearingEntity.addMedia(mediaEntity2);
            hearingEntity.addMedia(mediaEntity3);
            hearingEntity.addMedia(mediaEntity4);
            hearingEntity.addMedia(mediaEntity5);
            dartsDatabaseStub.getHearingRepository().saveAndFlush(hearingEntity);

            var inboundExternalObjectDirectoryEntity = dartsDatabaseStub.getExternalObjectDirectoryStub()
                .createExternalObjectDirectory(
                    mediaEntity,
                    storedStatus,
                    inboundLocation,
                    UUID.randomUUID().toString()
                );
            var inboundExternalObjectDirectoryEntity2 = dartsDatabaseStub.getExternalObjectDirectoryStub()
                .createExternalObjectDirectory(
                    mediaEntity2,
                    storedStatus,
                    inboundLocation,
                    UUID.randomUUID().toString()
                );
            var inboundExternalObjectDirectoryEntity3 = dartsDatabaseStub.getExternalObjectDirectoryStub()
                .createExternalObjectDirectory(
                    mediaEntity3,
                    storedStatus,
                    inboundLocation,
                    UUID.randomUUID().toString()
                );
            var inboundExternalObjectDirectoryEntity4 = dartsDatabaseStub.getExternalObjectDirectoryStub()
                .createExternalObjectDirectory(
                    mediaEntity4,
                    storedStatus,
                    inboundLocation,
                    UUID.randomUUID().toString()
                );
            var inboundExternalObjectDirectoryEntity5 = dartsDatabaseStub.getExternalObjectDirectoryStub()
                .createExternalObjectDirectory(
                    mediaEntity5,
                    storedStatus,
                    inboundLocation,
                    UUID.randomUUID().toString()
                );
            var unstructuredExternalObjectDirectoryEntity = dartsDatabaseStub.getExternalObjectDirectoryStub()
                .createExternalObjectDirectory(
                    mediaEntity,
                    storedStatus,
                    unstructuredLocation,
                    UUID.randomUUID().toString()
                );
            var unstructuredExternalObjectDirectoryEntity2 = dartsDatabaseStub.getExternalObjectDirectoryStub()
                .createExternalObjectDirectory(
                    mediaEntity2,
                    storedStatus,
                    unstructuredLocation,
                    UUID.randomUUID().toString()
                );
            var unstructuredExternalObjectDirectoryEntity3 = dartsDatabaseStub.getExternalObjectDirectoryStub()
                .createExternalObjectDirectory(
                    mediaEntity3,
                    storedStatus,
                    unstructuredLocation,
                    UUID.randomUUID().toString()
                );

            var unstructuredExternalObjectDirectoryEntity4 = dartsDatabaseStub.getExternalObjectDirectoryStub()
                .createExternalObjectDirectory(
                    mediaEntity4,
                    storedStatus,
                    unstructuredLocation,
                    UUID.randomUUID().toString()
                );

            dartsDatabaseStub.getExternalObjectDirectoryRepository().saveAndFlush(inboundExternalObjectDirectoryEntity);
            dartsDatabaseStub.getExternalObjectDirectoryRepository().saveAndFlush(inboundExternalObjectDirectoryEntity2);
            dartsDatabaseStub.getExternalObjectDirectoryRepository().saveAndFlush(inboundExternalObjectDirectoryEntity3);
            dartsDatabaseStub.getExternalObjectDirectoryRepository().saveAndFlush(inboundExternalObjectDirectoryEntity4);
            dartsDatabaseStub.getExternalObjectDirectoryRepository().saveAndFlush(inboundExternalObjectDirectoryEntity5);
            dartsDatabaseStub.getExternalObjectDirectoryRepository().saveAndFlush(unstructuredExternalObjectDirectoryEntity);
            dartsDatabaseStub.getExternalObjectDirectoryRepository().saveAndFlush(unstructuredExternalObjectDirectoryEntity2);
            dartsDatabaseStub.getExternalObjectDirectoryRepository().saveAndFlush(unstructuredExternalObjectDirectoryEntity3);
            dartsDatabaseStub.getExternalObjectDirectoryRepository().saveAndFlush(unstructuredExternalObjectDirectoryEntity4);

        }
    }

    public UserAccountEntity aUserAccount(String emailAddress) {

        userAccountEntity = dartsDatabaseStub.getUserAccountStub().getIntegrationTestUserAccountEntity();
        userAccountEntity.setEmailAddress(emailAddress);

        dartsDatabaseStub.getUserAccountRepository()
            .saveAndFlush(userAccountEntity);

        return userAccountEntity;
    }

    public HearingEntity aHearingWith(String caseNumber, String courthouseName, String courtroomName, LocalDateTime hearingDate) {
        hearingEntity = dartsDatabaseStub.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            caseNumber,
            courthouseName,
            courtroomName,
            hearingDate
        );

        return hearingEntity;
    }

    public void aMediaRequestEntityForHearingWithRequestType(HearingEntity hearing, AudioRequestType audioRequestType,
                                                             UserAccountEntity userAccountEntity) {
        mediaRequestEntity = new MediaRequestEntity();
        mediaRequestEntity.setHearing(hearing);
        mediaRequestEntity.setStatus(OPEN);
        mediaRequestEntity.setRequestType(audioRequestType);
        mediaRequestEntity.setRequestor(userAccountEntity);
        mediaRequestEntity.setCurrentOwner(userAccountEntity);
        mediaRequestEntity.setStartTime(TIME_12_00);
        mediaRequestEntity.setEndTime(TIME_13_00);
        mediaRequestEntity.setCreatedBy(userAccountEntity);
        mediaRequestEntity.setLastModifiedBy(userAccountEntity);

        dartsDatabaseStub.getMediaRequestRepository()
            .saveAndFlush(mediaRequestEntity);
    }

    public void aMediaRequestEntityForHearingWithRequestType(HearingEntity hearing, AudioRequestType audioRequestType,
                                                             UserAccountEntity userAccountEntity, OffsetDateTime start, OffsetDateTime end) {
        mediaRequestEntity = new MediaRequestEntity();
        mediaRequestEntity.setHearing(hearing);
        mediaRequestEntity.setStatus(OPEN);
        mediaRequestEntity.setRequestType(audioRequestType);
        mediaRequestEntity.setRequestor(userAccountEntity);
        mediaRequestEntity.setCurrentOwner(userAccountEntity);
        mediaRequestEntity.setStartTime(start);
        mediaRequestEntity.setEndTime(end);
        mediaRequestEntity.setCreatedBy(userAccountEntity);
        mediaRequestEntity.setLastModifiedBy(userAccountEntity);

        dartsDatabaseStub.getMediaRequestRepository()
            .saveAndFlush(mediaRequestEntity);
    }
}
