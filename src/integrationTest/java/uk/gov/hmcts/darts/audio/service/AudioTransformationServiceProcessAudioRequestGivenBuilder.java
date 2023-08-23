package uk.gov.hmcts.darts.audio.service;

import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.model.AudioRequestType;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.testutils.data.ExternalObjectDirectoryTestData;
import uk.gov.hmcts.darts.testutils.stubs.DartsDatabaseStub;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;
import static uk.gov.hmcts.darts.audio.enums.AudioRequestStatus.OPEN;
import static uk.gov.hmcts.darts.common.entity.ObjectDirectoryStatusEnum.STORED;

@Transactional
@Service
@Getter
@RequiredArgsConstructor
@Setter
@Scope(scopeName = SCOPE_PROTOTYPE)
@SuppressWarnings("MethodName")
public class AudioTransformationServiceProcessAudioRequestGivenBuilder {

    private static final OffsetDateTime TIME_12_00 = OffsetDateTime.parse("2023-01-01T12:00Z");
    private static final OffsetDateTime TIME_12_10 = OffsetDateTime.parse("2023-01-01T12:10Z");
    private static final OffsetDateTime TIME_13_00 = OffsetDateTime.parse("2023-01-01T13:00Z");


    private final DartsDatabaseStub dartsDatabase;

    private MediaRequestEntity mediaRequestEntity;
    private HearingEntity hearingEntity;
    private UserAccountEntity userAccountEntity;

    public void aMediaEntityGraph() {
        var mediaEntity = dartsDatabase.createMediaEntity(
            TIME_12_00,
            TIME_12_10,
            1
        );

        hearingEntity.addMedia(mediaEntity);
        dartsDatabase.getHearingRepository().saveAndFlush(hearingEntity);

        var systemUser = dartsDatabase.createSystemUserAccountEntity();
        var testUser = dartsDatabase.createIntegrationTestUserAccountEntity(systemUser);
        var externalLocationTypeEntity = dartsDatabase.getExternalLocationTypeEntity(
            ExternalLocationTypeEnum.UNSTRUCTURED);
        var objectDirectoryStatusEntity = dartsDatabase.getObjectDirectoryStatusEntity(STORED);

        var externalObjectDirectoryEntity = ExternalObjectDirectoryTestData.createExternalObjectDirectory(
            testUser,
            mediaEntity,
            objectDirectoryStatusEntity,
            externalLocationTypeEntity,
            UUID.randomUUID()
        );
        dartsDatabase.getExternalObjectDirectoryRepository()
            .saveAndFlush(externalObjectDirectoryEntity);
    }

    public UserAccountEntity aUserAccount(UserAccountEntity systemUser, String emailAddress) {

        userAccountEntity = dartsDatabase.createIntegrationTestUserAccountEntity(systemUser);
        userAccountEntity.setEmailAddress(emailAddress);

        dartsDatabase.getUserAccountRepository()
            .saveAndFlush(userAccountEntity);

        return userAccountEntity;
    }

    public HearingEntity aHearingWith(String caseNumber, String courthouseName, String courtroomName) {
        hearingEntity = dartsDatabase.givenTheDatabaseContainsCourtCaseWithHearingAndCourthouseWithRoom(
            caseNumber,
            courthouseName,
            courtroomName,
            LocalDate.now()
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
        mediaRequestEntity.setStartTime(TIME_12_00);
        mediaRequestEntity.setEndTime(TIME_13_00);
        mediaRequestEntity.setCreatedBy(userAccountEntity);
        mediaRequestEntity.setModifiedBy(userAccountEntity);

        dartsDatabase.getMediaRequestRepository()
            .saveAndFlush(mediaRequestEntity);
    }

}
