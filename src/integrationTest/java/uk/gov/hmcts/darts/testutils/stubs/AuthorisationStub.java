package uk.gov.hmcts.darts.testutils.stubs;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.common.entity.AnnotationEntity;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.SecurityGroupEntity;
import uk.gov.hmcts.darts.common.entity.TranscriptionEntity;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.SecurityGroupRepository;
import uk.gov.hmcts.darts.test.common.data.CourtroomTestData;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneOffset.UTC;
import static java.time.format.DateTimeFormatter.BASIC_ISO_DATE;
import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.COMPLETED;
import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.OPEN;
import static uk.gov.hmcts.darts.audiorequests.model.AudioRequestType.DOWNLOAD;
import static uk.gov.hmcts.darts.common.entity.MediaEntity.MEDIA_TYPE_DEFAULT;

@Component
@RequiredArgsConstructor
@Getter
public class AuthorisationStub {

    private static final OffsetDateTime YESTERDAY = now(UTC).minusDays(1).withHour(9).withMinute(0)
        .withSecond(0).withNano(0);

    private final DartsDatabaseStub dartsDatabaseStub;
    private final DartsPersistence dartsPersistence;

    private UserAccountEntity systemUser;
    private UserAccountEntity testUser;
    private SecurityGroupEntity bristolStaff;
    private SecurityGroupEntity bristolAppr;
    private CourtroomEntity courtroomEntity;
    private CourthouseEntity courthouseEntity;
    private CourtCaseEntity courtCaseEntity;
    private HearingEntity hearingEntity;
    private MediaRequestEntity mediaRequestEntity;
    private MediaRequestEntity mediaRequestEntitySystemUser;
    private MediaEntity mediaEntity;
    private TransformedMediaEntity transformedMediaEntity;
    private TranscriptionEntity transcriptionEntity;
    private AnnotationEntity annotationEntity;
    private UserAccountEntity separateIntegrationUser;

    @Transactional
    public void givenTestSchema() {
        courtroomEntity = CourtroomTestData.someMinimalCourtRoom();
        courtroomEntity = dartsPersistence.save(courtroomEntity);

        courthouseEntity = dartsDatabaseStub.getCourthouseRepository()
            .findById(courtroomEntity.getCourthouse().getId()).orElseThrow();

        systemUser = dartsDatabaseStub.getUserAccountStub().getSystemUserAccountEntity();
        testUser = dartsDatabaseStub.getUserAccountStub().getIntegrationTestUserAccountEntity();

        SecurityGroupRepository securityGroupRepository = dartsDatabaseStub.getSecurityGroupRepository();
        bristolStaff = securityGroupRepository.findById(-2).orElseThrow();
        bristolStaff.setCourthouseEntities(Set.of(courthouseEntity));
        bristolAppr = securityGroupRepository.findById(-1).orElseThrow();
        bristolAppr.setCourthouseEntities(Set.of(courthouseEntity));

        testUser.getSecurityGroupEntities().addAll(List.of(bristolStaff, bristolAppr));
        testUser = dartsDatabaseStub.getUserAccountRepository().save(testUser);

        createCourtCase();

        createHearing();

        mediaRequestEntity = PersistableFactory.getMediaRequestTestData().someMinimalRequestData();
        mediaRequestEntity.setHearing(hearingEntity);
        mediaRequestEntity.setRequestor(testUser);
        mediaRequestEntity.setCurrentOwner(testUser);
        mediaRequestEntity.setStatus(COMPLETED);
        mediaRequestEntity.setRequestType(DOWNLOAD);
        mediaRequestEntity.setStartTime(YESTERDAY);
        mediaRequestEntity.setEndTime(YESTERDAY.plusHours(1));
        mediaRequestEntity = dartsPersistence.save(mediaRequestEntity);

        transformedMediaEntity = dartsDatabaseStub.getTransformedMediaStub().createTransformedMediaEntity(mediaRequestEntity);

        mediaRequestEntitySystemUser = PersistableFactory.getMediaRequestTestData().someMinimalRequestData();
        mediaRequestEntitySystemUser.setHearing(hearingEntity);
        mediaRequestEntitySystemUser.setRequestor(systemUser);
        mediaRequestEntitySystemUser.setCurrentOwner(systemUser);
        mediaRequestEntitySystemUser.setStatus(OPEN);
        mediaRequestEntitySystemUser.setRequestType(DOWNLOAD);
        mediaRequestEntitySystemUser.setStartTime(YESTERDAY);
        mediaRequestEntitySystemUser.setEndTime(YESTERDAY.plusHours(1));
        mediaRequestEntitySystemUser = dartsPersistence.save(mediaRequestEntitySystemUser);

        mediaEntity = PersistableFactory.getMediaTestData().someMinimalMedia();
        mediaEntity.setChannel(1);
        mediaEntity.setTotalChannels(2);
        mediaEntity.setCourtroom(courtroomEntity);
        mediaEntity.setStart(now());
        mediaEntity.setEnd(now().plusMinutes(30));
        mediaEntity.setMediaFile("media file");
        mediaEntity.setFileSize(1000L);
        mediaEntity.setMediaFormat("mp3");
        mediaEntity.setChecksum("checksum");
        mediaEntity.setMediaType(MEDIA_TYPE_DEFAULT);
        mediaEntity = dartsPersistence.save(mediaEntity);

        hearingEntity.addMedia(mediaEntity);
        hearingEntity = dartsPersistence.save(hearingEntity);

        separateIntegrationUser = dartsDatabaseStub.getUserAccountStub().getSeparateIntegrationTestUserAccountEntity();
        separateIntegrationUser.getSecurityGroupEntities().addAll(List.of(bristolStaff, bristolAppr));
        dartsDatabaseStub.getUserAccountRepository().save(separateIntegrationUser);

        transcriptionEntity = dartsDatabaseStub.getTranscriptionStub()
            .createAndSaveAwaitingAuthorisationTranscription(testUser, null, hearingEntity, YESTERDAY);
    }

    public TranscriptionEntity addNewTranscription() {
        return dartsDatabaseStub.getTranscriptionStub()
            .createAndSaveAwaitingAuthorisationTranscription(separateIntegrationUser, null, hearingEntity, YESTERDAY);
    }

    private void createHearing() {
        hearingEntity = PersistableFactory.getHearingTestData().someMinimalHearing();
        hearingEntity.setCourtCase(courtCaseEntity);
        hearingEntity.setCourtroom(courtroomEntity);
        hearingEntity.setHearingDate(YESTERDAY.toLocalDate());
        hearingEntity.setHearingIsActual(true);
        hearingEntity.setScheduledStartTime(LocalTime.now());
        hearingEntity = dartsPersistence.save(hearingEntity);
    }

    private void createCourtCase() {
        courtCaseEntity = PersistableFactory.getCourtCaseTestData().createSomeMinimalCase();
        courtCaseEntity.setCaseNumber(String.format("T%s", YESTERDAY.format(BASIC_ISO_DATE)));
        courtCaseEntity.setCourthouse(courthouseEntity);
        courtCaseEntity.setClosed(false);
        courtCaseEntity.setInterpreterUsed(false);
        courtCaseEntity = dartsPersistence.save(courtCaseEntity);
    }

}